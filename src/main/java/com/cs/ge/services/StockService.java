package com.cs.ge.services;

import com.cs.ge.dto.prices.BackofficePrice;
import com.cs.ge.dto.prices.Data;
import com.cs.ge.dto.prices.Pricing;
import com.cs.ge.entites.ApplicationPayment;
import com.cs.ge.entites.Stock;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.StockType;
import com.cs.ge.feign.FeignBackoffice;
import com.cs.ge.repositories.StockRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.EMAIL;
import static com.cs.ge.enums.Channel.QRCODE;
import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.enums.StockType.CREDIT;
import static com.cs.ge.enums.StockType.DEBIT;
import static com.cs.ge.utils.Data.DEFAULT_STOCK_SIZE;

@AllArgsConstructor
@Service
public class StockService {
    private StockRepository stockRepository;
    private ProfileService profileService;
    private FeignBackoffice feignBackoffice;


    public List<Stock> generateDefaultStocks(final String user) {
        final List<Stock> stocks = List.of(SMS, WHATSAPP, EMAIL, QRCODE).parallelStream().map(entry -> new Stock(
                null,
                user,
                "default",
                "",
                entry,
                CREDIT,
                DEFAULT_STOCK_SIZE,
                LocalDateTime.now()
        )).collect(Collectors.toList());

        return this.stockRepository.saveAll(stocks);
    }

    public Stock update(final String user, final ApplicationPayment payment, final String description, final Integer size, final Channel channel, final StockType type) {
        String label = "default";
        Channel finalChannel = channel;
        int finalSize = size;
        if (payment != null) {
            label = payment.getProductName();
        }

        if (payment != null && Objects.equals(CREDIT, type)) {
            finalChannel = payment.getChannel();
            final BackofficePrice response = this.feignBackoffice.page(payment.getProductId(), "id,prices.pricing_id.id,prices.pricing_id.credits");
            final Data data = response.data();
            final List<Pricing> prices = data.prices();
            final Optional<Pricing> pricingOptional = prices.parallelStream().filter(price -> Objects.equals(price.pricing_id().id(), Integer.parseInt(payment.getOptionId()))).findFirst();
            if (pricingOptional.isPresent()) {
                finalSize = pricingOptional.get().pricing_id().credits();
            }
        }
        return this.stockRepository.save(new Stock(
                null,
                user,
                label,
                description,
                finalChannel,
                type,
                finalSize,
                LocalDateTime.now()));


    }

    public Map<Channel, Integer> authenticatedUserStatistics() {
        final UserAccount account = this.profileService.getAuthenticateUser();
        return this.getChannelsStatistics(account.getId(), List.of(SMS, WHATSAPP, EMAIL, QRCODE));
    }

    public Map<Channel, Integer> getChannelsStatistics(final String user, final List<Channel> channelList) {
        final Map<Channel, Integer> stats = new HashMap<>();
        final List<Stock> stocks = this.stockRepository.findByUser(user);
        channelList.parallelStream().forEach(channel -> {
            final int computed = stocks
                    .parallelStream()
                    .filter(stock -> stock.getChannel().equals(channel))
                    .reduce(0, (a, b) -> {
                        final int value;
                        if (b.getType().equals(DEBIT)) {
                            value = -b.getSize();
                        } else {
                            value = b.getSize();
                        }
                        return a + value;
                    }, Integer::sum);
            stats.put(channel, computed);
        });
        return stats;
    }
}
