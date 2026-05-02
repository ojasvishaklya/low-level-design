package com.oshaklya.rate_limiter;


import static com.oshaklya.rate_limiter.AlgorithmType.SLIDING_WINDOW_LOG;
import static com.oshaklya.rate_limiter.AlgorithmType.TOKEN_BUCKET;

enum AlgorithmType {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG
}

class RateLimiterFactory {
    public static RateLimitingStrategy create(EndpointConfig config) {
        if (config.algorithm == TOKEN_BUCKET) {
            TokenBucketConfig tb = (TokenBucketConfig) config;
            return new TokenBucketLimiter();
        } else if (config.algorithm == SLIDING_WINDOW_LOG) {
            SlidingWindowLogConfig sw = (SlidingWindowLogConfig) config;
            return new SlidingWindowLogLimiter();
        }
        throw new IllegalArgumentException("Unsupported algorithm");
    }
}

class SlidingWindowLogConfig extends EndpointConfig {
    final long windowSizeMs;
    final int maxRequests;

    public SlidingWindowLogConfig(String endpoint, long windowSizeMs, int maxRequests) {
        super(endpoint, SLIDING_WINDOW_LOG);
        this.windowSizeMs = windowSizeMs;
        this.maxRequests = maxRequests;
    }
}

class TokenBucketConfig extends EndpointConfig {
    final int capacity;
    final double refillRatePerSecond;

    public TokenBucketConfig(String endpoint, int capacity, double refillRatePerSecond) {
        super(endpoint, TOKEN_BUCKET);
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }
}


abstract class EndpointConfig {
    final String endpoint;
    final AlgorithmType algorithm;

    protected EndpointConfig(String endpoint, AlgorithmType algorithm) {
        this.endpoint = endpoint;
        this.algorithm = algorithm;
    }
}
