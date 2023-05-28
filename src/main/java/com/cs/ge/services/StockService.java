package com.cs.ge.services;

import com.cs.ge.entites.Stock;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.StockType;
import com.cs.ge.repositories.StockRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.EMAIL;
import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.enums.StockType.CREDIT;
import static com.cs.ge.enums.StockType.DEBIT;

@AllArgsConstructor
@Service
public class StockService {
    private StockRepository stockRepository;
    private ProfileService profileService;

    public List<Stock> generateDefaultStocks(String user) {
        List<Stock> stocks = List.of(SMS, WHATSAPP, EMAIL).parallelStream().map(entry -> new Stock(
                null,
                user,
                "default",
                "",
                entry,
                CREDIT,
                3,
                LocalDateTime.now()
        )).collect(Collectors.toList());

        return this.stockRepository.saveAll(stocks);
    }

    public Stock update(String user, Channel channel, Integer size, StockType type) {
        return this.stockRepository.save(new Stock(
                null,
                user,
                "default",
                "",
                channel,
                type,
                size,
                LocalDateTime.now()));
    }

    public Map<Channel, Integer> authenticatedUserStatistics() {
        UserAccount account = this.profileService.getAuthenticateUser();
        return getChannelsStatistics(account.getId(), List.of(SMS, WHATSAPP, EMAIL));
    }

    public Map<Channel, Integer> getChannelsStatistics(String user, List<Channel> channelList) {
        Map<Channel, Integer> stats = new HashMap<>();
        List<Stock> stocks = this.stockRepository.findByUser(user);
        channelList.parallelStream().forEach(channel -> {
            int computed = stocks
                    .parallelStream()
                    .filter(stock -> stock.getChannel().equals(channel))
                    .reduce(0, (a, b) -> {
                        int value;
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
