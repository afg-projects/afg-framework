package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.api.security.ApiKeyManager;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.security.DefaultApiKeyManager;
import io.github.afgprojects.framework.ai.core.security.DefaultContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.security.DefaultPiiDetector;
import io.github.afgprojects.framework.ai.core.security.PiiService;
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

        /**
         * 配置 API Key 管理器
         */
        @Bean
        @ConditionalOnProperty(prefix = "afg.ai.security.api-key", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean(ApiKeyManager.class)
        public DefaultApiKeyManager defaultApiKeyManager() {
            return new DefaultApiKeyManager();
        }

        /**
         * 配置内容安全检查器
         */
        @Bean
        @ConditionalOnProperty(prefix = "afg.ai.security.content-safety", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean(ContentSafetyChecker.class)
        public DefaultContentSafetyChecker defaultContentSafetyChecker() {
            return new DefaultContentSafetyChecker();
        }

        /**
         * 配置 PII 检测器
         */
        @Bean
        @ConditionalOnProperty(prefix = "afg.ai.security.pii", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean(PiiDetector.class)
        public DefaultPiiDetector defaultPiiDetector() {
            return new DefaultPiiDetector();
        }

        /**
         * 配置 PII 检测服务
         */
        @Bean
        @ConditionalOnProperty(prefix = "afg.ai.security.pii", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean(PiiService.class)
        public PiiService piiService(PiiDetector piiDetector) {
            return new PiiService(piiDetector);
        }
    }
}