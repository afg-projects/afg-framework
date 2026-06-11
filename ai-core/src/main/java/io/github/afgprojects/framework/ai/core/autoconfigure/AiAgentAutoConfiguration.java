package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.agent.*;
import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRoutingStrategy;
import io.github.afgprojects.framework.ai.core.api.multiagent.Coordinator;
import io.github.afgprojects.framework.ai.core.api.multiagent.Orchestrator;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentWorkflow;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.CommunicationBus;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.MessageHandler;
import io.github.afgprojects.framework.ai.core.api.multiagent.decomposition.TaskDecomposer;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.StateManager;
import io.github.afgprojects.framework.ai.core.api.planning.PlanExecuteExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.ReActExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.ReflectionExecutor;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.tool.DefaultToolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 智能体自动配置。
 *
 * <p>配置前缀：{@code afg.ai.agent}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiAgentAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class AgentConfiguration {

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

        @Bean
        @ConditionalOnMissingBean(CommunicationBus.class)
        public InMemoryCommunicationBus inMemoryCommunicationBus() {
            return new InMemoryCommunicationBus();
        }

        @Bean
        @ConditionalOnMissingBean(StateManager.class)
        public InMemoryStateManager inMemoryStateManager() {
            return new InMemoryStateManager();
        }

        @Bean
        @ConditionalOnMissingBean(TaskDecomposer.class)
        public TemplateTaskDecomposer templateTaskDecomposer() {
            return new TemplateTaskDecomposer();
        }

        @Bean
        @ConditionalOnMissingBean(ReActExecutor.class)
        @ConditionalOnProperty(prefix = "afg.ai.agent.react", name = "enabled", havingValue = "true", matchIfMissing = true)
        public ReActExecutor defaultReActExecutor(
                @Autowired(required = false) AfgChatClient chatClient,
                @Autowired(required = false) ToolRegistry toolRegistry,
                AfgAiProperties properties) {
            if (chatClient == null) {
                return new NoOpReActExecutor();
            }
            return new DefaultReActExecutor(chatClient,
                    toolRegistry != null ? toolRegistry : new DefaultToolRegistry(),
                    properties.getAgent().getReAct().getMaxSteps());
        }

        @Bean
        @ConditionalOnMissingBean(MessageHandler.class)
        public LoggingMessageHandler loggingMessageHandler() {
            return new LoggingMessageHandler();
        }

        @Bean
        @ConditionalOnMissingBean(AgentExecutor.class)
        public DefaultAgentExecutor defaultAgentExecutor() {
            return new DefaultAgentExecutor();
        }

        @Bean
        @ConditionalOnMissingBean(Agent.class)
        @ConditionalOnBean({AfgChatClient.class, ToolRegistry.class})
        public DefaultAgent defaultAgent(
                AfgChatClient chatClient,
                ToolRegistry toolRegistry,
                AfgAiProperties properties) {
            return DefaultAgent.builder()
                    .chatClient(chatClient)
                    .toolRegistry(toolRegistry)
                    .maxIterations(properties.getAgent().getMaxIterations())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(PlanExecuteExecutor.class)
        @ConditionalOnBean({AfgChatClient.class, AgentExecutor.class, Agent.class})
        public DefaultPlanExecuteExecutor defaultPlanExecuteExecutor(
                AfgChatClient chatClient,
                AgentExecutor agentExecutor,
                Agent agent) {
            return new DefaultPlanExecuteExecutor(chatClient, agentExecutor, agent);
        }

        @Bean
        @ConditionalOnMissingBean(ReflectionExecutor.class)
        @ConditionalOnBean(AfgChatClient.class)
        public DefaultReflectionExecutor defaultReflectionExecutor(AfgChatClient chatClient) {
            return new DefaultReflectionExecutor(chatClient);
        }

        @Bean
        @ConditionalOnMissingBean(Orchestrator.class)
        @ConditionalOnBean({AgentExecutor.class, CommunicationBus.class})
        public DefaultOrchestrator defaultOrchestrator(
                AgentRoutingStrategy routingStrategy,
                AgentExecutor agentExecutor,
                CommunicationBus communicationBus) {
            return new DefaultOrchestrator(routingStrategy, agentExecutor, communicationBus);
        }

        @Bean
        @ConditionalOnMissingBean(AgentWorkflow.class)
        @ConditionalOnBean(Orchestrator.class)
        public DefaultAgentWorkflow defaultAgentWorkflow(Orchestrator orchestrator) {
            return new DefaultAgentWorkflow(
                    "default-workflow",
                    "Default Agent Workflow",
                    java.util.List.of(),
                    orchestrator);
        }
    }
}