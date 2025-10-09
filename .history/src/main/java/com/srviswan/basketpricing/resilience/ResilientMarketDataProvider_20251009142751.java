package com.srviswan.basketpricing.resilience;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Primary
public class ResilientMarketDataProvider implements MarketDataProvider {

    private final MarketDataProvider delegate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    private CircuitBreaker circuitBreaker;
    private RateLimiter rateLimiter;
    private Retry retry;

    public ResilientMarketDataProvider(
            @Qualifier("refinitivEmaProvider") MarketDataProvider delegate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry) {
        this.delegate = delegate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public Map<String, PriceSnapshot> getLatestPrices(Collection<String> symbols) {
        log.debug("ResilientMarketDataProvider.getLatestPrices() called for symbols: {}", symbols);
        
        if (circuitBreaker == null) {
            initializeResilienceComponents();
        }

        try {
            // Rate limiting
            rateLimiter.acquirePermission();
            log.debug("Rate limiter permission acquired");
            
            // Circuit breaker protection
            return circuitBreaker.executeSupplier(() -> {
                log.debug("Circuit breaker executing supplier");
                // Retry with exponential backoff
                return retry.executeSupplier(() -> {
                    log.debug("Retry executing supplier, delegating to: {}", delegate.getClass().getName());
                    return delegate.getLatestPrices(symbols);
                });
            });
        } catch (Exception throwable) {
            log.warn("Failed to get prices for symbols: {}, error: {}", symbols, throwable.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @Cacheable(value = "subscriptions", key = "#symbols.hashCode()")
    public void subscribe(Collection<String> symbols) {
        if (circuitBreaker == null) {
            initializeResilienceComponents();
        }

        try {
            rateLimiter.acquirePermission();
            circuitBreaker.executeRunnable(() -> {
                retry.executeRunnable(() -> {
                    delegate.subscribe(symbols);
                });
            });
        } catch (Exception throwable) {
            log.warn("Failed to subscribe to symbols: {}, error: {}", symbols, throwable.getMessage());
        }
    }

    @Override
    public void unsubscribe(Collection<String> symbols) {
        if (circuitBreaker == null) {
            initializeResilienceComponents();
        }

        try {
            rateLimiter.acquirePermission();
            circuitBreaker.executeRunnable(() -> {
                retry.executeRunnable(() -> {
                    delegate.unsubscribe(symbols);
                });
            });
        } catch (Exception throwable) {
            log.warn("Failed to unsubscribe from symbols: {}, error: {}", symbols, throwable.getMessage());
        }
    }

    @Override
    public Set<String> getSubscribedSymbols() {
        if (circuitBreaker == null) {
            initializeResilienceComponents();
        }

        try {
            rateLimiter.acquirePermission();
            return circuitBreaker.executeSupplier(() -> {
                return retry.executeSupplier(() -> {
                    return delegate.getSubscribedSymbols();
                });
            });
        } catch (Exception throwable) {
            log.warn("Failed to get subscribed symbols, error: {}", throwable.getMessage());
            return Collections.emptySet();
        }
    }

    private void initializeResilienceComponents() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("marketDataProvider");
        rateLimiter = rateLimiterRegistry.rateLimiter("marketDataProvider");
        retry = retryRegistry.retry("marketDataProvider");
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.info("Circuit breaker state transition: {}", event.getStateTransition()));
        
        rateLimiter.getEventPublisher()
                .onFailure(event -> 
                    log.warn("Rate limiter failure: {}", event.getEventType()));
        
        retry.getEventPublisher()
                .onRetry(event -> 
                    log.debug("Retry attempt: {}", event.getNumberOfRetryAttempts()));
    }
}
