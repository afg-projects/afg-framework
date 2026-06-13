package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.tool.remote.ToolDiscoveryClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code toolName} (required) - name of the MCP tool to invoke</li>
 *   <li>{@code serverName} (optional) - name of the MCP server providing the tool</li>
 *   <li>{@code arguments} (optional) - arguments for the tool invocation</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> MCP protocol integration is in early development.
 * Current implementation stores the invocation metadata.</p>
 */
@Slf4j
public class McpToolNode extends AbstractWorkflowNode {

    public static final String TYPE = "mcp-tool";

    private final ToolDiscoveryClient toolDiscoveryClient;

    public McpToolNode(String nodeId, ToolDiscoveryClient toolDiscoveryClient) {
        super(nodeId, TYPE);
        this.toolDiscoveryClient = toolDiscoveryClient;
    }

    public McpToolNode(String nodeId) {
        super(nodeId, TYPE);
        this.toolDiscoveryClient = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String toolName = getRequiredParam(params, "toolName");
        String serverName = getParam(params, "serverName", null);

        log.debug("McpToolNode [{}] invoking MCP tool: {} from server: {}", getNodeId(), toolName, serverName);

        Object arguments = params.get("arguments");

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
