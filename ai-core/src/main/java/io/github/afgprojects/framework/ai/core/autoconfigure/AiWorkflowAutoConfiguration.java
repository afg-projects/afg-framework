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

import java.util.function.Function;

/**
 * AFG AI 工作流自动配置。
 *
 * <p>配置前缀：{@code afg.ai.workflow}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiWorkflowAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class WorkflowConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public DefaultDagEngine defaultDagEngine(Function<String, WorkflowNode> nodeResolver) {
            return new DefaultDagEngine(nodeResolver);
        }

        @Bean
        @ConditionalOnMissingBean
        public DefaultNodeTypeRegistry defaultNodeTypeRegistry() {
            return new DefaultNodeTypeRegistry();
        }

        @Bean
        @ConditionalOnMissingBean
        public DefaultDslConverter defaultDslConverter() {
            return new DefaultDslConverter();
        }

        @Bean
        @ConditionalOnMissingBean
        public DefaultDslValidator defaultDslValidator() {
            return new DefaultDslValidator();
        }

        @Bean
        @ConditionalOnMissingBean
        public DefaultVariableResolver defaultVariableResolver() {
            return new DefaultVariableResolver();
        }

        @Bean
        @ConditionalOnMissingBean(CheckpointManager.class)
        public InMemoryCheckpointManager inMemoryCheckpointManager() {
            return new InMemoryCheckpointManager();
        }

        // TODO: 阶段4添加AOP切面Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public WorkflowAspect workflowAspect() {
        //     return new WorkflowAspect();
        // }
    }
}
