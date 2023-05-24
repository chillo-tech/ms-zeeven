package com.cs.ge.controllers;

import com.cs.ge.enums.Channel;
import com.cs.ge.services.StockService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@AllArgsConstructor
@RestController
@RequestMapping(path = "stocks", produces = APPLICATION_JSON_VALUE)
public class StocksController {
    private final StockService stockService;

    @GetMapping(path = "statistics")
    public Map<Channel, Integer> statistics() {
        return this.stockService.statistics();
    }
}
