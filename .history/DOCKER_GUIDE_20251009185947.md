# 🐳 Docker Deployment Guide

Complete guide for running the Basket Pricing Service in Docker.

---

## 📋 **Table of Contents**

1. [Quick Start](#quick-start)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Configuration](#configuration)
5. [Running the Service](#running-the-service)
6. [Monitoring](#monitoring)
7. [Troubleshooting](#troubleshooting)
8. [Docker Commands Reference](#docker-commands-reference)

---

## 🚀 **Quick Start**

### **1. Prerequisites**
- Docker Desktop installed and running
- At least 4GB RAM allocated to Docker
- Ports available: 8080, 8081, 9090, 3000, 6379, 9091, 9092, 29092

### **2. Configure Environment**
```bash
# Copy example environment file
cp env.example .env

# Edit .env with your Refinitiv credentials (if applicable)
# Default values work for testing
```

### **3. Start All Services**
```bash
# Start in foreground (see logs)
./scripts/docker-start.sh

# Or start in background (detached)
./scripts/docker-start.sh --detach
```

### **4. Verify Service is Running**
```bash
# Check status
./scripts/docker-status.sh

# Or directly
curl http://localhost:8080/actuator/health
```

### **5. Access Services**
- **REST API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9091

---

## 🏗️ **Architecture**

### **Services in the Stack**

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose Stack                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    Basket Pricing Service (Main Application)         │  │
│  │    - REST API (8080)                                  │  │
│  │    - gRPC (9090)                                      │  │
│  │    - Actuator (8081)                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│         ┌────────────────┼────────────────┐                 │
│         │                │                │                 │
│    ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐           │
│    │  Redis   │    │  Kafka   │    │Prometheus│           │
│    │  (6379)  │    │ (29092)  │    │  (9091)  │           │
│    └──────────┘    └──────────┘    └────┬─────┘           │
│                          │                │                 │
│                    ┌─────▼─────┐    ┌────▼─────┐           │
│                    │ Zookeeper │    │ Grafana  │           │
│                    │  (2181)   │    │  (3000)  │           │
│                    └───────────┘    └──────────┘           │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### **Component Details**

| Service | Purpose | Port(s) | Health Check |
|---------|---------|---------|--------------|
| **basket-pricing-service** | Main application | 8080, 8081, 9090 | http://localhost:8080/actuator/health |
| **redis** | Caching layer | 6379 | redis-cli ping |
| **kafka** | Event streaming | 9092, 29092 | - |
| **zookeeper** | Kafka coordinator | 2181 | - |
| **prometheus** | Metrics collection | 9091 | http://localhost:9091 |
| **grafana** | Visualization | 3000 | http://localhost:3000 |

---

## ⚙️ **Configuration**

### **Environment Variables**

Create a `.env` file in the project root:

```bash
# Refinitiv Configuration
REFINITIV_HOST=ads1
REFINITIV_PORT=14002
REFINITIV_SERVICE_NAME=ELEKTRON_DD
REFINITIV_USERNAME=user1

# Application Ports
SERVER_PORT=8080
MANAGEMENT_SERVER_PORT=8081
GRPC_SERVER_PORT=9090

# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# JVM Options
JAVA_OPTS=-Xmx2g -Xms1g

# Logging Level
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_SRVISWAN=DEBUG
```

### **Spring Profiles**

The Docker setup uses the `docker` profile which:
- Connects to Redis at `redis:6379`
- Connects to Kafka at `kafka:9092`
- Enables all actuator endpoints
- Configures Prometheus metrics export
- Sets up health probes for Docker

---

## 🎯 **Running the Service**

### **Helper Scripts**

We provide convenient scripts for Docker operations:

#### **1. Start Services**
```bash
# Start with logs visible
./scripts/docker-start.sh

# Start in background (detached mode)
./scripts/docker-start.sh --detach

# Build and start
./scripts/docker-start.sh --build

# Clean rebuild (removes all data)
./scripts/docker-start.sh --clean
```

#### **2. Check Status**
```bash
# Get comprehensive status
./scripts/docker-status.sh

# Shows:
# - Container status
# - Health check results
# - Service URLs
# - Quick metrics
# - Volumes and networks
```

#### **3. View Logs**
```bash
# View last 100 lines
./scripts/docker-logs.sh

# Follow logs in real-time
./scripts/docker-logs.sh --follow

# View specific service
./scripts/docker-logs.sh --service redis --follow

# View all services
./scripts/docker-logs.sh --all

# Show more lines
./scripts/docker-logs.sh --tail 500
```

#### **4. Stop Services**
```bash
# Stop (keeps data)
./scripts/docker-stop.sh

# Stop and remove volumes (deletes all data)
./scripts/docker-stop.sh --volumes
```

### **Manual Docker Commands**

If you prefer raw Docker commands:

```bash
# Start all services
docker-compose up -d

# View status
docker-compose ps

# View logs
docker-compose logs -f basket-pricing-service

# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes
docker-compose down -v

# Restart a service
docker-compose restart basket-pricing-service

# Execute shell in container
docker-compose exec basket-pricing-service sh

# View resource usage
docker stats
```

### **Building Images**

```bash
# Build all images
docker-compose build

# Build specific service
docker-compose build basket-pricing-service

# Build with no cache (clean build)
docker-compose build --no-cache

# Pull latest base images
docker-compose pull
```

---

## 📊 **Monitoring**

### **1. Grafana Dashboards**

Access Grafana at http://localhost:3000

**Default credentials:**
- Username: `admin`
- Password: `admin`

Grafana is pre-configured with:
- Prometheus as data source
- Dashboard auto-provisioning

**To create dashboards:**
1. Go to http://localhost:3000
2. Login with admin/admin
3. Go to Dashboards → New Dashboard
4. Add panels with Prometheus queries

**Useful metrics:**
```promql
# JVM Memory Usage
jvm_memory_used_bytes{application="basket-pricing-service"}

# HTTP Request Rate
rate(http_server_requests_seconds_count[1m])

# HTTP Request Duration
http_server_requests_seconds{application="basket-pricing-service"}

# Circuit Breaker State
resilience4j_circuitbreaker_state{application="basket-pricing-service"}

# Active gRPC Connections
grpc_server_connections_active
```

### **2. Prometheus Metrics**

Access Prometheus at http://localhost:9091

**Explore metrics:**
1. Go to http://localhost:9091
2. Click "Graph"
3. Enter metric name or use the dropdown
4. Execute query

**Direct metrics endpoint:**
```bash
curl http://localhost:8081/actuator/prometheus
```

### **3. Application Actuator**

Health check:
```bash
curl http://localhost:8080/actuator/health
```

All metrics:
```bash
curl http://localhost:8081/actuator/metrics
```

Specific metric:
```bash
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

Available endpoints:
- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - All metrics
- `/actuator/prometheus` - Prometheus format
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Logger configuration
- `/actuator/threaddump` - Thread dump
- `/actuator/heapdump` - Heap dump (download)

---

## 🔧 **Troubleshooting**

### **Service Won't Start**

**Check Docker is running:**
```bash
docker info
```

**Check port conflicts:**
```bash
# macOS/Linux
lsof -i :8080
lsof -i :9090

# Or use netstat
netstat -an | grep 8080
```

**View container logs:**
```bash
./scripts/docker-logs.sh --follow
```

**Check container status:**
```bash
docker-compose ps
```

### **Service Starts but Crashes**

**Check logs:**
```bash
docker-compose logs basket-pricing-service
```

**Common issues:**
1. **Memory issues** - Increase Docker memory allocation
2. **Configuration errors** - Check `.env` file
3. **Port conflicts** - Ensure ports are free
4. **Missing dependencies** - Rebuild: `./scripts/docker-start.sh --build`

### **Cannot Connect to Service**

**Verify service is running:**
```bash
./scripts/docker-status.sh
```

**Check health endpoint:**
```bash
curl -v http://localhost:8080/actuator/health
```

**Wait for startup:**
The service needs 30-60 seconds to fully initialize. Check logs:
```bash
docker-compose logs -f basket-pricing-service
```

Look for: `Started BasketPricingApplication`

### **Slow Performance**

**Check resource allocation:**
```bash
docker stats
```

**Increase memory:**
Edit `docker-compose.yml`:
```yaml
services:
  basket-pricing-service:
    environment:
      - JAVA_OPTS=-Xmx4g -Xms2g  # Increase from default
```

**Check container limits:**
```bash
docker inspect basket-pricing-service | grep -i memory
```

### **Clean Restart**

If things are broken, do a clean restart:

```bash
# Stop everything and remove volumes
./scripts/docker-stop.sh --volumes

# Clean rebuild
./scripts/docker-start.sh --clean --build

# Wait for startup
sleep 60

# Check status
./scripts/docker-status.sh
```

### **Database/Cache Issues**

**Reset Redis:**
```bash
docker-compose exec redis redis-cli FLUSHALL
```

**Reset Kafka topics:**
```bash
docker-compose restart kafka zookeeper
```

**Remove all data and restart:**
```bash
./scripts/docker-stop.sh --volumes
./scripts/docker-start.sh
```

### **View Container Shell**

Get a shell inside the container:
```bash
# Bash (if available)
docker-compose exec basket-pricing-service bash

# Or use sh
docker-compose exec basket-pricing-service sh
```

Inside the container:
```bash
# Check Java version
java -version

# Check running processes
ps aux

# Check environment variables
env | grep -i spring

# Check logs
tail -f /app/logs/application.log

# Test connectivity
curl localhost:8080/actuator/health
```

---

## 📝 **Docker Commands Reference**

### **Container Management**

```bash
# List running containers
docker-compose ps

# Start services
docker-compose up -d

# Stop services
docker-compose stop

# Restart a service
docker-compose restart basket-pricing-service

# Remove containers
docker-compose down

# Remove containers and volumes
docker-compose down -v

# Recreate containers
docker-compose up -d --force-recreate
```

### **Logs**

```bash
# View logs
docker-compose logs basket-pricing-service

# Follow logs
docker-compose logs -f basket-pricing-service

# Last N lines
docker-compose logs --tail=100 basket-pricing-service

# All services logs
docker-compose logs -f

# Logs with timestamps
docker-compose logs -t basket-pricing-service
```

### **Building**

```bash
# Build images
docker-compose build

# Build without cache
docker-compose build --no-cache

# Build specific service
docker-compose build basket-pricing-service

# Pull and build
docker-compose pull && docker-compose build
```

### **Exec and Debugging**

```bash
# Execute command in container
docker-compose exec basket-pricing-service <command>

# Get a shell
docker-compose exec basket-pricing-service sh

# Run as root
docker-compose exec -u root basket-pricing-service sh

# Copy files from container
docker cp basket-pricing-service:/app/logs/application.log ./

# Copy files to container
docker cp ./config/app.yaml basket-pricing-service:/app/config/
```

### **Volumes and Networks**

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect basket-pricing-service_redis-data

# Remove unused volumes
docker volume prune

# List networks
docker network ls

# Inspect network
docker network inspect basket-pricing-service_basket-pricing-network
```

### **Resource Management**

```bash
# Show resource usage
docker stats

# Show disk usage
docker system df

# Clean up
docker system prune

# Clean up everything
docker system prune -a --volumes
```

---

## 🎛️ **Advanced Configuration**

### **Custom Docker Compose**

Create `docker-compose.override.yml` for local customizations:

```yaml
version: '3.8'

services:
  basket-pricing-service:
    environment:
      - JAVA_OPTS=-Xmx4g -Xms2g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    ports:
      - "5005:5005"  # Debug port
    volumes:
      - ./custom-config:/app/custom-config
```

This file is automatically merged with `docker-compose.yml` and not tracked by Git.

### **Multi-Stage Build Optimization**

The Dockerfile uses multi-stage builds:
- **Builder stage**: Uses Maven to compile the application
- **Runtime stage**: Uses slim JRE image for smaller size

**Image size comparison:**
- With single stage: ~800MB
- With multi-stage: ~250MB

### **Health Checks**

All services have health checks configured:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### **Resource Limits**

To add CPU/memory limits, edit `docker-compose.yml`:

```yaml
services:
  basket-pricing-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
        reservations:
          cpus: '1'
          memory: 2G
```

---

## 🚀 **Production Considerations**

### **Things to Change for Production**

1. **Security:**
   - Change Grafana default password
   - Use secrets management for credentials
   - Enable authentication on Redis/Kafka
   - Use TLS/SSL for external connections

2. **Persistence:**
   - Use named volumes or bind mounts for critical data
   - Configure backup strategies
   - Set up volume snapshots

3. **Scaling:**
   - Use Docker Swarm or Kubernetes for orchestration
   - Configure horizontal scaling
   - Use load balancers

4. **Monitoring:**
   - Add alerting rules in Prometheus
   - Configure Grafana notifications
   - Set up log aggregation (ELK/Loki)

5. **Resources:**
   - Properly size memory and CPU limits
   - Configure JVM heap sizes appropriately
   - Monitor and adjust based on load

---

## 📚 **Additional Resources**

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Prometheus Docker Guide](https://prometheus.io/docs/prometheus/latest/installation/)
- [Grafana Docker Guide](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/)

---

## 🎉 **Quick Commands Cheat Sheet**

```bash
# Start everything
./scripts/docker-start.sh --detach

# Check status
./scripts/docker-status.sh

# View logs
./scripts/docker-logs.sh --follow

# Test API
curl http://localhost:8080/actuator/health

# Open Grafana
open http://localhost:3000  # macOS
# or visit http://localhost:3000 in browser

# Stop everything
./scripts/docker-stop.sh

# Clean restart
./scripts/docker-stop.sh --volumes && ./scripts/docker-start.sh --build
```

---

**Need Help?** Check the [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed debugging steps.

