package io.github.afgprojects.framework.core.web.health.spi;

/**
 * Redis 健康检查结果
 */
public record RedisHealthResult(
    String status,
    String message,
    long responseTimeMs
) {
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";

    public static RedisHealthResult up(long responseTimeMs) {
        return new RedisHealthResult(UP, null, responseTimeMs);
    }

    public static RedisHealthResult down(String message, long responseTimeMs) {
        return new RedisHealthResult(DOWN, message, responseTimeMs);
    }

    public static RedisHealthResult notConfigured() {
        return new RedisHealthResult(NOT_CONFIGURED, null, 0);
    }
}
