package io.github.afgprojects.framework.ai.workflow.config;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;

import java.util.List;

/**
 * Interface for platform-level registration of workflow node instances.
 * <p>
 * Implementations provide a list of {@link WorkflowNode} instances that will be
 * automatically registered with the workflow engine during Spring context startup.
 * <p>
 * Example:
 * <pre>
 * &#64;Component
 * public class MyWorkflowNodeProvider implements WorkflowNodeProvider {
 *     &#64;Override
 *     public List&lt;WorkflowNode&gt; getNodes() {
 *         return List.of(
 *             new MyLlmNode("my-llm-1", myAiService),
 *             new MyToolNode("my-tool-1", myToolService)
 *         );
 *     }
 * }
 * </pre>
 */
public interface WorkflowNodeProvider {

    /**
     * Returns the list of workflow node instances to register with the engine.
     *
     * @return list of workflow nodes, never null
     */
    List<WorkflowNode> getNodes();
}
