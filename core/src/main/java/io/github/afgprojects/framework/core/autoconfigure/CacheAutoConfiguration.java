package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.cache.CacheAspect;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 缓存自动配置
 * <p>
 * 自动配置条件：
 * <ul>
 *   <li>afg.core.cache.enabled=true（默认为 true）</li>
 * </ul>
 * </p>
 * <p>
 * 支持三种缓存类型：
 * <ul>
 *   <li>local: 本地缓存（Caffeine）</li>
 *   <li>distributed: 分布式缓存（Redisson）- 需要 RedissonClient</li>
 *   <li>multi-level: 多级缓存（Caffeine + Redisson）- 需要 RedissonClient</li>
 * </ul>
 * 默认使用本地缓存，当配置分布式缓存且 RedissonClient 存在时自动切换。
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.core.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class CacheAutoConfiguration {

    /**
     * 配置本地缓存管理器
     */
    @Bean("cacheManager")
    @ConditionalOnMissingBean(DefaultCacheManager.class)
    public DefaultCacheManager cacheManager(AfgCoreProperties properties) {
        return new DefaultCacheManager(properties);
    }

    /**
     * 配置缓存切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DefaultCacheManager.class)
    @ConditionalOnProperty(
            prefix = "afg.core.cache.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CacheAspect cacheAspect(DefaultCacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }
}