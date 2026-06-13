package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log output node - logs data as workflow output.
 *
 * <p>Logs the provided data at a specified log level. Useful for debugging
 * workflows and recording intermediate state.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code message} (required) - the message to log</li>
 *   <li>{@code level} (optional) - log level (DEBUG/INFO/WARN/ERROR), defaults to INFO</li>
 *   <li>{@code data} (optional) - additional data to include in the log</li>
 * </ul>
 */
@Slf4j
public class LogOutputNode extends AbstractWorkflowNode {

    public static final String TYPE = "log-output";

    public LogOutputNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String message = getRequiredParam(params, "message");
        String level = getParam(params, "level", "INFO");
        Object data = params.get("data");

        String logMessage = data != null
                ? message + " | data: " + data
                : message;

        switch (level.toUpperCase()) {
            case "DEBUG" -> log.debug("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            case "WARN" -> log.warn("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            case "ERROR" -> log.error("LogOutputNode [{}]: {}", getNodeId(), logMessage);
            default -> log.info("LogOutputNode [{}]: {}", getNodeId(), logMessage);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logged", true);
        result.put("level", level);
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
