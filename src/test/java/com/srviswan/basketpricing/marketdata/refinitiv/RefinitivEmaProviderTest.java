package com.srviswan.basketpricing.marketdata.refinitiv;

import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.resilience.BackpressureManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefinitivEmaProviderTest {

    @Mock
    private PricingMetrics pricingMetrics;

    @Mock
    private BackpressureManager backpressureManager;

    @Mock
    private com.srviswan.basketpricing.grpc.PricingGrpcService grpcService;

    private RefinitivEmaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RefinitivEmaProvider(pricingMetrics, backpressureManager);
        ReflectionTestUtils.setField(provider, "grpcService", grpcService);
        ReflectionTestUtils.setField(provider, "host", "localhost");
        ReflectionTestUtils.setField(provider, "port", "14002");
        ReflectionTestUtils.setField(provider, "user", "testuser");
        ReflectionTestUtils.setField(provider, "service", "ELEKTRON_DD");
    }

    @Test
    void getLatestPrices_ShouldReturnEmptyMap_WhenNoPricesAvailable() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");

        // When
        Map<String, PriceSnapshot> result = provider.getLatestPrices(symbols);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getLatestPrices_ShouldReturnAvailablePrices_WhenSomePricesExist() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");
        
        // Simulate adding a price snapshot
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .symbol("IBM.N")
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(Instant.now())
                .build();
        
        // Access the internal snapshot map and add a price
        Map<String, PriceSnapshot> snapshotMap = (Map<String, PriceSnapshot>) 
                ReflectionTestUtils.getField(provider, "snapshotByRic");
        snapshotMap.put("IBM.N", snapshot);

        // When
        Map<String, PriceSnapshot> result = provider.getLatestPrices(symbols);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("IBM.N");
        assertThat(result.get("IBM.N").getSymbol()).isEqualTo("IBM.N");
        assertThat(result.get("IBM.N").getBid()).isEqualTo(150.0);
    }

    @Test
    void subscribe_ShouldTrackSubscriptions_WhenSymbolsProvided() {
        // Given
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O");

        // When
        provider.subscribe(symbols);

        // Then
        Set<String> subscribedSymbols = provider.getSubscribedSymbols();
        assertThat(subscribedSymbols).containsExactlyInAnyOrder("IBM.N", "MSFT.O");
        verify(pricingMetrics, times(2)).incrementActiveSubscriptions();
    }

    @Test
    void subscribe_ShouldNotDuplicateSubscriptions_WhenSymbolAlreadySubscribed() {
        // Given
        List<String> symbols1 = Arrays.asList("IBM.N");
        List<String> symbols2 = Arrays.asList("IBM.N", "MSFT.O");

        // When
        provider.subscribe(symbols1);
        provider.subscribe(symbols2);

        // Then
        Set<String> subscribedSymbols = provider.getSubscribedSymbols();
        assertThat(subscribedSymbols).containsExactlyInAnyOrder("IBM.N", "MSFT.O");
        // Should only increment once for each unique symbol
        verify(pricingMetrics, times(2)).incrementActiveSubscriptions();
    }

    @Test
    void unsubscribe_ShouldRemoveSubscriptions_WhenSymbolsProvided() {
        // Given
        List<String> subscribeSymbols = Arrays.asList("IBM.N", "MSFT.O", "AAPL.O");
        List<String> unsubscribeSymbols = Arrays.asList("IBM.N", "MSFT.O");

        provider.subscribe(subscribeSymbols);

        // When
        provider.unsubscribe(unsubscribeSymbols);

        // Then
        Set<String> remainingSymbols = provider.getSubscribedSymbols();
        assertThat(remainingSymbols).containsExactly("AAPL.O");
        verify(pricingMetrics, times(2)).decrementActiveSubscriptions();
    }

    @Test
    void unsubscribe_ShouldHandleNonExistentSymbols_WhenSymbolsNotSubscribed() {
        // Given
        List<String> symbols = Arrays.asList("NONEXISTENT.N");

        // When
        provider.unsubscribe(symbols);

        // Then
        // Should not throw exception and should not decrement metrics
        verify(pricingMetrics, never()).decrementActiveSubscriptions();
    }

    @Test
    void getSubscribedSymbols_ShouldReturnEmptySet_WhenNoSubscriptions() {
        // When
        Set<String> result = provider.getSubscribedSymbols();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void handleMessage_ShouldCreatePriceSnapshot_WhenValidPayload() {
        // Given
        String symbol = "IBM.N";
        double bid = 150.0;
        double ask = 150.5;
        double last = 150.25;

        // Create mock field list
        com.refinitiv.ema.access.FieldList fieldList = mock(com.refinitiv.ema.access.FieldList.class);
        com.refinitiv.ema.access.FieldEntry bidField = mock(com.refinitiv.ema.access.FieldEntry.class);
        com.refinitiv.ema.access.FieldEntry askField = mock(com.refinitiv.ema.access.FieldEntry.class);
        com.refinitiv.ema.access.FieldEntry lastField = mock(com.refinitiv.ema.access.FieldEntry.class);

        when(bidField.name()).thenReturn("BID");
        when(bidField.load()).thenReturn(bid);
        when(askField.name()).thenReturn("ASK");
        when(askField.load()).thenReturn(ask);
        when(lastField.name()).thenReturn("TRDPRC_1");
        when(lastField.load()).thenReturn(last);

        Iterator<com.refinitiv.ema.access.FieldEntry> iterator = Arrays.asList(bidField, askField, lastField).iterator();
        when(fieldList.iterator()).thenReturn(iterator);

        com.refinitiv.ema.access.Payload payload = mock(com.refinitiv.ema.access.Payload.class);
        when(payload.dataType()).thenReturn(com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST);
        when(payload.fieldList()).thenReturn(fieldList);

        when(backpressureManager.offerUpdate(any())).thenReturn(true);

        // When
        ReflectionTestUtils.invokeMethod(provider, "handleMessage", symbol, payload);

        // Then
        verify(backpressureManager).offerUpdate(any());
        verify(pricingMetrics).recordPriceUpdate();
        verify(grpcService).broadcastPriceUpdate(eq(symbol), any(PriceSnapshot.class));
    }

    @Test
    void handleMessage_ShouldHandleBackpressure_WhenQueueIsFull() {
        // Given
        String symbol = "IBM.N";
        com.refinitiv.ema.access.FieldList fieldList = mock(com.refinitiv.ema.access.FieldList.class);
        com.refinitiv.ema.access.Payload payload = mock(com.refinitiv.ema.access.Payload.class);
        
        when(payload.dataType()).thenReturn(com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST);
        when(payload.fieldList()).thenReturn(fieldList);
        when(fieldList.iterator()).thenReturn(Collections.emptyIterator());
        when(backpressureManager.offerUpdate(any())).thenReturn(false);

        // When
        ReflectionTestUtils.invokeMethod(provider, "handleMessage", symbol, payload);

        // Then
        verify(backpressureManager).offerUpdate(any());
        verify(pricingMetrics).recordPriceUpdate();
        verify(grpcService).broadcastPriceUpdate(eq(symbol), any(PriceSnapshot.class));
    }

    @Test
    void handleMessage_ShouldSkipProcessing_WhenPayloadIsNull() {
        // Given
        String symbol = "IBM.N";

        // When
        ReflectionTestUtils.invokeMethod(provider, "handleMessage", symbol, null);

        // Then
        verify(backpressureManager, never()).offerUpdate(any());
        verify(pricingMetrics, never()).recordPriceUpdate();
        verify(grpcService, never()).broadcastPriceUpdate(any(), any());
    }

    @Test
    void handleMessage_ShouldSkipProcessing_WhenPayloadIsNotFieldList() {
        // Given
        String symbol = "IBM.N";
        com.refinitiv.ema.access.Payload payload = mock(com.refinitiv.ema.access.Payload.class);
        when(payload.dataType()).thenReturn(com.refinitiv.ema.access.DataType.DataTypes.ELEMENT_LIST);

        // When
        ReflectionTestUtils.invokeMethod(provider, "handleMessage", symbol, payload);

        // Then
        verify(backpressureManager, never()).offerUpdate(any());
        verify(pricingMetrics, never()).recordPriceUpdate();
        verify(grpcService, never()).broadcastPriceUpdate(any(), any());
    }

    @Test
    void parseDouble_ShouldReturnNull_WhenFieldValueIsInvalid() {
        // Given
        com.refinitiv.ema.access.FieldEntry fieldEntry = mock(com.refinitiv.ema.access.FieldEntry.class);
        when(fieldEntry.load()).thenReturn("invalid_number");

        // When
        Double result = ReflectionTestUtils.invokeMethod(provider, "parseDouble", fieldEntry);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseDouble_ShouldReturnValue_WhenFieldValueIsValid() {
        // Given
        com.refinitiv.ema.access.FieldEntry fieldEntry = mock(com.refinitiv.ema.access.FieldEntry.class);
        when(fieldEntry.load()).thenReturn("150.25");

        // When
        Double result = ReflectionTestUtils.invokeMethod(provider, "parseDouble", fieldEntry);

        // Then
        assertThat(result).isEqualTo(150.25);
    }
}
