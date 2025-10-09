#!/bin/bash

# Real-time monitoring dashboard for pricing service

BASE_URL="http://localhost:8080"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

get_metric() {
    local metric_name=$1
    curl -s "$BASE_URL/actuator/metrics/$metric_name" 2>/dev/null | jq -r '.measurements[0].value' 2>/dev/null || echo "N/A"
}

while true; do
    clear
    print_color $PURPLE "╔════════════════════════════════════════════════════════════╗"
    print_color $PURPLE "║     Basket Pricing Service - Live Monitoring Dashboard     ║"
    print_color $PURPLE "╚════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Timestamp
    print_color $CYAN "📅 $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    # Connection Status
    conn_status=$(get_metric "pricing.connection.status")
    if [ "$conn_status" = "1.0" ]; then
        print_color $GREEN "🟢 Connection Status: CONNECTED"
    elif [ "$conn_status" = "0.0" ]; then
        print_color $RED "🔴 Connection Status: DISCONNECTED"
    else
        print_color $YELLOW "⚪ Connection Status: UNKNOWN"
    fi
    
    # Active Subscriptions
    subs=$(get_metric "pricing.subscriptions.active")
    print_color $BLUE "📊 Active Subscriptions: $subs"
    
    # Price Updates
    updates=$(get_metric "pricing.updates.count")
    print_color $BLUE "📈 Total Price Updates: $updates"
    
    # API Requests
    requests=$(get_metric "pricing.api.requests")
    print_color $BLUE "🌐 Total API Requests: $requests"
    
    echo ""
    print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_color $CYAN "                    BACKPRESSURE STATUS"
    print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # Backpressure Metrics
    bp_util=$(get_metric "pricing.backpressure.utilization")
    bp_processed=$(get_metric "pricing.backpressure.processed")
    bp_dropped=$(get_metric "pricing.backpressure.dropped")
    
    # Calculate percentage
    if [ "$bp_util" != "N/A" ]; then
        # Convert to integer percentage (multiply by 100)
        bp_util_pct=$(printf "%.0f" $(awk "BEGIN {print $bp_util * 100}"))
        
        # Compare using integer arithmetic
        if [ "$bp_util_pct" -lt 50 ]; then
            print_color $GREEN "⚡ Queue Utilization: ${bp_util_pct}% (HEALTHY)"
        elif [ "$bp_util_pct" -lt 80 ]; then
            print_color $YELLOW "⚡ Queue Utilization: ${bp_util_pct}% (MODERATE)"
        else
            print_color $RED "⚡ Queue Utilization: ${bp_util_pct}% (HIGH!)"
        fi
    else
        print_color $YELLOW "⚡ Queue Utilization: N/A"
    fi
    
    print_color $BLUE "✅ Processed Updates: $bp_processed"
    
    if [ "$bp_dropped" != "0.0" ] && [ "$bp_dropped" != "N/A" ]; then
        print_color $RED "❌ Dropped Updates: $bp_dropped (WARNING!)"
    else
        print_color $GREEN "✅ Dropped Updates: 0"
    fi
    
    echo ""
    print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_color $CYAN "                    PERFORMANCE METRICS"
    print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # JVM Memory
    mem_used=$(get_metric "jvm.memory.used")
    mem_max=$(get_metric "jvm.memory.max")
    
    if [ "$mem_used" != "N/A" ] && [ "$mem_max" != "N/A" ]; then
        # Convert to MB using awk
        mem_used_mb=$(awk "BEGIN {printf \"%.0f\", $mem_used / 1048576}")
        mem_max_mb=$(awk "BEGIN {printf \"%.0f\", $mem_max / 1048576}")
        mem_pct=$(awk "BEGIN {printf \"%.0f\", ($mem_used / $mem_max) * 100}")
        
        # Compare using integer arithmetic
        if [ "$mem_pct" -lt 70 ]; then
            print_color $GREEN "💾 Memory: ${mem_used_mb}MB / ${mem_max_mb}MB (${mem_pct}%)"
        elif [ "$mem_pct" -lt 85 ]; then
            print_color $YELLOW "💾 Memory: ${mem_used_mb}MB / ${mem_max_mb}MB (${mem_pct}%)"
        else
            print_color $RED "💾 Memory: ${mem_used_mb}MB / ${mem_max_mb}MB (${mem_pct}%) HIGH!"
        fi
    else
        print_color $YELLOW "💾 Memory: N/A"
    fi
    
    # CPU Usage
    cpu=$(get_metric "system.cpu.usage")
    if [ "$cpu" != "N/A" ]; then
        # Convert to integer percentage using awk
        cpu_pct=$(awk "BEGIN {printf \"%.0f\", $cpu * 100}")
        
        # Compare using integer arithmetic
        if [ "$cpu_pct" -lt 50 ]; then
            print_color $GREEN "🖥️  CPU Usage: ${cpu_pct}%"
        elif [ "$cpu_pct" -lt 80 ]; then
            print_color $YELLOW "🖥️  CPU Usage: ${cpu_pct}%"
        else
            print_color $RED "🖥️  CPU Usage: ${cpu_pct}% HIGH!"
        fi
    else
        print_color $YELLOW "🖥️  CPU Usage: N/A"
    fi
    
    # Thread Count
    threads=$(get_metric "jvm.threads.live")
    print_color $BLUE "🧵 Active Threads: $threads"
    
    echo ""
    print_color $CYAN "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    print_color $YELLOW "Press Ctrl+C to exit | Refreshing every 2 seconds..."
    
    sleep 2
done

