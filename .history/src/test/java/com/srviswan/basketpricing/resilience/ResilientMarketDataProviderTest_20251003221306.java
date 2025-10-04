package com.srviswan.basketpricing.resilience;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientMarketDataProviderTest {

    @Mock
    private MarketDataProvider delegate;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private Retry retry;

    private ResilientMarketDataProvider resilientProvider;

    @BeforeEach
    void setUp() {
        resilientProvider = new ResilientMarketDataProvider(
                delegate, circuitBreakerRegistry, rateLimiterRegistry, retryRegistry);

        when(circuitBreakerRegistry.circuitBreaker("marketDataProvider")).thenReturn(circuitBreaker);
        when(rateLimiterRegistry.rateLimiter("marketDataProvider")).thenReturn(rateLimiter);
        when(retryRegistry.retry("marketDataProvider")).thenReturn(retry);
    }

    @Test
    void getLatestPrices_ShouldReturnPrices_WhenDelegateSucceeds() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");
        Map<String, PriceSnapshot> expectedPrices = createMockPrices();

        when(delegate.getLatestPrices(symbols)).thenReturn(expectedPrices);
        when(circuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(0)).get();
        });
        when(retry.executeSupplier(any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(0)).get();
        });

        // When
        Map<String, PriceSnapshot> result = resilientProvider.getLatestPrices(symbols);

        // Then
        assertThat(result).isEqualTo(expectedPrices);
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeSupplier(any());
        verify(retry).executeSupplier(any());
        verify(delegate).getLatestPrices(symbols);
    }

    @Test
    void getLatestPrices_ShouldReturnEmptyMap_WhenDelegateThrowsException() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");
        
        when(circuitBreaker.executeSupplier(any())).thenThrow(new RuntimeException("Circuit breaker error"));

        // When
        Map<String, PriceSnapshot> result = resilientProvider.getLatestPrices(symbols);

        // Then
        assertThat(result).isEmpty();
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeSupplier(any());
    }

    @Test
    void subscribe_ShouldDelegateToProvider_WhenSuccessful() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");

        when(circuitBreaker.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        when(retry.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });

        // When
        resilientProvider.subscribe(symbols);

        // Then
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeRunnable(any());
        verify(retry).executeRunnable(any());
        verify(delegate).subscribe(symbols);
    }

    @Test
    void subscribe_ShouldHandleException_WhenDelegateThrows() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");
        
        doThrow(new RuntimeException("Subscription error")).when(delegate).subscribe(symbols);
        when(circuitBreaker.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        when(retry.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });

        // When
        resilientProvider.subscribe(symbols);

        // Then
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeRunnable(any());
        verify(retry).executeRunnable(any());
        verify(delegate).subscribe(symbols);
        // Should not throw exception
    }

    @Test
    void unsubscribe_ShouldDelegateToProvider_WhenSuccessful() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");

        when(circuitBreaker.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });
        when(retry.executeRunnable(any())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        });

        // When
        resilientProvider.unsubscribe(symbols);

        // Then
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeRunnable(any());
        verify(retry).executeRunnable(any());
        verify(delegate).unsubscribe(symbols);
    }

    @Test
    void getSubscribedSymbols_ShouldReturnSymbols_WhenDelegateSucceeds() {
        // Given
        Set<String> expectedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O"));

        when(delegate.getSubscribedSymbols()).thenReturn(expectedSymbols);
        when(circuitBreaker.executeSupplier(any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(0)).get();
        });
        when(retry.executeSupplier(any())).thenAnswer(invocation -> {
            return ((java.util.function.Supplier<?>) invocation.getArgument(0)).get();
        });

        // When
        Set<String> result = resilientProvider.getSubscribedSymbols();

        // Then
        assertThat(result).isEqualTo(expectedSymbols);
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeSupplier(any());
        verify(retry).executeSupplier(any());
        verify(delegate).getSubscribedSymbols();
    }

    @Test
    void getSubscribedSymbols_ShouldReturnEmptySet_WhenDelegateThrowsException() {
        // Given
        when(circuitBreaker.executeSupplier(any())).thenThrow(new RuntimeException("Circuit breaker error"));

        // When
        Set<String> result = resilientProvider.getSubscribedSymbols();

        // Then
        assertThat(result).isEmpty();
        verify(rateLimiter).acquirePermission();
        verify(circuitBreaker).executeSupplier(any());
    }

    @Test
    void initializeResilienceComponents_ShouldBeCalledLazily() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");

        // When
        resilientProvider.getLatestPrices(symbols);

        // Then
        verify(circuitBreakerRegistry).circuitBreaker("marketDataProvider");
        verify(rateLimiterRegistry).rateLimiter("marketDataProvider");
        verify(retryRegistry).retry("marketDataProvider");
    }

    @Test
    void initializeResilienceComponents_ShouldBeCalledOnlyOnce() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");

        // When
        resilientProvider.getLatestPrices(symbols);
        resilientProvider.getLatestPrices(symbols);

        // Then
        verify(circuitBreakerRegistry, times(1)).circuitBreaker("marketDataProvider");
        verify(rateLimiterRegistry, times(1)).rateLimiter("marketDataProvider");
        verify(retryRegistry, times(1)).retry("marketDataProvider");
    }

    private Map<String, PriceSnapshot> createMockPrices() {
        Map<String, PriceSnapshot> prices = new HashMap<>();
        prices.put("IBM.N", PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(Instant.now())
                .build());
        prices.put("MSFT.O", PriceSnapshot.builder()
                .symbol("MSFT.O")
                .bid(300.0)
                .ask(300.5)
                .last(300.25)
                .timestamp(Instant.now())
                .build());
        return prices;
    }
}
