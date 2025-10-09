# 🐳 Docker Image Pull Policy Guide

## **How to Use Local Images Instead of Docker Hub**

### **Current Configuration**

All services in `docker-compose.yml` and `docker-compose-minimal.yml` now use the `pull_policy: missing` setting, which means:

✅ **Use local image if it exists**  
✅ **Only pull from Docker Hub if not found locally**  
✅ **Saves time and bandwidth**  

---

## 📋 **Pull Policy Options**

Docker Compose supports these pull policies:

| Policy | Behavior | Use Case |
|--------|----------|----------|
| **`missing`** | Only pull if image doesn't exist locally | **Default, recommended** |
| **`always`** | Always pull from registry, even if local exists | Force latest updates |
| **`never`** | Never pull, only use local | Completely offline |
| **`build`** | Always build, never pull | Custom applications |

---

## 🎯 **Current Setup**

### **Your Application (basket-pricing-service):**
```yaml
basket-pricing-service:
  build:
    context: .
    dockerfile: Dockerfile
  image: basket-pricing-service:latest
  pull_policy: build  # Always build, never pull
```
**Effect:** Always builds from local Dockerfile, never pulls from registry

### **Prometheus:**
```yaml
prometheus:
  image: prom/prometheus:latest
  pull_policy: missing  # Only pull if not available locally
```
**Effect:** Uses local `prom/prometheus:latest` if available, otherwise pulls

### **Grafana:**
```yaml
grafana:
  image: grafana/grafana:latest
  pull_policy: missing  # Only pull if not available locally
```
**Effect:** Uses local `grafana/grafana:latest` if available, otherwise pulls

---

## 🚀 **How to Pre-Pull Images**

If you want to ensure images are available locally before starting:

### **Option 1: Pull Specific Images**
```bash
# Pull Prometheus
docker pull prom/prometheus:latest

# Pull Grafana
docker pull grafana/grafana:latest

# Pull Redis (if using full stack)
docker pull redis:7-alpine

# Pull Kafka and Zookeeper (if using full stack)
docker pull confluentinc/cp-kafka:7.5.0
docker pull confluentinc/cp-zookeeper:7.5.0
```

### **Option 2: Pull All Images from Compose File**
```bash
# Pull all images defined in docker-compose
docker-compose pull

# Or for minimal stack
docker-compose -f docker-compose-minimal.yml pull
```

### **Option 3: Build Your Application Image**
```bash
# Build the basket-pricing-service image
docker-compose build basket-pricing-service

# Or manually
docker build -t basket-pricing-service:latest .
```

---

## 📦 **View Local Images**

Check what images you have locally:

```bash
# List all images
docker images

# Filter for specific images
docker images | grep prometheus
docker images | grep grafana
docker images | grep basket-pricing

# See image details
docker image inspect prom/prometheus:latest
```

---

## 🔄 **Workflow for Offline Development**

If you want to work completely offline:

### **1. Pre-Pull Everything:**
```bash
# Pull all external images
docker-compose -f docker-compose-minimal.yml pull

# Build your application
docker-compose -f docker-compose-minimal.yml build
```

### **2. Verify Images:**
```bash
docker images
```

You should see:
- `basket-pricing-service:latest`
- `prom/prometheus:latest`
- `grafana/grafana:latest`

### **3. Now You Can Work Offline:**
```bash
# Start services (will use local images)
./scripts/docker-start-minimal.sh --detach
```

---

## 🎯 **Force Using Local Images Only**

If you want to **never** pull from Docker Hub:

### **Option 1: Change Pull Policy to `never`**

Edit `docker-compose-minimal.yml`:
```yaml
prometheus:
  image: prom/prometheus:latest
  pull_policy: never  # Never pull, only use local
```

### **Option 2: Use Docker Offline Mode**
```bash
# Set Docker to offline mode
export DOCKER_BUILDKIT=1
export BUILDKIT_PROGRESS=plain

# Start without pulling
docker-compose -f docker-compose-minimal.yml up --no-pull
```

---

## 🔍 **Troubleshooting**

### **Problem: "Image not found locally"**

**Solution 1:** Pull the image first:
```bash
docker pull prom/prometheus:latest
docker pull grafana/grafana:latest
```

**Solution 2:** Use a different tag (specify version):
```yaml
prometheus:
  image: prom/prometheus:v2.45.0  # Specific version you have locally
```

### **Problem: "Using old cached image"**

**Solution:** Pull latest:
```bash
# Pull latest version
docker pull prom/prometheus:latest

# Or remove old image and pull fresh
docker rmi prom/prometheus:latest
docker pull prom/prometheus:latest
```

### **Problem: "Want to use custom Prometheus/Grafana image"**

**Solution:** Build your own:
```dockerfile
# Dockerfile.prometheus
FROM prom/prometheus:latest
COPY my-custom-prometheus.yml /etc/prometheus/prometheus.yml
```

Then update docker-compose:
```yaml
prometheus:
  build:
    context: ./monitoring
    dockerfile: Dockerfile.prometheus
  pull_policy: build
```

---

## 📊 **Image Size Reference**

Typical image sizes (compressed):

| Image | Size | Purpose |
|-------|------|---------|
| `basket-pricing-service:latest` | ~250 MB | Your app |
| `prom/prometheus:latest` | ~230 MB | Metrics |
| `grafana/grafana:latest` | ~280 MB | Dashboards |
| `redis:7-alpine` | ~30 MB | Cache (optional) |
| `confluentinc/cp-kafka:7.5.0` | ~700 MB | Events (optional) |

**Total for minimal stack:** ~760 MB  
**Total for full stack:** ~1.5 GB

---

## 🎓 **Best Practices**

### **For Development:**
✅ Use `pull_policy: missing` (current setup)  
✅ Pull images once, use for multiple runs  
✅ Build application locally  

### **For CI/CD:**
✅ Use `pull_policy: always` to get latest  
✅ Cache images between builds  
✅ Use specific version tags  

### **For Production:**
✅ Use specific version tags (not `:latest`)  
✅ Pre-pull all images during deployment  
✅ Use private registry for custom images  

---

## 🔧 **Quick Commands**

```bash
# See what images you have
docker images

# Pull specific image
docker pull prom/prometheus:latest

# Pull all images from compose
docker-compose pull

# Build your app
docker-compose build

# Start without pulling
docker-compose up --no-pull

# Remove unused images
docker image prune

# Remove specific image
docker rmi prom/prometheus:latest

# Check image details
docker image inspect prom/prometheus:latest
```

---

## ✅ **Summary**

Your docker-compose files are now configured to:

1. ✅ **Build your application locally** (never pull)
2. ✅ **Use local Prometheus if available** (only pull if missing)
3. ✅ **Use local Grafana if available** (only pull if missing)
4. ✅ **Save bandwidth and time**
5. ✅ **Work offline after first pull**

**To ensure offline capability:**
```bash
# One-time setup
docker-compose -f docker-compose-minimal.yml pull
docker-compose -f docker-compose-minimal.yml build

# Now you can work offline
./scripts/docker-start-minimal.sh --detach
```

Perfect! 🎉

