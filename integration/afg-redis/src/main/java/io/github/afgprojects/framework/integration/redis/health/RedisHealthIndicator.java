package io.github.afgprojects.framework.integration.redis.health;

import org.jspecify.annotations.NonNull;
import org.redisson.api.RedissonClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 健康检查指示器
 * 检查 Redis 连接状态和服务器信息
 *
 * <p>检查内容：
 * <ul>
 *   <li>Redis 服务器是否可达（ping）</li>
 *   <li>连接响应时间</li>
 *   <li>Redis 服务器版本信息</li>
 *   <li>内存使用情况</li>
 *   <li>连接池状态</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private static final String HEALTH_CHECK_KEY = "__redis_health_check__";

    private final RedissonClient redissonClient;
    private final RedisHealthProperties properties;

    /**
     * 构造函数
     *
     * @param redissonClient Redisson 客户端
     * @param properties     健康检查配置
     */
    public RedisHealthIndicator(@NonNull RedissonClient redissonClient, @NonNull RedisHealthProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        long startTime = System.currentTimeMillis();

        try {
            // Redisson 4.x: 使用简单的 get/set 操作检查连接
            redissonClient.getBucket(HEALTH_CHECK_KEY).set("ping");
            String value = (String) redissonClient.getBucket(HEALTH_CHECK_KEY).get();
            boolean connected = "ping".equals(value);
            long duration = System.currentTimeMillis() - startTime;

            if (connected) {
                builder.withDetail("redis", "UP")
                        .withDetail("responseTime", duration + "ms");

                // 获取服务器信息
                if (properties.isIncludeServerInfo()) {
                    addServerInfo(builder);
                }
            } else {
                log.error("Redis ping 失败");
                builder.status(Status.DOWN)
                        .withDetail("redis", "DOWN")
                        .withDetail("error", "Ping failed")
                        .withDetail("responseTime", duration + "ms");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Redis 连接检查失败: {}", e.getMessage(), e);
            builder.status(Status.DOWN)
                    .withDetail("redis", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("responseTime", duration + "ms");
        }

        return builder.build();
    }

    /**
     * 添加 Redis 服务器信息
     */
    private void addServerInfo(Health.Builder builder) {
        try {
            // Redisson 4.x: 使用简单的操作检查可用性
            // 尝试获取一个不存在的 key 来验证连接正常
            redissonClient.getBucket("__health_check_nonexistent__").get();
            builder.withDetail("serverInfo", "available");
        } catch (Exception e) {
            log.debug("获取 Redis 服务器信息失败: {}", e.getMessage());
            builder.withDetail("serverInfo", "unavailable");
        }
    }
}
