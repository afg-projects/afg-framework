package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Condition node - evaluates a condition and routes to different branches.
 *
 * <p>Evaluates a boolean condition expression against the workflow context
 * and outputs a result whose anchor is "true" or "false", enabling the DAG
 * engine to route to different downstream nodes.</p>
 *
 * <p>Parameters are declared on {@link Params}; a programmatic
 * {@code Predicate<ExecutionContext>} may also be supplied at construction time
 * and takes precedence over parameter-based evaluation.</p>
 */
@Slf4j
public class ConditionNode extends AbstractWorkflowNode<ConditionNode.Params> {

    public static final String TYPE = "condition";

    /** Strongly-typed parameters for {@link ConditionNode}. */
    public record Params(
            @Param(displayName = "Variable name", description = "A variable name from the context to evaluate")
            String variable,
            @Param(displayName = "Expected value", description = "The expected value to compare against")
            String expectedValue,
            @Param(displayName = "Condition expression", description = "A simple expression like \"variable == value\"")
            String expression
    ) {}

    /** Output descriptor for {@link ConditionNode}. */
    public record Output(
            @Out(description = "Condition result") boolean result
    ) {}

    private final Predicate<ExecutionContext> condition;

    public ConditionNode(String nodeId, Predicate<ExecutionContext> condition) {
        super(nodeId, TYPE, Params.class);
        this.condition = condition;
    }

    public ConditionNode(String nodeId) {
        this(nodeId, null);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    public Set<String> getSourceAnchors() {
        return Set.of("true", "false");
    }

    @Override
    public Set<String> getTargetAnchors() {
        return Set.of("input");
    }

    @Override
    protected NodeResult doExecuteRich(ExecutionContext context, Params params) {
        boolean result = condition != null ? condition.test(context) : evaluateCondition(context, params);

        log.debug("ConditionNode [{}] evaluated to: {}", getNodeId(), result);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result", result);

        String anchor = result ? "true" : "false";
        return NodeResult.of(data, anchor);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        return doExecuteRich(context, params).data();
    }

    private boolean evaluateCondition(ExecutionContext context, Params params) {
        // Strategy 1: Simple variable comparison
        String variable = params.variable();
        String expectedValue = params.expectedValue();

        if (variable != null) {
            Object actualValue = context.getVariables() != null ? context.getVariables().get(variable) : null;
            if (expectedValue == null) {
                return actualValue != null;
            }
            return expectedValue.equals(actualValue != null ? actualValue.toString() : null);
        }

        // Strategy 2: Expression-based evaluation
        String expression = params.expression();
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
            Object actual = context.getVariables() != null ? context.getVariables().get(varName) : null;
            return expected.equals(actual != null ? actual.toString() : null);
        }

        if (trimmed.contains("!=")) {
            String[] parts = trimmed.split("!=", 2);
            String varName = parts[0].trim();
            String expected = parts[1].trim().replace("\"", "").replace("'", "");
            Object actual = context.getVariables() != null ? context.getVariables().get(varName) : null;
            return !expected.equals(actual != null ? actual.toString() : null);
        }

        // Bare variable name: truthy check
        Object value = context.getVariables() != null ? context.getVariables().get(trimmed) : null;
        if (value instanceof Boolean bool) return bool;
        return value != null;
    }
}
