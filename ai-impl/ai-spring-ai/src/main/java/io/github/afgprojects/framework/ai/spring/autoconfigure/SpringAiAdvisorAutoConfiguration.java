package io.github.afgprojects.framework.ai.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.spring.chat.advisor.AfgToolCallback;
import io.github.afgprojects.framework.ai.spring.chat.advisor.AuditLoggingAdvisor;
import io.github.afgprojects.framework.ai.spring.chat.advisor.ContentSafetyAdvisor;
import io.github.afgprojects.framework.ai.spring.chat.advisor.PiiDetectionAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI Advisor 自动配置
 *
 * <p>根据 classpath 和 Bean 可用性，自动注册 AFG 的企业级 Advisor：
 * <ul>
 *   <li>{@link PiiDetectionAdvisor} - 需要 {@link PiiDetector} Bean</li>
 *   <li>{@link ContentSafetyAdvisor} - 需要 {@link ContentSafetyChecker} Bean</li>
 *   <li>{@link AuditLoggingAdvisor} - 需要 {@link AuditLogger} Bean</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(
    after = SpringAiChatAutoConfiguration.class,
    afterName = {
        "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration",
        "io.github.afgprojects.framework.ai.core.autoconfigure.AiCoreAutoConfiguration"
    }
)
@ConditionalOnProperty(prefix = "afg.ai.spring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiAdvisorAutoConfiguration {

    @Bean
    @ConditionalOnBean(PiiDetector.class)
    @ConditionalOnMissingBean(PiiDetectionAdvisor.class)
    public PiiDetectionAdvisor piiDetectionAdvisor(PiiDetector piiDetector) {
        log.info("Creating PiiDetectionAdvisor");
        return new PiiDetectionAdvisor(piiDetector);
    }

    @Bean
    @ConditionalOnBean(ContentSafetyChecker.class)
    @ConditionalOnMissingBean(ContentSafetyAdvisor.class)
    public ContentSafetyAdvisor contentSafetyAdvisor(ContentSafetyChecker safetyChecker) {
        log.info("Creating ContentSafetyAdvisor");
        return new ContentSafetyAdvisor(safetyChecker);
    }

    @Bean
    @ConditionalOnBean(AuditLogger.class)
    @ConditionalOnMissingBean(AuditLoggingAdvisor.class)
    public AuditLoggingAdvisor auditLoggingAdvisor(AuditLogger auditLogger) {
        log.info("Creating AuditLoggingAdvisor");
        return new AuditLoggingAdvisor(auditLogger);
    }

    @Bean
    @ConditionalOnBean({ToolRegistry.class, ObjectMapper.class})
    public List<ToolCallback> afgToolCallbacks(
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper) {
        var callbacks = new ArrayList<ToolCallback>();
        for (Tool<?, ?> tool : toolRegistry.getAllTools()) {
            callbacks.add(new AfgToolCallback(tool, toolRegistry, objectMapper));
        }
        log.info("Registered {} AFG tool callbacks", callbacks.size());
        return callbacks;
    }
}
