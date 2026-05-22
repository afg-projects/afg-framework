package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.feature.FeatureFlagManager;
import io.github.afgprojects.framework.core.feature.FeatureToggleAspect;

/**
 * 功能开关自动配置类
 * <p>
 * 注意：RedissonStorageClient 已移至 afg-redis 模块。
 * 如需使用 Redis 分布式存储，请引入 afg-redis 模块并使用其自动配置。
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.core.feature", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class FeatureFlagAutoConfiguration {

    /**
     * 功能开关管理器（内存模式）
     *
     * @param properties 配置属性
     * @return 功能开关管理器
     */
    @Bean
    @ConditionalOnMissingBean(FeatureFlagManager.class)
    public FeatureFlagManager featureFlagManager(AfgCoreProperties properties) {
        return new FeatureFlagManager(properties);
    }

    /**
     * 功能开关切面
     *
     * @param featureFlagManager 功能开关管理器
     * @return 功能开关切面
     */
    @Bean
    @ConditionalOnMissingBean(FeatureToggleAspect.class)
    public FeatureToggleAspect featureToggleAspect(FeatureFlagManager featureFlagManager) {
        return new FeatureToggleAspect(featureFlagManager);
    }
}