# 📊 Data Quality Testing for Pricing Service

## **Overview**

Comprehensive data quality testing framework for real-time pricing data from Refinitiv EMA.

---

## 🎯 **Data Quality Dimensions**

### **1. Accuracy**
- Prices match source data
- No calculation errors
- Correct decimal precision

### **2. Completeness**
- All subscribed symbols have prices
- No missing fields (bid, ask, last, volume)
- Timestamps present

### **3. Consistency**
- Bid ≤ Last ≤ Ask
- Prices within expected ranges
- No contradictory data

### **4. Timeliness**
- Data freshness (age < threshold)
- Update frequency meets SLA
- No stale data

### **5. Validity**
- Prices are positive numbers
- Timestamps are valid
- RICs are well-formed

### **6. Uniqueness**
- No duplicate price updates
- Unique message IDs

---

## 🏗️ **Testing Strategy**

### **Level 1: Unit Tests**
- Test individual components
- Mock external dependencies
- Fast execution

### **Level 2: Integration Tests**
- Test with real Refinitiv connection
- Validate end-to-end flow
- Use test environment

### **Level 3: Contract Tests**
- Verify API contracts
- Schema validation
- Backward compatibility

### **Level 4: Chaos Tests**
- Network failures
- Slow responses
- Invalid data

### **Level 5: Production Monitoring**
- Real-time quality metrics
- Alerting on anomalies
- Continuous validation

---

## 💻 **Implementation**

### **1. Data Quality Test Class**

