# 🐳 Dockerfile Strategies Comparison

## **Question: Should I build in Docker or copy pre-built artifacts?**

**Short Answer:** Both approaches work! Choose based on your workflow and environment.

---

## 📊 **Two Approaches Available**

### **Approach 1: Multi-Stage Build (Dockerfile)**
Builds the application **inside** Docker

### **Approach 2: Pre-Built Artifact (Dockerfile.local)**  
Copies **locally built** JAR into Docker

---

## ⚖️ **Detailed Comparison**

| Aspect | Multi-Stage Build | Pre-Built Artifact |
|--------|-------------------|-------------------|
| **Build Location** | Inside Docker | On your machine |
| **Build Time** | Slower (downloads deps) | Faster (uses local cache) |
| **Consistency** | Same on all machines | Depends on local env |
| **CI/CD** | Better (reproducible) | Requires build step first |
| **Development** | Slower iteration | Faster iteration |
| **Dependencies** | Needs Docker only | Needs Maven + Java |
| **Cache** | Docker layer cache | Maven local cache |
| **Image Size** | Same (~250MB) | Same (~250MB) |
| **Security** | Better (controlled env) | Same |

---

## 🎯 **When to Use Each**

### **Use Multi-Stage Build (`Dockerfile`) When:**

✅ **CI/CD pipelines** - Reproducible builds  
✅ **Production deployments** - Consistent environment  
✅ **Team collaboration** - Same build for everyone  
✅ **No local Java/Maven** - Docker handles everything  
✅ **Clean room builds** - No local cache interference  

**Example Workflow:**
```bash
# Just Docker needed
docker-compose build
docker-compose up
```

---

### **Use Pre-Built Artifact (`Dockerfile.local`) When:**

✅ **Development** - Faster iterations  
✅ **Local testing** - Quick rebuilds  
✅ **Maven is already running** - Reuse local build  
✅ **Debugging** - Control over build process  
✅ **CI with separate build step** - Build once, deploy many  

**Example Workflow:**
```bash
# Build locally
mvn clean package -DskipTests

# Quick Docker build (just copies JAR)
docker build -f Dockerfile.local -t basket-pricing-service:latest .
```

---

## 🚀 **Performance Comparison**

### **Multi-Stage Build:**
```bash
# First build (cold cache)
time docker build -t basket-pricing-service .
# Time: ~3-5 minutes (downloads all dependencies)

# Subsequent builds (warm cache)
time docker build -t basket-pricing-service .
# Time: ~1-2 minutes (only if pom.xml or src changed)
```

### **Pre-Built Artifact:**
```bash
# Build locally
time mvn clean package -DskipTests
# Time: ~30-60 seconds (uses local Maven cache)

# Docker build (just copy JAR)
time docker build -f Dockerfile.local -t basket-pricing-service .
# Time: ~10-15 seconds (very fast!)

# Total: ~45-75 seconds
```

**⚡ Pre-built is 2-4x faster for development!**

---

## 💻 **Implementation**

### **Option 1: Multi-Stage Build (Current)**

**`Dockerfile`:**
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
# ... rest of config
```

**Usage:**
```bash
# docker-compose.yml
services:
  basket-pricing-service:
    build:
      context: .
      dockerfile: Dockerfile
