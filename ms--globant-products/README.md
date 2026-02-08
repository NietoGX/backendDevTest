# ms-globant-products

Microservicio Spring Boot que expone un endpoint para obtener productos similares, consumiendo APIs externas con patrones de resiliencia y caché.

## Tabla de Contenidos

- [Inicialización del Proyecto](#inicialización-del-proyecto)
- [Arquitectura](#arquitectura)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Implementación de Caché](#implementación-de-caché)
- [Circuit Breaker](#circuit-breaker)
- [WebClient vs WebDriver](#webclient-vs-webdriver)
- [API Endpoints](#api-endpoints)
- [Technologas y Dependencias](#tecnologas-y-dependencias)
- [Testing](#testing)

---

## Inicialización del Proyecto

### Requisitos Previos

- Java 21
- Gradle 8.x (o usar el wrapper incluido)
- API externa corriendo en `localhost:3001`

### Ejecución

```bash
# Compilar el proyecto
./gradlew build

# Ejecutar la aplicación
./gradlew bootRun
```

La aplicación se iniciará en el puerto **5000**.

### Configuración

La configuración principal se encuentra en `src/main/resources/application.yml`:

| Propiedad | Valor por defecto | Descripción |
|-----------|-------------------|-------------|
| `server.port` | 5000 | Puerto de escucha del servidor |
| `external-api.base-url` | http://localhost:3001 | URL base de la API externa |
| `external-api.timeout` | 5000ms | Timeout para llamadas externas |

### Perfiles

- **default**: Configuración de producción con caché habilitado
- **integration**: Deshabilita caché para tests de integración

```bash
# Ejecutar con perfil de integración
./gradlew bootRun --args='--spring.profiles.active=integration'
```

---

## Arquitectura

El proyecto sigue el patrón de **Arquitectura Hexagonal (Ports and Adapters)**, separando claramente la lógica de negocio de los detalles técnicos.

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

### Principios de Diseño

1. **Domain Layer**: Contiene la lógica de negocio pura, sin dependencias externas
2. **Application Layer**: Orquesta los casos de uso (UseCases)
3. **Infrastructure Layer**: Implementa detalles técnicos (WebClient, caché, etc.)
4. **Dependency Inversion**: El dominio define interfaces que la infraestructura implementa

---

## Estructura del Proyecto

```
src/main/java/com/globant/david/msglobantproducts/
│
├── MsGlobantProductsApplication.java    # Clase principal de Spring Boot
│
├── application/                         # Capa de Aplicación (Casos de Uso)
│   └── GetSimilarProductsUseCase.java
│
├── domain/                              # Capa de Dominio
│   ├── model/
│   │   └── ProductDetail.java          # Entidad de dominio (inmutable)
│   └── repository/
│       └── ProductRepository.java      # Interfaz del repositorio
│
└── infrastructure/                      # Capa de Infraestructura
    ├── config/
    │   ├── CacheConfig.java            # Configuración de caché Caffeine
    │   └── WebClientConfig.java        # Configuración de WebClient
    ├── input/
    │   └── ProductController.java      # Endpoint REST
    └── output/
        ├── ResilientProductWebClient.java  # WebClient con Circuit Breaker
        ├── WebClientProductRepository.java # Implementación del repositorio
        └── dto/
            └── ProductResponse.java    # DTO para respuestas externas
```

---

## Implementación de Caché

El proyecto utiliza **Caffeine** como proveedor de caché, una biblioteca de alto rendimiento para Java.

### Caches Configurados

| Cache | Propósito | Máximo Entradas | TTL |
|-------|-----------|-----------------|-----|
| `productDetailCache` | Almacenar detalles de productos | 1000 | 10 minutos |
| `similarProductsCache` | Almacenar listas de productos similares | 500 | 5 minutos |
| `similarIdsCache` | Almacenar IDs de productos similares | 500 | 5 minutos |

### Configuración

La configuración se encuentra en `CacheConfig.java:14-61`:

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

### Patrón Cache-Aside

La implementación sigue el patrón *cache-aside* en `WebClientProductRepository.java:36-51`:

1. **Read-Through**: Primero verifica si el dato está en caché
2. **Cache Miss**: Si no está, hace la llamada a la API externa
3. **Cache Populate**: Almacena el resultado en caché para futuras consultas

```java
public Mono<List<String>> findSimilarIds(String productId) {
    List<String> cached = similarIdsCache.getIfPresent(productId);
    if (cached != null) {
        return Mono.just(cached);  // Retorna desde caché
    }
    return productWebClient.getSimilarIds(productId)
            .doOnNext(ids -> similarIdsCache.put(productId, ids));
}
```

### Por qué Caffeine

- **Alto rendimiento**: Mejor rendimiento que ConcurrentHashMap o Guava
- **Bajo overhead**: Optimizado para escenarios de alta concurrencia
- **Estadísticas integradas**: Métricas de hit rate, eviction, etc.
- **Configuración flexible**: TTL, tamaño máximo, expiración basada en acceso

---

## Circuit Breaker

El proyecto implementa el patrón **Circuit Breaker** usando **Resilience4j** para manejar fallos en llamadas a servicios externos.

### Configuración

La configuración en `application.yml:44-84` define:

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `sliding-window-size` | 10 | Ventana de llamadas para evaluar |
| `minimum-number-of-calls` | 5 | Mínimo de llamadas antes de evaluar |
| `failure-rate-threshold` | 50% | Porcentaje de fallos para abrir |
| `wait-duration-in-open-state` | 10s | Tiempo de espera antes de half-open |
| `permitted-number-of-calls-in-half-open-state` | 3 | Llamadas permitidas en half-open |

### Estados del Circuit Breaker

```
┌─────────┐      ┌─────────┐      ┌─────────────┐
│  CLOSED │ ───> │  OPEN   │ ───> │  HALF-OPEN  │
└─────────┘      └─────────┘      └─────────────┘
    ^                                  │
    │                                  │
    └──────────────────────────────────┘
           (fallos < 50%)  (éxito)
```

1. **CLOSED**: Estado normal, las peticiones pasan directamente
2. **OPEN**: Umbral de fallos superado, se rechazan las peticiones inmediatamente
3. **HALF-OPEN**: Se permiten algunas peticiones para verificar si el servicio se ha recuperado

### Implementación

En `ResilientProductWebClient.java:39-59`:

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

### Fallback ante Fallos

En `WebClientProductRepository.java:42-50`:

```java
.onErrorResume(CallNotPermittedException.class, e -> {
    log.warn("Circuit breaker is OPEN - serving cached or empty response");
    return Mono.just(List.of());
})
```

Cuando el circuito está abierto:
- Se retorna respuesta vacía o desde caché
- El servicio permanece disponible degradadamente
- Se loggea el evento para monitoreo

### Retry Strategy

- **Max attempts**: 2 intentos
- **Wait duration**: 500ms entre reintentos
- **Excepciones reintentables**: IOException, TimeoutException, WebClientRequestException

### Monitoreo

Spring Boot Actuator expone métricas del circuit breaker:

```bash
curl http://localhost:5000/actuator/circuitbreakers
curl http://localhost:5000/actuator/health
```

---

## WebClient vs Feign

### ¿Por qué WebClient en lugar de Feign?

Este proyecto utiliza **Spring WebFlux WebClient** en lugar de **OpenFeign** para las llamadas HTTP a servicios externos.

### Comparativa Técnica

| Aspecto | Feign | WebClient (WebFlux) |
|---------|-------|---------------------|
| **Modelo de ejecución** | Bloqueante (síncrono) | No bloqueante (reactivo) |
| **Tipo de retorno** | Objeto directo, wrappers opcionales | `Mono`/`Flux` (Project Reactor) |
| **Backpressure** | No soportado | Soportado nativamente |
| **Concurrency** | Un hilo por petición | I/O asíncrono con event loop |
| **Declarativo** | Anotaciones `@FeignClient` | API fluente/programática |
| **Integración Stack** | Spring MVC/Servlet | Spring WebFlux/Reactor |

### Rendimiento: k6 Benchmark

Según los tests de carga ejecutados con **k6**, WebClient muestra un rendimiento significativamente superior:

| Métrica | Feign | WebClient | Mejora |
|---------|-------|-----------|--------|
| **Throughput** | ~50 req/s | ~150 req/s | **3x** |
| **Latencia media** | ~40ms | ~12ms | **70% menor** |
| **Latencia p95** | ~80ms | ~25ms | **69% menor** |

> **Resultado**: WebClient procesa **3 veces más peticiones por segundo** que Feign bajo la misma carga.

### ¿Por qué esta diferencia?

1. **Modelo reactivo**: WebClient no bloquea hilos mientras espera la respuesta HTTP
2. **Connection pooling eficiente**: Reutilización óptima de conexiones HTTP/2
3. **Zero-copy**: Menor overhead en serialización/deserialización
4. **Event loop**: Un único hilo puede manejar miles de conexiones concurrentes

### Ventajas de WebClient en este Proyecto

1. **Alto rendimiento**: ~150 req/s vs ~50 req/s de Feign
2. **Escalabilidad**: Maneja más conexiones con menos recursos
3. **Backpressure**: Control de flujo automático ante picos de tráfico
4. **Integración con Resilience4j**: Operadores reactivos para Circuit Breaker
5. **Testing simplificado**: WireMock simula el servidor HTTP sin overhead

### Configuración de WebClient

En `WebClientConfig.java` se configura con:
- Connection pool con máximo 500 conexiones
- Timeouts configurables (connect: 2s, read: 3s)
- Evitación de recursos inactivos

### Testing

Los tests utilizan **WireMock** para simular la API externa:

```java
@AutoConfigureWireMock(port = 0)
class ProductControllerTest {
    // WireMock simula el servidor HTTP en un puerto aleatorio
}
```

---

## API Endpoints

### Obtener Productos Similares

```
GET /product/{productId}/similar
```

**Path Parameters:**
- `productId` (string): ID del producto

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
GET /actuator/health           # Estado del servicio
GET /actuator/circuitbreakers  # Estado del circuit breaker
GET /actuator/metrics          # Métricas de la aplicación
```

---

## Tecnologías y Dependencias

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| Spring Boot | 4.0.2 | Framework principal |
| Java | 21 | Lenguaje |
| Spring WebFlux | - | Programación reactiva |
| Resilience4j | 2.3.0 | Circuit Breaker, Retry |
| Caffeine | - | Caché en memoria |
| Spring Validation | - | Validación de entrada |
| Spring Actuator | - | Métricas y health checks |
| Lombok | - | Reducción de boilerplate |

---

## Testing

### Ejecutar Tests

```bash
# Todos los tests
./gradlew test

# Solo tests unitarios
./gradlew test --tests "*Test"

# Tests de integración
./gradlew test --tests "*IT" --args='--spring.profiles.active=integration'
```

### Estrategia de Testing

1. **Unit Tests**: Prueban componentes individuales
2. **Integration Tests**: Usan WireMock para simular API externa
3. **WebTestClient**: Para probar endpoints sin servidor HTTP real

### Coverage

```bash
./gradlew test jacocoTestReport
```

---

## Autor

David Nieto - Backend Dev Technical Test
