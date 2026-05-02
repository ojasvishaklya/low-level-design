package com.oshaklya.rate_limiter;

import java.util.HashMap;
import java.util.Map;

class RateLimitingRequest {
    String url;
    String clientId;
}

class RateLimitingResponse {
    boolean shouldRateLimit;
    int limitRemaining;
    long retryAfterMs;

    public RateLimitingResponse(boolean shouldRateLimit, int limitRemaining, long retryAfterMs) {
        this.shouldRateLimit = shouldRateLimit;
        this.limitRemaining = limitRemaining;
        this.retryAfterMs = retryAfterMs;
    }
}

interface RateLimitingStrategy {
    RateLimitingResponse rateLimit(RateLimitingRequest rateLimitingRequest);
}

class RateLimiter {
    Map<String, RateLimitingStrategy> strategyByUrlMap;
    RateLimitingStrategy defaultStrategy;

    RateLimiter() {
        strategyByUrlMap = new HashMap<>();
        String endpoint1 = "url-1";
        SlidingWindowLogConfig slidingWindowLogConfig =
                new SlidingWindowLogConfig(endpoint1, 1000, 2);
        strategyByUrlMap.put(endpoint1, RateLimiterFactory.create(slidingWindowLogConfig));
        String endpoint2 = "url-2";
        TokenBucketConfig tokenBucketConfig =
                new TokenBucketConfig(endpoint1, 5, 2);
        strategyByUrlMap.put(endpoint2, RateLimiterFactory.create(tokenBucketConfig));

        this.defaultStrategy = RateLimiterFactory.create(tokenBucketConfig);
    }

    RateLimitingResponse rateLimit(RateLimitingRequest rateLimitingRequest) {
        return strategyByUrlMap.getOrDefault(rateLimitingRequest.url, defaultStrategy).rateLimit(rateLimitingRequest);
    }
}


public class Main {
    public static void main(String[] args) {

    }
}
