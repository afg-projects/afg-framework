package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockAspect;
import io.github.afgprojects.framework.core.lock.NoOpDistributedLock;

/**
 * 分布式锁自动配置
 * <p>
 * 注意：Redis 分布式锁实现已移至 afg-redis 模块。
 * 如需使用 Redis 分布式锁，请引入 afg-redis 模块并使用其自动配置。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     lock:
 *       enabled: true
 *       key-prefix: "myapp:lock"
 *       default-wait-time: 5000
 *       default-lease-time: -1
 *       annotations:
 *         enabled: true
 * </pre>
 * </p>
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class LockAutoConfiguration {

    /**
     * NoOp 分布式锁降级实现
     * <p>
     * 当没有 Redis 等分布式锁后端时，提供本地降级。
     * 所有锁操作总是成功获取并立即释放，注解式加锁会"透传"。
     *
     * @return NoOp 分布式锁实例
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock noOpDistributedLock() {
        return new NoOpDistributedLock();
    }

    /**
     * 配置锁切面
     * <p>
     * 当 afg.core.lock.annotations.enabled=true 且存在 DistributedLock bean 时启用
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
            prefix = "afg.core.lock.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LockAspect lockAspect(DistributedLock distributedLock, AfgCoreProperties properties) {
        return new LockAspect(distributedLock, properties);
    }
}