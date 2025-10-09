# 🐳 Docker Quick Start Guide

## 🚀 Get Started in 5 Minutes

### **Step 1: Copy Environment File**
```bash
cp env.example .env
```

### **Step 2: Start All Services**
```bash
./scripts/docker-start.sh --detach
```

This starts:
- ✅ Basket Pricing Service (REST API + gRPC)
- ✅ Redis (caching)
- ✅ Kafka (event streaming)
- ✅ Prometheus (metrics)
- ✅ Grafana (dashboards)

### **Step 3: Check Status**
```bash
./scripts/docker-status.sh
```

### **Step 4: Test the Service**
```bash
# Check health
curl http://localhost:8080/actuator/health

# Subscribe to symbols
curl -X POST "http://localhost:8080/api/prices/subscribe?symbols=IBM.N,MSFT.O"

# Get prices
curl "http://localhost:8080/api/prices?symbols=IBM.N,MSFT.O"

# Or run automated tests
./scripts/test-docker-service.sh
```

---

## 📍 **Access Services**

| Service | URL | Credentials |
|---------|-----|-------------|
| **REST API** | http://localhost:8080 | - |
| **Health Check** | http://localhost:8080/actuator/health | - |
| **Metrics** | http://localhost:8081/actuator/prometheus | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9091 | - |
| **gRPC** | localhost:9090 | - |
| **Redis** | localhost:6379 | - |

---

## 🛠️ **Common Commands**

```bash
# View logs
./scripts/docker-logs.sh --follow

# Check status
./scripts/docker-status.sh

# Run tests
./scripts/test-docker-service.sh

# Stop services (keep data)
./scripts/docker-stop.sh

# Stop and remove all data
./scripts/docker-stop.sh --volumes

# Restart services
docker-compose restart basket-pricing-service

# Rebuild and start
./scripts/docker-start.sh --clean --build
```

---

## 📊 **What's Included**

### **Application Stack**
- **Spring Boot Application** with REST and gRPC APIs
- **Redis** for caching
- **Kafka** for event streaming
- **Prometheus** for metrics collection
- **Grafana** for visualization

### **Monitoring**
- Pre-configured Prometheus scraping
- Grafana datasource auto-configured
- All metrics exposed at `/actuator/prometheus`
- Health checks on all services

### **Networking**
- All services connected via Docker network
- Isolated from host by default
- Exposed ports for external access

### **Data Persistence**
- Redis data persists in volume
- Kafka data persists in volume
- Prometheus data persists in volume
- Grafana data persists in volume
- Application logs mounted to `./logs`

---

## 🔧 **Troubleshooting**

### **Service Won't Start**
```bash
# Check Docker is running
docker info

# Check port conflicts
lsof -i :8080

# View error logs
./scripts/docker-logs.sh --follow
```

### **Cannot Connect to Service**
```bash
# Wait for startup (30-60 seconds)
sleep 60

# Check health
curl http://localhost:8080/actuator/health

# Check status
./scripts/docker-status.sh
```

### **Clean Restart**
```bash
# Stop and remove everything
./scripts/docker-stop.sh --volumes

# Build and start fresh
./scripts/docker-start.sh --clean --build

# Wait for startup
sleep 60

# Test
./scripts/test-docker-service.sh
```

---

## 📖 **Learn More**

For detailed documentation, see:
- **[DOCKER_GUIDE.md](DOCKER_GUIDE.md)** - Complete Docker deployment guide
- **[README.md](README.md)** - Application documentation
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Detailed troubleshooting
- **[MONITORING_GUIDE.md](MONITORING_GUIDE.md)** - Monitoring and metrics

---

## 🎯 **Architecture Overview**

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose Stack                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    Basket Pricing Service                            │  │
│  │    REST (8080) | gRPC (9090) | Metrics (8081)       │  │
│  └────────┬────────────┬────────────┬────────────────────┘  │
│           │            │            │                        │
│      ┌────▼───┐   ┌────▼───┐   ┌───▼────┐                  │
│      │ Redis  │   │ Kafka  │   │Prometh-│                  │
│      │ (6379) │   │(29092) │   │eus     │                  │
│      └────────┘   └────┬───┘   │ (9091) │                  │
│                        │        └───┬────┘                  │
│                   ┌────▼───┐    ┌───▼────┐                  │
│                   │Zookeep-│    │Grafana │                  │
│                   │er      │    │ (3000) │                  │
│                   │ (2181) │    └────────┘                  │
│                   └────────┘                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ **Features**

- ✅ One-command deployment
- ✅ Full monitoring stack
- ✅ Health checks on all services
- ✅ Persistent data volumes
- ✅ Automatic service recovery
- ✅ Production-ready configuration
- ✅ Comprehensive testing suite
- ✅ Easy log access
- ✅ Multi-stage optimized builds
- ✅ Non-root container security

---

## 🎉 **That's It!**

You now have a fully functional basket pricing service running in Docker with:
- REST and gRPC APIs
- Real-time monitoring
- Data persistence
- Easy management scripts

**Start exploring:**
```bash
# Check the service
curl http://localhost:8080/actuator/health

# View dashboards
open http://localhost:3000

# See what's running
./scripts/docker-status.sh
```

Happy coding! 🚀

