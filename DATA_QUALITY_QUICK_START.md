# 📊 Data Quality - Quick Start

## **What Is It?**

Real-time data quality monitoring that validates **every price update** as it flows through your system and proactively catches issues.

---

## ⚡ **How It Works**

```
Price Update → Validation → Metrics → Alerts → You Know Immediately!
```

Every price from Refinitiv is automatically validated for:
- ✅ **Completeness** - All fields present
- ✅ **Validity** - Prices in valid range
- ✅ **Consistency** - Bid ≤ Last ≤ Ask
- ✅ **Timeliness** - Data is fresh (< 60s old)
- ✅ **Accuracy** - Correct precision

---

## 🚀 **Quick Start (3 Steps)**

### **Step 1: It's Already Running!**

The framework is automatically enabled when you start the application:

```bash
# Start the service
./scripts/docker-start-minimal.sh --detach

# Or locally
mvn spring-boot:run
```

### **Step 2: Check Quality Score**

```bash
curl http://localhost:8080/api/data-quality/score
```

Response:
```json
{
  "score": 98.5,
  "threshold": 95.0,
  "healthy": true
}
```

### **Step 3: View Issues (if any)**

```bash
curl http://localhost:8080/api/data-quality/summary
```

Response:
```json
{
  "totalIssues": 12,
  "affectedSymbols": 3,
  "totalErrors": 2,
  "totalWarnings": 10,
  "topOffenders": [
    {"IBM.N": 5},
    {"MSFT.O": 4}
  ]
}
```

---

## 📍 **Key Endpoints**

| What You Want | Endpoint | Example |
|---------------|----------|---------|
| **Overall quality** | `/api/data-quality/score` | `curl localhost:8080/api/data-quality/score` |
| **Symbol quality** | `/api/data-quality/score/{ric}` | `curl localhost:8080/api/data-quality/score/IBM.N` |
| **Recent issues** | `/api/data-quality/issues?hours=1` | `curl localhost:8080/api/data-quality/issues?hours=1` |
| **Problem symbols** | `/api/data-quality/top-offenders` | `curl localhost:8080/api/data-quality/top-offenders` |
| **Quick summary** | `/api/data-quality/summary` | `curl localhost:8080/api/data-quality/summary` |
| **Health check** | `/api/data-quality/health` | `curl localhost:8080/api/data-quality/health` |

---

## 📊 **Grafana Dashboard**

### **Quick Setup:**

1. **Open Grafana**: http://localhost:3000 (admin/admin)

2. **Create Dashboard** with these panels:

#### **Panel 1: Quality Score**
```promql
pricing_data_quality_score
```
**Type:** Gauge | **Thresholds:** Red < 90, Yellow < 95, Green >= 95

#### **Panel 2: Valid vs Invalid**
```promql
rate(pricing_data_quality_valid_total[5m])
rate(pricing_data_quality_invalid_total[5m])
```
**Type:** Graph

#### **Panel 3: Issues by Type**
```promql
sum by (dimension) (rate(pricing_data_quality_issues_total[5m]))
```
**Type:** Stacked Area

---

## 🚨 **When to Investigate**

### **🔴 Critical (Investigate Immediately)**
- Quality score < 90%
- Error rate > 0.1/sec
- Specific symbol has > 10 errors

### **🟡 Warning (Monitor)**
- Quality score 90-95%
- Stale data detected
- Wide spreads (> 10%)

### **🟢 Healthy**
- Quality score >= 95%
- No errors
- All checks passing

---

## 🔍 **Investigation Workflow**

### **When Quality Drops:**

```bash
# 1. Check overall score
curl localhost:8080/api/data-quality/score

# 2. Identify problem symbols
curl localhost:8080/api/data-quality/top-offenders

# 3. See what's wrong
curl localhost:8080/api/data-quality/issues/IBM.N?hours=1

# 4. Check detailed breakdown
curl localhost:8080/api/data-quality/breakdown

# 5. Review logs
grep "DATA QUALITY ALERT" logs/application.log
```

