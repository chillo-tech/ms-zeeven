package com.cs.ge.services;

import com.cs.ge.entites.Stock;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
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

    public List<Stock> generateDefaultStocks() {
        List<Stock> stocks = List.of(SMS, WHATSAPP, EMAIL).parallelStream().map(entry -> new Stock(
                null,
                "default",
                "",
                entry,
                CREDIT,
                3,
                LocalDateTime.now()
        )).collect(Collectors.toList());

        return this.stockRepository.saveAll(stocks);
    }

    public Map<Channel, Integer> statistics() {
        UserAccount account = this.profileService.getAuthenticateUser();
        Map<Channel, Integer> stats = new HashMap<>();
        List.of(SMS, WHATSAPP, EMAIL).parallelStream().forEach(channel -> {
            int computed = account.getStocks()
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
