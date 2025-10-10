# 🛑 Graceful Shutdown Guide

## **Overview**

Graceful shutdown ensures that your application stops cleanly without dropping in-flight requests or losing data.

---

## ✅ **Configuration**

### **application.yaml**
```yaml
server:
  shutdown: graceful  # Enable graceful shutdown

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Wait up to 30s for shutdown
```

---

## 🔄 **How It Works**

### **Shutdown Sequence:**

1. **Shutdown signal received** (SIGTERM, Ctrl+C, etc.)
2. **Stop accepting new requests**
   - Server stops accepting new HTTP connections
   - gRPC server stops accepting new RPCs
3. **Wait for in-flight requests** (up to 30 seconds)
   - Complete active HTTP requests
   - Complete active gRPC streams
   - Finish background tasks
4. **Clean up resources**
   - Close database connections
   - Flush caches
   - Stop scheduled tasks
   - Close Refinitiv EMA connection
5. **Exit application**

---

## 💻 **How to Shutdown**

### **Method 1: SIGTERM (Recommended)**
```bash
# Find the process ID
ps aux | grep basket-pricing

# Send SIGTERM signal
kill <PID>

# Or use pkill
pkill -f basket-pricing
```

### **Method 2: Spring Boot Actuator**
```bash
# Enable shutdown endpoint (add to application.yaml)
management:
  endpoint:
    shutdown:
      enabled: true
  endpoints:
    web:
      exposure:
        include: shutdown

# Trigger shutdown via REST API
curl -X POST http://localhost:8081/actuator/shutdown
```

### **Method 3: Ctrl+C (Development)**
```bash
# In terminal where app is running
Ctrl+C
```

### **Method 4: Docker**
```bash
# Graceful stop (sends SIGTERM, waits, then SIGKILL)
docker stop basket-pricing-service

# With custom timeout
docker stop -t 60 basket-pricing-service  # Wait 60 seconds

# Docker Compose
docker-compose stop
```

### **Method 5: Kubernetes**
```yaml
# In deployment.yaml
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 60  # Wait 60s before SIGKILL
      containers:
      - name: basket-pricing-service
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 5"]  # Delay before shutdown
```

---

## 📊 **Monitoring Shutdown**

### **Application Logs**
```bash
# Watch for shutdown messages
tail -f logs/application.log | grep -i shutdown

# Expected log messages:
# "Commencing graceful shutdown. Waiting for active requests to complete"
# "Closing Refinitiv EMA consumer..."
# "Stopping BackpressureManager..."
# "BackpressureManager stopped"
```

### **Check Active Connections**
```bash
# Before shutdown - see active connections
netstat -an | grep :8080 | grep ESTABLISHED

# During shutdown - connections should decrease
watch -n 1 "netstat -an | grep :8080 | grep ESTABLISHED | wc -l"
```

---

## ⚙️ **Timeout Configuration**

### **Adjust Timeout Based on Your Needs:**

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Default: 30 seconds
```

**Recommendations:**
- **Fast APIs**: 10-15 seconds
- **Long-running requests**: 30-60 seconds
- **Streaming/WebSocket**: 60-120 seconds
- **Batch processing**: 300+ seconds

---

## 🎯 **What Gets Cleaned Up**

### **Your Application Components:**

1. **Refinitiv EMA Provider**
   ```java
   @PreDestroy
   public void stop() {
       if (consumer != null) {
           consumer.uninitialize();
       }
   }
   ```

2. **BackpressureManager**
   ```java
   @PreDestroy
   public void stop() {
       processingExecutor.shutdown();
       processingExecutor.awaitTermination(5, TimeUnit.SECONDS);
   }
   ```

3. **gRPC Server**
   - Completes active RPCs
   - Stops accepting new RPCs
   - Closes server socket

4. **HTTP Server (Tomcat)**
   - Completes active requests
   - Stops accepting new connections
   - Closes thread pools

5. **Spring Context**
   - Destroys all beans with `@PreDestroy`
   - Closes application context
   - Releases resources

---

## 🚨 **Handling Timeout**

### **If Shutdown Takes Too Long:**

**What happens after timeout:**
1. Spring logs warning: "Graceful shutdown timeout exceeded"
2. Application forcefully shuts down
3. In-flight requests may be interrupted

**Solutions:**
```yaml
# Increase timeout
spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s

# Or configure component-specific timeouts
refinitiv:
  shutdown-timeout: 10s

backpressure:
  shutdown-timeout: 5s
```

---

## 🔍 **Testing Graceful Shutdown**

### **Test Script:**
```bash
#!/bin/bash
# test-graceful-shutdown.sh

echo "Starting application..."
java -jar target/basket-pricing-service-0.1.0-SNAPSHOT.jar &
APP_PID=$!

echo "Waiting for startup..."
sleep 10

echo "Sending test requests..."
for i in {1..10}; do
    curl -s http://localhost:8080/api/prices/subscriptions &
