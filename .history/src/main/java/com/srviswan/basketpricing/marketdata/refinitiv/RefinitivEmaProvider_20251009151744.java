package com.srviswan.basketpricing.marketdata.refinitiv;

import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.DataType;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.FieldEntry;
import com.refinitiv.ema.access.FieldList;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerConfig;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.ReqMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.UpdateMsg;
import com.refinitiv.ema.access.Payload;
import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.marketdata.PriceSnapshot;
import com.srviswan.basketpricing.monitoring.PricingMetrics;
import com.srviswan.basketpricing.resilience.BackpressureManager;
import com.srviswan.basketpricing.events.PriceUpdateEvent;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("refinitivEmaProvider")
@RequiredArgsConstructor
public class RefinitivEmaProvider implements MarketDataProvider, OmmConsumerClient {

    private final PricingMetrics pricingMetrics;
    private final BackpressureManager backpressureManager;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, PriceSnapshot> snapshotByRic = new ConcurrentHashMap<>();
    private final Map<String, Long> handleByRic = new ConcurrentHashMap<>();

    @Value("${refinitiv.host}")
    private String host;

    @Value("${refinitiv.port}")
    private String port;

    @Value("${refinitiv.user}")
    private String user;

    @Value("${refinitiv.service:ELEKTRON_DD}")
    private String service;

    private OmmConsumer consumer;

