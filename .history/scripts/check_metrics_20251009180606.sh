#!/bin/bash

# Quick metrics check script for pricing service

BASE_URL="http://localhost:8080"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║         Basket Pricing Service - Metrics Summary           ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${BLUE}📊 Active Subscriptions:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.subscriptions.active | jq '.measurements[0].value'

echo -e "\n${BLUE}📈 Total Price Updates:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.updates.count | jq '.measurements[0].value'

echo -e "\n${BLUE}🌐 Total API Requests:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.api.requests | jq '.measurements[0].value'

echo -e "\n${BLUE}🔗 Connection Status:${NC}"
conn_status=$(curl -s $BASE_URL/actuator/metrics/pricing.connection.status | jq -r '.measurements[0].value')
if [ "$conn_status" = "1.0" ]; then
    echo -e "${GREEN}Connected ✅${NC}"
else
    echo -e "Disconnected ❌"
fi

echo -e "\n${BLUE}⚡ Backpressure Utilization:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.backpressure.utilization | jq '.measurements[0].value'

echo -e "\n${BLUE}✅ Processed Updates:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.backpressure.processed | jq '.measurements[0].value'

echo -e "\n${BLUE}❌ Dropped Updates:${NC}"
curl -s $BASE_URL/actuator/metrics/pricing.backpressure.dropped | jq '.measurements[0].value'

echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "\n${BLUE}💾 Memory Usage:${NC}"
mem_used=$(curl -s $BASE_URL/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value')
mem_max=$(curl -s $BASE_URL/actuator/metrics/jvm.memory.max | jq -r '.measurements[0].value')

if [ "$mem_used" != "null" ] && [ "$mem_max" != "null" ]; then
    # Convert to MB using awk
    mem_used_mb=$(awk "BEGIN {printf \"%.0f\", $mem_used / 1048576}")
    mem_max_mb=$(awk "BEGIN {printf \"%.0f\", $mem_max / 1048576}")
    mem_pct=$(awk "BEGIN {printf \"%.1f\", ($mem_used / $mem_max) * 100}")
    echo "${mem_used_mb}MB / ${mem_max_mb}MB (${mem_pct}%)"
else
    echo "N/A"
fi

echo -e "\n${BLUE}🖥️  CPU Usage:${NC}"
cpu=$(curl -s $BASE_URL/actuator/metrics/system.cpu.usage | jq -r '.measurements[0].value')
if [ "$cpu" != "null" ]; then
    # Convert to percentage using awk
    cpu_pct=$(awk "BEGIN {printf \"%.1f\", $cpu * 100}")
    echo "${cpu_pct}%"
else
    echo "N/A"
fi

echo -e "\n${BLUE}🧵 Active Threads:${NC}"
curl -s $BASE_URL/actuator/metrics/jvm.threads.live | jq '.measurements[0].value'

echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "\n${GREEN}For detailed diagnostics, run:${NC}"
echo -e "${BLUE}  curl http://localhost:8080/api/prices/diagnostics | jq '.'${NC}"
echo -e "\n${GREEN}For real-time dashboard, run:${NC}"
echo -e "${BLUE}  ./scripts/monitor_dashboard.sh${NC}"
echo ""

