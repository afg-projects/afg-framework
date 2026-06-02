package io.github.afgprojects.framework.ai.security.autoconfigure;

import io.github.afgprojects.framework.ai.core.autoconfigure.AiConfigurationProperties;
import io.github.afgprojects.framework.ai.core.api.security.ApiKeyManager;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.security.DefaultApiKeyManager;
import io.github.afgprojects.framework.ai.security.DefaultContentSafetyChecker;
import io.github.afgprojects.framework.ai.security.DefaultPiiDetector;
import io.github.afgprojects.framework.ai.security.PiiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 安全模块自动配置
 *
 * <p>配置 API Key 管理器、内容安全检查器、PII 检测器。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     security:
 *       enabled: true
 *       api-key:
 *         enabled: true
 *       content-safety:
 *         enabled: true
 *       pii:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass({DefaultApiKeyManager.class, DefaultContentSafetyChecker.class, DefaultPiiDetector.class})
@ConditionalOnProperty(prefix = "afg.ai.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    /**
     * 配置 API Key 管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.security.api-key", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ApiKeyManager.class)
    public ApiKeyManager apiKeyManager() {
        log.info("Creating default API key manager");

        return new DefaultApiKeyManager();
    }

    /**
     * 配置内容安全检查器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.security.content-safety", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ContentSafetyChecker.class)
    public ContentSafetyChecker contentSafetyChecker() {
        log.info("Creating default content safety checker");

        return new DefaultContentSafetyChecker();
    }

    /**
     * 配置 PII 检测器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.security.pii", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(PiiDetector.class)
    public PiiDetector piiDetector() {
        log.info("Creating default PII detector");

        return new DefaultPiiDetector();
    }

    /**
     * 配置 PII 检测服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.security.pii", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(PiiService.class)
    public PiiService piiService(PiiDetector piiDetector) {
        log.info("Creating PII service");

        return new PiiService(piiDetector);
    }
}
