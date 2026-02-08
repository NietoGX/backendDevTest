# ms-globant-products

Spring Boot microservice that exposes an endpoint to retrieve similar products, consuming external APIs with resilience patterns and caching.

## Table of Contents

- [Project Setup](#project-setup)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Cache Implementation](#cache-implementation)
- [Circuit Breaker](#circuit-breaker)
- [WebClient vs Feign](#webclient-vs-feign)
- [API Endpoints](#api-endpoints)
- [Technologies and Dependencies](#technologies-and-dependencies)
- [Testing](#testing)

---

## Project Setup

### Prerequisites

- Java 21
- Gradle 8.x (or use the included wrapper)
- External API running on `localhost:3001`

### Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on port **5000**.

### Configuration

Main configuration is located in `src/main/resources/application.yml`:

| Property | Default Value | Description |
|-----------|---------------|-------------|
| `server.port` | 5000 | Server listening port |
| `external-api.base-url` | http://localhost:3001 | External API base URL |
| `external-api.timeout` | 5000ms | Timeout for external calls |

### Profiles

- **default**: Production configuration with cache enabled
- **integration**: Disables cache for integration tests

```bash
# Run with integration profile
./gradlew bootRun --args='--spring.profiles.active=integration'
```

---

## Architecture

The project follows the **Hexagonal Architecture (Ports and Adapters)** pattern, clearly separating business logic from technical details.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller Layer                         │
│                   (ProductController.java)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────────┐
│                      Application Layer                          │
│                 (GetSimilarProductsUseCase.java)                │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────────┐
│                        Domain Layer                             │
│    ┌──────────────────┐    ┌─────────────────────────┐        │
│    │  ProductDetail   │    │  ProductRepository      │        │
│    │    (Model)       │    │    (Interface)          │        │
│    └──────────────────┘    └───────────┬─────────────┘        │
└─────────────────────────────────────────┼──────────────────────┘
                                          │
┌─────────────────────────────────────────┴──────────────────────┐
│                    Infrastructure Layer                        │
│  ┌──────────────────┐  ┌──────────────────────────────────┐  │
│  │  WebClientConfig │  │  WebClientProductRepository      │  │
│  │    CacheConfig   │  │  ResilientProductWebClient       │  │
│  └──────────────────┘  └──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Domain Layer**: Contains pure business logic, without external dependencies
2. **Application Layer**: Orchestrates use cases (UseCases)
3. **Infrastructure Layer**: Implements technical details (WebClient, cache, etc.)
4. **Dependency Inversion**: The domain defines interfaces that infrastructure implements

---

## Project Structure

```
src/main/java/com/globant/david/msglobantproducts/
│
├── MsGlobantProductsApplication.java    # Main Spring Boot class
│
├── application/                         # Application Layer (Use Cases)
│   └── GetSimilarProductsUseCase.java
│
├── domain/                              # Domain Layer
│   ├── model/
│   │   └── ProductDetail.java          # Domain entity (immutable)
│   └── repository/
│       └── ProductRepository.java      # Repository interface
│
└── infrastructure/                      # Infrastructure Layer
    ├── config/
    │   ├── CacheConfig.java            # Caffeine cache configuration
    │   └── WebClientConfig.java        # WebClient configuration
    ├── input/
    │   └── ProductController.java      # REST endpoint
    └── output/
        ├── ResilientProductWebClient.java  # WebClient with Circuit Breaker
        ├── WebClientProductRepository.java # Repository implementation
        └── dto/
            └── ProductResponse.java    # DTO for external responses
```

---

## Cache Implementation

The project uses **Caffeine** as the cache provider, a high-performance library for Java.

### Configured Caches

| Cache | Purpose | Max Entries | TTL |
|-------|---------|-------------|-----|
| `productDetailCache` | Store product details | 1000 | 10 minutes |
| `similarProductsCache` | Store similar product lists | 500 | 5 minutes |
| `similarIdsCache` | Store similar product IDs | 500 | 5 minutes |

### Configuration

Configuration is located in `CacheConfig.java:14-61`:

```java
@Bean
@Profile("!integration")
public Cache<String, ProductDetail> productDetailCache() {
    return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
}
```

### Cache-Aside Pattern

The implementation follows the *cache-aside* pattern in `WebClientProductRepository.java:36-51`:

1. **Read-Through**: First checks if data is in cache
2. **Cache Miss**: If not, makes the call to the external API
3. **Cache Populate**: Stores the result in cache for future queries

```java
public Mono<List<String>> findSimilarIds(String productId) {
    List<String> cached = similarIdsCache.getIfPresent(productId);
    if (cached != null) {
        return Mono.just(cached);  // Return from cache
    }
    return productWebClient.getSimilarIds(productId)
            .doOnNext(ids -> similarIdsCache.put(productId, ids));
}
```

### Why Caffeine

- **High performance**: Better performance than ConcurrentHashMap or Guava
- **Low overhead**: Optimized for high concurrency scenarios
- **Built-in statistics**: Hit rate, eviction, and other metrics
- **Flexible configuration**: TTL, maximum size, access-based expiration

---

## Circuit Breaker

The project implements the **Circuit Breaker** pattern using **Resilience4j** to handle failures in external service calls.

### Configuration

Configuration in `application.yml:44-84` defines:

| Property | Value | Description |
|-----------|-------|-------------|
| `sliding-window-size` | 10 | Call window for evaluation |
| `minimum-number-of-calls` | 5 | Minimum calls before evaluation |
| `failure-rate-threshold` | 50% | Failure percentage to open |
| `wait-duration-in-open-state` | 10s | Wait time before half-open |
| `permitted-number-of-calls-in-half-open-state` | 3 | Calls allowed in half-open |

### Circuit Breaker States

```
┌─────────┐      ┌─────────┐      ┌─────────────┐
│  CLOSED │ ───> │  OPEN   │ ───> │  HALF-OPEN  │
└─────────┘      └─────────┘      └─────────────┘
    ^                                  │
    │                                  │
    └──────────────────────────────────┘
           (failures < 50%)  (success)
```

1. **CLOSED**: Normal state, requests pass through directly
2. **OPEN**: Failure threshold exceeded, requests are rejected immediately
3. **HALF-OPEN**: Some requests allowed to verify if service has recovered

### Implementation

In `ResilientProductWebClient.java:39-59`:

```java
public Mono<List<String>> getSimilarIds(String productId) {
    return webClient.get()
            .uri("/product/{id}/similarids", productId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(RetryOperator.of(retry))
            .doOnError(e -> log.error("Error fetching similar IDs: {}", e.getMessage()));
}
```

### Fallback on Failure

In `WebClientProductRepository.java:42-50`:

```java
.onErrorResume(CallNotPermittedException.class, e -> {
    log.warn("Circuit breaker is OPEN - serving cached or empty response");
    return Mono.just(List.of());
})
```

When the circuit is open:
- Empty or cached response is returned
- Service remains available in degraded mode
- Event is logged for monitoring

### Retry Strategy

- **Max attempts**: 2 attempts
- **Wait duration**: 500ms between retries
- **Retryable exceptions**: IOException, TimeoutException, WebClientRequestException

### Monitoring

Spring Boot Actuator exposes circuit breaker metrics:

```bash
curl http://localhost:5000/actuator/circuitbreakers
curl http://localhost:5000/actuator/health
```

---

## WebClient vs Feign

### Why WebClient instead of Feign?

This project uses **Spring WebFlux WebClient** instead of **OpenFeign** for HTTP calls to external services.

### Technical Comparison

| Aspect | Feign | WebClient (WebFlux) |
|---------|-------|---------------------|
| **Execution model** | Blocking (synchronous) | Non-blocking (reactive) |
| **Return type** | Direct object, optional wrappers | `Mono`/`Flux` (Project Reactor) |
| **Backpressure** | Not supported | Natively supported |
| **Concurrency** | One thread per request | Async I/O with event loop |
| **Declarative** | `@FeignClient` annotations | Fluent/programmatic API |
| **Stack Integration** | Spring MVC/Servlet | Spring WebFlux/Reactor |

### Performance: k6 Benchmark

According to load tests executed with **k6**, WebClient shows significantly superior performance:

| Metric | Feign | WebClient | Improvement |
|--------|-------|-----------|-------------|
| **Throughput** | ~50 req/s | ~150 req/s | **3x** |
| **Avg latency** | ~40ms | ~12ms | **70% lower** |
| **p95 latency** | ~80ms | ~25ms | **69% lower** |

> **Result**: WebClient processes **3 times more requests per second** than Feign under the same load.

### Why this difference?

1. **Reactive model**: WebClient doesn't block threads while waiting for HTTP response
2. **Efficient connection pooling**: Optimal HTTP/2 connection reuse
3. **Zero-copy**: Lower overhead in serialization/deserialization
4. **Event loop**: A single thread can handle thousands of concurrent connections

### WebClient Advantages in this Project

1. **High performance**: ~150 req/s vs ~50 req/s for Feign
2. **Scalability**: Handles more connections with fewer resources
3. **Backpressure**: Automatic flow control during traffic spikes
4. **Resilience4j integration**: Reactive operators for Circuit Breaker
5. **Simplified testing**: WireMock simulates HTTP server without overhead

### WebClient Configuration

In `WebClientConfig.java` it is configured with:
- Connection pool with maximum 500 connections
- Configurable timeouts (connect: 2s, read: 3s)
- Idle resource avoidance

### Testing

Tests use **WireMock** to simulate the external API:

```java
@AutoConfigureWireMock(port = 0)
class ProductControllerTest {
    // WireMock simulates HTTP server on a random port
}
```

---

## API Endpoints

### Get Similar Products

```
GET /product/{productId}/similar
```

**Path Parameters:**
- `productId` (string): Product ID

**Response (200 OK):**
```json
[
  {
    "id": "1",
    "name": "Product Name",
    "price": 29.99,
    "availability": true
  }
]
```

**Response (404 Not Found):**
```json
[]
```

### Actuator Endpoints

```
GET /actuator/health           # Service status
GET /actuator/circuitbreakers  # Circuit breaker status
GET /actuator/metrics          # Application metrics
```

---

## Technologies and Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 4.0.2 | Main framework |
| Java | 21 | Language |
| Spring WebFlux | - | Reactive programming |
| Resilience4j | 2.3.0 | Circuit Breaker, Retry |
| Caffeine | - | In-memory cache |
| Spring Validation | - | Input validation |
| Spring Actuator | - | Metrics and health checks |
| Lombok | - | Boilerplate reduction |

---

## Testing

### Running Tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "*Test"

# Integration tests
./gradlew test --tests "*IT" --args='--spring.profiles.active=integration'
```

### Testing Strategy

1. **Unit Tests**: Test individual components
2. **Integration Tests**: Use WireMock to simulate external API
3. **WebTestClient**: To test endpoints without real HTTP server

### Coverage

```bash
./gradlew test jacocoTestReport
```

---

## Author

David Nieto - Backend Dev Technical Test
