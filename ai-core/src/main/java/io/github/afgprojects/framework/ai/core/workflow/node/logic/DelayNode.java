package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delay node - introduces a timed delay in the workflow.
 *
 * <p>Pauses workflow execution for a specified duration. Useful for rate limiting,
 * waiting for external processes, or simulating delays in testing.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code delayMs} (required) - delay duration in milliseconds</li>
 *   <li>{@code reason} (optional) - description of why the delay is needed</li>
 * </ul>
 */
@Slf4j
public class DelayNode extends AbstractWorkflowNode {

    public static final String TYPE = "delay";

    public DelayNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        long delayMs = getLongParam(params, "delayMs", 0);
        String reason = getParam(params, "reason", null);

        if (delayMs <= 0) {
            throw new IllegalArgumentException("Required parameter 'delayMs' must be a positive number");
        }

        log.debug("DelayNode [{}] waiting {}ms{}", getNodeId(), delayMs,
                reason != null ? " (reason: " + reason + ")" : "");

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("DelayNode [{}] interrupted", getNodeId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("delayMs", delayMs);
        result.put("reason", reason);
        result.put("completed", true);
        return result;
    }

    private long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.longValue();
        return Long.parseLong(value.toString());
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
