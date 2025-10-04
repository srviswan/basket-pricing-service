package com.srviswan.basketpricing.refinitiv;

import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.AckMsgEvent;
import com.refinitiv.ema.access.Ema;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.Event;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.GenericMsgEvent;
import com.refinitiv.ema.access.LoginMsg;
import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.OmmConsumerImpl;
import com.refinitiv.ema.access.OmmConsumerRole;
import com.refinitiv.ema.access.OmmException;
import com.refinitiv.ema.access.OmmNiProviderRole;
import com.refinitiv.ema.access.OmmProvider;
import com.refinitiv.ema.access.OmmState;
import com.refinitiv.ema.access.PostMsg;
import com.refinitiv.ema.access.PostMsgEvent;
import com.refinitiv.ema.access.ReqMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.RefreshMsgEvent;
import com.refinitiv.ema.access.ServiceEndpointDiscovery;
import com.refinitiv.ema.access.ServiceEndpointDiscoveryClient;
import com.refinitiv.ema.access.ServiceEndpointDiscoveryEvent;
import com.refinitiv.ema.access.ServiceEndpointDiscoveryInfo;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.StatusMsgEvent;
import com.refinitiv.ema.access.UpdateMsg;
import com.refinitiv.ema.access.UpdateMsgEvent;
import com.refinitiv.ema.rdm.EmaRdm;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
public class EmaRealtimeConsumer implements OmmConsumerClient {

    private static final String DEFAULT_APP_ID = "256";
    private static final String DEFAULT_POSITION = "127.0.0.1/net";

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
        final String clientId = required(config, "REFINITIV_CLIENT_ID");
        final String appId = config.getProperty("REFINITIV_APP_ID", DEFAULT_APP_ID);
        final String position = config.getProperty("REFINITIV_POSITION", DEFAULT_POSITION);
        final String ricListCsv = required(config, "REFINITIV_RICS");
        final List<String> rics = parseCsv(ricListCsv);

        final String host = config.getProperty("REFINITIV_HOST");
        final String port = config.getProperty("REFINITIV_PORT");
        final boolean useDirectHost = host != null && port != null;

        OmmConsumer consumer = null;
        try {
            if (useDirectHost) {
                log.info("Connecting to {}:{} (enterprise feed or ADS)", host, port);
                consumer = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig()
                        .username(userName)
                        .host(host + ":" + port)
                        .operationModel(OmmConsumerConfig.OperationModel.USER_DISPATCH));
            } else {
                log.info("Using service discovery for RDP (no host/port provided)");
                consumer = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig()
                        .username(userName)
                        .clientId(clientId)
                        .applicationId(appId)
                        .position(position)
                        .operationModel(OmmConsumerConfig.OperationModel.USER_DISPATCH));
            }

            ReqMsg loginReq = EmaFactory.createReqMsg().domainType(EmaRdm.MMT_LOGIN).name(userName);
            consumer.registerClient(loginReq, this);

            for (String ric : rics) {
                ReqMsg req = EmaFactory.createReqMsg().serviceName("ELEKTRON_DD").name(ric);
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
        if (refreshMsg.hasPayload() && refreshMsg.payload().dataType() == com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST) {
            logFieldList(refreshMsg.payload().fieldList());
        }
    }

    @Override
    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event) {
        String itemName = Optional.ofNullable(updateMsg.name()).map(Object::toString).orElse("<unknown>");
        log.info("UPDATE {}", itemName);
        if (updateMsg.hasPayload() && updateMsg.payload().dataType() == com.refinitiv.ema.access.DataType.DataTypes.FIELD_LIST) {
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

    private void logFieldList(com.refinitiv.ema.access.FieldList fieldList) {
        StringBuilder sb = new StringBuilder();
        fieldList.forEach(fieldEntry -> {
            sb.append(fieldEntry.name())
                    .append('=')
                    .append(fieldEntry.load().toString())
                    .append(' ');
        });
        log.info("FIELDS {}", sb);
    }
}


