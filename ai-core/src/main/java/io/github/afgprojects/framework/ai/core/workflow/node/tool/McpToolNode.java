package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool node - invokes a tool via the Model Context Protocol.
 *
 * <p>Calls a tool discovered through the MCP protocol, which enables
 * dynamic tool discovery and invocation across distributed services.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link ToolDiscoveryClient} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node degrades gracefully when MCP is not
 * configured.</p>
 *
 * <p><strong>Alpha feature:</strong> MCP protocol integration is in early development.
 * Current implementation stores the invocation metadata.</p>
 */
@Slf4j
public class McpToolNode extends AbstractWorkflowNode<McpToolNode.Params> {

    public static final String TYPE = "mcp-tool";

    /** Strongly-typed parameters for {@link McpToolNode}. */
    public record Params(
            @Param(displayName = "Tool name", description = "Name of the MCP tool to invoke", required = true)
            String toolName,
            @Param(displayName = "Server name", description = "Name of the MCP server providing the tool")
            String serverName,
            @Param(displayName = "Arguments", description = "Arguments for the tool invocation")
            Object arguments
    ) {}

    /** Output descriptor for {@link McpToolNode}. */
    public record Output(
            @Out(description = "Tool name") String toolName,
            @Out(description = "Server name") String serverName,
            @Out(description = "Whether has arguments") boolean hasArguments,
            @Out(description = "Whether executed") boolean executed
    ) {}

    private final ToolDiscoveryClient toolDiscoveryClient;

    public McpToolNode(String nodeId, ToolDiscoveryClient toolDiscoveryClient) {
        super(nodeId, TYPE, Params.class);
        this.toolDiscoveryClient = toolDiscoveryClient;
    }

    public McpToolNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.toolDiscoveryClient = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String toolName = params.toolName();
        String serverName = params.serverName();
        Object arguments = params.arguments();

        log.debug("McpToolNode [{}] invoking MCP tool: {} from server: {}", getNodeId(), toolName, serverName);

        if (toolDiscoveryClient == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("toolName", toolName);
            result.put("serverName", serverName);
            result.put("error", "No ToolDiscoveryClient available - MCP not configured");
            return result;
        }

        // Alpha: MCP tool invocation pending full MCP protocol support
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("toolName", toolName);
        result.put("serverName", serverName);
        result.put("hasArguments", arguments != null);
        result.put("executed", false);
        result.put("message", "MCP tool invocation pending full MCP protocol integration");
        return result;
    }
}
