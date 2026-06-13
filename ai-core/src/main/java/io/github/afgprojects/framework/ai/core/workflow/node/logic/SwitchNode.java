package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Switch node - multi-branch routing based on a value.
 *
 * <p>Evaluates a switch expression against the workflow context and
 * outputs a result with an anchor matching the selected case. This enables
 * the DAG engine to route to different downstream nodes based on the
 * matched case.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code variable} (optional) - variable name from the context to switch on</li>
 *   <li>{@code value} (optional) - direct value to switch on (overrides variable)</li>
 *   <li>{@code cases} (optional) - Map of caseValue -> caseName for matching</li>
 *   <li>{@code defaultCase} (optional) - the case name when no match found, defaults to "default"</li>
 * </ul>
 */
@Slf4j
public class SwitchNode implements WorkflowNode {

    public static final String TYPE = "switch";

    private final String nodeId;

    public SwitchNode(String nodeId) {
        this.nodeId = nodeId;
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
            // Resolve the switch value
            String switchValue = resolveSwitchValue(context, params);

            // Find matching case
            @SuppressWarnings("unchecked")
            Map<String, String> cases = (Map<String, String>) params.get("cases");
            String defaultCase = getParam(params, "defaultCase", "default");

            String matchedCase = defaultCase;
            if (cases != null && switchValue != null) {
                matchedCase = cases.get(switchValue);
                if (matchedCase == null) {
                    matchedCase = defaultCase;
                }
            }

            log.debug("SwitchNode [{}] value='{}' matched case='{}'", nodeId, switchValue, matchedCase);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("switchValue", switchValue);
            data.put("matchedCase", matchedCase);

            long duration = System.currentTimeMillis() - startTime;
            return new NodeOutput(data, matchedCase, 0, 0, duration);

        } catch (Exception e) {
            log.error("SwitchNode [{}] execution failed", nodeId, e);
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

    private String resolveSwitchValue(ExecutionContext context, Map<String, Object> params) {
        // Direct value takes precedence
        Object directValue = params.get("value");
        if (directValue != null) {
            return directValue.toString();
        }

        // Variable from context
        String variable = (String) params.get("variable");
        if (variable != null) {
            Object contextValue = context.getVariables().get(variable);
            return contextValue != null ? contextValue.toString() : null;
        }

        return null;
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
