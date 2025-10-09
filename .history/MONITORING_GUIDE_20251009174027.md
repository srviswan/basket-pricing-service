# Monitoring Metrics Guide

## 🎯 **How to Access Monitoring Metrics**

The Basket Pricing Service exposes comprehensive metrics through Spring Boot Actuator endpoints.

## 📊 **Available Endpoints**

### **1. Health Check**
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

### **2. All Metrics (List)**
```bash
curl http://localhost:8080/actuator/metrics
```

**Response:**
```json
{
  "names": [
    "pricing.api.requests",
    "pricing.api.response.time",
    "pricing.subscriptions.active",
    "pricing.subscriptions.requests",
    "pricing.unsubscriptions.requests",
    "pricing.updates.count",
    "pricing.updates.latency",
    "pricing.connection.status",
    "pricing.connection.errors",
    "pricing.backpressure.utilization",
    "pricing.backpressure.processed",
    "pricing.backpressure.dropped",
    "jvm.memory.used",
    "jvm.threads.live",
    "system.cpu.usage",
    ...
  ]
}
```

---

### **3. Specific Metric Details**
```bash
# Get details for a specific metric
curl http://localhost:8080/actuator/metrics/{metric-name}
```

---

## 📈 **Pricing Service Metrics**

### **API Request Metrics**

#### **Total API Requests**
```bash
curl http://localhost:8080/actuator/metrics/pricing.api.requests
```

**Response:**
```json
{
  "name": "pricing.api.requests",
  "description": "Total API requests",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1234.0
    }
  ],
  "availableTags": [
    {
      "tag": "endpoint",
      "values": ["getPrices", "subscribe", "unsubscribe", "getSubscriptions"]
    }
  ]
}
```

**Filter by endpoint:**
```bash
curl "http://localhost:8080/actuator/metrics/pricing.api.requests?tag=endpoint:getPrices"
curl "http://localhost:8080/actuator/metrics/pricing.api.requests?tag=endpoint:subscribe"
```

#### **API Response Time**
```bash
curl http://localhost:8080/actuator/metrics/pricing.api.response.time
```

**Response:**
```json
{
  "name": "pricing.api.response.time",
  "description": "API response time",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 1234.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 45.678
    },
    {
      "statistic": "MAX",
      "value": 0.523
    }
  ]
}
```

---

### **Subscription Metrics**

#### **Active Subscriptions**
```bash
curl http://localhost:8080/actuator/metrics/pricing.subscriptions.active
```

**Response:**
```json
{
  "name": "pricing.subscriptions.active",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 15.0
    }
  ]
}
```

#### **Subscription Requests**
```bash
curl http://localhost:8080/actuator/metrics/pricing.subscriptions.requests
```

#### **Unsubscription Requests**
```bash
curl http://localhost:8080/actuator/metrics/pricing.unsubscriptions.requests
```

---

### **Price Update Metrics**

#### **Price Updates Count**
```bash
curl http://localhost:8080/actuator/metrics/pricing.updates.count
```

**Response:**
```json
{
  "name": "pricing.updates.count",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 45678.0
    }
  ]
}
```

#### **Price Update Latency**
```bash
curl http://localhost:8080/actuator/metrics/pricing.updates.latency
```

**Response:**
```json
{
  "name": "pricing.updates.latency",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 45678.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 234.567
    },
    {
      "statistic": "MAX",
      "value": 0.025
    },
    {
      "statistic": "MEAN",
      "value": 0.005
    }
  ]
}
```

---

### **Connection Metrics**

#### **Connection Status**
```bash
curl http://localhost:8080/actuator/metrics/pricing.connection.status
```

**Response:**
```json
{
  "name": "pricing.connection.status",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 1.0  // 1.0 = connected, 0.0 = disconnected
    }
  ]
}
```

#### **Connection Errors**
```bash
curl http://localhost:8080/actuator/metrics/pricing.connection.errors
```

---

### **Backpressure Metrics**

