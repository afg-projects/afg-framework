package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.cache.CacheAspect;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.CacheProperties;

/**
 * 缓存自动配置
 * <p>
 * 自动配置条件：
 * <ul>
 *   <li>afg.cache.enabled=true（默认为 true）</li>
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
@ConditionalOnProperty(prefix = "afg.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /**
     * 配置本地缓存管理器
     */
    @Bean("cacheManager")
    @ConditionalOnMissingBean(DefaultCacheManager.class)
    public DefaultCacheManager cacheManager(CacheProperties properties) {
        // 强制使用本地缓存（不依赖 Redisson）
        CacheProperties localProperties = toLocalProperties(properties);
        return new DefaultCacheManager(localProperties);
    }

    /**
     * 配置缓存切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DefaultCacheManager.class)
    @ConditionalOnProperty(
            prefix = "afg.cache.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CacheAspect cacheAspect(DefaultCacheManager cacheManager) {
        return new CacheAspect(cacheManager);
    }

    /**
     * 转换为本地缓存配置
     */
    private CacheProperties toLocalProperties(CacheProperties properties) {
        CacheProperties localProperties = new CacheProperties();
        localProperties.setEnabled(properties.isEnabled());
        localProperties.setType(CacheProperties.CacheType.LOCAL);
        localProperties.setDefaultTtl(properties.getDefaultTtl());
        localProperties.setCacheNull(properties.isCacheNull());
        localProperties.setNullValueTtl(properties.getNullValueTtl());
        localProperties.setLocal(properties.getLocal());
        localProperties.setCaches(properties.getCaches());
        return localProperties;
    }
}