```java
package com.srviswan.basketpricing.testing;

import com.srviswan.basketpricing.model.PriceSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class DataQualityValidator {

    // Configuration
    private static final double MAX_SPREAD_PERCENTAGE = 10.0; // 10% max spread
    private static final Duration MAX_AGE = Duration.ofSeconds(60); // 1 minute max age
    private static final double MIN_PRICE = 0.01;
    private static final double MAX_PRICE = 1_000_000.0;
    
    /**
     * Comprehensive data quality check
     */
    public DataQualityReport validate(Map<String, PriceSnapshot> prices) {
        DataQualityReport report = new DataQualityReport();
        
        for (Map.Entry<String, PriceSnapshot> entry : prices.entrySet()) {
            String ric = entry.getKey();
            PriceSnapshot price = entry.getValue();
            
            // Run all validation checks
            validateCompleteness(ric, price, report);
            validateAccuracy(ric, price, report);
            validateConsistency(ric, price, report);
            validateTimeliness(ric, price, report);
            validateValidity(ric, price, report);
        }
        
        return report;
    }
    
    /**
     * Check 1: Completeness - All required fields present
     */
    private void validateCompleteness(String ric, PriceSnapshot price, DataQualityReport report) {
        List<String> missingFields = new ArrayList<>();
        
        if (price == null) {
            report.addError(ric, "COMPLETENESS", "Price snapshot is null");
            return;
        }
        
        if (price.getBid() == null) missingFields.add("bid");
        if (price.getAsk() == null) missingFields.add("ask");
        if (price.getLast() == null) missingFields.add("last");
        if (price.getTimestamp() == null) missingFields.add("timestamp");
        
        if (!missingFields.isEmpty()) {
            report.addError(ric, "COMPLETENESS", 
                "Missing fields: " + String.join(", ", missingFields));
        } else {
            report.addPass(ric, "COMPLETENESS");
        }
    }
    
    /**
     * Check 2: Accuracy - Prices have correct precision
     */
    private void validateAccuracy(String ric, PriceSnapshot price, DataQualityReport report) {
        if (price == null) return;
        
        // Check decimal precision (max 6 decimal places for most instruments)
        if (price.getBid() != null && !hasValidPrecision(price.getBid(), 6)) {
            report.addWarning(ric, "ACCURACY", 
                "Bid price has invalid precision: " + price.getBid());
        }
        
        if (price.getAsk() != null && !hasValidPrecision(price.getAsk(), 6)) {
            report.addWarning(ric, "ACCURACY", 
                "Ask price has invalid precision: " + price.getAsk());
        }
        
        report.addPass(ric, "ACCURACY");
    }
    
    /**
     * Check 3: Consistency - Bid <= Last <= Ask
     */
    private void validateConsistency(String ric, PriceSnapshot price, DataQualityReport report) {
        if (price == null) return;
        
        Double bid = price.getBid();
        Double ask = price.getAsk();
        Double last = price.getLast();
        
        // Check bid <= ask
        if (bid != null && ask != null && bid > ask) {
            report.addError(ric, "CONSISTENCY", 
                String.format("Bid (%.4f) > Ask (%.4f)", bid, ask));
            return;
        }
        
        // Check last is between bid and ask
        if (bid != null && ask != null && last != null) {
            if (last < bid || last > ask) {
                report.addWarning(ric, "CONSISTENCY", 
                    String.format("Last (%.4f) outside bid-ask spread [%.4f, %.4f]", 
                        last, bid, ask));
            }
        }
        
        // Check spread is reasonable
        if (bid != null && ask != null) {
            double spread = ((ask - bid) / bid) * 100;
            if (spread > MAX_SPREAD_PERCENTAGE) {
                report.addWarning(ric, "CONSISTENCY", 
                    String.format("Spread too wide: %.2f%%", spread));
            }
        }
        
        report.addPass(ric, "CONSISTENCY");
    }
    
    /**
     * Check 4: Timeliness - Data is fresh
     */
    private void validateTimeliness(String ric, PriceSnapshot price, DataQualityReport report) {
        if (price == null || price.getTimestamp() == null) return;
        
        Instant now = Instant.now();
        Instant priceTime = price.getTimestamp();
        Duration age = Duration.between(priceTime, now);
        
        if (age.compareTo(MAX_AGE) > 0) {
            report.addWarning(ric, "TIMELINESS", 
                String.format("Stale data: %d seconds old", age.getSeconds()));
        } else {
            report.addPass(ric, "TIMELINESS");
        }
    }
    
    /**
     * Check 5: Validity - Prices are in valid range
     */
    private void validateValidity(String ric, PriceSnapshot price, DataQualityReport report) {
        if (price == null) return;
        
        // Check bid validity
        if (price.getBid() != null) {
            if (price.getBid() <= 0) {
                report.addError(ric, "VALIDITY", "Bid price is not positive: " + price.getBid());
                return;
            }
            if (price.getBid() < MIN_PRICE || price.getBid() > MAX_PRICE) {
                report.addWarning(ric, "VALIDITY", 
                    "Bid price outside expected range: " + price.getBid());
            }
        }
        
        // Check ask validity
        if (price.getAsk() != null) {
            if (price.getAsk() <= 0) {
                report.addError(ric, "VALIDITY", "Ask price is not positive: " + price.getAsk());
                return;
            }
            if (price.getAsk() < MIN_PRICE || price.getAsk() > MAX_PRICE) {
                report.addWarning(ric, "VALIDITY", 
                    "Ask price outside expected range: " + price.getAsk());
            }
        }
        
        // Check volume validity
        if (price.getVolume() != null && price.getVolume() < 0) {
            report.addError(ric, "VALIDITY", "Volume is negative: " + price.getVolume());
            return;
        }
        
        report.addPass(ric, "VALIDITY");
    }
    
    /**
     * Helper: Check decimal precision
     */
    private boolean hasValidPrecision(double value, int maxDecimals) {
        String str = String.valueOf(value);
        int decimalIndex = str.indexOf('.');
        if (decimalIndex == -1) return true;
        
        int decimals = str.length() - decimalIndex - 1;
        return decimals <= maxDecimals;
    }
}
```

### **2. Data Quality Report**

