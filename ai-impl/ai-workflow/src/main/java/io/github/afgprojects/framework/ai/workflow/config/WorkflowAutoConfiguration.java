package io.github.afgprojects.framework.ai.workflow.config;

import io.github.afgprojects.framework.ai.core.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DslConverter;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DslValidator;
import io.github.afgprojects.framework.ai.core.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.node.NodeTypeRegistry;
import io.github.afgprojects.framework.ai.workflow.dsl.DefaultDslConverter;
import io.github.afgprojects.framework.ai.workflow.dsl.DefaultDslValidator;
import io.github.afgprojects.framework.ai.workflow.engine.DefaultDagEngine;
import io.github.afgprojects.framework.ai.workflow.node.DefaultNodeTypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Auto-configuration for the workflow engine.
 * <p>
 * Provides default beans for:
 * <ul>
 *   <li>{@link NodeTypeRegistry} - registry for node type definitions</li>
 *   <li>{@link DslConverter} - JSON DSL converter</li>
 *   <li>{@link DslValidator} - workflow DSL validator</li>
 *   <li>{@link DagEngine} - DAG execution engine</li>
 * </ul>
 * <p>
 * Platform-level configuration can override any of these beans by providing
 * their own implementations. Node instances are registered via
 * {@link WorkflowNodeProvider} beans.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DagEngine.class)
public class WorkflowAutoConfiguration {

    /**
     * Creates the node type registry and registers all provided node definitions.
     *
     * @param definitions list of node definitions to register (injected from other modules)
     * @return the node type registry
     */
    @Bean
    @ConditionalOnMissingBean(NodeTypeRegistry.class)
    public NodeTypeRegistry nodeTypeRegistry(List<NodeDefinition> definitions) {
        log.info("Creating DefaultNodeTypeRegistry with {} definitions", definitions.size());
        DefaultNodeTypeRegistry registry = new DefaultNodeTypeRegistry();
        for (NodeDefinition definition : definitions) {
            registry.register(definition);
            log.debug("Registered node type: {}", definition.getType());
        }
        return registry;
    }

    /**
     * Creates the DSL converter for JSON serialization/deserialization.
     */
    @Bean
    @ConditionalOnMissingBean(DslConverter.class)
    public DslConverter dslConverter() {
        log.info("Creating DefaultDslConverter");
        return new DefaultDslConverter();
    }

    /**
     * Creates the DSL validator for workflow definitions.
     */
    @Bean
    @ConditionalOnMissingBean(DslValidator.class)
    public DslValidator dslValidator() {
        log.info("Creating DefaultDslValidator");
        return new DefaultDslValidator();
    }

    /**
     * Creates the DAG execution engine.
     * <p>
     * The engine uses a node resolver that looks up nodes from the
     * {@link WorkflowNodeProvider} beans. Platform-level code can
     * override this bean to provide a custom engine implementation.
     *
     * @param providers list of node providers that supply workflow node instances
     * @return the DAG engine
     */
    @Bean
    @ConditionalOnMissingBean(DagEngine.class)
    public DagEngine dagEngine(List<WorkflowNodeProvider> providers) {
        log.info("Creating DefaultDagEngine with {} node providers", providers.size());

        // Build a nodeId -> node map from all providers
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        for (WorkflowNodeProvider provider : providers) {
            for (WorkflowNode node : provider.getNodes()) {
                nodeMap.put(node.getNodeId(), node);
                log.debug("Registered workflow node: {} (type: {})",
                        node.getNodeId(), node.getType());
            }
        }

        // Create a node resolver function
        Function<String, WorkflowNode> nodeResolver = nodeMap::get;

        return new DefaultDagEngine(nodeResolver);
    }
}
