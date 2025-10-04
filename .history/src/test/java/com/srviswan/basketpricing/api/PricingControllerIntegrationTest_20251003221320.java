package com.srviswan.basketpricing.api;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.resilience.BackpressureManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PricingController.class)
class PricingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataProvider marketDataProvider;

    @MockBean
    private PricingMetrics pricingMetrics;

    @MockBean
    private BackpressureManager backpressureManager;

    @Test
    void getPrices_ShouldReturnPrices_WhenValidRequest() throws Exception {
        // Given
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.5);

        // When & Then
        mockMvc.perform(get("/api/prices")
                .param("symbols", "IBM.N,MSFT.O"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.IBM.N.symbol").value("IBM.N"))
                .andExpect(jsonPath("$.IBM.N.bid").value(150.0))
                .andExpect(jsonPath("$.MSFT.O.symbol").value("MSFT.O"))
                .andExpect(jsonPath("$.MSFT.O.bid").value(300.0));
    }

    @Test
    void getPrices_ShouldReturnEmptyMap_WhenNoPricesAvailable() throws Exception {
        // Given
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(Collections.emptyMap());
        when(backpressureManager.getQueueUtilization()).thenReturn(0.0);

        // When & Then
        mockMvc.perform(get("/api/prices")
                .param("symbols", "NONEXISTENT.N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void subscribe_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        // Given
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.3);
        when(backpressureManager.getProcessedUpdates()).thenReturn(100L);
        when(backpressureManager.getDroppedUpdates()).thenReturn(5L);

        // When & Then
        mockMvc.perform(post("/api/prices/subscribe")
                .param("symbols", "IBM.N,MSFT.O"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").isArray())
                .andExpect(jsonPath("$.subscribed[0]").value("IBM.N"))
                .andExpect(jsonPath("$.subscribed[1]").value("MSFT.O"))
                .andExpect(jsonPath("$.totalSubscriptions").value(2))
                .andExpect(jsonPath("$.backpressureStatus.queueUtilization").value(0.3));
    }

    @Test
    void unsubscribe_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        // Given
        Set<String> remainingSymbols = new HashSet<>(Arrays.asList("MSFT.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(remainingSymbols);

        // When & Then
        mockMvc.perform(delete("/api/prices/unsubscribe")
                .param("symbols", "IBM.N"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unsubscribed").isArray())
                .andExpect(jsonPath("$.unsubscribed[0]").value("IBM.N"))
                .andExpect(jsonPath("$.remainingSubscriptions").value(1));
    }

    @Test
    void getSubscriptions_ShouldReturnCurrentSubscriptions_WhenRequested() throws Exception {
        // Given
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);

        // When & Then
        mockMvc.perform(get("/api/prices/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribedSymbols").isArray())
                .andExpect(jsonPath("$.subscribedSymbols").value(subscribedSymbols))
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void getPrices_ShouldHandleEmptySymbols_WhenNoSymbolsProvided() throws Exception {
        // Given
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(Collections.emptyMap());
        when(backpressureManager.getQueueUtilization()).thenReturn(0.0);

        // When & Then
        mockMvc.perform(get("/api/prices")
                .param("symbols", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPrices_ShouldHandleWhitespaceInSymbols_WhenSymbolsContainSpaces() throws Exception {
        // Given
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);
        when(backpressureManager.getQueueUtilization()).thenReturn(0.5);

        // When & Then
        mockMvc.perform(get("/api/prices")
                .param("symbols", " IBM.N , MSFT.O , "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.IBM.N.symbol").value("IBM.N"))
                .andExpect(jsonPath("$.MSFT.O.symbol").value("MSFT.O"));
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
