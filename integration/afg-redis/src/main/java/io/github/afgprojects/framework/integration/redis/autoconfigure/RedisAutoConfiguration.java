package io.github.afgprojects.framework.integration.redis.autoconfigure;

import io.github.afgprojects.framework.core.cache.CacheManager;
import io.github.afgprojects.framework.core.api.scheduler.DelayQueue;
import io.github.afgprojects.framework.core.api.scheduler.DistributedTaskScheduler;
import io.github.afgprojects.framework.core.audit.AuditLogProperties;
import io.github.afgprojects.framework.core.audit.AuditLogStorage;
import io.github.afgprojects.framework.core.feature.FeatureFlagManager;
import io.github.afgprojects.framework.core.feature.FeatureFlagProperties;
import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.integration.redis.audit.RedisAuditLogStorage;
import io.github.afgprojects.framework.integration.redis.cache.RedisCacheManager;
import io.github.afgprojects.framework.integration.redis.cache.RedisCacheProperties;
import io.github.afgprojects.framework.integration.redis.feature.RedissonStorageClient;
import io.github.afgprojects.framework.integration.redis.health.RedisHealthIndicator;
import io.github.afgprojects.framework.integration.redis.health.RedisHealthProperties;
import io.github.afgprojects.framework.integration.redis.lock.LockAspect;
import io.github.afgprojects.framework.integration.redis.lock.LockProperties;
import io.github.afgprojects.framework.integration.redis.lock.RedisDistributedLock;
import io.github.afgprojects.framework.integration.redis.scheduler.RedissonDelayQueue;
import io.github.afgprojects.framework.integration.redis.scheduler.RedissonSchedulerProperties;
import io.github.afgprojects.framework.integration.redis.scheduler.RedissonTaskScheduler;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis 自动配置
 * <p>
 * 自动配置条件:
 * <ul>
 *   <li>存在 RedissonClient bean</li>
 *   <li>afg.redis.cache.enabled=true (默认为 true) - 缓存配置</li>
 *   <li>afg.redis.lock.enabled=true (默认为 true) - 锁配置</li>
 *   <li>afg.scheduler.redisson.enabled=true (默认为 true) - 调度器配置</li>
 * </ul>
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动配置 Redis 缓存管理器</li>
 *   <li>自动配置 Redis 分布式锁</li>
 *   <li>自动配置 Redisson 分布式任务调度器</li>
 *   <li>自动配置 Redisson 延迟队列</li>
 *   <li>支持全局默认 TTL 配置</li>
 *   <li>支持按缓存名单独配置 TTL</li>
 *   <li>支持 @Lock 注解切面</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   redis:
 *     cache:
 *       enabled: true
 *       default-ttl: 30m
 *       caches:
 *         users: 1h
 *         products: 10m
 *     lock:
 *       enabled: true
 *       key-prefix: "myapp:lock"
 *       default-wait-time: 5000
 *       default-lease-time: -1
 *       annotations:
 *         enabled: true
 *   scheduler:
 *     redisson:
 *       enabled: true
 *       executor-name: "afg-scheduler"
 *       worker-count: 4
 *       task-timeout: 30m
 *       delay-queue:
 *         enabled: true
 *         name: "afg-delay-queue"
 *         consumer-threads: 2
 *         batch-size: 10
 * </pre>
 */
@AutoConfiguration
@ConditionalOnBean(RedissonClient.class)
@EnableConfigurationProperties({
        RedisCacheProperties.class,
        LockProperties.class,
        RedissonSchedulerProperties.class,
        AuditLogProperties.class,
        FeatureFlagProperties.class,
        RedisHealthProperties.class
})
public class RedisAutoConfiguration {

    // ==================== Cache Configuration ====================

