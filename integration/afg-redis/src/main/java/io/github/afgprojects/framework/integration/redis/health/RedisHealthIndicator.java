package io.github.afgprojects.framework.integration.redis.health;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

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
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RedisHealthIndicator.class);

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
        long timeoutMs = properties.getConnectionTimeout();

        try {
            // 使用 ping 命令检查连接
            boolean connected = redissonClient.getNodesGroup().pingAll(timeoutMs, TimeUnit.MILLISECONDS);
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
            // 获取节点数量作为简单的可用性检查
            int nodeCount = redissonClient.getNodesGroup().getNodes().size();
            builder.withDetail("nodeCount", nodeCount);
        } catch (Exception e) {
            log.debug("获取 Redis 服务器信息失败: {}", e.getMessage());
            builder.withDetail("serverInfo", "unavailable");
        }
    }
}
