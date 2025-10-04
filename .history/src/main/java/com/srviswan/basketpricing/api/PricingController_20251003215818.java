package com.srviswan.basketpricing.api;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PricingController {

    private final MarketDataProvider marketDataProvider;

    @GetMapping
    public ResponseEntity<Map<String, PriceSnapshot>> getPrices(@RequestParam("symbols") String symbolsCsv) {
        List<String> symbols = parseCsv(symbolsCsv);
        Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
        return ResponseEntity.ok(prices);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestParam("symbols") String symbolsCsv) {
        List<String> symbols = parseCsv(symbolsCsv);
        marketDataProvider.subscribe(symbols);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscribed", symbols);
        response.put("totalSubscriptions", marketDataProvider.getSubscribedSymbols().size());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestParam("symbols") String symbolsCsv) {
        List<String> symbols = parseCsv(symbolsCsv);
        marketDataProvider.unsubscribe(symbols);
        
        Map<String, Object> response = new HashMap<>();
        response.put("unsubscribed", symbols);
        response.put("remainingSubscriptions", marketDataProvider.getSubscribedSymbols().size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> getSubscriptions() {
        Set<String> subscribedSymbols = marketDataProvider.getSubscribedSymbols();
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscribedSymbols", subscribedSymbols);
        response.put("count", subscribedSymbols.size());
        
        return ResponseEntity.ok(response);
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


