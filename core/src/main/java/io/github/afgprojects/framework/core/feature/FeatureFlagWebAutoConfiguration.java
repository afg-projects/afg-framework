package io.github.afgprojects.framework.core.feature;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.web.feature.FeatureFlagController;

/**
 * 功能开关 Web 自动配置类
 * <p>
 * 当存在 FeatureFlagManager 且为 Web 应用时，自动装配 REST API Controller
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(FeatureFlagManager.class)
@ConditionalOnProperty(prefix = "afg.feature.api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeatureFlagWebAutoConfiguration {

    /**
     * 功能开关管理 REST API Controller
     *
     * @param featureFlagManager 功能开关管理器
     * @return Controller
     */
    @Bean
    @ConditionalOnMissingBean(FeatureFlagController.class)
    public FeatureFlagController featureFlagController(FeatureFlagManager featureFlagManager) {
        return new FeatureFlagController(featureFlagManager);
    }
}
