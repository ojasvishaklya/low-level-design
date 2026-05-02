package com.oshaklya.rate_limiter;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RequestLog {
    Queue<Long> timestamps = new ArrayDeque<>();
    Lock lock = new ReentrantLock();
}

class SlidingWindowLogLimiter implements RateLimitingStrategy {
    SlidingWindowLogConfig slidingWindowLogConfig;
    Map<String, RequestLog> requestLogByUserId = new ConcurrentHashMap<>();

    SlidingWindowLogLimiter(SlidingWindowLogConfig slidingWindowLogConfig) {
        this.slidingWindowLogConfig = slidingWindowLogConfig;
    }

    @Override
    public RateLimitingResponse rateLimit(RateLimitingRequest rateLimitingRequest) {
        RequestLog requestLog = requestLogByUserId.computeIfAbsent(
                rateLimitingRequest.clientId,
                k -> new RequestLog()
        );
        try {
            requestLog.lock.lock();

            long now = System.currentTimeMillis();
            long cutoff = now - this.slidingWindowLogConfig.windowSizeMs;

            while (!requestLog.timestamps.isEmpty() && requestLog.timestamps.peek() < cutoff) {
                requestLog.timestamps.poll();
            }

            if (requestLog.timestamps.size() >= this.slidingWindowLogConfig.maxRequests) {
                return new RateLimitingResponse(false, 0, 1000);
            }
            requestLog.timestamps.add(System.currentTimeMillis());

            return new RateLimitingResponse(true, this.slidingWindowLogConfig.maxRequests - requestLog.timestamps.size(), 0);
        } finally {
            requestLog.lock.unlock();
        }
    }
}
