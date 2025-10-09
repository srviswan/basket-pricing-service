# Troubleshooting Guide

## 🔧 **Common Issues and Solutions**

### **Issue 1: "PricingServiceGrpc does not exist" after `mvn clean`**

#### **Problem:**
```
[ERROR] cannot find symbol
  symbol:   class PricingServiceGrpc
  location: package com.srviswan.basketpricing.grpc
```

#### **Root Cause:**
- `mvn clean` deletes the entire `target/` directory
- This includes generated gRPC/Protobuf files
- Sometimes the protobuf-maven-plugin doesn't regenerate files properly
- Compilation fails because generated files are missing

#### **Solutions:**

**Solution 1: Use the Build Script** ⭐ **RECOMMENDED**
```bash
./scripts/build.sh
```
This script:
- Cleans only necessary directories
- Explicitly generates protobuf files first
- Then compiles the project
- Handles the issue automatically

**Solution 2: Generate Protobuf Files First**
```bash
# Instead of:
mvn clean compile

# Do this:
rm -rf target/classes target/test-classes
mvn protobuf:compile protobuf:compile-custom
mvn compile -DskipTests
```

**Solution 3: Use Package Instead of Compile**
```bash
# This ensures proper build order
mvn clean package -DskipTests
```

**Solution 4: Avoid Clean**
```bash
# Just compile without cleaning (faster anyway)
mvn compile -DskipTests
```

**Solution 5: Full Rebuild**
```bash
# Nuclear option - delete everything and rebuild
rm -rf target/
mvn clean install -DskipTests
```

---

### **Issue 2: "snapshotByRic map is empty" / "Prices are null"**

#### **Problem:**
- `GET /api/prices?symbols=IBM.N` returns empty `{}`
- `snapshotByRic` map has no entries

#### **Root Causes:**

**Cause 1: No Subscription Made**
```bash
# Check subscriptions
curl http://localhost:8080/api/prices/subscriptions

# If empty, subscribe first
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O"
```

**Cause 2: Refinitiv Connection Failed**
```bash
# Check connection status
curl http://localhost:8080/actuator/metrics/pricing.connection.status

# If 0.0, check logs for connection errors
grep "Failed to start Refinitiv" logs/application.log
```

**Cause 3: Cold Start - No Updates Received Yet**
```bash
# Subscribe first
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N"

# Wait 5-10 seconds for initial RefreshMsg
sleep 10

# Then get prices
curl "http://localhost:8080/api/prices?symbols=IBM.N"
```

**Cause 4: Market is Closed**
- Check if the market is open for the symbols you're requesting
- US stocks: 9:30 AM - 4:00 PM ET (Mon-Fri)
- Try subscribing to actively traded symbols

**Cause 5: BackpressureManager Not Processing** (FIXED)
- This was a bug - now fixed with processing threads
- Check: `curl http://localhost:8080/actuator/metrics/pricing.backpressure.processed`
- Should be increasing if updates are being processed

#### **Diagnostic Steps:**
```bash
# 1. Run comprehensive diagnostics
./scripts/diagnose_pricing_service.sh

# 2. Check service diagnostics
curl http://localhost:8080/api/prices/diagnostics | jq '.'

# 3. Check metrics
./scripts/check_metrics.sh
```

---

### **Issue 3: "Circular dependency" errors**

#### **Problem:**
```
The dependencies of some of the beans in the application context form a cycle
```

#### **Status:** ✅ **FIXED**

The circular dependency issue has been resolved using:
- `@Primary` on `ResilientMarketDataProvider`
- `@Qualifier("refinitivEmaProvider")` on delegate injection
- Event-driven architecture for price updates

**Verification:**
```bash
# Check that ResilientMarketDataProvider is being used
curl http://localhost:8080/api/prices/diagnostics | jq '.marketDataProviderClass'

# Should return: "com.srviswan.basketpricing.resilience.ResilientMarketDataProvider"
```

---

### **Issue 4: Application fails to start - Refinitiv connection timeout**

#### **Problem:**
```
Failed to start Refinitiv EMA provider
login failed (timed out after waiting 45000 milliseconds)
```

