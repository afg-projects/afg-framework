package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.langchain4j.advisor.Lc4jAuditAdvisor;
import io.github.afgprojects.framework.ai.langchain4j.advisor.Lc4jContentSafetyAdvisor;
import io.github.afgprojects.framework.ai.langchain4j.advisor.Lc4jPiiAdvisor;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * LangChain4J Advisor 自动配置
 *
 * <p>当 classpath 上存在 LangChain4J {@link ChatModelListener} 且
 * AFG 安全/审计接口可用时自动激活。注册以下 Advisor：
 * <ul>
 *   <li>{@link Lc4jAuditAdvisor} - 审计日志（需要 {@link AuditLogger}）</li>
 *   <li>{@link Lc4jContentSafetyAdvisor} - 内容安全检查（需要 {@link ContentSafetyChecker}）</li>
 *   <li>{@link Lc4jPiiAdvisor} - PII 检测（需要 {@link PiiDetector}）</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     langchain4j:
 *       enabled: true
 *     security:
 *       content-safety:
 *         enabled: true
 *       pii:
 *         enabled: true
 *     observability:
 *       audit:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.model.chat.listener.ChatModelListener")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jAdvisorAutoConfiguration {

    @Bean("lc4jAuditAdvisor")
    @ConditionalOnMissingBean(name = "lc4jAuditAdvisor")
    @ConditionalOnBean(AuditLogger.class)
    @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Lc4jAuditAdvisor lc4jAuditAdvisor(AuditLogger auditLogger) {
        log.info("Creating Lc4jAuditAdvisor bridging LC4J ChatModelListener to AFG AuditLogger");
        return new Lc4jAuditAdvisor(auditLogger);
    }

    @Bean("lc4jContentSafetyAdvisor")
    @ConditionalOnMissingBean(name = "lc4jContentSafetyAdvisor")
    @ConditionalOnBean(ContentSafetyChecker.class)
    @ConditionalOnProperty(prefix = "afg.ai.security.content-safety", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Lc4jContentSafetyAdvisor lc4jContentSafetyAdvisor(ContentSafetyChecker contentSafetyChecker) {
        log.info("Creating Lc4jContentSafetyAdvisor bridging LC4J ChatModelListener to AFG ContentSafetyChecker");
        return new Lc4jContentSafetyAdvisor(contentSafetyChecker);
    }

    @Bean("lc4jPiiAdvisor")
    @ConditionalOnMissingBean(name = "lc4jPiiAdvisor")
    @ConditionalOnBean(PiiDetector.class)
    @ConditionalOnProperty(prefix = "afg.ai.security.pii", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Lc4jPiiAdvisor lc4jPiiAdvisor(PiiDetector piiDetector) {
        log.info("Creating Lc4jPiiAdvisor bridging LC4J ChatModelListener to AFG PiiDetector");
        return new Lc4jPiiAdvisor(piiDetector);
    }
}
