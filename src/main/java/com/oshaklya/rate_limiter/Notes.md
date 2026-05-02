// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

REQUIREMENTS
1. The ratelimiter should support different rate limiting algorithms (e.g. TokenBucket, SlidingWindowLog)
2. For each endpoint the user can configure which algorithm is to be used along with algorithm-specific parameters (e.g. capacity, refillRatePerSecond for TokenBucket)
3. The primary operation is: given a (clientId, endpoint) pair, return an allow/deny decision
4. Each client is rate limited independently per endpoint — client A and client B on the same endpoint have separate counters
5. The response should be structured: (allowed: boolean, remaining: int, retryAfterMs: long)
6. If a request comes in for an endpoint with no configuration, fall back to a default rate limit
7. Rate limiter must run in memory (single process, no distributed coordination)

Out of Scope:
1. Persistent storage
2. Distributed rate limiting (Redis, coordination across nodes)
3. Dynamic configuration updates after startup

// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

ENTITIES
- RateLimiter: Main facade, routes (clientId, endpoint) → strategy
- RateLimitingStrategy: Interface for different algorithms (TokenBucket, SlidingWindowLog)
- BucketState: Per-client token bucket data (tokens, lastRefillTimestamp, lock)
- RequestLog: Per-client sliding window data (timestamps queue, lock)
- EndpointConfig: Base config (endpoint, algorithm type)
- TokenBucketConfig: capacity, refillRatePerSecond
- SlidingWindowLogConfig: windowSizeMs, maxRequests
- RateLimiterFactory: Creates strategy instances from config

RELATIONSHIPS
- RateLimiter has-many RateLimitingStrategy (1 per endpoint + default)
- TokenBucketLimiter has-many BucketState (1 per clientId)
- SlidingWindowLogLimiter has-many RequestLog (1 per clientId)
- Each strategy instance owns a ConcurrentHashMap of per-client state

// ═══════════════════════════════════════════════════════════════════════════
// DESIGN DECISIONS
// ═══════════════════════════════════════════════════════════════════════════

PATTERN: Strategy Pattern
- RateLimitingStrategy interface allows pluggable algorithms
- Each endpoint can be configured with a different strategy
- New algorithms (FixedWindow, SlidingWindowCounter) can be added without modifying existing code

PATTERN: Factory Pattern
- RateLimiterFactory.create(EndpointConfig) → RateLimitingStrategy
- Factory switches on AlgorithmType enum to instantiate correct implementation
- Centralizes object creation logic

CONCURRENCY: Entity-Level Locking
- ConcurrentHashMap<String, PerClientState> at strategy level
- Each PerClientState has its own ReentrantLock
- Pattern: computeIfAbsent() to get/create state, then lock.lock() for atomic operations
- Why: Contention only between requests from same clientId, not across all clients
- Alternative rejected: Global lock would serialize all requests regardless of clientId

MEMORY MANAGEMENT: Background Cleanup
- TokenBucketLimiter runs ScheduledExecutorService every 1 minute
- Evicts BucketState entries inactive for 5+ minutes
- Uses tryLock() to avoid blocking cleanup thread
- Why: Prevents unbounded memory growth from one-time clients

ALGORITHM CHOICE TRADEOFFS
Token Bucket:
- Pro: Smooth traffic, allows bursts up to capacity, memory efficient (2 fields per client)
- Con: Approximate (refill calculated on-demand), fixed-point arithmetic may drift
- Best for: General API rate limiting with burst tolerance

Sliding Window Log:
- Pro: Precise request counting, strict per-window enforcement
- Con: Memory grows with maxRequests (stores timestamp per request), queue cleanup overhead
- Best for: Low-rate endpoints where precision matters

// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

class RateLimiter
- Map<String, RateLimitingStrategy> strategyByEndpoint;
- RateLimitingStrategy defaultRateLimitingStrategy;
+ RateLimiter(configs: List<EndpointConfig>, defaultConfig: EndpointConfig)
+ rateLimit(request: RateLimitRequest) -> RateLimitResponse

class RateLimiterFactory
+ create(config: EndpointConfig) -> RateLimitingStrategy  // switches on config.algorithm to instantiate correct strategy

interface RateLimitingStrategy
+ rateLimit(request: RateLimitRequest) -> RateLimitResponse

// Per-client state for token bucket algorithm
class BucketState
+ tokens: int
+ lastRefillTimestamp: long
+ lock: ReentrantLock

class TokenBucketLimiter implements RateLimitingStrategy
- bucketStateByClientId: ConcurrentHashMap<String, BucketState>
- tokenBucketConfig: TokenBucketConfig
- cleanupExecutor: ScheduledExecutorService
+ TokenBucketLimiter(tokenBucketConfig: TokenBucketConfig)
+ rateLimit(request: RateLimitingRequest) -> RateLimitingResponse
- evictInactiveClients(): void  // background cleanup

// Per-client state for sliding window log algorithm
class RequestLog
+ timestamps: Queue<Long>  // ArrayDeque
+ lock: ReentrantLock

class SlidingWindowLogLimiter implements RateLimitingStrategy
- requestLogByUserId: ConcurrentHashMap<String, RequestLog>
- slidingWindowLogConfig: SlidingWindowLogConfig
+ SlidingWindowLogLimiter(slidingWindowLogConfig: SlidingWindowLogConfig)
+ rateLimit(request: RateLimitingRequest) -> RateLimitingResponse

class RateLimitingRequest
+ clientId: String
+ url: String

