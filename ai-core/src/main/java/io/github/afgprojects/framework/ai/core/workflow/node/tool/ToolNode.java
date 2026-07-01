package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link ToolRegistry} is a construction-time dependency (not a parameter);
 * the no-arg constructor leaves it null so the node degrades gracefully when
 * no registry is configured.</p>
 */
@Slf4j
public class ToolNode extends AbstractWorkflowNode<ToolNode.Params> {

    public static final String TYPE = "tool";

    /** Strongly-typed parameters for {@link ToolNode}. */
    public record Params(
            @Param(displayName = "Tool name", description = "Name of the tool to invoke", required = true)
            String toolName,
            @Param(displayName = "Tool input", description = "Input data for the tool (any type)")
            Object toolInput
    ) {}

    /** Output descriptor for {@link ToolNode}. */
    public record Output(
            @Out(description = "Tool name") String toolName,
            @Out(description = "Tool result") Object result
    ) {}

    private final ToolRegistry toolRegistry;

    public ToolNode(String nodeId, ToolRegistry toolRegistry) {
        super(nodeId, TYPE, Params.class);
        this.toolRegistry = toolRegistry;
    }

    public ToolNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.toolRegistry = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String toolName = params.toolName();

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

        Object toolInput = params.toolInput();
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
}