done

echo "Initiating graceful shutdown..."
kill -TERM $APP_PID

echo "Monitoring shutdown..."
tail -f logs/application.log | grep -i "shutdown\|stopped" &
TAIL_PID=$!

# Wait for process to exit
wait $APP_PID
EXIT_CODE=$?

kill $TAIL_PID

echo "Application exited with code: $EXIT_CODE"
```

### **Expected Output:**
```
Commencing graceful shutdown. Waiting for active requests to complete
Stopping BackpressureManager...
✅ BackpressureManager stopped
Closing Refinitiv EMA consumer...
Shutting down gRPC server...
Application exited with code: 0
```

---

## 🐳 **Docker Graceful Shutdown**

### **docker-compose.yml Configuration:**
```yaml
services:
  basket-pricing-service:
    stop_grace_period: 60s  # Wait 60s before SIGKILL
    stop_signal: SIGTERM    # Send SIGTERM (default)
```

### **Dockerfile STOPSIGNAL:**
```dockerfile
# Set stop signal (optional, SIGTERM is default)
STOPSIGNAL SIGTERM
```

### **Docker Stop Behavior:**
1. `docker stop` sends SIGTERM
2. Waits for `stop_grace_period` (default: 10s)
3. If still running, sends SIGKILL (force kill)

---

## ⚡ **Best Practices**

### **1. Set Appropriate Timeouts**
```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Match your longest request

server:
  tomcat:
    connection-timeout: 20000  # Should be less than shutdown timeout
```

### **2. Implement @PreDestroy Methods**
```java
@Component
public class MyService {
    @PreDestroy
    public void cleanup() {
        // Clean up resources
        // Close connections
        // Save state
    }
}
```

### **3. Handle Shutdown in Long-Running Tasks**
```java
@Component
public class BackgroundTask {
    private volatile boolean running = true;
    
    @PreDestroy
    public void stop() {
        running = false;
    }
    
    public void process() {
        while (running) {
            // Check running flag regularly
            if (!running) break;
            // Do work...
        }
    }
}
```

### **4. Configure Load Balancer**
```yaml
# Kubernetes example
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5

# On shutdown, readiness probe fails
# Load balancer stops sending traffic
# Then graceful shutdown begins
```

### **5. Use Circuit Breakers**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      marketDataProvider:
        # Circuit opens on failures
        # Prevents cascading failures during shutdown
```

---

## 📋 **Troubleshooting**

### **Problem: Shutdown Takes Full Timeout**

**Cause:** Threads not responding to interrupts

**Solution:**
```java
// Check for interruption
if (Thread.currentThread().isInterrupted()) {
    return;
}

// Or use timeout on blocking operations
future.get(5, TimeUnit.SECONDS);
```

### **Problem: Requests Still Fail During Shutdown**

**Cause:** Load balancer still sending traffic

**Solution:**
1. Implement readiness probe
2. Add pre-stop hook delay
```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 5"]
```

### **Problem: Database Connections Not Closing**

**Cause:** Connection pool not shutting down

**Solution:**
```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 5000
      maximum-pool-size: 10
      # Connections close during shutdown
```

---

## 🎓 **Signal Reference**

| Signal | Behavior | Use Case |
|--------|----------|----------|
| **SIGTERM** | Graceful shutdown | Production deployments |
| **SIGINT** | Graceful shutdown | Ctrl+C in terminal |
| **SIGKILL** | Immediate kill | Force stop (no cleanup) |
| **SIGHUP** | Reload config | Some apps (not Spring Boot) |

---

## ✅ **Verification Checklist**

- [ ] `server.shutdown: graceful` configured
- [ ] `timeout-per-shutdown-phase` set appropriately
- [ ] All `@PreDestroy` methods implemented
- [ ] Long-running tasks check shutdown flag
- [ ] Load balancer configured with readiness probe
- [ ] Docker `stop_grace_period` matches timeout
- [ ] Tested shutdown under load
- [ ] Monitoring in place for shutdown events
- [ ] Logs show clean shutdown sequence

---

## 🚀 **Quick Commands**

```bash
# Start application
java -jar target/basket-pricing-service-0.1.0-SNAPSHOT.jar

# Graceful shutdown
kill -TERM $(pgrep -f basket-pricing)

# Force kill (not graceful)
kill -KILL $(pgrep -f basket-pricing)

# Docker graceful stop
docker stop basket-pricing-service

# Check if process is still running
ps aux | grep basket-pricing

# Monitor shutdown logs
tail -f logs/application.log | grep -i shutdown
```

---

## 📚 **References**

- [Spring Boot Graceful Shutdown](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.graceful-shutdown)
- [Docker Stop Behavior](https://docs.docker.com/engine/reference/commandline/stop/)
- [Kubernetes Pod Lifecycle](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)

---

**Your application is now configured for graceful shutdown!** 🎉

