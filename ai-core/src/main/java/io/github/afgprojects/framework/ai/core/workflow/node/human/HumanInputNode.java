package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Human input node - pauses execution to collect human input.
 *
 * <p>Pauses the workflow until a human provides input data. The input
 * can be free-form text or structured data based on a provided schema.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code prompt} (required) - prompt message for the human</li>
 *   <li>{@code schema} (optional) - expected input schema for structured input</li>
 *   <li>{@code timeoutMs} (optional) - timeout for input, defaults to 0 (no timeout)</li>
 * </ul>
 */
@Slf4j
public class HumanInputNode implements WorkflowNode {

    public static final String TYPE = "human-input";

    private final String nodeId;
    private final HumanInteraction humanInteraction;

    public HumanInputNode(String nodeId, HumanInteraction humanInteraction) {
        this.nodeId = nodeId;
        this.humanInteraction = humanInteraction;
    }

    public HumanInputNode(String nodeId) {
        this.nodeId = nodeId;
        this.humanInteraction = null;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            String prompt = getRequiredParam(params, "prompt");
            Object schema = params.get("schema");
            long timeoutMs = getLongParam(params, "timeoutMs", 0L);

            log.debug("HumanInputNode [{}] requesting input: {}", nodeId, prompt);

            if (humanInteraction == null) {
                // No human interaction available - return empty input
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("input", null);
                result.put("autoResponded", true);
                result.put("message", "Auto-responded (no HumanInteraction configured)");
                long duration = System.currentTimeMillis() - startTime;
                return NodeOutput.of(result).withDuration(duration);
            }

            Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);
            Object userInput = humanInteraction.requestInput(
                    context.getWorkflowId(), prompt, schema, timeout).get();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("input", userInput);

            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(result).withDuration(duration);

        } catch (Exception e) {
            log.error("HumanInputNode [{}] execution failed", nodeId, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        return Flux.just(NodeEvent.complete(execute(context, params)));
    }

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.longValue();
        return Long.parseLong(value.toString());
    }
}
