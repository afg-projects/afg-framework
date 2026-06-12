package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.workflow.checkpoint.CheckpointManager;
import io.github.afgprojects.framework.ai.core.api.workflow.dsl.DslConverter;
import io.github.afgprojects.framework.ai.core.api.workflow.dsl.DslValidator;
import io.github.afgprojects.framework.ai.core.api.workflow.dsl.VariableResolver;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypeRegistry;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DefaultDslConverter;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DefaultDslValidator;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DefaultVariableResolver;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultDagEngine;
import io.github.afgprojects.framework.ai.core.workflow.node.DefaultNodeTypeRegistry;
import io.github.afgprojects.framework.ai.core.workflow.InMemoryCheckpointManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.function.Function;

/**
 * AFG AI 工作流自动配置。
 *
 * <p>配置前缀：{@code afg.ai.workflow}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiAgentAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiWorkflowAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class WorkflowConfiguration {

        @Bean
        @ConditionalOnMissingBean(DagEngine.class)
        public DefaultDagEngine defaultDagEngine(@Nullable Function<String, WorkflowNode> nodeResolver) {
            if (nodeResolver != null) {
                return new DefaultDagEngine(nodeResolver);
            }
            // 如果没有节点解析器，返回一个不做任何事的引擎
            return new DefaultDagEngine(type -> null);
        }

        @Bean
        @ConditionalOnMissingBean(NodeTypeRegistry.class)
        public DefaultNodeTypeRegistry defaultNodeTypeRegistry() {
            return new DefaultNodeTypeRegistry();
        }

        @Bean
        @ConditionalOnMissingBean(DslConverter.class)
        public DefaultDslConverter defaultDslConverter() {
            return new DefaultDslConverter();
        }

        @Bean
        @ConditionalOnMissingBean(DslValidator.class)
        public DefaultDslValidator defaultDslValidator() {
            return new DefaultDslValidator();
        }

        @Bean
        @ConditionalOnMissingBean(VariableResolver.class)
        public DefaultVariableResolver defaultVariableResolver() {
            return new DefaultVariableResolver();
        }

        @Bean
        @ConditionalOnMissingBean(CheckpointManager.class)
        public InMemoryCheckpointManager inMemoryCheckpointManager() {
            return new InMemoryCheckpointManager();
        }

        // Future: WorkflowAspect AOP bean to be added
        // @Bean
        // @ConditionalOnMissingBean
        // public WorkflowAspect workflowAspect() {
        //     return new WorkflowAspect();
        // }
    }
}
