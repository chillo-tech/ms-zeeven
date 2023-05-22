package com.cs.ge.services;

import com.cs.ge.entites.Stock;
import com.cs.ge.repositories.StockRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.EMAIL;
import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.enums.StockType.CREDIT;

@AllArgsConstructor
@Service
public class StockService {
    private StockRepository stockRepository;

    public List<Stock> generateDefaultStocks() {
        return List.of(SMS, WHATSAPP, EMAIL).parallelStream().map(entry -> new Stock(
                null,
                "default",
                "",
                entry,
                CREDIT,
                3,
                LocalDateTime.now()
        )).collect(Collectors.toList());
    }
}
