package io.github.afgprojects.framework.core.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;
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
 *   <li>distributed: 分布式缓存（Redisson）</li>
 *   <li>multi-level: 多级缓存（Caffeine + Redisson）</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /**
     * 配置缓存管理器
     *
     * @param properties     缓存配置属性
     * @param redissonClient Redisson 客户端（可选）
     * @return 缓存管理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultCacheManager cacheManager(CacheProperties properties, @Nullable RedissonClient redissonClient) {
        // 如果需要分布式缓存但没有 RedissonClient，回退到本地缓存
        if (needsDistributedCache(properties) && redissonClient == null) {
            // 自动回退到本地缓存
            CacheProperties fallbackProperties = new CacheProperties();
            fallbackProperties.setEnabled(properties.isEnabled());
            fallbackProperties.setType(CacheProperties.CacheType.LOCAL);
            fallbackProperties.setDefaultTtl(properties.getDefaultTtl());
            fallbackProperties.setCacheNull(properties.isCacheNull());
            fallbackProperties.setNullValueTtl(properties.getNullValueTtl());
            fallbackProperties.setLocal(properties.getLocal());
            fallbackProperties.setDistributed(properties.getDistributed());
            fallbackProperties.setCaches(properties.getCaches());
            return new DefaultCacheManager(fallbackProperties, null);
        }
        return new DefaultCacheManager(properties, redissonClient);
    }

    /**
     * 配置缓存切面
     *
     * @param cacheManager 缓存管理器
     * @return 缓存切面实例
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
     * 检查是否需要分布式缓存
     */
    private boolean needsDistributedCache(CacheProperties properties) {
        return properties.getType() == CacheProperties.CacheType.DISTRIBUTED
                || properties.getType() == CacheProperties.CacheType.MULTI_LEVEL;
    }
}