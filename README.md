# Basket Pricing Service

A real-time pricing service that streams market data from Refinitiv using EMA (RTSDK) and exposes it via REST and gRPC APIs.

## Features

- **Real-time Market Data**: Stream prices from Refinitiv EMA
- **Dual API Support**: REST and gRPC endpoints
- **Resilience**: Circuit breaker, rate limiting, and retry mechanisms
- **Monitoring**: Prometheus metrics and Grafana dashboards
- **Backpressure Management**: Handle high-frequency price updates
- **Caching**: Caffeine-based local caching
- **Provider Agnostic**: Abstract market data provider interface

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Refinitiv     │    │  Pricing Service │    │     Clients     │
│      EMA        │◄──►│                  │◄──►│                 │
│                 │    │  ┌─────────────┐ │    │  ┌─────────────┐ │
└─────────────────┘    │  │   REST API  │ │    │  │    Web      │ │
                       │  └─────────────┘ │    │  └─────────────┘ │
                       │  ┌─────────────┐ │    │  ┌─────────────┐ │
                       │  │   gRPC API  │ │    │  │   Mobile    │ │
                       │  └─────────────┘ │    │  └─────────────┘ │
                       │  ┌─────────────┐ │    │  ┌─────────────┐ │
                       │  │ Monitoring  │ │    │  │  Analytics  │ │
                       │  └─────────────┘ │    │  └─────────────┘ │
                       └──────────────────┘    └─────────────────┘
```

## Quick Start

### Prerequisites

- Java 21
- Maven 3.6+
- Docker & Docker Compose
- Refinitiv EMA credentials

### Environment Variables

```bash
export REFINITIV_HOST=your-refinitiv-host
export REFINITIV_PORT=14002
export REFINITIV_USER=your-username
export REFINITIV_SERVICE=ELEKTRON_DD
```

### Running the Service

```bash
# Build the project
mvn clean package

# Run with Docker Compose (includes monitoring stack)
docker-compose up -d

# Or run directly
java -jar target/basket-pricing-service-0.1.0-SNAPSHOT.jar
```

## API Endpoints

### REST API (Port 8080)

#### Get Current Prices
```bash
GET /api/prices?symbols=IBM.N,MSFT.O
```

#### Subscribe to Symbols
```bash
POST /api/prices/subscribe?symbols=IBM.N,MSFT.O
```

#### Unsubscribe from Symbols
```bash
DELETE /api/prices/unsubscribe?symbols=IBM.N
```

#### Get Current Subscriptions
```bash
GET /api/prices/subscriptions
```

### gRPC API (Port 9090)

#### Get Prices
```protobuf
rpc GetPrices(GetPricesRequest) returns (GetPricesResponse);
```

#### Subscribe
```protobuf
rpc Subscribe(SubscribeRequest) returns (SubscribeResponse);
```

#### Stream Real-time Updates
```protobuf
rpc StreamPrices(StreamPricesRequest) returns (stream PriceUpdate);
```

## Monitoring

### Prometheus Metrics
- `pricing_connection_status`: Refinitiv connection status
- `pricing_subscriptions_active`: Number of active subscriptions
- `pricing_updates_total`: Total price updates received
- `pricing_api_requests_total`: API request count
- `pricing_backpressure_queue_utilization`: Backpressure queue usage

### Grafana Dashboards
Access Grafana at `http://localhost:3000` (admin/admin)

### Health Checks
- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/metrics`
- `http://localhost:8080/actuator/prometheus`

## Configuration

### Application Properties

```yaml
server:
  port: 8080

grpc:
  server:
    port: 9090

refinitiv:
  host: ${REFINITIV_HOST}
  port: ${REFINITIV_PORT:14002}
  user: ${REFINITIV_USER}
  service: ${REFINITIV_SERVICE:ELEKTRON_DD}

resilience4j:
  circuitbreaker:
    instances:
      marketDataProvider:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  ratelimiter:
    instances:
      pricingApi:
        limit-for-period: 200
        limit-refresh-period: 1s

spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=30s
```

## Development

### Running Tests
```bash
mvn test
```

### Building Docker Image
```bash
docker build -t basket-pricing-service .
```

### Code Structure
```
src/main/java/com/srviswan/basketpricing/
├── api/                    # REST controllers
├── grpc/                   # gRPC services
├── marketdata/             # Market data interfaces
│   └── refinitiv/         # Refinitiv EMA implementation
├── monitoring/             # Metrics and health checks
├── resilience/             # Circuit breaker, rate limiting
└── config/                 # Configuration classes
```

## Performance

### Benchmarks
- **REST API**: <500ms P95 response time
- **gRPC API**: <10ms P99 response time
- **Price Updates**: <1ms P99 processing latency
- **Throughput**: 10,000+ updates/second

### Scaling
- Horizontal scaling with load balancer
- Redis clustering for distributed caching
- Kafka for high-throughput streaming

## Troubleshooting

### Common Issues

1. **Connection to Refinitiv fails**
   - Check `REFINITIV_HOST` and `REFINITIV_PORT`
   - Verify network connectivity
   - Check EMA credentials

2. **High memory usage**
   - Monitor backpressure queue utilization
   - Adjust cache size limits
   - Check for memory leaks in price updates

3. **Slow API responses**
   - Check circuit breaker status
   - Monitor rate limiter rejections
   - Verify cache hit rates

### Logs
```bash
# View application logs
docker-compose logs -f basket-pricing-service

# View Prometheus logs
docker-compose logs -f prometheus

# View Grafana logs
docker-compose logs -f grafana
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the monitoring dashboards