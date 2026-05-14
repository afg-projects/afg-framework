package io.github.afgprojects.framework.core.web.health.spi;

/**
 * 默认 Redis 健康检查器
 * <p>
 * 当没有配置 Redis 时使用，返回 NOT_CONFIGURED 状态。
 */
public class NoOpRedisHealthChecker implements RedisHealthChecker {

    @Override
    public RedisHealthResult check() {
        return RedisHealthResult.notConfigured();
    }

    @Override
    public String getName() {
        return "noop";
    }
}