```java
package com.srviswan.basketpricing.testing;

import lombok.Data;
import java.util.*;

@Data
public class DataQualityReport {
    private final Map<String, List<Issue>> issues = new HashMap<>();
    private final Map<String, Set<String>> passedChecks = new HashMap<>();
    private int totalChecks = 0;
    private int passedChecks = 0;
    private int warnings = 0;
    private int errors = 0;
    
    public void addError(String ric, String dimension, String message) {
        addIssue(ric, new Issue(IssueLevel.ERROR, dimension, message));
        errors++;
        totalChecks++;
    }
    
    public void addWarning(String ric, String dimension, String message) {
        addIssue(ric, new Issue(IssueLevel.WARNING, dimension, message));
        warnings++;
        totalChecks++;
    }
    
    public void addPass(String ric, String dimension) {
        passedChecks.computeIfAbsent(ric, k -> new HashSet<>()).add(dimension);
        passedChecks++;
        totalChecks++;
    }
    
    private void addIssue(String ric, Issue issue) {
        issues.computeIfAbsent(ric, k -> new ArrayList<>()).add(issue);
    }
    
    public boolean isHealthy() {
        return errors == 0;
    }
    
    public double getQualityScore() {
        if (totalChecks == 0) return 100.0;
        return (passedChecks * 100.0) / totalChecks;
    }
    
    public String getSummary() {
        return String.format(
            "Quality Score: %.2f%% | Total: %d | Passed: %d | Warnings: %d | Errors: %d",
            getQualityScore(), totalChecks, passedChecks, warnings, errors
        );
    }
    
    @Data
    public static class Issue {
        private final IssueLevel level;
        private final String dimension;
        private final String message;
        private final Instant timestamp = Instant.now();
    }
    
    public enum IssueLevel {
        ERROR, WARNING
    }
}
```

### **3. Integration Test**

```java
package com.srviswan.basketpricing.testing;

import com.srviswan.basketpricing.marketdata.MarketDataProvider;
import com.srviswan.basketpricing.model.PriceSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DataQualityIntegrationTest {

    @Autowired
    private MarketDataProvider marketDataProvider;
    
    @Autowired
    private DataQualityValidator validator;
    
    @Test
    public void testPriceDataQuality() throws InterruptedException {
        // Subscribe to test symbols
        List<String> symbols = Arrays.asList("IBM.N", "MSFT.O", "AAPL.O");
        marketDataProvider.subscribe(symbols);
        
        // Wait for prices to arrive
        Thread.sleep(5000);
        
        // Get prices
        Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
        
        // Validate data quality
        DataQualityReport report = validator.validate(prices);
        
        // Assert quality standards
        assertThat(report.getQualityScore()).isGreaterThan(95.0);
        assertThat(report.getErrors()).isEqualTo(0);
        
        // Log report
        System.out.println("=== Data Quality Report ===");
        System.out.println(report.getSummary());
        
        // Print issues if any
        if (!report.getIssues().isEmpty()) {
            System.out.println("\nIssues found:");
            report.getIssues().forEach((ric, issues) -> {
                System.out.println("  " + ric + ":");
                issues.forEach(issue -> 
                    System.out.println("    [" + issue.getLevel() + "] " + 
                        issue.getDimension() + ": " + issue.getMessage())
                );
            });
        }
    }
}
```

---

## 📈 **Real-Time Monitoring**

### **1. Quality Metrics**

```java
package com.srviswan.basketpricing.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DataQualityMetrics {
    
    private final Counter validPrices;
    private final Counter invalidPrices;
    private final Counter stalePrices;
    private final Counter inconsistentPrices;
    
    private volatile double currentQualityScore = 100.0;
    
    public DataQualityMetrics(MeterRegistry registry) {
        this.validPrices = Counter.builder("pricing.data.quality.valid")
            .description("Number of valid price updates")
            .register(registry);
            
        this.invalidPrices = Counter.builder("pricing.data.quality.invalid")
            .description("Number of invalid price updates")
            .register(registry);
            
        this.stalePrices = Counter.builder("pricing.data.quality.stale")
            .description("Number of stale price updates")
            .register(registry);
            
        this.inconsistentPrices = Counter.builder("pricing.data.quality.inconsistent")
            .description("Number of inconsistent price updates")
            .register(registry);
            
        Gauge.builder("pricing.data.quality.score", () -> currentQualityScore)
            .description("Overall data quality score (0-100)")
            .register(registry);
    }
    
    public void recordValid() {
        validPrices.increment();
        updateQualityScore();
    }
    
    public void recordInvalid() {
        invalidPrices.increment();
        updateQualityScore();
    }
    
    public void recordStale() {
        stalePrices.increment();
        updateQualityScore();
    }
    
    public void recordInconsistent() {
        inconsistentPrices.increment();
        updateQualityScore();
    }
    
    private void updateQualityScore() {
        double total = validPrices.count() + invalidPrices.count();
        if (total > 0) {
            currentQualityScore = (validPrices.count() / total) * 100.0;
        }
    }
}
```

