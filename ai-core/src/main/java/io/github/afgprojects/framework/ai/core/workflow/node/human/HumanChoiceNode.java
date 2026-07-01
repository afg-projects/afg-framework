package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Human choice node - presents options for human selection.
 *
 * <p>Pauses the workflow and presents a set of choices to a human operator.
 * The selected choice determines the workflow's next path via the output
 * anchor ("choice_&lt;value&gt;").</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link HumanInteraction} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node auto-selects the first choice when
 * no interaction service is configured.</p>
 */
@Slf4j
public class HumanChoiceNode extends AbstractWorkflowNode<HumanChoiceNode.Params> {

    public static final String TYPE = "human-choice";

    /** Strongly-typed parameters for {@link HumanChoiceNode}. */
    public record Params(
            @Param(displayName = "Prompt", description = "Prompt message presenting the choices", required = true)
            String prompt,
            @Param(displayName = "Choices", description = "List of choice options (maps with \"label\" and \"value\" keys)", required = true)
            List<Map<String, Object>> choices,
            @Param(displayName = "Timeout (ms)", description = "Timeout for selection in milliseconds", defaultValue = "0")
            Long timeoutMs
    ) {
        /** Effective timeout in milliseconds, defaulting to 0 (no timeout). */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 0L : timeoutMs;
        }
    }

    /** Output descriptor for {@link HumanChoiceNode}. */
    public record Output(
            @Out(description = "Selected choice value") Object selectedChoice,
            @Out(description = "Selected label") Object selectedLabel
    ) {}

    private final HumanInteraction humanInteraction;

    public HumanChoiceNode(String nodeId, HumanInteraction humanInteraction) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = humanInteraction;
    }

    public HumanChoiceNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected NodeResult doExecuteRich(ExecutionContext context, Params params) {
        String prompt = params.prompt();
        List<Map<String, Object>> choices = params.choices();
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'choices' must be a non-empty list");
        }
        long timeoutMs = params.effectiveTimeoutMs();

        log.debug("HumanChoiceNode [{}] presenting {} choices", getNodeId(), choices.size());

        if (humanInteraction == null) {
            // No human interaction available - auto-select first choice
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("selectedChoice", firstChoice.get("value"));
            result.put("selectedLabel", firstChoice.get("label"));
            result.put("autoSelected", true);
            String anchor = "choice_" + firstChoice.get("value");
            return NodeResult.of(result, anchor);
        }

        Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);
        Object userInput = awaitInteraction(() -> humanInteraction.requestInput(
                context.getWorkflowId(), prompt, choices, timeout).get());

        // Find the matching choice
        String selectedValue = userInput != null ? userInput.toString() : null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selectedChoice", selectedValue);

        String anchor = "choice_" + (selectedValue != null ? selectedValue : "default");
        return NodeResult.of(result, anchor);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        return doExecuteRich(context, params).data();
    }

    /**
     * Await a blocking human-interaction call, unwrapping checked exceptions
     * into a {@link RuntimeException} so the base class {@code execute} can
     * handle them uniformly.
     */
    private <T> T awaitInteraction(java.util.concurrent.Callable<T> task) {
        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