```

---

### **Option 2: Pre-Built Artifact (New)**

**`Dockerfile.local`:**
```dockerfile
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy pre-built JAR from local target/
COPY target/*.jar app.jar
COPY src/main/resources/application*.yaml ./config/
# ... rest of config
```

**Usage:**
```bash
# Build locally first
mvn clean package -DskipTests

# Then build Docker image
docker build -f Dockerfile.local -t basket-pricing-service:latest .

# Or with docker-compose-local.yml
docker-compose -f docker-compose-local.yml up
```

---

## ⚠️ **Potential Issues & Solutions**

### **Issue 1: JAR File Not Found**

**Problem:**
```
COPY failed: file not found in build context: target/*.jar
```

**Solution:**
```bash
# Make sure to build locally first!
mvn clean package -DskipTests

# Verify JAR exists
ls -lh target/*.jar

# Then build Docker
docker build -f Dockerfile.local .
```

---

### **Issue 2: .dockerignore Excludes target/**

**Problem:**
Your `.dockerignore` might exclude `target/` directory

**Solution:**
Update `.dockerignore` when using `Dockerfile.local`:

```bash
# For Dockerfile.local, comment out this line in .dockerignore:
# target/

# Or use separate .dockerignore files
```

---

### **Issue 3: Wrong Architecture (M1 Mac)**

**Problem:**
Built on M1 Mac (arm64), running on Intel (amd64)

**Solution:**
```bash
# Build for specific platform
docker build --platform linux/amd64 -f Dockerfile.local .

# Or use multi-stage build (auto-detects)
docker build -f Dockerfile .
```

---

### **Issue 4: Stale JAR**

**Problem:**
Old JAR gets copied if you forget to rebuild

**Solution:**
```bash
# Create build script that ensures fresh JAR
#!/bin/bash
echo "Building JAR..."
mvn clean package -DskipTests

echo "Building Docker image..."
docker build -f Dockerfile.local -t basket-pricing-service:latest .
```

---

### **Issue 5: Missing Dependencies in JAR**

**Problem:**
JAR is not a fat/uber JAR (dependencies not included)

**Solution:**
```xml
<!-- pom.xml - already configured! -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <!-- Creates executable fat JAR -->
</plugin>
```

---

## 📋 **Best Practices**

### **For Development: Use Pre-Built**

**Workflow:**
```bash
# 1. Make code changes
# 2. Build locally
mvn clean package -DskipTests

# 3. Quick Docker build
docker build -f Dockerfile.local -t basket-pricing-service:dev .

# 4. Test
docker run -p 8080:8080 basket-pricing-service:dev
```

**Advantages:**
- ✅ Faster iterations (Maven cache used)
- ✅ IDE integration (same build process)
- ✅ Better debugging (can run locally or in Docker)
- ✅ Incremental builds

---

### **For CI/CD: Use Multi-Stage**

**Workflow:**
```yaml
# .github/workflows/build.yml
- name: Build Docker Image
  run: docker build -t myregistry/basket-pricing:${{ github.sha }} .

- name: Push to Registry
  run: docker push myregistry/basket-pricing:${{ github.sha }}
```

**Advantages:**
- ✅ Reproducible builds
- ✅ No local dependencies needed
- ✅ Clean build environment
- ✅ Consistent across all CI runs

---

### **For Production: Use Multi-Stage**

**Why:**
- ✅ Traceable builds
- ✅ No local contamination
- ✅ Security scanning easier
- ✅ Audit trail

---

## 🔧 **Hybrid Approach (Best of Both)**

Create both Dockerfiles and choose based on context:

```yaml
# docker-compose.yml (for CI/CD)
services:
  basket-pricing-service:
    build:
      context: .
      dockerfile: Dockerfile  # Multi-stage build

# docker-compose-local.yml (for development)
services:
  basket-pricing-service:
    build:
      context: .
      dockerfile: Dockerfile.local  # Pre-built JAR
```

**Development:**
```bash
mvn clean package -DskipTests
docker-compose -f docker-compose-local.yml up --build
```

**Production:**
```bash
docker-compose up --build
```

---

## 🎯 **Recommended Setup**

### **Create docker-compose-local.yml:**

```yaml
version: '3.8'

services:
  basket-pricing-service:
    build:
      context: .
      dockerfile: Dockerfile.local  # Use pre-built JAR
    image: basket-pricing-service:dev
    container_name: basket-pricing-service-dev
    # ... rest same as docker-compose-minimal.yml
```

### **Create build-and-run script:**

```bash
#!/bin/bash
# scripts/dev-docker.sh

echo "🔨 Building application locally..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Maven build failed"
    exit 1
fi

echo "✅ JAR built successfully"
echo ""

echo "🐳 Building Docker image..."
docker build -f Dockerfile.local -t basket-pricing-service:dev .

if [ $? -ne 0 ]; then
    echo "❌ Docker build failed"
    exit 1
fi

echo "✅ Docker image built"
echo ""

echo "🚀 Starting services..."
docker-compose -f docker-compose-local.yml up -d

echo "✅ Services started!"
```

---

## ⚡ **Performance Gains**

### **Development Iteration:**

**Multi-Stage Build:**
```
Code change → Docker build (2 min) → Test → Repeat
```

**Pre-Built Artifact:**
```
Code change → Maven build (30s) → Docker copy (10s) → Test → Repeat
```

**💡 3-4x faster iterations!**

---

### **CI/CD Build Times:**

**Multi-Stage Build:**
```
git clone → docker build → docker push
Time: 3-5 minutes per build
```

**Pre-Built Artifact (Separate Steps):**
```
git clone → mvn build → docker build → docker push
Time: 2-3 minutes per build
```

**But:** Multi-stage is better for CI because:
- Reproducible
- No local Maven cache needed
- Simpler pipeline

---

## 🔒 **Security Considerations**

### **Both Approaches:**
- ✅ Use non-root user
- ✅ Minimal base image (JRE only)
- ✅ Health checks
- ✅ No secrets in image

### **Multi-Stage Advantage:**
- ✅ Build tools not in final image
- ✅ Source code not in final image
- ✅ Smaller attack surface

### **Pre-Built Consideration:**
- ⚠️  Must ensure JAR is built in clean environment
- ⚠️  Could contain local config if not careful

---

## 📦 **Will It Cause Issues?**

### **✅ NO Issues If:**
- JAR is a fat/uber JAR (includes all dependencies) ✅ You have this!
- JAR is built with same Java version as Docker image ✅
- JAR doesn't depend on local files ✅
- Configuration is externalized ✅

### **⚠️ Potential Issues:**

#### **Issue 1: Platform Mismatch**
```
# Built on M1 Mac (arm64)
# Trying to run on Intel (amd64)
```

**Solution:** Java is platform-independent! JAR works on both.  
**Only concern:** Native libraries (Refinitiv EMA)

**Check:**
```bash
# If EMA has native libs, they're in the JAR
jar -tf target/basket-pricing-service-*.jar | grep -E "\.so$|\.dll$|\.dylib$"

# If found, you need platform-specific builds
```

#### **Issue 2: Local Config Embedded**
```
# application.yaml has localhost references
```

**Solution:** Use environment variables and profiles ✅ Already done!

#### **Issue 3: Missing Dependencies**
```
# JAR doesn't include dependencies
```

**Solution:** Spring Boot Maven plugin creates fat JAR ✅ Already configured!

---

## 🎯 **Recommendation**

### **Development: Pre-Built Artifact**
```bash
# Fast iteration cycle
mvn package -DskipTests && docker build -f Dockerfile.local . && docker-compose -f docker-compose-local.yml up
```

### **CI/CD: Multi-Stage Build**
```bash
# Reproducible builds
docker build -t myregistry/app:tag .
```

### **Production: Multi-Stage Build**
```bash
# Controlled, auditable builds
docker build -t myregistry/app:version .
```

---

## 🚀 **Quick Commands**

### **Multi-Stage Build:**
```bash
docker build -t basket-pricing-service:latest .
docker run -p 8080:8080 basket-pricing-service:latest
```

### **Pre-Built Artifact:**
```bash
# Build JAR first
mvn clean package -DskipTests

# Then build Docker
docker build -f Dockerfile.local -t basket-pricing-service:dev .
docker run -p 8080:8080 basket-pricing-service:dev
```

### **Compare Build Times:**
```bash
# Multi-stage
time docker build -f Dockerfile -t app:multi .

# Pre-built
time mvn package -DskipTests
time docker build -f Dockerfile.local -t app:local .
```

---

## 📈 **Build Time Breakdown**

### **Multi-Stage Build:**
```
1. Download Maven base image    ~1-2 min (first time)
2. Download dependencies         ~1-2 min (first time)
3. Compile Java code            ~30-60 sec
4. Package JAR                  ~10-20 sec
5. Copy to runtime image        ~5-10 sec
───────────────────────────────────────────
Total (first):  ~3-5 minutes
Total (cached): ~1-2 minutes
```

### **Pre-Built Artifact:**
```
1. Maven build (local)          ~30-60 sec
2. Download JRE base image      ~30 sec (first time)
3. Copy JAR                     ~5 sec
4. Layer assembly               ~5 sec
───────────────────────────────────────────
Total (first):  ~70-100 seconds
Total (cached): ~40-65 seconds
```

**🎯 Pre-built is 2-4x faster!**

---

## 🔄 **Migration Path**

### **Switch from Multi-Stage to Pre-Built:**

1. **Update .dockerignore:**
```bash
# Comment out to allow target/ in build context
# target/
```

2. **Build locally:**
```bash
mvn clean package -DskipTests
```

3. **Build Docker:**
```bash
docker build -f Dockerfile.local -t basket-pricing-service:dev .
```

### **Switch from Pre-Built to Multi-Stage:**

1. **Just use original Dockerfile:**
```bash
docker build -f Dockerfile -t basket-pricing-service:latest .
```

No other changes needed!

---

## 💡 **Best Practice: Support Both**

Keep both Dockerfiles and choose based on context:

```bash
# Development script
./scripts/dev-docker.sh  # Uses Dockerfile.local

# Production build
docker build -f Dockerfile -t app:prod .
```

---

## ⚙️ **.dockerignore Considerations**

### **Current .dockerignore:**
```
target/      # Excluded for multi-stage build
*.class      # Excluded
*.jar        # Excluded (except in target/)
```

### **For Pre-Built Artifact:**
```
# Option 1: Update .dockerignore
# Comment out target/ exclusion
# target/

# Option 2: Use --file to specify explicit files
docker build --file Dockerfile.local \
  --build-context target=./target \
  -t app:local .
```

### **Recommended: Context-Specific .dockerignore**
```bash
# .dockerignore (for Dockerfile - multi-stage)
target/
*.class
src/test/

# .dockerignore.local (for Dockerfile.local)
*.class
src/test/
# Note: target/ is NOT excluded
```

Then:
```bash
# Use appropriate ignore file
cp .dockerignore.local .dockerignore
docker build -f Dockerfile.local .
cp .dockerignore.multistage .dockerignore
```

---

## 🎓 **Real-World Scenarios**

### **Scenario 1: Developer Makes Code Change**

**Multi-Stage:**
```bash
# Edit code
vim src/main/java/...

# Build (slow - 2 min)
docker-compose up --build

# Test, repeat...
```

**Pre-Built:**
```bash
# Edit code
vim src/main/java/...

# Build (fast - 45 sec)
mvn package -DskipTests
docker build -f Dockerfile.local .
docker-compose -f docker-compose-local.yml up

# Test, repeat...
```

---

### **Scenario 2: CI/CD Pipeline**

**Multi-Stage (Recommended):**
```yaml
# .github/workflows/build.yml
- name: Build Docker Image
  run: docker build -t $IMAGE .

- name: Push
  run: docker push $IMAGE
```

**Advantages:**
- Reproducible
- No Maven cache to manage
- Same result everywhere

---

### **Scenario 3: Production Deployment**

**Multi-Stage (Recommended):**
```bash
# Kubernetes deployment
docker build -t registry.company.com/basket-pricing:v1.2.3 .
docker push registry.company.com/basket-pricing:v1.2.3
kubectl set image deployment/basket-pricing app=registry.company.com/basket-pricing:v1.2.3
```

**Why:**
- Auditable build process
- No dependency on build machine state
- Secure build environment

---

### **Scenario 4: Quick Local Test**

**Pre-Built (Faster):**
```bash
# Quick test script
./scripts/quick-test.sh

# Contents:
mvn package -DskipTests
docker build -f Dockerfile.local -t test:latest .
docker run --rm -p 8080:8080 test:latest
```

---

## 📊 **Pros & Cons Summary**

### **Multi-Stage Build**

**Pros:**
- ✅ Reproducible builds
- ✅ No local dependencies needed
- ✅ Better for CI/CD
- ✅ Cleaner separation
- ✅ Production-ready

**Cons:**
- ❌ Slower build times
- ❌ Docker cache invalidation issues
- ❌ Larger build context
- ❌ Slower development iterations

### **Pre-Built Artifact**

**Pros:**
- ✅ 2-4x faster builds
- ✅ Uses Maven local cache
- ✅ Better for development
- ✅ Smaller Docker build context
- ✅ More control over build

**Cons:**
- ❌ Requires Java + Maven locally
- ❌ Must remember to build first
- ❌ Less reproducible
- ❌ State depends on local environment

---

## ✅ **My Recommendation**

### **Use BOTH:**

**Development:**
```bash
# Create docker-compose.override.yml
version: '3.8'
services:
  basket-pricing-service:
    build:
      dockerfile: Dockerfile.local  # Override to use pre-built
```

**Production:**
```bash
# Use docker-compose.yml as-is
# Uses Dockerfile (multi-stage)
```

### **Update .dockerignore:**

```bash
# Allow target/ for Dockerfile.local
# But add a comment
# target/  # Uncomment for Dockerfile.local
```

---

## 🎯 **Decision Matrix**

| Your Situation | Use This | Why |
|----------------|----------|-----|
| Quick local test | `Dockerfile.local` | Fastest |
| CI/CD pipeline | `Dockerfile` | Reproducible |
| Production deploy | `Dockerfile` | Secure |
| Development loop | `Dockerfile.local` | Faster iteration |
| No Java locally | `Dockerfile` | Self-contained |
| First time setup | `Dockerfile` | No local build needed |
| Debugging build | `Dockerfile.local` | More control |
| Multiple devs | `Dockerfile` | Consistent |

---

## 🚀 **Summary**

**Answer: Using pre-built artifacts is perfectly fine and often better for development!**

**Will it cause issues?**
- ❌ **NO** - as long as you build the JAR first
- ❌ **NO** - Spring Boot fat JAR includes everything
- ❌ **NO** - Java bytecode is platform-independent
- ✅ **Actually FASTER** for development

**Best approach:**
- Use `Dockerfile.local` for **development** (faster)
- Use `Dockerfile` for **CI/CD and production** (reproducible)
- Keep both in your repo
- Choose based on context

I've created `Dockerfile.local` for you - try it! 🎉

```bash
# Quick test
mvn package -DskipTests
docker build -f Dockerfile.local -t basket-pricing:dev .
docker run --rm -p 8080:8080 basket-pricing:dev
```

