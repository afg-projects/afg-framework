package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code workflowId} (required) - ID of the sub-workflow to execute</li>
 *   <li>{@code inputMapping} (optional) - Map of sub-workflow input variable names to current context values</li>
 *   <li>{@code outputMapping} (optional) - Map of sub-workflow output keys to this node's output keys</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> Requires a DagEngine and WorkflowDefinition resolver
 * to be available. Current implementation stores the sub-workflow reference; actual
 * nested execution will be added when the DAG engine supports sub-workflow invocation.</p>
 */
@Slf4j
public class SubWorkflowNode extends AbstractWorkflowNode {

    public static final String TYPE = "sub-workflow";

    private final DagEngine dagEngine;

    public SubWorkflowNode(String nodeId, DagEngine dagEngine) {
        super(nodeId, TYPE);
        this.dagEngine = dagEngine;
    }

    public SubWorkflowNode(String nodeId) {
        super(nodeId, TYPE);
        this.dagEngine = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String workflowId = getRequiredParam(params, "workflowId");

        log.debug("SubWorkflowNode [{}] invoking sub-workflow: {}", getNodeId(), workflowId);

        @SuppressWarnings("unchecked")
        Map<String, Object> inputMapping = (Map<String, Object>) params.get("inputMapping");

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }
}
