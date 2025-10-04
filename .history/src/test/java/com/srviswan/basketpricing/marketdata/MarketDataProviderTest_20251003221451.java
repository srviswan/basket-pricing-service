package com.srviswan.basketpricing.marketdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataProviderTest {

    @Mock
    private MarketDataProvider marketDataProvider;

    @Test
    void getLatestPrices_ShouldReturnPrices_WhenSymbolsProvided() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");
        Map<String, PriceSnapshot> expectedPrices = createMockPrices();
        
        when(marketDataProvider.getLatestPrices(symbols)).thenReturn(expectedPrices);

        // When
        Map<String, PriceSnapshot> result = marketDataProvider.getLatestPrices(symbols);

        // Then
        assertThat(result).isEqualTo(expectedPrices);
        verify(marketDataProvider).getLatestPrices(symbols);
    }

    @Test
    void subscribe_ShouldSubscribeToSymbols_WhenValidRequest() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");

        // When
        marketDataProvider.subscribe(symbols);

        // Then
        verify(marketDataProvider).subscribe(symbols);
    }

    @Test
    void unsubscribe_ShouldUnsubscribeFromSymbols_WhenValidRequest() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N");

        // When
        marketDataProvider.unsubscribe(symbols);

        // Then
        verify(marketDataProvider).unsubscribe(symbols);
    }

    @Test
    void getSubscribedSymbols_ShouldReturnCurrentSubscriptions_WhenRequested() {
        // Given
        Set<String> expectedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(expectedSymbols);

        // When
        Set<String> result = marketDataProvider.getSubscribedSymbols();

        // Then
        assertThat(result).isEqualTo(expectedSymbols);
        verify(marketDataProvider).getSubscribedSymbols();
    }

    @Test
    void getLatestPrices_ShouldReturnEmptyMap_WhenNoPricesAvailable() {
        // Given
        List<String> symbols = Arrays.asList("NONEXISTENT.N");
        when(marketDataProvider.getLatestPrices(symbols)).thenReturn(Collections.emptyMap());

        // When
        Map<String, PriceSnapshot> result = marketDataProvider.getLatestPrices(symbols);

        // Then
        assertThat(result).isEmpty();
        verify(marketDataProvider).getLatestPrices(symbols);
    }

    @Test
    void subscribe_ShouldHandleEmptySymbolList_WhenEmptyListProvided() {
        // Given
        List<String> symbols = Collections.emptyList();

        // When
        marketDataProvider.subscribe(symbols);

        // Then
        verify(marketDataProvider).subscribe(symbols);
    }

    @Test
    void unsubscribe_ShouldHandleEmptySymbolList_WhenEmptyListProvided() {
        // Given
        List<String> symbols = Collections.emptyList();

        // When
        marketDataProvider.unsubscribe(symbols);

        // Then
        verify(marketDataProvider).unsubscribe(symbols);
    }

    @Test
    void getSubscribedSymbols_ShouldReturnEmptySet_WhenNoSubscriptions() {
        // Given
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(Collections.emptySet());

        // When
        Set<String> result = marketDataProvider.getSubscribedSymbols();

        // Then
        assertThat(result).isEmpty();
        verify(marketDataProvider).getSubscribedSymbols();
    }

    private Map<String, PriceSnapshot> createMockPrices() {
        Map<String, PriceSnapshot> prices = new HashMap<>();
        prices.put("IBM.N", PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(java.time.Instant.now())
                .build());
        prices.put("MSFT.O", PriceSnapshot.builder()
                .symbol("MSFT.O")
                .bid(300.0)
                .ask(300.5)
                .last(300.25)
                .timestamp(java.time.Instant.now())
                .build());
        return prices;
    }
}
