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
import java.util.List;
import java.util.Map;

/**
 * Human choice node - presents options for human selection.
 *
 * <p>Pauses the workflow and presents a set of choices to a human operator.
 * The selected choice determines the workflow's next path via the output anchor.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code prompt} (required) - prompt message presenting the choices</li>
 *   <li>{@code choices} (required) - list of choice options (maps with "label" and "value" keys)</li>
 *   <li>{@code timeoutMs} (optional) - timeout for selection, defaults to 0 (no timeout)</li>
 * </ul>
 */
@Slf4j
public class HumanChoiceNode implements WorkflowNode {

    public static final String TYPE = "human-choice";

    private final String nodeId;
    private final HumanInteraction humanInteraction;

    public HumanChoiceNode(String nodeId, HumanInteraction humanInteraction) {
        this.nodeId = nodeId;
        this.humanInteraction = humanInteraction;
    }

    public HumanChoiceNode(String nodeId) {
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
    @SuppressWarnings("unchecked")
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            String prompt = getRequiredParam(params, "prompt");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) params.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalArgumentException("Required parameter 'choices' must be a non-empty list");
            }
            long timeoutMs = getLongParam(params, "timeoutMs", 0L);

            log.debug("HumanChoiceNode [{}] presenting {} choices", nodeId, choices.size());

            if (humanInteraction == null) {
                // No human interaction available - auto-select first choice
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("selectedChoice", firstChoice.get("value"));
                result.put("selectedLabel", firstChoice.get("label"));
                result.put("autoSelected", true);
                String anchor = "choice_" + firstChoice.get("value");
                long duration = System.currentTimeMillis() - startTime;
                return new NodeOutput(result, anchor, 0, 0, duration);
            }

            Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);
            Object userInput = humanInteraction.requestInput(
                    context.getWorkflowId(), prompt, choices, timeout).get();

            // Find the matching choice
            String selectedValue = userInput != null ? userInput.toString() : null;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("selectedChoice", selectedValue);

            String anchor = "choice_" + (selectedValue != null ? selectedValue : "default");
            long duration = System.currentTimeMillis() - startTime;
            return new NodeOutput(result, anchor, 0, 0, duration);

        } catch (Exception e) {
            log.error("HumanChoiceNode [{}] execution failed", nodeId, e);
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