#### **Root Cause:**
- No valid Refinitiv server at the configured host:port
- Invalid credentials
- Network connectivity issues

#### **Solutions:**

**For Development/Testing:**
Create a mock provider (future enhancement) or configure valid credentials.

**For Production:**
```yaml
# application.yaml
refinitiv:
  host: ${REFINITIV_HOST:your-refinitiv-host.com}
  port: ${REFINITIV_PORT:14002}
  user: ${REFINITIV_USER:your-username}
  service: ${REFINITIV_SERVICE:ELEKTRON_DD}
```

**Set environment variables:**
```bash
export REFINITIV_HOST=your-real-host.com
export REFINITIV_PORT=14002
export REFINITIV_USER=your-username
export REFINITIV_SERVICE=ELEKTRON_DD
```

**Make connection failure non-fatal** (workaround):
- Modify `RefinitivEmaProvider.start()` to catch and log errors instead of throwing
- Application will start but subscriptions will fail gracefully

---

### **Issue 5: High backpressure / Dropped updates**

#### **Problem:**
```bash
curl http://localhost:8080/actuator/metrics/pricing.backpressure.dropped
# Returns non-zero value
```

#### **Root Cause:**
- Too many price updates for the queue to handle
- Processing threads can't keep up
- Queue is full (max 1000 items)

#### **Solutions:**

**Solution 1: Increase Queue Size**
```java
// In BackpressureManager.java
private final BlockingQueue<PriceUpdateTask> updateQueue = 
    new LinkedBlockingQueue<>(5000);  // Increase from 1000 to 5000
```

**Solution 2: Add More Processing Threads**
```java
// In BackpressureManager.java
for (int i = 0; i < 10; i++) {  // Increase from 5 to 10
    processingExecutor.submit(() -> processQueue(threadNum));
}
```

**Solution 3: Reduce Subscriptions**
```bash
# Unsubscribe from less important symbols
curl -X DELETE "http://localhost:8080/api/prices/unsubscribe?symbols=SYMBOL1,SYMBOL2"
```

**Solution 4: Optimize Processing**
- Reduce stale task timeout (currently 5 seconds)
- Increase processing semaphore permits
- Use faster data structures

---

### **Issue 6: Test failures - ApplicationContext cannot load**

#### **Problem:**
```
Failed to load ApplicationContext
Caused by: login failed (timed out after waiting 45000 milliseconds)
```

#### **Root Cause:**
- Tests use `@SpringBootTest` which loads the full application context
- `RefinitivEmaProvider` tries to connect to Refinitiv during `@PostConstruct`
- No Refinitiv server available in test environment
- Context loading fails

#### **Solutions:**

**Solution 1: Use Mock Provider for Tests**
```java
@Profile("test")
@Component("refinitivEmaProvider")
@Primary
public class MockMarketDataProvider implements MarketDataProvider {
    // Mock implementation that doesn't connect to Refinitiv
}
```

**Solution 2: Skip Provider Initialization in Tests**
```yaml
# src/test/resources/application-test.yaml
spring:
  autoconfigure:
    exclude:
      - com.srviswan.basketpricing.marketdata.refinitiv.RefinitivEmaProvider
```

**Solution 3: Use MockBean**
```java
@SpringBootTest
@ActiveProfiles("test")
public class MyTest {
    
    @MockBean
    private RefinitivEmaProvider refinitivEmaProvider;
    
    // Test will use mock instead of real provider
}
```

---

### **Issue 7: Port already in use**

#### **Problem:**
```
Port 8080 is already in use
```

#### **Solutions:**

**Solution 1: Kill existing process**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

**Solution 2: Use different port**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Solution 3: Configure in application.yaml**
```yaml
server:
  port: ${SERVER_PORT:8081}
```

---

### **Issue 8: gRPC port already in use**

#### **Problem:**
```
Port 9090 is already in use
```

#### **Solutions:**

**Configure different port:**
```yaml
# application.yaml
grpc:
  server:
    port: ${GRPC_PORT:9091}
```

**Or use environment variable:**
```bash
export GRPC_PORT=9091
mvn spring-boot:run
```

