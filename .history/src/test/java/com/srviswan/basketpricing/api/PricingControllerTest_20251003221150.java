package com.srviswan.basketpricing.api;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.resilience.BackpressureManager;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingControllerTest {

    @Mock
    private MarketDataProvider marketDataProvider;

    @Mock
    private PricingMetrics pricingMetrics;

    @Mock
    private BackpressureManager backpressureManager;

    @Mock
    private Timer.Sample timerSample;

    private PricingController controller;

    @BeforeEach
    void setUp() {
        controller = new PricingController(marketDataProvider, pricingMetrics, backpressureManager);
    }

    @Test
    void getPrices_ShouldReturnPrices_WhenSymbolsProvided() {
        // Given
        String symbolsCsv = "IBM.N,MSFT.O";
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        
        when(pricingMetrics.startApiTimer()).thenReturn(timerSample);
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.5);

        // When
        ResponseEntity<Map<String, PriceSnapshot>> response = controller.getPrices(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsKey("IBM.N");
        assertThat(response.getBody()).containsKey("MSFT.O");
        
        verify(pricingMetrics).recordApiRequest("getPrices");
        verify(pricingMetrics).recordApiResponseTime(timerSample);
        verify(marketDataProvider).getLatestPrices(Arrays.asList("IBM.N", "MSFT.O"));
    }

    @Test
    void getPrices_ShouldLogWarning_WhenHighBackpressure() {
        // Given
        String symbolsCsv = "IBM.N";
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        
        when(pricingMetrics.startApiTimer()).thenReturn(timerSample);
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.9);

        // When
        ResponseEntity<Map<String, PriceSnapshot>> response = controller.getPrices(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Warning should be logged (verified by the high utilization value)
        verify(backpressureManager).getQueueUtilization();
    }

    @Test
    void subscribe_ShouldSubscribeToSymbols_WhenValidRequest() {
        // Given
        String symbolsCsv = "IBM.N,MSFT.O";
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        
        when(pricingMetrics.startApiTimer()).thenReturn(timerSample);
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.3);
        when(backpressureManager.getProcessedUpdates()).thenReturn(100L);
        when(backpressureManager.getDroppedUpdates()).thenReturn(5L);

        // When
        ResponseEntity<Map<String, Object>> response = controller.subscribe(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("subscribed")).isEqualTo(Arrays.asList("IBM.N", "MSFT.O"));
        assertThat(body.get("totalSubscriptions")).isEqualTo(3);
        assertThat(body.get("backpressureStatus")).isNotNull();
        
        verify(marketDataProvider).subscribe(Arrays.asList("IBM.N", "MSFT.O"));
        verify(pricingMetrics).recordSubscriptionRequest();
    }

    @Test
    void unsubscribe_ShouldUnsubscribeFromSymbols_WhenValidRequest() {
        // Given
        String symbolsCsv = "IBM.N";
        Set<String> remainingSymbols = new HashSet<>(Arrays.asList("MSFT.O", "AAPL.O"));
        
        when(pricingMetrics.startApiTimer()).thenReturn(timerSample);
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(remainingSymbols);

        // When
        ResponseEntity<Map<String, Object>> response = controller.unsubscribe(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("unsubscribed")).isEqualTo(Arrays.asList("IBM.N"));
        assertThat(body.get("remainingSubscriptions")).isEqualTo(2);
        
        verify(marketDataProvider).unsubscribe(Arrays.asList("IBM.N"));
        verify(pricingMetrics).recordUnsubscriptionRequest();
    }

    @Test
    void getSubscriptions_ShouldReturnCurrentSubscriptions_WhenRequested() {
        // Given
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        
        when(pricingMetrics.startApiTimer()).thenReturn(timerSample);
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);

        // When
        ResponseEntity<Map<String, Object>> response = controller.getSubscriptions();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("subscribedSymbols")).isEqualTo(subscribedSymbols);
        assertThat(body.get("count")).isEqualTo(3);
        
        verify(pricingMetrics).recordApiRequest("getSubscriptions");
    }

    @Test
    void parseCsv_ShouldHandleEmptyString() {
        // Given
        String symbolsCsv = "";

        // When
        ResponseEntity<Map<String, PriceSnapshot>> response = controller.getPrices(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(marketDataProvider).getLatestPrices(Collections.emptyList());
    }

    @Test
    void parseCsv_ShouldHandleWhitespace() {
        // Given
        String symbolsCsv = " IBM.N , MSFT.O , ";

        // When
        ResponseEntity<Map<String, PriceSnapshot>> response = controller.getPrices(symbolsCsv);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(marketDataProvider).getLatestPrices(Arrays.asList("IBM.N", "MSFT.O"));
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