### **2. Alerting Rules**

```yaml
# prometheus/alerts.yml
groups:
  - name: data_quality
    interval: 30s
    rules:
      # Alert if quality score drops below 95%
      - alert: LowDataQualityScore
        expr: pricing_data_quality_score < 95
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Data quality score is low"
          description: "Quality score is {{ $value }}%"
      
      # Alert if too many invalid prices
      - alert: HighInvalidPriceRate
        expr: rate(pricing_data_quality_invalid_total[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High rate of invalid prices"
          description: "{{ $value }} invalid prices per second"
      
      # Alert if prices are stale
      - alert: StalePriceData
        expr: rate(pricing_data_quality_stale_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Stale price data detected"
          description: "{{ $value }} stale prices per second"
```

---

## 🧪 **Test Scenarios**

### **Scenario 1: Normal Operation**
```java
@Test
public void testNormalPriceData() {
    PriceSnapshot price = PriceSnapshot.builder()
        .ric("IBM.N")
        .bid(150.25)
        .ask(150.30)
        .last(150.27)
        .volume(1000000L)
        .timestamp(Instant.now())
        .build();
    
    DataQualityReport report = validator.validate(Map.of("IBM.N", price));
    
    assertThat(report.getQualityScore()).isEqualTo(100.0);
    assertThat(report.getErrors()).isEqualTo(0);
}
```

### **Scenario 2: Missing Fields**
```java
@Test
public void testMissingFields() {
    PriceSnapshot price = PriceSnapshot.builder()
        .ric("IBM.N")
        .bid(150.25)
        // Missing ask, last, timestamp
        .build();
    
    DataQualityReport report = validator.validate(Map.of("IBM.N", price));
    
    assertThat(report.getErrors()).isGreaterThan(0);
    assertThat(report.getQualityScore()).isLessThan(100.0);
}
```

### **Scenario 3: Inverted Bid-Ask**
```java
@Test
public void testInvertedBidAsk() {
    PriceSnapshot price = PriceSnapshot.builder()
        .ric("IBM.N")
        .bid(150.30)  // Higher than ask!
        .ask(150.25)
        .last(150.27)
        .timestamp(Instant.now())
        .build();
    
    DataQualityReport report = validator.validate(Map.of("IBM.N", price));
    
    assertThat(report.getErrors()).isGreaterThan(0);
    List<DataQualityReport.Issue> issues = report.getIssues().get("IBM.N");
    assertThat(issues).anyMatch(i -> i.getDimension().equals("CONSISTENCY"));
}
```

### **Scenario 4: Stale Data**
```java
@Test
public void testStaleData() {
    PriceSnapshot price = PriceSnapshot.builder()
        .ric("IBM.N")
        .bid(150.25)
        .ask(150.30)
        .last(150.27)
        .timestamp(Instant.now().minus(Duration.ofMinutes(5)))  // 5 minutes old
        .build();
    
    DataQualityReport report = validator.validate(Map.of("IBM.N", price));
    
    assertThat(report.getWarnings()).isGreaterThan(0);
}
```

### **Scenario 5: Invalid Prices**
```java
@Test
public void testInvalidPrices() {
    PriceSnapshot price = PriceSnapshot.builder()
        .ric("IBM.N")
        .bid(-150.25)  // Negative price!
        .ask(150.30)
        .last(150.27)
        .timestamp(Instant.now())
        .build();
    
    DataQualityReport report = validator.validate(Map.of("IBM.N", price));
    
    assertThat(report.getErrors()).isGreaterThan(0);
}
```

