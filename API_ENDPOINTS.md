# API Endpoints Reference

## 🌐 **All Available Endpoints**

### **Base URL**
```
http://localhost:8080
```

---

## 📊 **Pricing REST API Endpoints**

### **1. Get Prices**
```http
GET /api/prices?symbols=IBM.N,MSFT.O,AAPL.O
```

**Example:**
```bash
curl "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O,AAPL.O"
```

**Response:**
```json
{
  "IBM.N": {
    "symbol": "IBM.N",
    "bid": 150.25,
    "ask": 150.30,
    "last": 150.27,
    "timestamp": "2025-10-09T18:00:00Z"
  },
  "MSFT.O": {
    "symbol": "MSFT.O",
    "bid": 380.50,
    "ask": 380.55,
    "last": 380.52,
    "timestamp": "2025-10-09T18:00:00Z"
  }
}
```

---

### **2. Subscribe to Symbols**
```http
POST /api/prices/subscribe?symbols=IBM.N,MSFT.O
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O"
```

**Response:**
```json
{
  "subscribed": ["IBM.N", "MSFT.O"],
  "totalSubscriptions": 2,
  "backpressureStatus": {
    "queueUtilization": 0.15,
    "processedUpdates": 1234,
    "droppedUpdates": 0
  }
}
```

---

### **3. Unsubscribe from Symbols**
```http
DELETE /api/prices/unsubscribe?symbols=IBM.N
```

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/prices/unsubscribe?symbols=IBM.N"
```

**Response:**
```json
{
  "unsubscribed": ["IBM.N"],
  "remainingSubscriptions": 1
}
```

---

### **4. Get Current Subscriptions**
```http
GET /api/prices/subscriptions
```

**Example:**
```bash
curl "http://localhost:8080/api/prices/subscriptions"
```

**Response:**
```json
{
  "subscribedSymbols": ["IBM.N", "MSFT.O", "AAPL.O"],
  "count": 3
}
```

---

### **5. Service Diagnostics** ⭐ **NEW**
```http
GET /api/prices/diagnostics
```

**Example:**
```bash
curl "http://localhost:8080/api/prices/diagnostics"
```

**Response:**
```json
{
  "marketDataProviderClass": "com.srviswan.basketpricing.resilience.ResilientMarketDataProvider",
  "isResilientProvider": true,
  "subscribedSymbols": ["IBM.N", "MSFT.O"],
  "subscriptionCount": 2,
  "availablePrices": 2,
  "priceKeys": ["IBM.N", "MSFT.O"],
  "backpressureUtilization": 0.15,
  "backpressureProcessed": 1234,
  "backpressureDropped": 0
}
```

**What it shows**:
- Which provider is being used (verifies architecture)
- Current subscriptions
- Available prices
- Backpressure status

---

## 🔧 **Actuator Endpoints**

### **Health Check**
```http
GET /actuator/health
```

**Example:**
```bash
curl "http://localhost:8080/actuator/health"
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

### **All Metrics (List)**
```http
GET /actuator/metrics
```

**Example:**
```bash
curl "http://localhost:8080/actuator/metrics"
```

**Response:**
```json
{
  "names": [
    "pricing.api.requests",
    "pricing.subscriptions.active",
    "pricing.updates.count",
    "pricing.connection.status",
    "pricing.backpressure.utilization",
    "jvm.memory.used",
    "system.cpu.usage",
    ...
  ]
}
```

---

### **Specific Metric**
```http
GET /actuator/metrics/{metric-name}
```

**Examples:**
```bash
# Connection status
curl "http://localhost:8080/actuator/metrics/pricing.connection.status"

# Active subscriptions
curl "http://localhost:8080/actuator/metrics/pricing.subscriptions.active"

# Price updates count
curl "http://localhost:8080/actuator/metrics/pricing.updates.count"

# Backpressure utilization
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.utilization"
```

---

### **Prometheus Metrics**
```http
GET /actuator/prometheus
```

**Example:**
```bash
curl "http://localhost:8080/actuator/prometheus"
```

**Response:** (Prometheus text format)
```
# HELP pricing_api_requests_total Total API requests
# TYPE pricing_api_requests_total counter
pricing_api_requests_total{endpoint="getPrices",} 1234.0

# HELP pricing_subscriptions_active Active subscriptions
# TYPE pricing_subscriptions_active gauge
pricing_subscriptions_active 15.0
...
```

---

## 🎮 **gRPC Endpoints**

### **gRPC Server**
```
localhost:9090
```

### **Available RPC Methods**

#### **1. GetPrices**
```protobuf
rpc GetPrices(GetPricesRequest) returns (GetPricesResponse);
```

**Example (using grpcurl)**:
```bash
grpcurl -plaintext -d '{"symbols": ["IBM.N", "MSFT.O"]}' \
  localhost:9090 com.srviswan.basketpricing.grpc.PricingService/GetPrices
```

---

#### **2. Subscribe**
```protobuf
rpc Subscribe(SubscribeRequest) returns (SubscribeResponse);
```

**Example:**
```bash
grpcurl -plaintext -d '{"symbols": ["IBM.N", "MSFT.O"]}' \
  localhost:9090 com.srviswan.basketpricing.grpc.PricingService/Subscribe
```

