package io.github.afgprojects.framework.ai.chat.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.chat.advisor.AfgToolCallback;
import io.github.afgprojects.framework.ai.chat.advisor.AuditLoggingAdvisor;
import io.github.afgprojects.framework.ai.chat.advisor.ContentSafetyAdvisor;
import io.github.afgprojects.framework.ai.chat.advisor.PiiDetectionAdvisor;
import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * AFG 企业级 Advisor 自动配置
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
@AutoConfiguration(after = ChatClientAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatClientAdvisorAutoConfiguration {

    @Bean
    @ConditionalOnBean(PiiDetector.class)
    @ConditionalOnMissingBean(PiiDetectionAdvisor.class)
    public PiiDetectionAdvisor piiDetectionAdvisor(PiiDetector piiDetector) {
        return new PiiDetectionAdvisor(piiDetector);
    }

    @Bean
    @ConditionalOnBean(ContentSafetyChecker.class)
    @ConditionalOnMissingBean(ContentSafetyAdvisor.class)
    public ContentSafetyAdvisor contentSafetyAdvisor(ContentSafetyChecker safetyChecker) {
        return new ContentSafetyAdvisor(safetyChecker);
    }

    @Bean
    @ConditionalOnBean(AuditLogger.class)
    @ConditionalOnMissingBean(AuditLoggingAdvisor.class)
    public AuditLoggingAdvisor auditLoggingAdvisor(AuditLogger auditLogger) {
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
        return callbacks;
    }
}
