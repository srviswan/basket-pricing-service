# Client Examples

This directory contains example clients demonstrating how to interact with the basket pricing service using both REST and gRPC APIs.

## Files

### 1. RestApiClientExample.java
Demonstrates REST API usage with HTTP client:
- Get current prices for symbols
- Subscribe to price updates
- Unsubscribe from symbols
- Get current subscriptions
- Continuous price polling
- Error handling and retry logic

### 2. GrpcClientExample.java
Demonstrates gRPC API usage:
- Get current prices (blocking calls)
- Subscribe to symbols (blocking calls)
- Stream real-time price updates (async calls)
- Unsubscribe from symbols
- Async operations with callbacks
- Continuous price polling
- Error handling and retry logic

### 3. ClientComparisonExample.java
Compares REST and gRPC approaches:
- Performance benchmarking
- Feature comparison
- Use case recommendations
- Streaming capabilities demonstration

## Running the Examples

### Prerequisites
1. Start the pricing service:
   ```bash
   mvn spring-boot:run
   ```

2. Ensure the service is running on:
   - REST API: `http://localhost:8080`
   - gRPC API: `localhost:9090`

### Running Individual Examples

#### REST API Client
```bash
cd src/test/java/com/srviswan/basketpricing/examples
javac -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" RestApiClientExample.java
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.srviswan.basketpricing.examples.RestApiClientExample
```

#### gRPC Client
```bash
cd src/test/java/com/srviswan/basketpricing/examples
javac -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" GrpcClientExample.java
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.srviswan.basketpricing.examples.GrpcClientExample
```

#### Client Comparison
```bash
cd src/test/java/com/srviswan/basketpricing/examples
javac -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" ClientComparisonExample.java
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" com.srviswan.basketpricing.examples.ClientComparisonExample
```

### Using Maven Test Runner
```bash
# Run REST API example
mvn test -Dtest=RestApiClientExample

# Run gRPC example
mvn test -Dtest=GrpcClientExample

# Run comparison example
mvn test -Dtest=ClientComparisonExample
```

## API Endpoints

### REST API Endpoints

#### Get Current Prices
```http
GET /api/prices?symbols=IBM.N,MSFT.O,AAPL.O
```

#### Subscribe to Symbols
```http
POST /api/prices/subscribe?symbols=IBM.N,MSFT.O
```

#### Unsubscribe from Symbols
```http
DELETE /api/prices/unsubscribe?symbols=IBM.N
```

#### Get Current Subscriptions
```http
GET /api/prices/subscriptions
```

### gRPC API Methods

#### Get Prices
```protobuf
rpc GetPrices(GetPricesRequest) returns (GetPricesResponse);
```

#### Subscribe
```protobuf
rpc Subscribe(SubscribeRequest) returns (SubscribeResponse);
```

#### Unsubscribe
```protobuf
rpc Unsubscribe(UnsubscribeRequest) returns (UnsubscribeResponse);
```

#### Get Subscriptions
```protobuf
rpc GetSubscriptions(GetSubscriptionsRequest) returns (GetSubscriptionsResponse);
```

#### Stream Real-time Updates
```protobuf
rpc StreamPrices(StreamPricesRequest) returns (stream PriceUpdate);
```

## Example Output

### REST API Example Output
```
2025-01-03 22:30:15.123 INFO  - Starting REST API Client Example
2025-01-03 22:30:15.124 INFO  - === Getting Current Prices ===
2025-01-03 22:30:15.156 INFO  - Prices Response: {"IBM.N":{"symbol":"IBM.N","bid":150.0,"ask":150.5,"last":150.25,"timestamp":"2025-01-03T22:30:15.123Z"}}
2025-01-03 22:30:15.157 INFO  - Symbol: IBM.N - Price: {symbol=IBM.N, bid=150.0, ask=150.5, last=150.25, timestamp=2025-01-03T22:30:15.123Z}
2025-01-03 22:30:15.158 INFO  - === Subscribing to Symbols ===
2025-01-03 22:30:15.189 INFO  - Subscription Response: {"subscribed":["IBM.N","MSFT.O"],"totalSubscriptions":2,"backpressureStatus":{"queueUtilization":0.3,"processedUpdates":100,"droppedUpdates":5}}
2025-01-03 22:30:15.190 INFO  - Subscribed to 2 symbols
2025-01-03 22:30:15.191 INFO  - Total subscriptions: 2
2025-01-03 22:30:15.192 INFO  - Backpressure - Queue utilization: 0.3%, Processed updates: 100, Dropped updates: 5
```

### gRPC Example Output
```
2025-01-03 22:30:15.123 INFO  - Starting gRPC Client Example
2025-01-03 22:30:15.124 INFO  - === Getting Current Prices (Blocking) ===
2025-01-03 22:30:15.145 INFO  - Received 3 price snapshots:
2025-01-03 22:30:15.146 INFO  - Symbol: IBM.N, Bid: 150.0, Ask: 150.5, Last: 150.25, Timestamp: 1704319815123
2025-01-03 22:30:15.147 INFO  - Symbol: MSFT.O, Bid: 300.0, Ask: 300.5, Last: 300.25, Timestamp: 1704319815123
2025-01-03 22:30:15.148 INFO  - Symbol: AAPL.O, Bid: 180.0, Ask: 180.5, Last: 180.25, Timestamp: 1704319815123
2025-01-03 22:30:15.149 INFO  - === Subscribing to Symbols (Blocking) ===
2025-01-03 22:30:15.167 INFO  - Successfully subscribed to 2 symbols
2025-01-03 22:30:15.168 INFO  - Subscribed symbols: [IBM.N, MSFT.O]
2025-01-03 22:30:15.169 INFO  - Total subscriptions: 2
2025-01-03 22:30:15.170 INFO  - Message: Successfully subscribed to 2 symbols
```

## Performance Comparison

The `ClientComparisonExample` demonstrates performance differences:

```
Performance Results (10 iterations):
  REST API - Total: 1250ms, Average: 125.00ms
  gRPC API - Total: 890ms, Average: 89.00ms
  gRPC is 28.8% faster than REST
```

## Use Case Recommendations

### Use REST API when:
- Building web applications
- Simple request/response patterns
- Need browser compatibility
- Quick prototyping
- Third-party integrations
- Microservices with different languages

### Use gRPC API when:
- High-performance requirements
- Real-time data streaming
- Internal service communication
- Mobile applications
- Microservices with same language
- Need strong typing
- Bidirectional communication

## Error Handling

Both examples include comprehensive error handling:
- Connection timeouts
- Retry logic with exponential backoff
- Graceful degradation
- Detailed error logging

## Dependencies

The examples require the following dependencies (already included in the project):
- `java.net.http.HttpClient` (Java 11+)
- `com.fasterxml.jackson.databind.ObjectMapper`
- `io.grpc.ManagedChannel`
- `io.grpc.stub.StreamObserver`
- `lombok.extern.slf4j.Slf4j`

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure the pricing service is running
   - Check port numbers (8080 for REST, 9090 for gRPC)
   - Verify firewall settings

2. **gRPC Connection Issues**
   - Use `usePlaintext()` for local development
   - Check gRPC server configuration
   - Verify protobuf definitions

3. **Timeout Issues**
   - Increase timeout values
   - Check network connectivity
   - Monitor service performance

### Debug Mode
Enable debug logging by setting the log level:
```bash
export LOG_LEVEL=DEBUG
```

## Contributing

When adding new examples:
1. Follow the existing code structure
2. Include comprehensive error handling
3. Add detailed logging
4. Update this README
5. Test with the actual service
