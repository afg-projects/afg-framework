package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockAspect;
import io.github.afgprojects.framework.core.lock.LockProperties;

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
 *   lock:
 *     enabled: true
 *     key-prefix: "myapp:lock"
 *     default-wait-time: 5000
 *     default-lease-time: -1
 *     annotations:
 *       enabled: true
 * </pre>
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {

    /**
     * 配置锁切面
     * <p>
     * 当 afg.lock.annotations.enabled=true 且存在 DistributedLock bean 时启用
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
            prefix = "afg.lock.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LockAspect lockAspect(DistributedLock distributedLock, LockProperties properties) {
        return new LockAspect(distributedLock, properties);
    }
}
