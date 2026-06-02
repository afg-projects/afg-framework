package io.github.afgprojects.framework.ai.agent.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.agent.coordinator.DefaultAgentRoutingStrategy;
import io.github.afgprojects.framework.ai.agent.coordinator.DefaultCoordinator;
import io.github.afgprojects.framework.ai.agent.executor.DefaultReActExecutor;
import io.github.afgprojects.framework.ai.agent.executor.SecureToolExecutor;
import io.github.afgprojects.framework.ai.agent.executor.ToolExecutor;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import io.github.afgprojects.framework.ai.chat.DefaultAfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRoutingStrategy;
import io.github.afgprojects.framework.ai.core.api.multiagent.Coordinator;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContextProvider;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.ai.core.api.tool.ToolPermissionChecker;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
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
            @Autowired(required = false) ToolExecutionRecorder executionRecorder,
            AiAgentProperties properties
    ) {
        return new SecureToolExecutor(
                toolRegistry,
                chatClient,
                contextProvider != null ? contextProvider : ToolContextProvider.empty(),
                permissionChecker,
                auditLogger,
                contentSafetyChecker,
                executionRecorder,
                properties.getToolExecution().getMaxIterations(),
                properties.getToolExecution().getTimeoutMs()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AgentRoutingStrategy.class)
    public AgentRoutingStrategy agentRoutingStrategy() {
        return new DefaultAgentRoutingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean(Coordinator.class)
    public DefaultCoordinator defaultCoordinator(AgentRoutingStrategy routingStrategy) {
        return new DefaultCoordinator(routingStrategy);
    }
}