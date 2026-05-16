package io.github.afgprojects.framework.integration.redis.health;

import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.web.health.spi.RedisHealthChecker;
import io.github.afgprojects.framework.core.web.health.spi.RedisHealthResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Redisson Redis 健康检查实现
 */
@Slf4j
public class RedissonHealthChecker implements RedisHealthChecker {

    private static final String HEALTH_CHECK_KEY = "__redis_health_check__";

    private final RedissonClient redissonClient;

    public RedissonHealthChecker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public RedisHealthResult check() {
        long startTime = System.currentTimeMillis();

        try {
            // 执行简单的 ping 操作
            redissonClient.getBucket(HEALTH_CHECK_KEY).set("ping");
            String value = (String) redissonClient.getBucket(HEALTH_CHECK_KEY).get();
            boolean connected = "ping".equals(value);

            long duration = System.currentTimeMillis() - startTime;

            if (connected) {
                return RedisHealthResult.up(duration);
            } else {
                return RedisHealthResult.down("Ping failed", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Redis 健康检查失败: {}", e.getMessage(), e);
            return RedisHealthResult.down(e.getMessage(), duration);
        }
    }

    @Override
    public String getName() {
        return "redisson";
    }
}
