package io.github.afgprojects.framework.data.jdbc.autoconfigure;

import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * 实体缓存自动配置
 * <p>
 * 当满足以下条件时自动配置实体二级缓存：
 * <ul>
 *     <li>配置属性 afg.jdbc.cache.enabled=true</li>
 *     <li>存在 DefaultCacheManager Bean</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, DefaultCacheManager.class})
@ConditionalOnProperty(prefix = "afg.jdbc.cache", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EntityCacheProperties.class)
public class EntityCacheAutoConfiguration {

    /**
     * 创建实体缓存管理器
     *
     * @param cacheManager     默认缓存管理器
     * @param cacheProperties  实体缓存配置
     * @return 实体缓存管理器
     */
    @Bean
    @NonNull
    @ConditionalOnMissingBean
    @ConditionalOnBean(DefaultCacheManager.class)
    public EntityCacheManager entityCacheManager(
            @NonNull DefaultCacheManager cacheManager,
            @NonNull EntityCacheProperties cacheProperties) {
        return new EntityCacheManager(cacheManager, cacheProperties);
    }
}
