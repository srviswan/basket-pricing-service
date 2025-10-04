package com.srviswan.basketpricing.api;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.resilience.BackpressureManager;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PricingController {

    private final MarketDataProvider marketDataProvider;
    private final PricingMetrics pricingMetrics;
    private final BackpressureManager backpressureManager;

    @GetMapping
    public ResponseEntity<Map<String, PriceSnapshot>> getPrices(@RequestParam("symbols") String symbolsCsv) {
        Timer.Sample sample = pricingMetrics.startApiTimer();
        try {
            pricingMetrics.recordApiRequest("getPrices");
            List<String> symbols = parseCsv(symbolsCsv);
            Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
            return ResponseEntity.ok(prices);
        } finally {
            pricingMetrics.recordApiResponseTime(sample);
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestParam("symbols") String symbolsCsv) {
        Timer.Sample sample = pricingMetrics.startApiTimer();
        try {
            pricingMetrics.recordApiRequest("subscribe");
            List<String> symbols = parseCsv(symbolsCsv);
            marketDataProvider.subscribe(symbols);
            pricingMetrics.recordSubscriptionRequest();
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscribed", symbols);
            response.put("totalSubscriptions", marketDataProvider.getSubscribedSymbols().size());
            
            return ResponseEntity.ok(response);
        } finally {
            pricingMetrics.recordApiResponseTime(sample);
        }
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestParam("symbols") String symbolsCsv) {
        Timer.Sample sample = pricingMetrics.startApiTimer();
        try {
            pricingMetrics.recordApiRequest("unsubscribe");
            List<String> symbols = parseCsv(symbolsCsv);
            marketDataProvider.unsubscribe(symbols);
            pricingMetrics.recordUnsubscriptionRequest();
            
            Map<String, Object> response = new HashMap<>();
            response.put("unsubscribed", symbols);
            response.put("remainingSubscriptions", marketDataProvider.getSubscribedSymbols().size());
            
            return ResponseEntity.ok(response);
        } finally {
            pricingMetrics.recordApiResponseTime(sample);
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<Map<String, Object>> getSubscriptions() {
        Timer.Sample sample = pricingMetrics.startApiTimer();
        try {
            pricingMetrics.recordApiRequest("getSubscriptions");
            Set<String> subscribedSymbols = marketDataProvider.getSubscribedSymbols();
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscribedSymbols", subscribedSymbols);
            response.put("count", subscribedSymbols.size());
            
            return ResponseEntity.ok(response);
        } finally {
            pricingMetrics.recordApiResponseTime(sample);
        }
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


