package io.github.afgprojects.framework.ai.workflow.node;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.github.afgprojects.framework.ai.core.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for workflow nodes that wrap LangChain4j @Tool methods.
 * <p>
 * Subclasses define the available tool specifications and implement the actual
 * tool execution logic. The base class handles parameter extraction from the
 * workflow execution context and delegates to the subclass for tool invocation.
 * <p>
 * Usage:
 * <pre>
 * public class WebSearchToolNode extends Lc4jToolNode {
 *     private final WebSearchService searchService;
 *
 *     public WebSearchToolNode(String nodeId, WebSearchService searchService) {
 *         super(nodeId, "web_search");
 *         this.searchService = searchService;
 *     }
 *
 *     protected List&lt;ToolSpecification&gt; getToolSpecs() {
 *         return List.of(
 *             ToolSpecification.builder()
 *                 .name("web_search")
 *                 .description("Search the web for information")
 *                 .build()
 *         );
 *     }
 *
 *     protected Object executeTool(String toolName, Map&lt;String, Object&gt; args) {
 *         return searchService.search((String) args.get("query"));
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class Lc4jToolNode implements WorkflowNode {

    private final String nodeId;
    private final String type;

    protected Lc4jToolNode(String nodeId, String type) {
        this.nodeId = nodeId;
        this.type = type;
    }

    /**
     * Returns the list of tool specifications this node provides.
     * Used for registration and discovery by the workflow engine.
     */
    protected abstract List<ToolSpecification> getToolSpecs();

    /**
     * Executes a specific tool by name with the given arguments.
     *
     * @param toolName the name of the tool to execute
     * @param args     the arguments for the tool invocation
     * @return the result of the tool execution
     */
    protected abstract Object executeTool(String toolName, Map<String, Object> args);

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            String toolName = extractToolName(params);
            Map<String, Object> toolArgs = extractToolParams(params);

            log.debug("Node [{}] executing tool: {} with args: {}", nodeId, toolName, toolArgs);

            Object result = executeTool(toolName, toolArgs);

            Map<String, Object> data = new HashMap<>();
            data.put("toolName", toolName);
            data.put("result", result);
            // Include tool specs metadata for downstream nodes
            data.put("availableTools", getToolSpecs().stream()
                    .map(ToolSpecification::name)
                    .toList());

            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(data).withDuration(duration);

        } catch (Exception e) {
            log.error("Node [{}] tool execution failed", nodeId, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        // Tool execution is inherently synchronous
        NodeOutput output = execute(context, params);
        return Flux.just(NodeEvent.complete(output));
    }

    /**
     * Extracts the tool name from the execution parameters.
     * Looks for "toolName" or "toolId" key in params.
     */
    protected String extractToolName(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("Tool parameters must not be null");
        }
        Object toolName = params.get("toolName");
        if (toolName == null) {
            toolName = params.get("toolId");
        }
        if (toolName == null) {
            throw new IllegalArgumentException(
                    "Tool parameters must contain 'toolName' or 'toolId'");
        }
        return toolName.toString();
    }

    /**
     * Extracts tool-specific arguments from the execution parameters.
     * Looks for "toolParams" key; if not found, uses the entire params map
     * minus the "toolName"/"toolId" keys.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractToolParams(Map<String, Object> params) {
        if (params == null) {
            return Map.of();
        }

        Object toolParams = params.get("toolParams");
        if (toolParams instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        // Fallback: use all params except toolName/toolId
        Map<String, Object> args = new HashMap<>(params);
        args.remove("toolName");
        args.remove("toolId");
        return args;
    }
}
