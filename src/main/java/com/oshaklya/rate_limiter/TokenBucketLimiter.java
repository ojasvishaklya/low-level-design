package com.oshaklya.rate_limiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BucketState {
    int tokens;
    long lastRefillTimestamp;
    Lock lock = new ReentrantLock();

    BucketState(int capacity) {
        this.tokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }
}

class TokenBucketLimiter implements RateLimitingStrategy {
    private static final long EVICTION_TTL_MS = 5 * 60 * 1000; // 5 minutes

    TokenBucketConfig tokenBucketConfig;
    Map<String, BucketState> bucketStateByClientId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    TokenBucketLimiter(TokenBucketConfig tokenBucketConfig) {
        this.tokenBucketConfig = tokenBucketConfig;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "TokenBucket-Cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleanupExecutor.scheduleAtFixedRate(this::evictInactiveClients, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public RateLimitingResponse rateLimit(RateLimitingRequest rateLimitingRequest) {
        BucketState bucketState = bucketStateByClientId.computeIfAbsent(
                rateLimitingRequest.clientId,
                k -> new BucketState(this.tokenBucketConfig.capacity)
        );

        try {
            bucketState.lock.lock();

            long millisElapsed = System.currentTimeMillis() - bucketState.lastRefillTimestamp;
            double secondsElapsed = millisElapsed / 1000.0;
            int tokensToRefill = (int) (secondsElapsed * tokenBucketConfig.refillRatePerSecond);

            if (tokensToRefill > 0) {
                bucketState.tokens = Math.min(this.tokenBucketConfig.capacity, bucketState.tokens + tokensToRefill);
                bucketState.lastRefillTimestamp = System.currentTimeMillis();
            }

            if (bucketState.tokens < 1) {
                return new RateLimitingResponse(false, 0, 1000);
            }

            bucketState.tokens--;
            return new RateLimitingResponse(true, bucketState.tokens, 0);
        } finally {
            bucketState.lock.unlock();
        }
    }

    /**
     * Removes client buckets that haven't been used in 5 minutes
     * Called periodically by background thread
     */
    private void evictInactiveClients() {
        long cutoffTime = System.currentTimeMillis() - EVICTION_TTL_MS;
        bucketStateByClientId.entrySet().removeIf(entry -> {
            BucketState state = entry.getValue();
            if (state.lock.tryLock()) {
                try {
                    return state.lastRefillTimestamp < cutoffTime;
                } finally {
                    state.lock.unlock();
                }
            }
            return false;
        });
    }
}