#### **Queue Utilization**
```bash
curl http://localhost:8080/actuator/metrics/pricing.backpressure.utilization
```

**Response:**
```json
{
  "name": "pricing.backpressure.utilization",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 0.15  // 15% of queue capacity used
    }
  ]
}
```

#### **Processed Updates**
```bash
curl http://localhost:8080/actuator/metrics/pricing.backpressure.processed
```

#### **Dropped Updates**
```bash
curl http://localhost:8080/actuator/metrics/pricing.backpressure.dropped
```

---

## 🔧 **Prometheus Format**

### **All Metrics in Prometheus Format**
```bash
curl http://localhost:8080/actuator/prometheus
```

**Response:** (Prometheus text format)
```
# HELP pricing_api_requests_total Total API requests
# TYPE pricing_api_requests_total counter
pricing_api_requests_total{endpoint="getPrices",} 1234.0
pricing_api_requests_total{endpoint="subscribe",} 567.0

# HELP pricing_subscriptions_active Active subscriptions
# TYPE pricing_subscriptions_active gauge
pricing_subscriptions_active 15.0

# HELP pricing_updates_count_total Total price updates
# TYPE pricing_updates_count_total counter
pricing_updates_count_total 45678.0

# HELP pricing_connection_status Connection status
# TYPE pricing_connection_status gauge
pricing_connection_status 1.0

# ... many more metrics
```

---

## 📊 **Using Prometheus & Grafana**

### **Start Monitoring Stack**
```bash
# Start the full monitoring stack (Prometheus + Grafana)
docker-compose up -d

# Check services
docker-compose ps
```

### **Access Dashboards**

#### **Prometheus**
- **URL**: http://localhost:9090
- **Query Examples**:
  ```promql
  # Active subscriptions
  pricing_subscriptions_active
  
  # Price update rate (per second)
  rate(pricing_updates_count_total[1m])
  
  # API request rate
  rate(pricing_api_requests_total[5m])
  
  # Average response time
  rate(pricing_api_response_time_seconds_sum[5m]) / 
  rate(pricing_api_response_time_seconds_count[5m])
  
  # Backpressure utilization
  pricing_backpressure_utilization
  ```

