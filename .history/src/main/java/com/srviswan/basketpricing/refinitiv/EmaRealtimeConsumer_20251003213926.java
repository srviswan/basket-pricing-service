package com.srviswan.basketpricing.refinitiv;

import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.FieldEntry;
import com.refinitiv.ema.access.FieldList;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerConfig;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.OmmException;
import com.refinitiv.ema.access.OmmState;
import com.refinitiv.ema.access.ReqMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.UpdateMsg;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class EmaRealtimeConsumer implements OmmConsumerClient {

    private static final String DEFAULT_SERVICE = "ELEKTRON_DD";

    public static void main(String[] args) {
        try {
            EmaRealtimeConsumer app = new EmaRealtimeConsumer();
            app.run();
        } catch (Exception ex) {
            log.error("Fatal error", ex);
            System.exit(1);
        }
    }

    private void run() {
        final Properties config = loadConfig();

        final String userName = required(config, "REFINITIV_USER");
        final String ricListCsv = required(config, "REFINITIV_RICS");
        final List<String> rics = parseCsv(ricListCsv);
        final String serviceName = config.getProperty("REFINITIV_SERVICE", DEFAULT_SERVICE);

        final String host = required(config, "REFINITIV_HOST");
        final String port = required(config, "REFINITIV_PORT");

        OmmConsumer consumer = null;
        try {
            log.info("Connecting to {}:{} (ADS/EDP-RT gateway)", host, port);
            consumer = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig()
                    .username(userName)
                    .host(host + ":" + port)
                    .operationModel(OmmConsumerConfig.OperationModel.USER_DISPATCH));

            for (String ric : rics) {
                ReqMsg req = EmaFactory.createReqMsg().serviceName(serviceName).name(ric);
                long handle = consumer.registerClient(req, this);
                log.info("Subscribed to {} with handle {}", ric, handle);
            }

            while (true) {
                consumer.dispatch(1000);
            }
        } catch (OmmException e) {
            log.error("EMA error: {}", e.getMessage(), e);
        } finally {
            if (consumer != null) {
                consumer.uninitialize();
            }
        }
    }

    private Properties loadConfig() {
        Properties properties = new Properties();
        System.getenv().forEach((k, v) -> {
            if (k.startsWith("REFINITIV_")) {
                properties.setProperty(k, v);
            }
        });
        return properties;
    }

    private String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required env: " + key);
        }
        return value;
    }

    private List<String> parseCsv(String csv) {
        List<String> list = new ArrayList<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    @Override
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event) {
        String itemName = Optional.ofNullable(refreshMsg.name()).map(Object::toString).orElse("<unknown>");
        OmmState state = refreshMsg.state();
        log.info("REFRESH {} | state={} {}", itemName, state.streamStateAsString(), state.statusText());
        if (refreshMsg.payload().dataType() == com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST) {
            logFieldList(refreshMsg.payload().fieldList());
        }
    }

    @Override
    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        String itemName = Optional.ofNullable(updateMsg.name()).map(Object::toString).orElse("<unknown>");
        log.info("UPDATE {}", itemName);
        if (updateMsg.payload().dataType() == com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST) {
            logFieldList(updateMsg.payload().fieldList());
        }
    }

    @Override
    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event) {
        String itemName = Optional.ofNullable(statusMsg.name()).map(Object::toString).orElse("<unknown>");
        if (statusMsg.hasState()) {
            OmmState state = statusMsg.state();
            log.warn("STATUS {} | state={} {}", itemName, state.streamStateAsString(), state.statusText());
        } else {
            log.warn("STATUS {}", itemName);
        }
    }

    @Override public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent event) { }
    @Override public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent event) { }
    @Override public void onAllMsg(Msg msg, OmmConsumerEvent event) { }

    private void logFieldList(FieldList fieldList) {
        StringBuilder sb = new StringBuilder();
        for (FieldEntry fieldEntry : fieldList) {
            sb.append(fieldEntry.name())
              .append('=')
              .append(fieldEntry.load().toString())
              .append(' ');
        }
        log.info("FIELDS {}", sb);
    }
}