---

## 📊 **Grafana Dashboard**

### **Data Quality Dashboard JSON**
```json
{
  "dashboard": {
    "title": "Pricing Data Quality",
    "panels": [
      {
        "title": "Quality Score",
        "targets": [{
          "expr": "pricing_data_quality_score"
        }],
        "type": "gauge",
        "thresholds": [
          {"value": 95, "color": "green"},
          {"value": 90, "color": "yellow"},
          {"value": 0, "color": "red"}
        ]
      },
      {
        "title": "Valid vs Invalid Prices",
        "targets": [
          {"expr": "rate(pricing_data_quality_valid_total[5m])", "legendFormat": "Valid"},
          {"expr": "rate(pricing_data_quality_invalid_total[5m])", "legendFormat": "Invalid"}
        ],
        "type": "graph"
      },
      {
        "title": "Data Quality Issues",
        "targets": [
          {"expr": "rate(pricing_data_quality_stale_total[5m])", "legendFormat": "Stale"},
          {"expr": "rate(pricing_data_quality_inconsistent_total[5m])", "legendFormat": "Inconsistent"}
        ],
        "type": "graph"
      }
    ]
  }
}
```

---

## 🎯 **Best Practices**

### **1. Define Quality Thresholds**
```yaml
# application.yaml
data-quality:
  thresholds:
    min-quality-score: 95.0
    max-spread-percentage: 10.0
    max-age-seconds: 60
    min-price: 0.01
    max-price: 1000000.0
```

### **2. Automated Testing**
```bash
# Run data quality tests in CI/CD
mvn test -Dtest=DataQualityIntegrationTest

# Fail build if quality score < 95%
if [ $QUALITY_SCORE -lt 95 ]; then
    echo "Data quality check failed!"
    exit 1
fi
```

### **3. Production Monitoring**
- Real-time quality metrics
- Alerting on anomalies
- Daily quality reports
- Trend analysis

### **4. Data Lineage**
- Track data source
- Record transformations
- Audit trail
- Version control

### **5. Reconciliation**
```java
// Compare with alternative data source
public void reconcile(Map<String, PriceSnapshot> primary, 
                      Map<String, PriceSnapshot> secondary) {
    for (String ric : primary.keySet()) {
        PriceSnapshot p1 = primary.get(ric);
        PriceSnapshot p2 = secondary.get(ric);
        
        if (p2 != null) {
            double diff = Math.abs(p1.getLast() - p2.getLast());
            double diffPercent = (diff / p1.getLast()) * 100;
            
            if (diffPercent > 1.0) {  // More than 1% difference
                log.warn("Price mismatch for {}: {}% difference", ric, diffPercent);
            }
        }
    }
}
```

---

## 📋 **Quality Checklist**

- [ ] All required fields present
- [ ] Prices are positive numbers
- [ ] Bid ≤ Last ≤ Ask
- [ ] Spread within acceptable range
- [ ] Data age < threshold
- [ ] Timestamps are valid
- [ ] No duplicate updates
- [ ] Precision is correct
- [ ] Volume is non-negative
- [ ] RICs are well-formed

---

## 🚀 **Quick Start**

```bash
# 1. Add validator to your service
@Autowired
private DataQualityValidator validator;

# 2. Validate prices
Map<String, PriceSnapshot> prices = marketDataProvider.getLatestPrices(symbols);
DataQualityReport report = validator.validate(prices);

# 3. Check quality
if (!report.isHealthy()) {
    log.error("Data quality issues: {}", report.getSummary());
}

# 4. Monitor metrics
curl http://localhost:8081/actuator/metrics/pricing.data.quality.score
```

---

## 📚 **References**

- [Data Quality Dimensions](https://en.wikipedia.org/wiki/Data_quality)
- [Financial Data Quality Standards](https://www.iso.org/standard/50798.html)
- [Testing Best Practices](https://martinfowler.com/testing/)

---

**Your pricing data quality is now testable and monitorable!** 🎉