class RateLimitingResponse
+ shouldRateLimit: boolean
+ limitRemaining: int
+ retryAfterMs: long

enum AlgorithmType
+ TOKEN_BUCKET
+ SLIDING_WINDOW_LOG

abstract class EndpointConfig
+ endpoint: String
+ algorithm: AlgorithmType

class TokenBucketConfig extends EndpointConfig
+ capacity: int
+ refillRatePerSecond: double

class SlidingWindowLogConfig extends EndpointConfig
+ windowSizeMs: long
+ maxRequests: int

// ═══════════════════════════════════════════════════════════════════════════
// KEY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

TokenBucketLimiter.rateLimit(request):
1. computeIfAbsent(clientId) → get or create BucketState
2. Acquire per-client lock
3. Calculate tokens to refill: (millisElapsed / 1000) * refillRatePerSecond
4. Cap at capacity, update lastRefillTimestamp if refilled
5. If tokens < 1, deny with retryAfter=1000ms
6. Decrement tokens, return allow response
7. Release lock

SlidingWindowLogLimiter.rateLimit(request):
1. computeIfAbsent(clientId) → get or create RequestLog
2. Acquire per-client lock
3. Evict timestamps older than (now - windowSizeMs)
4. If queue.size >= maxRequests, deny with retryAfter=1000ms
5. Add current timestamp to queue
6. Return allow response with remaining = maxRequests - queue.size
7. Release lock

TokenBucketLimiter.evictInactiveClients():
- Runs every 1 minute via ScheduledExecutorService
- cutoffTime = now - 5 minutes
- For each entry: tryLock(), check lastRefillTimestamp < cutoffTime, remove if stale
- Uses tryLock() to avoid blocking if client is actively rate limiting

RateLimiter.rateLimit(request):
- Lookup strategy by endpoint URL
- Fall back to defaultStrategy if not configured
- Delegate to strategy.rateLimit(request)

// ═══════════════════════════════════════════════════════════════════════════
// CONCURRENCY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

PATTERN: ConcurrentHashMap + Per-Entity Lock
- Used in both TokenBucketLimiter and SlidingWindowLogLimiter
- Map itself is thread-safe (ConcurrentHashMap)
- Each per-client state object (BucketState, RequestLog) has its own ReentrantLock
- computeIfAbsent() safely creates state on first access (atomic operation)
- Lock acquired only around state mutation, not map access

RACE CONDITIONS PREVENTED:

TokenBucketLimiter.rateLimit():
- Token refill + consumption is atomic within lock
- Without lock: two threads could both read tokens=1, both refill, both decrement → negative tokens
- With lock: read-refill-check-decrement is atomic per client

SlidingWindowLogLimiter.rateLimit():
- Eviction + size check + add is atomic within lock
- Without lock: two threads could both see size=maxRequests-1, both add timestamp → exceed limit
- With lock: evict-check-add is atomic per client

TokenBucketLimiter.evictInactiveClients():
- Uses tryLock() instead of lock()
- Avoids deadlock: cleanup thread won't block waiting for active client
- Trade-off: may skip eviction if client is busy, will retry next cycle

WHY NOT GLOBAL LOCK:
- Global lock would serialize all requests across all clients
- Current design: only requests from same clientId contend
- Scalability: N clients can rate limit in parallel

WHY NOT ATOMIC VARIABLES:
- Token bucket requires: read timestamp, calculate refill, cap, decrement (multi-step)
- Sliding window requires: evict old, check size, add new (multi-step)
- AtomicInteger/AtomicReference insufficient for multi-field atomic updates

// ═══════════════════════════════════════════════════════════════════════════
// INTERVIEW TALKING POINTS
// ═══════════════════════════════════════════════════════════════════════════

Q: Why entity-level locking instead of global lock?
A: Contention only between same clientId requests. 1000 different clients can rate limit in parallel. Global lock would serialize everything, destroying throughput.

Q: Why ConcurrentHashMap if you have locks?
A: Map operations (get, computeIfAbsent) are thread-safe. Lock is only for the multi-step algorithm logic (refill+consume, evict+add). Avoids holding lock during map access.

Q: Why tryLock() in cleanup thread?
A: Cleanup is best-effort, not critical. If client is actively rate limiting, skip eviction this cycle. Prevents cleanup from blocking or being blocked by active requests.

Q: Token bucket refill on-demand vs background thread?
A: On-demand refill is lazy (only when client sends request). Avoids background thread per client. Trade-off: first request after idle may calculate large refill. Simpler than tracking scheduled refill.

Q: Sliding window log memory concerns?
A: Stores 1 timestamp per request in window. For maxRequests=1000, windowSize=1min → 8KB per client. Bounded by maxRequests. For high-rate endpoints, prefer sliding window counter (fixed memory).

Q: What if ScheduledExecutorService fails?
A: cleanupExecutor is daemon thread, won't prevent shutdown. If cleanup fails, worst case is memory leak (unbounded map growth). Could add memory pressure monitoring or LRU eviction.

Q: Why not just use Guava RateLimiter?
A: Guava RateLimiter is single-rate global. This design supports per-endpoint, per-client rate limiting with different algorithms. More flexible for API gateway use case.

Q: How would you make this distributed?
A: Replace in-memory maps with Redis. Token bucket: Redis GET/SET with Lua script for atomic refill+consume. Sliding window log: Redis sorted set (ZADD, ZREMRANGEBYSCORE, ZCARD). Trade-off: network latency vs coordination.