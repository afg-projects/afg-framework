package io.github.afgprojects.framework.ai.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.agent.executor.DefaultReActExecutor;
import io.github.afgprojects.framework.ai.agent.executor.SecureToolExecutor;
import io.github.afgprojects.framework.ai.agent.executor.ToolExecutor;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import io.github.afgprojects.framework.ai.chat.DefaultAfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.tool.ToolContextProvider;
import io.github.afgprojects.framework.ai.core.tool.ToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI Agent 自动配置
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass({AfgChatClient.class, ToolRegistry.class})
@EnableConfigurationProperties(AiAgentProperties.class)
public class AiAgentAutoConfiguration {

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    @ConditionalOnMissingBean(ToolRegistry.class)
    public ToolRegistry toolRegistry() {
        return new DefaultToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultReActExecutor.class)
    public DefaultReActExecutor reActExecutor(AfgChatClient chatClient, ToolRegistry toolRegistry, AiAgentProperties properties) {
        return new DefaultReActExecutor(chatClient, toolRegistry, properties.getReAct().getMaxSteps());
    }

    @Bean
    @ConditionalOnMissingBean(ToolExecutor.class)
    public ToolExecutor toolExecutor(AfgChatClient chatClient, ToolRegistry toolRegistry, AiAgentProperties properties) {
        return new ToolExecutor(toolRegistry, chatClient,
                properties.getToolExecution().getMaxIterations(),
                properties.getToolExecution().getTimeoutMs());
    }

    @Bean
    @ConditionalOnMissingBean(SecureToolExecutor.class)
    public SecureToolExecutor secureToolExecutor(
            AfgChatClient chatClient,
            ToolRegistry toolRegistry,
            @Autowired(required = false) ToolContextProvider contextProvider,
            @Autowired(required = false) ToolPermissionChecker permissionChecker,
            @Autowired(required = false) ToolAuditLogger auditLogger,
            @Autowired(required = false) ContentSafetyChecker contentSafetyChecker,
            AiAgentProperties properties
    ) {
        return new SecureToolExecutor(
                toolRegistry,
                chatClient,
                contextProvider != null ? contextProvider : ToolContextProvider.empty(),
                permissionChecker,
                auditLogger,
                contentSafetyChecker,
                properties.getToolExecution().getMaxIterations(),
                properties.getToolExecution().getTimeoutMs()
        );
    }
}