package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Condition node - evaluates a condition and routes to different branches.
 *
 * <p>Evaluates a boolean condition expression against the workflow context
 * and outputs a result indicating which branch to take. The output anchor
 * is set to "true" or "false" based on the condition result, enabling
 * the DAG engine to route to different downstream nodes.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code expression} (optional) - a simple expression like "variable == value"</li>
 *   <li>{@code variable} (optional) - a variable name from the context to evaluate</li>
 *   <li>{@code expectedValue} (optional) - the expected value to compare against</li>
 * </ul>
 *
 * <p>The output anchor is set to "true" if the condition is met, "false" otherwise.
 * Downstream edges should use sourceAnchor "true" or "false" to route accordingly.</p>
 */
@Slf4j
public class ConditionNode implements WorkflowNode {

    public static final String TYPE = "condition";

    private final String nodeId;
    private final Predicate<ExecutionContext> condition;

    public ConditionNode(String nodeId, Predicate<ExecutionContext> condition) {
        this.nodeId = nodeId;
        this.condition = condition;
    }

    public ConditionNode(String nodeId) {
        this.nodeId = nodeId;
        this.condition = null;
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
            boolean result;

            if (condition != null) {
                result = condition.test(context);
            } else {
                result = evaluateCondition(context, params);
            }

            log.debug("ConditionNode [{}] evaluated to: {}", nodeId, result);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("result", result);

            long duration = System.currentTimeMillis() - startTime;
            String anchor = result ? "true" : "false";
            return new NodeOutput(data, anchor, 0, 0, duration);

        } catch (Exception e) {
            log.error("ConditionNode [{}] execution failed", nodeId, e);
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

    private boolean evaluateCondition(ExecutionContext context, Map<String, Object> params) {
        // Strategy 1: Simple variable comparison
        String variable = (String) params.get("variable");
        Object expectedValue = params.get("expectedValue");

        if (variable != null) {
            Object actualValue = context.getVariables().get(variable);
            if (expectedValue == null) {
                return actualValue != null;
            }
            return expectedValue.toString().equals(actualValue != null ? actualValue.toString() : null);
        }

        // Strategy 2: Expression-based evaluation
        String expression = (String) params.get("expression");
        if (expression != null) {
            return evaluateExpression(expression, context);
        }

        // Default: no condition specified, evaluate to true
        return true;
    }

    private boolean evaluateExpression(String expression, ExecutionContext context) {
        // Simple expression evaluation: supports "variable == value" and "variable != value"
        String trimmed = expression.trim();

        if (trimmed.contains("==")) {
            String[] parts = trimmed.split("==", 2);
            String varName = parts[0].trim();
            String expected = parts[1].trim().replace("\"", "").replace("'", "");
            Object actual = context.getVariables().get(varName);
            return expected.equals(actual != null ? actual.toString() : null);
        }

        if (trimmed.contains("!=")) {
            String[] parts = trimmed.split("!=", 2);
            String varName = parts[0].trim();
            String expected = parts[1].trim().replace("\"", "").replace("'", "");
            Object actual = context.getVariables().get(varName);
            return !expected.equals(actual != null ? actual.toString() : null);
        }

        // Bare variable name: truthy check
        Object value = context.getVariables().get(trimmed);
        if (value instanceof Boolean bool) return bool;
        return value != null;
    }
}
