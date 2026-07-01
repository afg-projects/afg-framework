package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sub-workflow node - executes another workflow as a node within the current workflow.
 *
 * <p>Invokes a nested workflow definition, passing parameters from the current
 * context. The sub-workflow executes independently and its results are collected
 * as this node's output.</p>
 *
 * <p>Parameters are declared on {@link Params}; the {@link DagEngine} is a
 * construction-time dependency (optional — when absent the node reports that
 * sub-workflow execution is unavailable).</p>
 *
 * <p><strong>Alpha feature:</strong> Requires a DagEngine and WorkflowDefinition resolver
 * to be available. Current implementation stores the sub-workflow reference; actual
 * nested execution will be added when the DAG engine supports sub-workflow invocation.</p>
 */
@Slf4j
public class SubWorkflowNode extends AbstractWorkflowNode<SubWorkflowNode.Params> {

    public static final String TYPE = "sub-workflow";

    /** Strongly-typed parameters for {@link SubWorkflowNode}. */
    public record Params(
            @Param(displayName = "Sub-workflow ID", description = "ID of the sub-workflow to execute", required = true)
            String workflowId,
            @Param(displayName = "Input mapping", description = "Map of sub-workflow input variable names to current context values")
            Map<String, Object> inputMapping,
            @Param(displayName = "Output mapping", description = "Map of sub-workflow output keys to this node's output keys")
            Map<String, String> outputMapping
    ) {}

    /** Output descriptor for {@link SubWorkflowNode}. */
    public record Output(
            @Out(description = "Sub-workflow ID") String subWorkflowId,
            @Out(description = "Input mapping") Map<String, Object> inputMapping,
            @Out(description = "Whether executed") boolean executed,
            @Out(description = "Message") String message
    ) {}

    private final DagEngine dagEngine;

    public SubWorkflowNode(String nodeId, DagEngine dagEngine) {
        super(nodeId, TYPE, Params.class);
        this.dagEngine = dagEngine;
    }

    public SubWorkflowNode(String nodeId) {
        this(nodeId, null);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String workflowId = params.workflowId();

        log.debug("SubWorkflowNode [{}] invoking sub-workflow: {}", getNodeId(), workflowId);

        Map<String, Object> inputMapping = params.inputMapping();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subWorkflowId", workflowId);
        result.put("inputMapping", inputMapping != null ? inputMapping : Map.of());

        if (dagEngine == null) {
            result.put("executed", false);
            result.put("message", "Sub-workflow execution requires DagEngine integration");
            return result;
        }

        // Future: resolve WorkflowDefinition by workflowId, create sub-context,
        // execute sub-workflow, and map results back.
        result.put("executed", false);
        result.put("message", "Sub-workflow execution pending DAG engine sub-workflow support");
        return result;
    }
}
