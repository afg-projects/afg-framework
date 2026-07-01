package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Human input node - pauses execution to collect human input.
 *
 * <p>Pauses the workflow until a human provides input data. The input
 * can be free-form text or structured data based on a provided schema.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link HumanInteraction} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node auto-responds when no interaction
 * service is configured.</p>
 */
@Slf4j
public class HumanInputNode extends AbstractWorkflowNode<HumanInputNode.Params> {

    public static final String TYPE = "human-input";

    /** Strongly-typed parameters for {@link HumanInputNode}. */
    public record Params(
            @Param(displayName = "Prompt", description = "Prompt message for the human", required = true)
            String prompt,
            @Param(displayName = "Schema", description = "Expected input schema for structured input")
            Object schema,
            @Param(displayName = "Timeout (ms)", description = "Timeout for input in milliseconds", defaultValue = "0")
            Long timeoutMs
    ) {
        /** Effective timeout in milliseconds, defaulting to 0 (no timeout). */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 0L : timeoutMs;
        }
    }

    /** Output descriptor for {@link HumanInputNode}. */
    public record Output(
            @Out(description = "Human input data") Object input
    ) {}

    private final HumanInteraction humanInteraction;

    public HumanInputNode(String nodeId, HumanInteraction humanInteraction) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = humanInteraction;
    }

    public HumanInputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String prompt = params.prompt();
        Object schema = params.schema();
        long timeoutMs = params.effectiveTimeoutMs();

        log.debug("HumanInputNode [{}] requesting input: {}", getNodeId(), prompt);

        if (humanInteraction == null) {
            // No human interaction available - return empty input
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("input", null);
            result.put("autoResponded", true);
            result.put("message", "Auto-responded (no HumanInteraction configured)");
            return result;
        }

        Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);
        Object userInput = awaitInteraction(() -> humanInteraction.requestInput(
                context.getWorkflowId(), prompt, schema, timeout).get());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("input", userInput);
        return result;
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