---

## 🛠️ **Diagnostic Tools**

### **Quick Health Check**
```bash
curl http://localhost:8080/actuator/health
```

### **Service Diagnostics**
```bash
curl http://localhost:8080/api/prices/diagnostics | jq '.'
```

### **Comprehensive Diagnostics**
```bash
./scripts/diagnose_pricing_service.sh
```

### **Metrics Summary**
```bash
./scripts/check_metrics.sh
```

### **Real-Time Monitoring**
```bash
./scripts/monitor_dashboard.sh
```

---

## 📊 **Useful Commands**

### **Build Commands**
```bash
# Reliable build
./scripts/build.sh

# Standard build
mvn clean package -DskipTests

# Compile only
mvn compile -DskipTests

# Run tests
mvn test

# Skip tests
mvn package -DskipTests
```

### **Run Commands**
```bash
# Run with Maven
mvn spring-boot:run

# Run with custom port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Run with profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Run packaged JAR
java -jar target/basket-pricing-service-0.1.0-SNAPSHOT.jar
```

### **Docker Commands**
```bash
# Build Docker image
docker build -t basket-pricing-service .

# Run container
docker run -p 8080:8080 -p 9090:9090 basket-pricing-service

# Run with environment variables
docker run -p 8080:8080 \
  -e REFINITIV_HOST=your-host \
  -e REFINITIV_USER=your-user \
  basket-pricing-service

# Run monitoring stack
docker-compose up -d

# View logs
docker-compose logs -f basket-pricing-service

# Stop stack
docker-compose down
```

---

## 🔍 **Log Analysis**

### **Check Application Logs**
```bash
# If using file logging
tail -f logs/application.log

# Check for errors
grep ERROR logs/application.log

# Check Refinitiv connection
grep "Refinitiv EMA provider" logs/application.log

# Check subscriptions
grep "subscribe" logs/application.log | grep -i "success"

# Check price updates
grep "Received RefreshMsg\|Received UpdateMsg" logs/application.log
```

### **Check Specific Components**
```bash
# BackpressureManager
grep "BackpressureManager\|backpressure-processor" logs/application.log

# ResilientMarketDataProvider
grep "ResilientMarketDataProvider" logs/application.log

# PricingController
grep "PricingController" logs/application.log
```

---

## 📞 **Getting Help**

### **If you're stuck:**

1. **Run diagnostics first**
   ```bash
   ./scripts/diagnose_pricing_service.sh
   ```

2. **Check the relevant documentation**
   - `MONITORING_GUIDE.md` - Metrics and monitoring
   - `DEBUGGING_EMPTY_PRICES.md` - Why prices are empty
   - `COLD_START_ISSUE.md` - Timing issues
   - `BACKPRESSURE_FIX.md` - Backpressure processing
   - `BEAN_RESOLUTION_VERIFICATION.md` - Dependency injection
   - `ARCHITECTURE_REVIEW.md` - Overall architecture

3. **Check metrics and logs**
   ```bash
   ./scripts/check_metrics.sh
   grep ERROR logs/application.log
   ```

4. **Verify configuration**
   ```bash
   # Check Refinitiv config
   grep -A 5 "refinitiv:" src/main/resources/application.yaml
   
   # Check environment variables
   env | grep REFINITIV
   ```

---

## 🎯 **Quick Fixes**

### **Problem: Build fails**
```bash
./scripts/build.sh
```

### **Problem: No prices available**
```bash
# 1. Subscribe first
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O"

# 2. Wait for updates
sleep 10

# 3. Get prices
curl "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O"
```

### **Problem: Want to see what's happening**
```bash
# Real-time dashboard
./scripts/monitor_dashboard.sh
```

### **Problem: Need detailed diagnostics**
```bash
./scripts/diagnose_pricing_service.sh
```

---

## 📚 **Additional Resources**

- **Refinitiv EMA Documentation**: https://developers.refinitiv.com/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **gRPC Java**: https://grpc.io/docs/languages/java/
- **Resilience4j**: https://resilience4j.readme.io/

---

**If none of these solutions work, check the project's GitHub issues or contact the development team.**

