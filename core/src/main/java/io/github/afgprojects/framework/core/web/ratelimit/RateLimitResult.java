package io.github.afgprojects.framework.core.web.ratelimit;

/**
 * 限流结果
 * <p>
 * 包含限流检查的结果信息，用于响应头和监控
 * </p>
 *
 * @param allowed      是否允许通过
 * @param remaining    剩余令牌数
 * @param limit        总限制数
 * @param resetTimeMs  重置时间（毫秒时间戳）
 * @param retryAfterMs 重试等待时间（毫秒）
 */
public record RateLimitResult(
        boolean allowed,
        long remaining,
        long limit,
        long resetTimeMs,
        long retryAfterMs
) {

    /**
     * 允许通过的结果
     *
     * @param remaining   剩余令牌数
     * @param limit       总限制数
     * @param resetTimeMs 重置时间
     * @return 允许结果
     */
    public static RateLimitResult allowed(long remaining, long limit, long resetTimeMs) {
        return new RateLimitResult(true, remaining, limit, resetTimeMs, 0);
    }

    /**
     * 拒绝的结果
     *
     * @param limit        总限制数
     * @param resetTimeMs  重置时间
     * @param retryAfterMs 重试等待时间
     * @return 拒绝结果
     */
    public static RateLimitResult rejected(long limit, long resetTimeMs, long retryAfterMs) {
        return new RateLimitResult(false, 0, limit, resetTimeMs, retryAfterMs);
    }

    /**
     * 获取响应头 X-RateLimit-Limit
     *
     * @return 限制值
     */
    public String getLimitHeader() {
        return String.valueOf(limit);
    }

    /**
     * 获取响应头 X-RateLimit-Remaining
     *
     * @return 剩余值
     */
    public String getRemainingHeader() {
        return String.valueOf(remaining);
    }

    /**
     * 获取响应头 X-RateLimit-Reset
     *
     * @return 重置时间（秒）
     */
    public String getResetHeader() {
        return String.valueOf(resetTimeMs / 1000);
    }

    /**
     * 获取响应头 Retry-After
     *
     * @return 重试等待时间（秒）
     */
    public String getRetryAfterHeader() {
        return String.valueOf((retryAfterMs + 999) / 1000); // 向上取整到秒
    }
}