#### **Grafana**
- **URL**: http://localhost:3000
- **Default Credentials**: admin / admin
- **Data Source**: Prometheus (http://prometheus:9090)

---

## 🎨 **Custom Diagnostics Endpoint**

### **Service Diagnostics**
```bash
curl http://localhost:8080/api/prices/diagnostics
```

**Response:**
```json
{
  "marketDataProviderClass": "com.srviswan.basketpricing.resilience.ResilientMarketDataProvider",
  "isResilientProvider": true,
  "subscribedSymbols": ["IBM.N", "MSFT.O", "AAPL.O"],
  "subscriptionCount": 3,
  "availablePrices": 3,
  "priceKeys": ["IBM.N", "MSFT.O", "AAPL.O"],
  "backpressureUtilization": 0.15,
  "backpressureProcessed": 45678,
  "backpressureDropped": 0
}
```

**What it shows**:
- ✅ Which provider is being used (verifies ResilientMarketDataProvider)
- ✅ Current subscriptions
- ✅ Available prices
- ✅ Backpressure status

---

## 🛠️ **Monitoring Scripts**

### **Quick Metrics Check**
```bash
#!/bin/bash

echo "=== Pricing Service Metrics ==="

echo -e "\n📊 Active Subscriptions:"
curl -s http://localhost:8080/actuator/metrics/pricing.subscriptions.active | jq '.measurements[0].value'

echo -e "\n📈 Total Price Updates:"
curl -s http://localhost:8080/actuator/metrics/pricing.updates.count | jq '.measurements[0].value'

echo -e "\n🔗 Connection Status:"
curl -s http://localhost:8080/actuator/metrics/pricing.connection.status | jq '.measurements[0].value'

echo -e "\n⚡ Backpressure Utilization:"
curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.utilization | jq '.measurements[0].value'

echo -e "\n✅ Processed Updates:"
curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.processed | jq '.measurements[0].value'

echo -e "\n❌ Dropped Updates:"
curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.dropped | jq '.measurements[0].value'
```

Save this as `scripts/check_metrics.sh` and run:
```bash
chmod +x scripts/check_metrics.sh
./scripts/check_metrics.sh
```

---

### **Comprehensive Diagnostics**
```bash
# Use the diagnostic script we created
./scripts/diagnose_pricing_service.sh
```

This will:
- ✅ Check service health
- ✅ Verify bean resolution
- ✅ Check subscriptions
- ✅ Test price retrieval
- ✅ Display all metrics
- ✅ Analyze logs
- ✅ Provide recommendations

---

## 📊 **Key Metrics to Monitor**

### **Health Indicators**
| Metric | Good Value | Bad Value | Action |
|--------|-----------|-----------|--------|
| `pricing.connection.status` | 1.0 | 0.0 | Check Refinitiv connection |
| `pricing.connection.errors` | 0 | >0 | Investigate connection issues |
| `pricing.subscriptions.active` | >0 | 0 | Subscribe to symbols |
| `pricing.updates.count` | Increasing | Static | Check if updates are being received |

### **Performance Indicators**
| Metric | Good Value | Bad Value | Action |
|--------|-----------|-----------|--------|
| `pricing.api.response.time` (mean) | <0.1s | >1s | Investigate slow queries |
| `pricing.updates.latency` (mean) | <0.01s | >0.1s | Check processing performance |
| `pricing.backpressure.utilization` | <0.5 | >0.8 | Increase queue size or processing threads |
| `pricing.backpressure.dropped` | 0 | >0 | Critical: Losing data! |

### **Business Indicators**
| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `pricing.api.requests` | Total API calls | Monitor for unusual spikes |
| `pricing.subscriptions.requests` | Subscription requests | Monitor growth |
| `pricing.updates.count` | Price updates received | Alert if stops increasing |

---

## 🚨 **Alerts (Prometheus)**

The service includes pre-configured alerts in `prometheus-alerts.yml`:

### **Critical Alerts**
- **Service Down**: Service not responding
- **Market Data Connection Down**: Refinitiv connection lost
- **No Price Updates**: No updates received for 5 minutes

### **Warning Alerts**
- **High API Latency**: Response time > 1 second
- **Low Update Rate**: < 10 updates/second
- **High Memory Usage**: > 80% memory used
- **High CPU Usage**: > 80% CPU used
- **High Backpressure**: Queue utilization > 80%
- **Dropped Updates**: Any updates dropped

---

## 🎨 **Grafana Dashboards**

### **Access Grafana**
1. Start monitoring stack: `docker-compose up -d`
2. Open: http://localhost:3000
3. Login: admin / admin
4. Add Prometheus data source: http://prometheus:9090

### **Create Dashboard Panels**

#### **Panel 1: Active Subscriptions**
```promql
pricing_subscriptions_active
```

#### **Panel 2: Price Update Rate**
```promql
rate(pricing_updates_count_total[1m])
```

#### **Panel 3: API Request Rate**
```promql
rate(pricing_api_requests_total[5m])
```

#### **Panel 4: Average Response Time**
```promql
rate(pricing_api_response_time_seconds_sum[5m]) / 
rate(pricing_api_response_time_seconds_count[5m])
```

#### **Panel 5: Backpressure Utilization**
```promql
pricing_backpressure_utilization
```

#### **Panel 6: Connection Status**
```promql
pricing_connection_status
```

#### **Panel 7: Dropped Updates**
```promql
pricing_backpressure_dropped
```

---

## 🔧 **Command Line Monitoring**

### **Watch Metrics in Real-Time**
```bash
# Watch active subscriptions
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/pricing.subscriptions.active | jq ".measurements[0].value"'

# Watch price updates
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/pricing.updates.count | jq ".measurements[0].value"'

# Watch backpressure
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.utilization | jq ".measurements[0].value"'
```

---

## 📱 **Monitoring Dashboard Script**

Create `scripts/monitor_dashboard.sh`:

```bash
#!/bin/bash

# Real-time monitoring dashboard for pricing service

clear

while true; do
    clear
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║     Basket Pricing Service - Live Monitoring Dashboard     ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Connection Status
    conn_status=$(curl -s http://localhost:8080/actuator/metrics/pricing.connection.status | jq -r '.measurements[0].value')
    if [ "$conn_status" = "1.0" ]; then
        echo "🟢 Connection: CONNECTED"
    else
        echo "🔴 Connection: DISCONNECTED"
    fi
    
    # Active Subscriptions
    subs=$(curl -s http://localhost:8080/actuator/metrics/pricing.subscriptions.active | jq -r '.measurements[0].value')
    echo "📊 Active Subscriptions: $subs"
    
    # Price Updates
    updates=$(curl -s http://localhost:8080/actuator/metrics/pricing.updates.count | jq -r '.measurements[0].value')
    echo "📈 Total Price Updates: $updates"
    
    # API Requests
    requests=$(curl -s http://localhost:8080/actuator/metrics/pricing.api.requests | jq -r '.measurements[0].value')
    echo "🌐 Total API Requests: $requests"
    
    # Backpressure
    bp_util=$(curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.utilization | jq -r '.measurements[0].value')
    bp_processed=$(curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.processed | jq -r '.measurements[0].value')
    bp_dropped=$(curl -s http://localhost:8080/actuator/metrics/pricing.backpressure.dropped | jq -r '.measurements[0].value')
    
    bp_util_pct=$(echo "$bp_util * 100" | bc)
    echo "⚡ Backpressure Utilization: ${bp_util_pct}%"
    echo "✅ Processed Updates: $bp_processed"
    
    if [ "$bp_dropped" != "0.0" ]; then
        echo "❌ Dropped Updates: $bp_dropped (WARNING!)"
    else
        echo "✅ Dropped Updates: 0"
    fi
    
    echo ""
    echo "Press Ctrl+C to exit"
    echo "Refreshing in 2 seconds..."
    
    sleep 2
done
```

**Usage:**
```bash
chmod +x scripts/monitor_dashboard.sh
./scripts/monitor_dashboard.sh
```

---

## 📊 **JVM and System Metrics**

### **Memory Usage**
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.memory.max
curl http://localhost:8080/actuator/metrics/jvm.memory.committed
```

### **Thread Count**
```bash
curl http://localhost:8080/actuator/metrics/jvm.threads.live
curl http://localhost:8080/actuator/metrics/jvm.threads.daemon
curl http://localhost:8080/actuator/metrics/jvm.threads.peak
```

### **CPU Usage**
```bash
curl http://localhost:8080/actuator/metrics/system.cpu.usage
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

### **Garbage Collection**
```bash
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
curl http://localhost:8080/actuator/metrics/jvm.gc.memory.allocated
```

---

## 🎯 **Monitoring Best Practices**

### **1. Set Up Alerts**
Configure Prometheus alerts for:
- Connection failures
- No price updates
- High latency
- Dropped updates
- High memory/CPU usage

### **2. Create Dashboards**
Build Grafana dashboards showing:
- Real-time price update rate
- API request patterns
- System health metrics
- Backpressure status

### **3. Log Aggregation**
- Collect logs in centralized system (ELK, Splunk)
- Set up log-based alerts
- Monitor error patterns

### **4. Regular Health Checks**
```bash
# Every minute
curl http://localhost:8080/actuator/health

# Check key metrics
curl http://localhost:8080/actuator/metrics/pricing.connection.status
curl http://localhost:8080/actuator/metrics/pricing.updates.count
```

---

## 🔍 **Troubleshooting with Metrics**

### **Issue: No Prices Available**

**Check:**
```bash
# 1. Is service connected?
curl http://localhost:8080/actuator/metrics/pricing.connection.status
# Should be 1.0

# 2. Are there subscriptions?
curl http://localhost:8080/actuator/metrics/pricing.subscriptions.active
# Should be > 0

# 3. Are updates being received?
curl http://localhost:8080/actuator/metrics/pricing.updates.count
# Should be increasing

# 4. Is backpressure processing?
curl http://localhost:8080/actuator/metrics/pricing.backpressure.processed
# Should be increasing
```

### **Issue: High Latency**

**Check:**
```bash
# API response time
curl http://localhost:8080/actuator/metrics/pricing.api.response.time

# Update latency
curl http://localhost:8080/actuator/metrics/pricing.updates.latency

# Backpressure utilization
curl http://localhost:8080/actuator/metrics/pricing.backpressure.utilization
```

### **Issue: Dropped Updates**

**Check:**
```bash
# How many dropped?
curl http://localhost:8080/actuator/metrics/pricing.backpressure.dropped

# Queue utilization
curl http://localhost:8080/actuator/metrics/pricing.backpressure.utilization

# If > 0.8, increase queue size or processing threads
```

---

## 📱 **Monitoring Tools**

### **1. Actuator Endpoints** (Built-in)
- `/actuator/health` - Health status
- `/actuator/metrics` - All metrics
- `/actuator/metrics/{name}` - Specific metric
- `/actuator/prometheus` - Prometheus format

### **2. Prometheus** (Time-series DB)
- Scrapes metrics every 15 seconds
- Stores historical data
- Provides alerting
- PromQL query language

### **3. Grafana** (Visualization)
- Beautiful dashboards
- Real-time charts
- Custom alerts
- Multiple data sources

### **4. Custom Diagnostics**
- `/api/prices/diagnostics` - Service diagnostics
- `scripts/diagnose_pricing_service.sh` - Comprehensive diagnostics
- `scripts/monitor_dashboard.sh` - Real-time dashboard

---

## 🎯 **Quick Start Guide**

### **1. Start the Service**
```bash
mvn spring-boot:run
```

### **2. Check Health**
```bash
curl http://localhost:8080/actuator/health
```

### **3. View All Metrics**
```bash
curl http://localhost:8080/actuator/metrics | jq '.names'
```

### **4. Check Specific Metrics**
```bash
# Connection status
curl http://localhost:8080/actuator/metrics/pricing.connection.status | jq '.'

# Active subscriptions
curl http://localhost:8080/actuator/metrics/pricing.subscriptions.active | jq '.'

# Price updates
curl http://localhost:8080/actuator/metrics/pricing.updates.count | jq '.'
```

### **5. Use Diagnostics**
```bash
curl http://localhost:8080/api/prices/diagnostics | jq '.'
```

### **6. Start Monitoring Stack** (Optional)
```bash
docker-compose up -d
# Access Prometheus: http://localhost:9090
# Access Grafana: http://localhost:3000
```

---

## 📚 **Additional Resources**

### **Documentation**
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/docs)
- [Prometheus](https://prometheus.io/docs/)
- [Grafana](https://grafana.com/docs/)

### **Configuration**
- `src/main/resources/application.yaml` - Actuator configuration
- `prometheus.yml` - Prometheus scrape configuration
- `prometheus-alerts.yml` - Alert rules
- `docker-compose.yml` - Monitoring stack

---

## 🎉 **Summary**

**You can access metrics through**:

1. **Actuator Endpoints**: `http://localhost:8080/actuator/metrics/*`
2. **Prometheus Format**: `http://localhost:8080/actuator/prometheus`
3. **Custom Diagnostics**: `http://localhost:8080/api/prices/diagnostics`
4. **Grafana Dashboards**: `http://localhost:3000` (after docker-compose up)
5. **Diagnostic Scripts**: `./scripts/diagnose_pricing_service.sh`

**Most useful for debugging**:
- `curl http://localhost:8080/api/prices/diagnostics | jq '.'`
- `./scripts/diagnose_pricing_service.sh`

These will show you exactly what's happening in the service! 🚀

