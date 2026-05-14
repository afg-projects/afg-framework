package io.github.afgprojects.framework.integration.redis.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.web.health.spi.RedisHealthChecker;
import io.github.afgprojects.framework.core.web.health.spi.RedisHealthResult;

/**
 * Redisson Redis 健康检查实现
 */
public class RedissonHealthChecker implements RedisHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(RedissonHealthChecker.class);
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