---

#### **3. Unsubscribe**
```protobuf
rpc Unsubscribe(UnsubscribeRequest) returns (UnsubscribeResponse);
```

**Example:**
```bash
grpcurl -plaintext -d '{"symbols": ["IBM.N"]}' \
  localhost:9090 com.srviswan.basketpricing.grpc.PricingService/Unsubscribe
```

---

#### **4. GetSubscriptions**
```protobuf
rpc GetSubscriptions(GetSubscriptionsRequest) returns (GetSubscriptionsResponse);
```

**Example:**
```bash
grpcurl -plaintext -d '{}' \
  localhost:9090 com.srviswan.basketpricing.grpc.PricingService/GetSubscriptions
```

---

#### **5. StreamPrices** (Server Streaming)
```protobuf
rpc StreamPrices(StreamPricesRequest) returns (stream PriceUpdate);
```

**Example:**
```bash
grpcurl -plaintext -d '{"symbols": ["IBM.N"]}' \
  localhost:9090 com.srviswan.basketpricing.grpc.PricingService/StreamPrices
```

---

## 🧪 **Testing Endpoints**

### **Using curl (REST)**

```bash
# Health check
curl http://localhost:8080/actuator/health

# Subscribe
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O"

# Get prices
curl "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O"

# Get subscriptions
curl "http://localhost:8080/api/prices/subscriptions"

# Diagnostics
curl "http://localhost:8080/api/prices/diagnostics"

# Unsubscribe
curl -X DELETE "http://localhost:8080/api/prices/unsubscribe?symbols=IBM.N"
```

---

### **Using the Test Clients**

#### **REST Client**
```bash
cd /Users/sreekumarviswanathan/ai-projects-cursor/basket-pricing-service
mvn exec:java -Dexec.mainClass="com.srviswan.basketpricing.examples.RestApiClientExample"
```

#### **gRPC Client**
```bash
mvn exec:java -Dexec.mainClass="com.srviswan.basketpricing.examples.GrpcClientExample"
```

---

## 🔍 **Endpoint Discovery**

### **List All REST Endpoints**
```bash
# Using Spring Boot Actuator (if mappings endpoint is enabled)
curl http://localhost:8080/actuator/mappings | jq '.'
```

### **List gRPC Services**
```bash
# Using grpcurl
grpcurl -plaintext localhost:9090 list

# List methods in a service
grpcurl -plaintext localhost:9090 list com.srviswan.basketpricing.grpc.PricingService
```

---

## ⚠️ **Common Mistakes**

### **Wrong URLs:**

❌ **WRONG:**
```bash
# Using dot instead of slash
curl "http://localhost:8080/api/prices.diagnostics"

# Missing /api prefix
curl "http://localhost:8080/prices/diagnostics"

# Wrong port
curl "http://localhost:9090/api/prices/diagnostics"
```

✅ **CORRECT:**
```bash
curl "http://localhost:8080/api/prices/diagnostics"
```

---

### **Service Not Running:**

If you get "Connection refused":
```bash
# Check if service is running
curl http://localhost:8080/actuator/health

# If not running, start it:
mvn spring-boot:run
```

---

## 📋 **Complete Endpoint Summary**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/prices?symbols=...` | Get current prices |
| `POST` | `/api/prices/subscribe?symbols=...` | Subscribe to symbols |
| `DELETE` | `/api/prices/unsubscribe?symbols=...` | Unsubscribe from symbols |
| `GET` | `/api/prices/subscriptions` | Get current subscriptions |
| `GET` | `/api/prices/diagnostics` | Get service diagnostics |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/metrics` | List all metrics |
| `GET` | `/actuator/metrics/{name}` | Get specific metric |
| `GET` | `/actuator/prometheus` | Prometheus format metrics |

---

## 🎯 **Quick Test**

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing all REST endpoints..."

echo -e "\n1. Health Check:"
curl -s "$BASE_URL/actuator/health" | jq '.status'

echo -e "\n2. Diagnostics:"
curl -s "$BASE_URL/api/prices/diagnostics" | jq '.'

echo -e "\n3. Subscriptions:"
curl -s "$BASE_URL/api/prices/subscriptions" | jq '.'

echo -e "\n4. Subscribe:"
curl -s -X POST "$BASE_URL/api/prices/subscribe?symbols=IBM.N,MSFT.O" | jq '.'

echo -e "\n5. Get Prices:"
curl -s "$BASE_URL/api/prices?symbols=IBM.N,MSFT.O" | jq '.'

echo -e "\nAll endpoints tested!"
```

Save as `scripts/test_endpoints.sh` and run:
```bash
chmod +x scripts/test_endpoints.sh
./scripts/test_endpoints.sh
```

---

## 🎉 **Quick Reference**

**Most useful endpoints for debugging**:

```bash
# 1. Check if service is running
curl http://localhost:8080/actuator/health

# 2. Check service diagnostics
curl http://localhost:8080/api/prices/diagnostics | jq '.'

# 3. Check subscriptions
curl http://localhost:8080/api/prices/subscriptions

# 4. Check metrics
curl http://localhost:8080/actuator/metrics/pricing.connection.status
```

**Remember**: Use forward slash `/` not dot `.` in URLs!

