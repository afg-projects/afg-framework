package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;

/**
 * Creates workflow node instances by type id.
 *
 * <p>The DAG engine resolves a node implementation through a
 * {@code Function<String, WorkflowNode>} keyed by type; this factory is the
 * production implementation of that function, aggregating one
 * {@link NodeFactory} per built-in node type. Stateless nodes are created with
 * just their nodeId; nodes with external dependencies (e.g. an AI client)
 * receive them through the factory that owns the dependency.</p>
 *
 * <p>A factory also exposes the type id it serves, so a registry can be built
 * by enumerating factories rather than maintaining a parallel type list.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface NodeFactory {

    /**
     * The node type id this factory creates instances for.
     */
    String type();

    /**
     * Create a node instance for the given nodeId.
     *
     * @param nodeId the runtime node id from the workflow definition
     * @return a new node instance
     */
    WorkflowNode create(String nodeId);
}
