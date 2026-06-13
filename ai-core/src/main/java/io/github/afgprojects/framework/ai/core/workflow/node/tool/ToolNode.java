package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool node - invokes a registered tool by name.
 *
 * <p>Looks up a tool by name from the {@link ToolRegistry} and executes it
 * with the provided input parameters. The tool's output is passed through
 * as the node's output data.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code toolName} (required) - name of the tool to invoke</li>
 *   <li>{@code toolInput} (optional) - input data for the tool</li>
 * </ul>
 */
@Slf4j
public class ToolNode extends AbstractWorkflowNode {

    public static final String TYPE = "tool";

    private final ToolRegistry toolRegistry;

    public ToolNode(String nodeId, ToolRegistry toolRegistry) {
        super(nodeId, TYPE);
        this.toolRegistry = toolRegistry;
    }

    public ToolNode(String nodeId) {
        super(nodeId, TYPE);
        this.toolRegistry = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String toolName = getRequiredParam(params, "toolName");

        log.debug("ToolNode [{}] invoking tool: {}", getNodeId(), toolName);

        if (toolRegistry == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("toolName", toolName);
            result.put("error", "No ToolRegistry available - tool invocation not configured");
            return result;
        }

        Tool<?, ?> tool = toolRegistry.getTool(toolName).orElse(null);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        Object toolInput = params.get("toolInput");
        Object toolOutput;
        try {
            @SuppressWarnings("unchecked")
            Tool<Object, Object> typedTool = (Tool<Object, Object>) tool;
            toolOutput = typedTool.execute(toolInput);
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("toolName", toolName);
            result.put("error", e.getMessage() != null ? e.getMessage() : "Tool execution failed");
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("result", toolOutput);
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
