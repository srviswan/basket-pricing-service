package com.srviswan.basketpricing.monitoring;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PricingHealthIndicator implements HealthIndicator {

    private final MarketDataProvider marketDataProvider;
    private final PricingMetrics pricingMetrics;

    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();
            
            // Check if we have active subscriptions
            Set<String> subscribedSymbols = marketDataProvider.getSubscribedSymbols();
            builder.withDetail("activeSubscriptions", subscribedSymbols.size());
            builder.withDetail("subscribedSymbols", subscribedSymbols);
            
            // Check if we can get prices for subscribed symbols
            if (!subscribedSymbols.isEmpty()) {
                Map<String, com.srviswan.basketpricing.marketdata.PriceSnapshot> prices = 
                    marketDataProvider.getLatestPrices(subscribedSymbols);
                
                builder.withDetail("priceDataAvailable", prices.size());
                builder.withDetail("priceDataCoverage", 
                    String.format("%.1f%%", (prices.size() * 100.0) / subscribedSymbols.size()));
                
                // Check for stale data (older than 5 minutes)
                long staleCount = prices.values().stream()
                    .mapToLong(snapshot -> {
                        long ageSeconds = Instant.now().getEpochSecond() - snapshot.getTimestamp().getEpochSecond();
                        return ageSeconds > 300 ? 1 : 0; // 5 minutes
                    })
                    .sum();
                    
                if (staleCount > 0) {
                    builder.withDetail("staleDataCount", staleCount);
                    if (staleCount > subscribedSymbols.size() / 2) {
                        builder.down().withDetail("reason", "More than 50% of price data is stale (>5 minutes)");
                    } else {
                        builder.status("DEGRADED").withDetail("reason", "Some price data is stale (>5 minutes)");
                    }
                }
            } else {
                builder.withDetail("status", "No active subscriptions");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now().toString())
                .build();
        }
    }
}
