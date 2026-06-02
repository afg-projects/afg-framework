package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.security.ApiKeyManager;
// import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
// import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 安全自动配置。
 *
 * <p>配置前缀：{@code afg.ai.security}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSecurityAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class SecurityConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultApiKeyManager defaultApiKeyManager(AfgAiProperties properties) {
        //     return new DefaultApiKeyManager(properties.getSecurity().getApiKey());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultContentSafetyChecker defaultContentSafetyChecker(AfgAiProperties properties) {
        //     return new DefaultContentSafetyChecker(properties.getSecurity().getContentSafety());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultPiiDetector defaultPiiDetector(AfgAiProperties properties) {
        //     return new DefaultPiiDetector(properties.getSecurity().getPii());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public ContentSafetyAspect contentSafetyAspect(ContentSafetyChecker contentSafetyChecker) {
        //     return new ContentSafetyAspect(contentSafetyChecker);
        // }
    }
}
