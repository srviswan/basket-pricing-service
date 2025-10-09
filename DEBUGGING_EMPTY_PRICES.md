# Debugging Empty snapshotByRic Map

## 🔍 **Problem Analysis**

The `snapshotByRic` map in `RefinitivEmaProvider` is empty, which means no prices are being cached. This can happen for several reasons.

## 🎯 **Root Causes and Solutions**

### 1. **Refinitiv Connection Not Established** ❌

**Symptom**: The EMA consumer fails to connect to Refinitiv

**Check logs for**:
```
❌ Failed to start Refinitiv EMA provider
```

**Possible causes**:
- Missing or invalid Refinitiv credentials
- Incorrect host/port configuration
- Network connectivity issues
- Refinitiv server is down

**Solution**:
```yaml
# In application.yaml or environment variables
refinitiv:
  host: your-refinitiv-host.com
  port: 14002
  user: your-username
  service: ELEKTRON_DD
```

**Environment variables**:
```bash
export REFINITIV_HOST=your-host.com
export REFINITIV_PORT=14002
export REFINITIV_USER=your-username
export REFINITIV_SERVICE=ELEKTRON_DD
```

---

### 2. **No Subscriptions Made** ❌

**Symptom**: The `handleByRic` map is empty

**Check logs for**:
```
RefinitivEmaProvider.subscribe() called for symbols: []
```

**Possible causes**:
- No one has called the `/api/prices/subscribe` endpoint
- Subscriptions were made but failed
- Application just started and no subscriptions exist yet

**Solution**:
```bash
# Subscribe to symbols first
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O,AAPL.O"

# Then get prices
curl "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O,AAPL.O"
```

**Check subscription status**:
```bash
curl "http://localhost:8080/api/prices/subscriptions"
```

---

### 3. **Subscription Failed** ❌

**Symptom**: Subscription was called but no handle was registered

**Check logs for**:
```
❌ Failed to subscribe to IBM.N: <error message>
```

**Possible causes**:
- Invalid symbol format
- Refinitiv service doesn't recognize the symbol
- Connection issues during subscription
- EMA consumer is null

**Solution**:
- Use correct Refinitiv RIC codes (e.g., `IBM.N`, `MSFT.O`, `AAPL.O`)
- Verify symbols are valid on Refinitiv
- Check that EMA consumer is initialized before subscribing

---

### 4. **No Price Updates Received** ❌

**Symptom**: Subscriptions are successful but no messages are received

**Check logs for**:
```
✅ Successfully subscribed to IBM.N with handle 12345
(but no "📥 Received RefreshMsg" or "📥 Received UpdateMsg" messages)
```

**Possible causes**:
- Refinitiv is not sending updates for the subscribed symbols
- EMA dispatcher thread is not running
- Market is closed (no price updates during off-hours)
- Symbols are not actively traded

**Solution**:
- Check that the EMA dispatcher thread is running
- Verify market hours for the subscribed symbols
- Check Refinitiv service status
- Try subscribing to actively traded symbols

**Check dispatcher thread**:
```bash
# Look for "EMA dispatcher thread started" in logs
grep "EMA dispatcher" logs/application.log
```

---

### 5. **Backpressure Queue Not Processing** ❌

**Symptom**: Messages are received but not processed

**Check logs for**:
```
✅ Price update task queued for IBM.N
(but no "💾 Stored price snapshot" messages)
```

**Possible causes**:
- BackpressureManager is not processing the queue
- Processing thread is blocked or stopped
- Queue is full and updates are being dropped

**Solution**:
- Check BackpressureManager logs
- Verify processing thread is running
- Check queue utilization metrics

**Check backpressure status**:
```bash
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.utilization"
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.dropped"
```

---

### 6. **Payload Parsing Issues** ❌

**Symptom**: Messages are received but payload is invalid

**Check logs for**:
```
⚠️  Payload is null for symbol: IBM.N
⚠️  Payload is not FIELD_LIST for symbol: IBM.N
```

**Possible causes**:
- Refinitiv is sending non-standard message formats
- Payload structure has changed
- Incorrect field names in parsing logic

**Solution**:
- Review Refinitiv EMA documentation for message formats
- Update field name mappings if needed
- Add support for additional message types

---

## 🛠️ **Debugging Steps**

### Step 1: Check Application Startup
```bash
# Look for successful startup
grep "Refinitiv EMA provider started successfully" logs/application.log

# Check for connection errors
grep "Failed to start Refinitiv EMA provider" logs/application.log
```

### Step 2: Check Subscriptions
```bash
# Check if subscriptions were made
grep "RefinitivEmaProvider.subscribe()" logs/application.log

# Check subscription success
grep "Successfully subscribed" logs/application.log

# Check current subscriptions
curl "http://localhost:8080/api/prices/subscriptions"
```

### Step 3: Check Message Reception
```bash
# Check for received messages
grep "Received RefreshMsg\|Received UpdateMsg" logs/application.log

# Check message processing
grep "handleMessage() called" logs/application.log

# Check field parsing
grep "Parsed .* fields" logs/application.log
```