    /**
     * 配置 Redis 缓存管理器
     *
     * @param redissonClient Redisson 客户端
     * @param properties     缓存配置属性
     * @return Redis 缓存管理器实例
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "afg.redis.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager(RedissonClient redissonClient, RedisCacheProperties properties) {
        return new RedisCacheManager(
                redissonClient,
                properties.getDefaultTtl(),
                properties.getCaches()
        );
    }

    // ==================== Lock Configuration ====================

    /**
     * 配置分布式锁实现
     *
     * @param redissonClient Redisson 客户端
     * @param properties     锁配置属性
     * @return 分布式锁实例
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnProperty(prefix = "afg.redis.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DistributedLock distributedLock(@NonNull RedissonClient redissonClient, @NonNull LockProperties properties) {
        return new RedisDistributedLock(redissonClient, properties);
    }

    /**
     * 配置锁切面
     * <p>
     * 当 afg.redis.lock.annotations.enabled=true 时启用
     * </p>
     *
     * @param distributedLock 分布式锁服务
     * @param properties      锁配置属性
     * @return 锁切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DistributedLock.class)
    @ConditionalOnProperty(
            prefix = "afg.redis.lock.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LockAspect lockAspect(@NonNull DistributedLock distributedLock, @NonNull LockProperties properties) {
        return new LockAspect(distributedLock, properties);
    }

    // ==================== Scheduler Configuration ====================

    /**
     * 配置分布式任务调度器
     *
     * @param redissonClient Redisson 客户端
     * @param properties     调度器配置属性
     * @return 分布式任务调度器实例
     */
    @Bean
    @ConditionalOnMissingBean(DistributedTaskScheduler.class)
    @ConditionalOnProperty(prefix = "afg.scheduler.redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonTaskScheduler redissonTaskScheduler(
            @NonNull RedissonClient redissonClient,
            @NonNull RedissonSchedulerProperties properties) {

        return new RedissonTaskScheduler(
                redissonClient,
                properties.getExecutorName(),
                properties.getWorkerCount(),
                properties.getTaskTimeout()
        );
    }

    /**
     * 配置延迟队列
     *
     * @param redissonClient Redisson 客户端
     * @param properties     调度器配置属性
     * @return RedissonDelayQueue 实例
     */
    @Bean
    @ConditionalOnMissingBean(DelayQueue.class)
    @ConditionalOnProperty(prefix = "afg.scheduler.redisson.delay-queue", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedissonDelayQueue<Object> redissonDelayQueue(
            @NonNull RedissonClient redissonClient,
            @NonNull RedissonSchedulerProperties properties) {

        RedissonSchedulerProperties.DelayQueueConfig delayQueueConfig = properties.getDelayQueue();

        RedissonDelayQueue<Object> delayQueue = new RedissonDelayQueue<>(
                redissonClient,
                delayQueueConfig.getName(),
                delayQueueConfig.getConsumerThreads()
        );

        delayQueue.start();
        return delayQueue;
    }

    // ==================== Audit Configuration ====================

    /**
     * 配置 Redis 审计日志存储
     * <p>
     * 条件：存在 RedissonClient Bean 且 storage-type=redis
     * </p>
     *
     * @param redissonClient Redisson 客户端
     * @param properties     审计日志配置
     * @return Redis 审计日志存储实例
     */
    @Bean
    @ConditionalOnMissingBean(AuditLogStorage.class)
    @ConditionalOnProperty(prefix = "afg.audit", name = "storage-type", havingValue = "redis")
    public AuditLogStorage redisAuditLogStorage(
            @NonNull RedissonClient redissonClient, @NonNull AuditLogProperties properties) {
        return new RedisAuditLogStorage(redissonClient, properties);
    }

    // ==================== Feature Flag Configuration ====================

    /**
     * 功能开关管理器（Redisson 模式）
     *
     * @param properties     配置属性
     * @param redissonClient Redisson 客户端
     * @param objectMapper   JSON 序列化器
     * @return 功能开关管理器
     */


    @Bean
    @ConditionalOnMissingBean(FeatureFlagManager.class)
    @ConditionalOnProperty(prefix = "afg.feature", name = "storage-type", havingValue = "redisson")
    public FeatureFlagManager featureFlagManagerWithRedisson(
            FeatureFlagProperties properties, RedissonClient redissonClient, ObjectMapper objectMapper) {
        RedissonStorageClient storageClient =
                new RedissonStorageClient(redissonClient, objectMapper, properties.getRedis().getKeyPrefix());
        return new FeatureFlagManager(properties, storageClient);
    }

    // ==================== Health Configuration ====================

    /**
     * Redis 健康检查指示器
     * 当存在 RedissonClient 时自动配置
     *
     * @param redissonClient Redisson 客户端
     * @param properties     健康检查配置属性
     * @return Redis 健康检查指示器实例
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnMissingBean(name = "redisHealthIndicator")
    @ConditionalOnProperty(prefix = "afg.health.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisHealthIndicator redisHealthIndicator(
            @NonNull RedissonClient redissonClient,
            @NonNull RedisHealthProperties properties) {
        return new RedisHealthIndicator(redissonClient, properties);
    }
}
