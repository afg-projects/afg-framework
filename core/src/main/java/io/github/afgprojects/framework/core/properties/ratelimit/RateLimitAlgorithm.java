package io.github.afgprojects.framework.core.properties.ratelimit;

/**
 * 限流算法枚举。
 */
public enum RateLimitAlgorithm {
    TOKEN_BUCKET,
    SLIDING_WINDOW,
    FIXED_WINDOW,
    LEAKY_BUCKET
}