### Step 4: Check Price Storage
```bash
# Check if prices are being stored
grep "Stored price snapshot" logs/application.log

# Check map size
grep "snapshotByRic map now contains" logs/application.log
```

### Step 5: Check Backpressure
```bash
# Check backpressure status
curl "http://localhost:8080/api/prices/subscribe?symbols=IBM.N" | jq '.backpressureStatus'

# Check metrics
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.utilization"
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.processed"
curl "http://localhost:8080/actuator/metrics/pricing.backpressure.dropped"
```

---

## 🔧 **Quick Diagnostic Script**

```bash
#!/bin/bash

echo "=== Refinitiv EMA Provider Diagnostics ==="

echo -e "\n1. Check Application Health:"
curl -s http://localhost:8080/actuator/health | jq '.'

echo -e "\n2. Check Current Subscriptions:"
curl -s http://localhost:8080/api/prices/subscriptions | jq '.'

echo -e "\n3. Subscribe to Test Symbols:"
curl -s -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O,AAPL.O" | jq '.'

echo -e "\n4. Wait for price updates (5 seconds)..."
sleep 5

echo -e "\n5. Get Latest Prices:"
curl -s "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O,AAPL.O" | jq '.'

echo -e "\n6. Check Metrics:"
echo "Active Subscriptions:"
curl -s http://localhost:8080/actuator/metrics/pricing.subscriptions.active | jq '.measurements[0].value'

echo "Price Updates:"
curl -s http://localhost:8080/actuator/metrics/pricing.updates.count | jq '.measurements[0].value'

echo "Connection Status:"
curl -s http://localhost:8080/actuator/metrics/pricing.connection.status | jq '.measurements[0].value'
```

---

## 📊 **Expected Log Flow (When Working Correctly)**

```
1. Application Startup:
   🚀 Starting Refinitiv EMA provider...
   Configuration: host=..., port=..., user=..., service=...
   Creating OmmConsumer with USER_DISPATCH operation model...
   ✅ OmmConsumer created successfully
   Starting EMA dispatcher thread...
   EMA dispatcher thread started
   ✅ Refinitiv EMA provider started successfully

2. Subscription Request:
   RefinitivEmaProvider.subscribe() called for symbols: [IBM.N, MSFT.O]
   ✅ Successfully subscribed to IBM.N with handle 12345
   ✅ Successfully subscribed to MSFT.O with handle 12346
   Subscription complete. Total subscriptions: 2

3. Price Updates Received:
   📥 Received RefreshMsg for symbol: IBM.N
   📨 handleMessage() called for symbol: IBM.N
   Parsed 25 fields for IBM.N: BID=150.25, ASK=150.30, LAST=150.27
   ✅ Price update task queued for IBM.N
   💾 Stored price snapshot for IBM.N: BID=150.25, ASK=150.30, LAST=150.27
   snapshotByRic map now contains 1 entries
   📢 Published PriceUpdateEvent for IBM.N

4. Get Prices Request:
   RefinitivEmaProvider.getLatestPrices() called for symbols: [IBM.N]
   Current snapshotByRic map size: 1, keys: [IBM.N]
   Found price snapshot for IBM.N: PriceSnapshot(symbol=IBM.N, bid=150.25, ...)
   Returning 1 price snapshots out of 1 requested symbols
```

---

## 🚨 **Common Issues**

### Issue 1: "Cannot subscribe: EMA consumer is not initialized!"
**Cause**: The `start()` method failed, so `consumer` is null
**Solution**: Fix the Refinitiv connection configuration

### Issue 2: "No price snapshot found for symbol: IBM.N"
**Cause**: Symbol was never subscribed or no updates received
**Solution**: 
1. Subscribe to the symbol first
2. Wait for price updates to arrive
3. Verify symbol is valid and market is open

### Issue 3: "Backpressure: queue full, updating directly"
**Cause**: Too many price updates, queue is overwhelmed
**Solution**: 
1. Increase queue size in BackpressureManager
2. Increase processing thread count
3. Reduce subscription count

### Issue 4: No RefreshMsg or UpdateMsg received
**Cause**: Refinitiv is not sending updates
**Solution**:
1. Verify market is open
2. Check Refinitiv service status
3. Verify symbols are actively traded
4. Check EMA dispatcher thread is running

---

## 🎯 **Recommended Actions**

### For Development/Testing:
1. **Mock the Refinitiv provider** for tests
2. **Use test data** instead of real Refinitiv connection
3. **Pre-populate** the `snapshotByRic` map with test data

### For Production:
1. **Verify Refinitiv credentials** are correct
2. **Check network connectivity** to Refinitiv servers
3. **Monitor connection status** via actuator endpoints
4. **Set up alerts** for connection failures
5. **Implement health checks** that verify price updates are being received

---

## 📝 **Next Steps**

1. **Run the application** with the enhanced logging
2. **Check the logs** for the specific error messages
3. **Follow the debugging steps** above
4. **Verify each component** in the chain is working

The enhanced logging will now show exactly where the issue is occurring in the price update flow.

