package com.srviswan.basketpricing.api;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PricingController {

    private final MarketDataProvider marketDataProvider;

    @GetMapping
    public ResponseEntity<Map<String, PriceSnapshot>> getPrices(@RequestParam("symbols") String symbolsCsv) {
        List<String> symbols = parseCsv(symbolsCsv);
        marketDataProvider.subscribe(symbols);
        Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
        return ResponseEntity.ok(prices);
    }

    private List<String> parseCsv(String csv) {
        List<String> list = new ArrayList<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }
}


