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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RefinitivEmaProvider implements MarketDataProvider, OmmConsumerClient {

    private final PricingMetrics pricingMetrics;

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
        if (consumer != null) consumer.uninitialize();
    }

    @Override
    public Map<String, PriceSnapshot> getLatestPrices(Collection<String> symbols) {
        Map<String, PriceSnapshot> out = new HashMap<>();
        for (String s : symbols) {
            PriceSnapshot snap = snapshotByRic.get(s);
            if (snap != null) out.put(s, snap);
        }
        return out;
    }

    @Override
    public void subscribe(Collection<String> symbols) {
        for (String ric : symbols) {
            handleByRic.computeIfAbsent(ric, r -> {
                ReqMsg req = EmaFactory.createReqMsg().serviceName(service).name(r);
                long handle = consumer.registerClient(req, this);
                log.info("Subscribed {} handle {}", r, handle);
                return handle;
            });
        }
    }

    @Override
    public void unsubscribe(Collection<String> symbols) {
        for (String ric : symbols) {
            Long handle = handleByRic.remove(ric);
            if (handle != null) {
                consumer.unregisterClient(handle);
                log.info("Unsubscribed {} handle {}", ric, handle);
            }
        }
    }

    @Override
    public Set<String> getSubscribedSymbols() {
        return handleByRic.keySet();
    }

    @Override
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        handleMessage(refreshMsg.name(), refreshMsg.payload());
    }

    @Override
    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        handleMessage(updateMsg.name(), updateMsg.payload());
    }

    @Override
    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
        log.debug("STATUS {}", statusMsg);
    }

    @Override public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent event) {}
    @Override public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent event) {}
    @Override public void onAllMsg(Msg msg, OmmConsumerEvent event) {}

    private void handleMessage(String name, Payload payload) {
        if (payload == null || payload.dataType() != DataType.DataTypes.FIELD_LIST) return;
        FieldList fields = payload.fieldList();
        Double bid = null, ask = null, last = null;
        for (FieldEntry fe : fields) {
            String fname = fe.name();
            switch (fname) {
                case "BID" -> bid = parseDouble(fe);
                case "ASK" -> ask = parseDouble(fe);
                case "TRDPRC_1" -> last = parseDouble(fe);
                default -> {}
            }
        }
        PriceSnapshot snap = PriceSnapshot.builder()
                .symbol(name)
                .bid(bid)
                .ask(ask)
                .last(last)
                .timestamp(Instant.now())
                .build();
        snapshotByRic.put(name, snap);
    }

    private Double parseDouble(FieldEntry fe) {
        try {
            return Double.valueOf(fe.load().toString());
        } catch (Exception e) {
            return null;
        }
    }
}