    @PostConstruct
    public void start() {
        try {
            consumer = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig()
                    .username(user)
                    .host(host + ":" + port)
                    .operationModel(OmmConsumerConfig.OperationModel.USER_DISPATCH));

            pricingMetrics.setConnectionStatus(true);
            pricingMetrics.initializeGauges();

            Thread dispatcher = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        consumer.dispatch(500);
                    } catch (Exception e) {
                        log.warn("Dispatch error", e);
                        pricingMetrics.recordConnectionError();
                        pricingMetrics.setConnectionStatus(false);
                    }
                }
            }, "ema-dispatcher");
            dispatcher.setDaemon(true);
            dispatcher.start();
            
            log.info("Refinitiv EMA provider started successfully");
        } catch (Exception e) {
            log.error("Failed to start Refinitiv EMA provider", e);
            pricingMetrics.recordConnectionError();
            pricingMetrics.setConnectionStatus(false);
            throw e;
        }
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) {
            consumer.uninitialize();
            pricingMetrics.setConnectionStatus(false);
        }
    }

    @Override
    public Map<String, PriceSnapshot> getLatestPrices(Collection<String> symbols) {
        log.debug("RefinitivEmaProvider.getLatestPrices() called for symbols: {}", symbols);
        log.debug("Current snapshotByRic map size: {}, keys: {}", snapshotByRic.size(), snapshotByRic.keySet());
        log.debug("Current handleByRic map size: {}, keys: {}", handleByRic.size(), handleByRic.keySet());
        
        Map<String, PriceSnapshot> out = new HashMap<>();
        for (String s : symbols) {
            PriceSnapshot snap = snapshotByRic.get(s);
            if (snap != null) {
                log.debug("Found price snapshot for {}: {}", s, snap);
                out.put(s, snap);
            } else {
                log.warn("No price snapshot found for symbol: {} (not subscribed or no updates received yet)", s);
            }
        }
        
        log.debug("Returning {} price snapshots out of {} requested symbols", out.size(), symbols.size());
        return out;
    }

    @Override
    public void subscribe(Collection<String> symbols) {
        log.info("RefinitivEmaProvider.subscribe() called for symbols: {}", symbols);
        
        if (consumer == null) {
            log.error("Cannot subscribe: EMA consumer is not initialized!");
            return;
        }
        
        for (String ric : symbols) {
            handleByRic.computeIfAbsent(ric, r -> {
                try {
                    ReqMsg req = EmaFactory.createReqMsg().serviceName(service).name(r);
                    long handle = consumer.registerClient(req, this);
                    log.info("✅ Successfully subscribed to {} with handle {}", r, handle);
                    pricingMetrics.incrementActiveSubscriptions();
                    return handle;
                } catch (Exception e) {
                    log.error("❌ Failed to subscribe to {}: {}", r, e.getMessage(), e);
                    return null;
                }
            });
        }
        
        log.info("Subscription complete. Total subscriptions: {}", handleByRic.size());
    }

    @Override
    public void unsubscribe(Collection<String> symbols) {
        for (String ric : symbols) {
            Long handle = handleByRic.remove(ric);
            if (handle != null) {
                // Note: EMA doesn't have unregisterClient method, just remove from tracking
                log.info("Unsubscribed {} handle {}", ric, handle);
                pricingMetrics.decrementActiveSubscriptions();
            }
        }
    }

    @Override
    public Set<String> getSubscribedSymbols() {
        return handleByRic.keySet();
    }

    @Override
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        log.info("📥 Received RefreshMsg for symbol: {}", refreshMsg.name());
        long startTime = System.currentTimeMillis();
        handleMessage(refreshMsg.name(), refreshMsg.payload());
        pricingMetrics.recordPriceUpdateLatency(System.currentTimeMillis() - startTime);
    }

    @Override
    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        log.debug("📥 Received UpdateMsg for symbol: {}", updateMsg.name());
        long startTime = System.currentTimeMillis();
        handleMessage(updateMsg.name(), updateMsg.payload());
        pricingMetrics.recordPriceUpdateLatency(System.currentTimeMillis() - startTime);
    }

    @Override
    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
        log.info("📊 STATUS message received: {}", statusMsg);
    }

    @Override public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent event) {}
    @Override public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent event) {}
    @Override public void onAllMsg(Msg msg, OmmConsumerEvent event) {}

    private void handleMessage(String name, Payload payload) {
        log.debug("📨 handleMessage() called for symbol: {}", name);
        
        if (payload == null) {
            log.warn("⚠️  Payload is null for symbol: {}", name);
            return;
        }
        
        if (payload.dataType() != DataType.DataTypes.FIELD_LIST) {
            log.warn("⚠️  Payload is not FIELD_LIST for symbol: {}. Type: {}", name, payload.dataType());
            return;
        }
        
        FieldList fields = payload.fieldList();
        Double bid = null, ask = null, last = null;
        int fieldCount = 0;
        
        for (FieldEntry fe : fields) {
            fieldCount++;
            String fname = fe.name();
            switch (fname) {
                case "BID" -> {
                    bid = parseDouble(fe);
                    log.debug("  BID field: {}", bid);
                }
                case "ASK" -> {
                    ask = parseDouble(fe);
                    log.debug("  ASK field: {}", ask);
                }
                case "TRDPRC_1" -> {
                    last = parseDouble(fe);
                    log.debug("  LAST field: {}", last);
                }
                default -> {}
            }
        }
        
        log.debug("Parsed {} fields for {}: BID={}, ASK={}, LAST={}", fieldCount, name, bid, ask, last);
        
        // Use backpressure to handle high-frequency updates
        final String symbol = name;
        final Double finalBid = bid;
        final Double finalAsk = ask;
        final Double finalLast = last;
        
        BackpressureManager.PriceUpdateTask task = new BackpressureManager.PriceUpdateTask(symbol, () -> {
            PriceSnapshot snap = PriceSnapshot.builder()
                    .symbol(symbol)
                    .bid(finalBid)
                    .ask(finalAsk)
                    .last(finalLast)
                    .timestamp(Instant.now())
                    .build();
            snapshotByRic.put(symbol, snap);
            log.info("💾 Stored price snapshot for {}: BID={}, ASK={}, LAST={}", symbol, finalBid, finalAsk, finalLast);
            log.debug("snapshotByRic map now contains {} entries", snapshotByRic.size());
            pricingMetrics.recordPriceUpdate();
        });

        boolean offered = backpressureManager.offerUpdate(task);
        if (!offered) {
            // If queue is full, update directly but log the backpressure
            log.warn("⚠️  Backpressure: queue full, updating directly for {}", name);
            task.getUpdateAction().run();
        } else {
            log.debug("✅ Price update task queued for {}", name);
        }
        
        // Publish event for other components to listen to (gRPC, etc.)
        PriceSnapshot snap = PriceSnapshot.builder()
                .symbol(symbol)
                .bid(finalBid)
                .ask(finalAsk)
                .last(finalLast)
                .timestamp(Instant.now())
                .build();
        
        eventPublisher.publishEvent(new PriceUpdateEvent(this, symbol, snap));
        log.debug("📢 Published PriceUpdateEvent for {}", symbol);
    }

    private Double parseDouble(FieldEntry fe) {
        try {
            return Double.valueOf(fe.load().toString());
        } catch (Exception e) {
            return null;
        }
    }
}


