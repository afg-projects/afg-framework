package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Switch node - multi-branch routing based on a value.
 *
 * <p>Evaluates a switch value against the workflow context and outputs a
 * result whose anchor matches the selected case, enabling the DAG engine to
 * route to different downstream nodes based on the matched case.</p>
 *
 * <p>Parameters are declared on {@link Params}.</p>
 */
@Slf4j
public class SwitchNode extends AbstractWorkflowNode<SwitchNode.Params> {

    public static final String TYPE = "switch";

    /** Strongly-typed parameters for {@link SwitchNode}. */
    public record Params(
            @Param(displayName = "Switch variable", description = "Variable name from the context to switch on")
            String variable,
            @Param(displayName = "Switch value", description = "Direct value to switch on (overrides variable)")
            String value,
            @Param(displayName = "Case mapping", description = "Map of caseValue -> caseName for matching")
            Map<String, String> cases,
            @Param(displayName = "Default case", description = "The case name when no match found", defaultValue = "default")
            String defaultCase
    ) {
        /** Effective default case name. */
        public String effectiveDefaultCase() {
            return defaultCase == null || defaultCase.isBlank() ? "default" : defaultCase;
        }
    }

    /** Output descriptor for {@link SwitchNode}. */
    public record Output(
            @Out(description = "Switch value") String switchValue,
            @Out(description = "Matched case name") String matchedCase
    ) {}

    public SwitchNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected NodeResult doExecuteRich(ExecutionContext context, Params params) {
        // Resolve the switch value
        String switchValue = resolveSwitchValue(context, params);

        // Find matching case
        Map<String, String> cases = params.cases();
        String defaultCase = params.effectiveDefaultCase();

        String matchedCase = defaultCase;
        if (cases != null && switchValue != null) {
            matchedCase = cases.get(switchValue);
            if (matchedCase == null) {
                matchedCase = defaultCase;
            }
        }

        log.debug("SwitchNode [{}] value='{}' matched case='{}'", getNodeId(), switchValue, matchedCase);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("switchValue", switchValue);
        data.put("matchedCase", matchedCase);

        // Anchor = matched case, so the DAG engine routes to the matching branch.
        return NodeResult.of(data, matchedCase);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        return doExecuteRich(context, params).data();
    }

    private String resolveSwitchValue(ExecutionContext context, Params params) {
        // Direct value takes precedence
        if (params.value() != null) {
            return params.value();
        }

        // Variable from context
        String variable = params.variable();
        if (variable != null && context.getVariables() != null) {
            Object contextValue = context.getVariables().get(variable);
            return contextValue != null ? contextValue.toString() : null;
        }

        return null;
    }
}
