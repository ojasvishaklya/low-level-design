# Rate Limiter - Class Diagram

```
┌─────────────────────────┐
│     RateLimiter         │
├─────────────────────────┤
│ -strategyByUrlMap: Map  │
│ -defaultStrategy        │
├─────────────────────────┤
│ +rateLimit(): Response  │
└───────────┬─────────────┘
            │
            │ aggregation (1:many)
            ↓
┌─────────────────────────────────┐
│   «interface»                   │
│   RateLimitingStrategy          │
├─────────────────────────────────┤
│ +rateLimit(req): Response       │
└────────┬───────────────┬────────┘
         │               │
         │ implements    │ implements
         ↓               ↓
┌───────────────────────┐          ┌───────────────────────┐
│ TokenBucketLimiter    │          │ SlidingWindowLog-     │
│                       │          │ Limiter               │
├───────────────────────┤          ├───────────────────────┤
│ -tokenBucketConfig    │          │ -slidingWindowLog-    │
│ -bucketStateBy-       │          │  Config               │
│  ClientId: Map        │          │ -requestLogBy-        │
│ -cleanupExecutor      │          │  UserId: Map          │
├───────────────────────┤          ├───────────────────────┤
│ +rateLimit(): Res     │          │ +rateLimit(): Res     │
│ -evictInactive-       │          └───────────┬───────────┘
│  Clients()            │                      │
└───────────┬───────────┘                      │
            │                                  │
            │ composition (1:many)             │ composition (1:many)
            ↓                                  ↓
┌───────────────────────┐          ┌───────────────────────┐
│    BucketState        │          │     RequestLog        │
├───────────────────────┤          ├───────────────────────┤
│ +tokens: int          │          │ +timestamps: Queue    │
│ +lastRefillTimestamp  │          │ +lock: Lock           │
│ +lock: Lock           │          └───────────────────────┘
└───────────────────────┘


┌─────────────────────────┐          ┌─────────────────────────┐
│  RateLimitingRequest    │          │  RateLimitingResponse   │
├─────────────────────────┤          ├─────────────────────────┤
│ +url: String            │          │ +shouldRateLimit: bool  │
│ +clientId: String       │          │ +limitRemaining: int    │
└─────────────────────────┘          │ +retryAfterMs: long     │
                                     └─────────────────────────┘


┌─────────────────────────┐
│  RateLimiterFactory     │
├─────────────────────────┤
│ +create(config): Strat  │──uses──>  RateLimitingStrategy
└───────────┬─────────────┘
            │
            │ uses
            ↓
┌─────────────────────────────────┐
│     EndpointConfig              │
├─────────────────────────────────┤
│ #endpoint: String               │
│ #algorithm: AlgorithmType       │
└────────┬───────────────┬────────┘
         │               │
         │ is-a          │ is-a
         ↓               ↓
┌───────────────────────┐          ┌───────────────────────┐
│ TokenBucketConfig     │          │ SlidingWindowLog-     │
│                       │          │ Config                │
├───────────────────────┤          ├───────────────────────┤
│ +capacity: int        │          │ +windowSizeMs: long   │
│ +refillRatePerSecond  │          │ +maxRequests: int     │
└───────────────────────┘          └───────────────────────┘


┌─────────────────────┐
│   «enumeration»     │
│   AlgorithmType     │
├─────────────────────┤
│ TOKEN_BUCKET        │
│ SLIDING_WINDOW_LOG  │
└─────────────────────┘
```

## Relationships

- **RateLimiter → RateLimitingStrategy**: aggregation (1:many) - RateLimiter maintains references to strategy instances but does not own their lifecycle. Strategies can be shared and exist independently.

- **TokenBucketLimiter → RateLimitingStrategy**: implements - Implements the strategy interface using token bucket algorithm with refill logic.

- **SlidingWindowLogLimiter → RateLimitingStrategy**: implements - Implements the strategy interface using sliding window log algorithm with timestamp tracking.

- **TokenBucketLimiter → BucketState**: composition (1:many) - Each limiter owns and manages BucketState instances per client. BucketState lifecycle is tied to the map within TokenBucketLimiter. When limiter is destroyed or eviction occurs, BucketState is destroyed.

- **SlidingWindowLogLimiter → RequestLog**: composition (1:many) - Each limiter owns and manages RequestLog instances per client. RequestLog lifecycle is tied to the map within SlidingWindowLogLimiter.

- **RateLimiterFactory → EndpointConfig**: uses - Factory consumes config objects to instantiate appropriate strategy implementations based on algorithm type.

- **RateLimiterFactory → RateLimitingStrategy**: uses - Factory creates and returns strategy instances based on configuration.

- **TokenBucketConfig → EndpointConfig**: is-a - Extends EndpointConfig with token bucket specific parameters (capacity, refill rate).

- **SlidingWindowLogConfig → EndpointConfig**: is-a - Extends EndpointConfig with sliding window specific parameters (window size, max requests).

- **EndpointConfig → AlgorithmType**: uses - Config stores algorithm type enum to determine which strategy to instantiate.

- **BucketState → Lock**: composition (1:1) - Each BucketState owns a ReentrantLock for thread-safe token updates. Lock lifecycle is tied to BucketState instance.

- **RequestLog → Lock**: composition (1:1) - Each RequestLog owns a ReentrantLock for thread-safe timestamp queue operations. Lock lifecycle is tied to RequestLog instance.

- **TokenBucketLimiter → ScheduledExecutorService**: composition (1:1) - Limiter owns a single-threaded executor for background cleanup. Executor lifecycle is managed by the limiter.

## Core Flow

1. RateLimiter.rateLimit(request) → Looks up strategy by URL from strategyByUrlMap, falls back to defaultStrategy
2. RateLimitingStrategy.rateLimit(request) → Delegates to TokenBucketLimiter or SlidingWindowLogLimiter based on config
3. TokenBucketLimiter.rateLimit(request) → Acquires per-client lock, refills tokens based on elapsed time, decrements token if available
4. BucketState.lock.lock() → Thread-safe token updates using ReentrantLock per client entity
5. TokenBucketLimiter.evictInactiveClients() → Background cleanup thread uses tryLock() pattern to remove stale entries after 5 minutes
6. RateLimitingResponse → Returns shouldRateLimit flag, limitRemaining count, and retryAfterMs delay

## Key Decisions

**Strategy Pattern**: RateLimiter delegates to interchangeable RateLimitingStrategy implementations, allowing runtime algorithm selection per endpoint without modifying core logic.

**Entity-Level Locking**: ConcurrentHashMap provides thread-safe map operations while per-entity ReentrantLock in BucketState/RequestLog ensures atomic state updates for individual clients. This allows concurrent request processing for different clients while preventing race conditions within a single client's state.

**Non-Blocking Cleanup**: Background eviction thread uses tryLock() instead of lock() to avoid blocking request processing. If a lock is held by an active request, the client is skipped and will be retried in the next cleanup cycle.

**Factory Pattern**: RateLimiterFactory encapsulates strategy instantiation logic, isolating creation complexity from business logic and enabling easy addition of new algorithms.
