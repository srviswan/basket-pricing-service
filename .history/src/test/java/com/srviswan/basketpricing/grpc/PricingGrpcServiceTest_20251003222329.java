package com.srviswan.basketpricing.grpc;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingGrpcServiceTest {

    @Mock
    private MarketDataProvider marketDataProvider;

    @Mock
    private PricingMetrics pricingMetrics;

    private PricingGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new PricingGrpcService(marketDataProvider, pricingMetrics);
    }

    @Test
    void getPrices_ShouldReturnPrices_WhenValidRequest() {
        // Given
        GetPricesRequest request = GetPricesRequest.newBuilder()
                .addSymbols("IBM.N")
                .addSymbols("MSFT.O")
                .build();
        
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);

        StreamObserver<GetPricesResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.getPrices(request, responseObserver);

        // Then
        ArgumentCaptor<GetPricesResponse> responseCaptor = ArgumentCaptor.forClass(GetPricesResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        GetPricesResponse response = responseCaptor.getValue();
        assertThat(response.getPricesList()).hasSize(2);
        assertThat(response.getPricesList().stream().map(PriceSnapshot::getSymbol))
                .containsExactlyInAnyOrder("IBM.N", "MSFT.O");
        
        verify(pricingMetrics).recordApiRequest("grpc.getPrices");
        verify(marketDataProvider).getLatestPrices(Arrays.asList("IBM.N", "MSFT.O"));
    }

    @Test
    void getPrices_ShouldHandleError_WhenProviderThrowsException() {
        // Given
        GetPricesRequest request = GetPricesRequest.newBuilder()
                .addSymbols("IBM.N")
                .build();
        
        when(marketDataProvider.getLatestPrices(anyList())).thenThrow(new RuntimeException("Provider error"));

        StreamObserver<GetPricesResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.getPrices(request, responseObserver);

        // Then
        verify(responseObserver).onError(any(RuntimeException.class));
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    void subscribe_ShouldSubscribeToSymbols_WhenValidRequest() {
        // Given
        SubscribeRequest request = SubscribeRequest.newBuilder()
                .addSymbols("IBM.N")
                .addSymbols("MSFT.O")
                .build();
        
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);

        StreamObserver<SubscribeResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.subscribe(request, responseObserver);

        // Then
        ArgumentCaptor<SubscribeResponse> responseCaptor = ArgumentCaptor.forClass(SubscribeResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        SubscribeResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getSubscribedSymbolsList()).containsExactlyInAnyOrder("IBM.N", "MSFT.O");
        assertThat(response.getTotalSubscriptions()).isEqualTo(3);
        assertThat(response.getMessage()).contains("Successfully subscribed to 2 symbols");
        
        verify(marketDataProvider).subscribe(Arrays.asList("IBM.N", "MSFT.O"));
        verify(pricingMetrics).recordSubscriptionRequest();
    }

    @Test
    void subscribe_ShouldHandleError_WhenProviderThrowsException() {
        // Given
        SubscribeRequest request = SubscribeRequest.newBuilder()
                .addSymbols("IBM.N")
                .build();
        
        doThrow(new RuntimeException("Subscription error")).when(marketDataProvider).subscribe(anyList());

        StreamObserver<SubscribeResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.subscribe(request, responseObserver);

        // Then
        ArgumentCaptor<SubscribeResponse> responseCaptor = ArgumentCaptor.forClass(SubscribeResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        SubscribeResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Failed to subscribe");
    }

    @Test
    void unsubscribe_ShouldUnsubscribeFromSymbols_WhenValidRequest() {
        // Given
        UnsubscribeRequest request = UnsubscribeRequest.newBuilder()
                .addSymbols("IBM.N")
                .build();
        
        Set<String> remainingSymbols = new HashSet<>(Arrays.asList("MSFT.O", "AAPL.O"));
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(remainingSymbols);

        StreamObserver<UnsubscribeResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.unsubscribe(request, responseObserver);

        // Then
        ArgumentCaptor<UnsubscribeResponse> responseCaptor = ArgumentCaptor.forClass(UnsubscribeResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        UnsubscribeResponse response = responseCaptor.getValue();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getUnsubscribedSymbolsList()).containsExactly("IBM.N");
        assertThat(response.getRemainingSubscriptions()).isEqualTo(2);
        assertThat(response.getMessage()).contains("Successfully unsubscribed from 1 symbols");
        
        verify(marketDataProvider).unsubscribe(Arrays.asList("IBM.N"));
        verify(pricingMetrics).recordUnsubscriptionRequest();
    }

    @Test
    void getSubscriptions_ShouldReturnCurrentSubscriptions_WhenRequested() {
        // Given
        GetSubscriptionsRequest request = GetSubscriptionsRequest.newBuilder().build();
        Set<String> subscribedSymbols = new HashSet<>(Arrays.asList("IBM.N", "MSFT.O", "AAPL.O"));
        
        when(marketDataProvider.getSubscribedSymbols()).thenReturn(subscribedSymbols);

        StreamObserver<GetSubscriptionsResponse> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.getSubscriptions(request, responseObserver);

        // Then
        ArgumentCaptor<GetSubscriptionsResponse> responseCaptor = ArgumentCaptor.forClass(GetSubscriptionsResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        GetSubscriptionsResponse response = responseCaptor.getValue();
        assertThat(response.getSubscribedSymbolsList()).containsExactlyInAnyOrder("IBM.N", "MSFT.O", "AAPL.O");
        assertThat(response.getCount()).isEqualTo(3);
        
        verify(pricingMetrics).recordApiRequest("grpc.getSubscriptions");
    }

    @Test
    void streamPrices_ShouldStartStreaming_WhenValidRequest() {
        // Given
        StreamPricesRequest request = StreamPricesRequest.newBuilder()
                .addSymbols("IBM.N")
                .build();
        
        Map<String, PriceSnapshot> mockPrices = createMockPrices();
        when(marketDataProvider.getLatestPrices(anyList())).thenReturn(mockPrices);

        StreamObserver<PriceUpdate> responseObserver = mock(StreamObserver.class);

        // When
        grpcService.streamPrices(request, responseObserver);

        // Then
        verify(marketDataProvider).subscribe(Arrays.asList("IBM.N"));
        verify(pricingMetrics).recordApiRequest("grpc.streamPrices");
        
        // Wait a bit for the streaming to start
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify that price updates are being sent
        verify(responseObserver, atLeastOnce()).onNext(any(PriceUpdate.class));
    }

    @Test
    void broadcastPriceUpdate_ShouldSendUpdateToStreams_WhenStreamsExist() {
        // Given
        String symbol = "IBM.N";
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .symbol(symbol)
                .bid(150.0)
                .ask(150.5)
                .last(150.25)
                .timestamp(Instant.now())
                .build();

        StreamObserver<PriceUpdate> streamObserver = mock(StreamObserver.class);
        
        // Add a stream for the symbol
        grpcService.activeStreams.computeIfAbsent(symbol, k -> new java.util.concurrent.CopyOnWriteArraySet<>())
                .add(streamObserver);

        // When
        grpcService.broadcastPriceUpdate(symbol, snapshot);

        // Then
        verify(streamObserver).onNext(any(PriceUpdate.class));
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