---

## ⚙️ **Configuration**

### **Default Settings (application.yaml)**

```yaml
data-quality:
  enabled: true                    # Turn on/off
  alerting-enabled: true           # Enable alerts
  min-price: 0.01                  # Minimum valid price
  max-price: 1000000.0             # Maximum valid price
  max-spread-percentage: 10.0      # Max bid-ask spread %
  max-age: 60s                     # Max data age
  max-decimal-places: 6            # Max precision
  min-quality-score: 95.0          # Quality threshold
```

### **Adjust for Your Needs:**

```yaml
# For FX (tighter spreads)
data-quality:
  max-spread-percentage: 0.1

# For real-time (fresher data)
data-quality:
  max-age: 10s

# For development (more lenient)
data-quality:
  min-quality-score: 90.0
  alerting-enabled: false
```

---

## 📈 **Metrics to Watch**

```bash
# Quality score
curl localhost:8081/actuator/metrics/pricing.data.quality.score

# Valid prices
curl localhost:8081/actuator/metrics/pricing.data.quality.valid

# Invalid prices
curl localhost:8081/actuator/metrics/pricing.data.quality.invalid

# All metrics
curl localhost:8081/actuator/prometheus | grep pricing_data_quality
```

---

## 🎯 **Real-World Examples**

### **Example 1: Monitor Quality in Real-Time**
```bash
# Watch quality score every 5 seconds
watch -n 5 'curl -s localhost:8080/api/data-quality/score | jq ".score"'
```

### **Example 2: Alert on Low Quality**
```bash
#!/bin/bash
# check-quality.sh

SCORE=$(curl -s localhost:8080/api/data-quality/score | jq -r '.score')

if (( $(echo "$SCORE < 95" | bc -l) )); then
    echo "⚠️  Quality score is low: ${SCORE}%"
    # Send notification
    # curl -X POST slack-webhook...
fi
```

### **Example 3: Daily Quality Report**
```bash
#!/bin/bash
# daily-report.sh

DATE=$(date +%Y-%m-%d)
curl -s localhost:8080/api/data-quality/report > reports/quality-${DATE}.json

echo "Data Quality Report - ${DATE}"
echo "═══════════════════════════════════"
jq -r '.summary | "Score: \(.overallScore)% | Issues: \(.totalIssues) | Errors: \(.totalErrors)"' reports/quality-${DATE}.json
```

---

## 🎓 **What Makes This Special**

### **Proactive vs Reactive**

**❌ Traditional Approach (Reactive):**
```
User reports issue → Investigate → Find problem → Fix
(Users already impacted!)
```

**✅ Our Approach (Proactive):**
```
Price arrives → Validate → Detect issue → Alert → Fix
(Users never see the problem!)
```

### **Real-Time vs Batch**

**❌ Batch Validation:**
- Check data once per hour/day
- Issues accumulate
- Delayed detection

**✅ Real-Time Validation:**
- Check every single update
- Immediate detection
- Instant alerts

---

## 💡 **Tips**

1. **Start with default config** - It's already tuned for most use cases
2. **Monitor the dashboard** - Set up Grafana panels
3. **Set up alerts** - Get notified on quality drops
4. **Review daily** - Check summary reports
5. **Adjust thresholds** - Fine-tune based on your data

---

## 📚 **Learn More**

- **[REALTIME_DATA_QUALITY.md](REALTIME_DATA_QUALITY.md)** - Complete framework documentation
- **[DATA_QUALITY_TESTING.md](DATA_QUALITY_TESTING.md)** - Testing strategies
- **[MONITORING_GUIDE.md](MONITORING_GUIDE.md)** - General monitoring guide

---

## ✅ **You're All Set!**

The data quality framework is:
- ✅ Already running
- ✅ Validating every price
- ✅ Recording metrics
- ✅ Ready for alerts
- ✅ Accessible via REST API

Just check the score and start monitoring! 🎉

```bash
curl http://localhost:8080/api/data-quality/score
```

