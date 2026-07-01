package io.github.afgprojects.framework.ai.core.workflow.node.logic;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params}.</p>
 */
@Slf4j
public class DelayNode extends AbstractWorkflowNode<DelayNode.Params> {

    public static final String TYPE = "delay";

    /** Strongly-typed parameters for {@link DelayNode}. */
    public record Params(
            @Param(displayName = "Delay (ms)", description = "Delay duration in milliseconds", required = true)
            Long delayMs,
            @Param(displayName = "Delay reason", description = "Description of why the delay is needed")
            String reason
    ) {}

    /** Output descriptor for {@link DelayNode}. */
    public record Output(
            @Out(description = "Delay duration") long delayMs,
            @Out(description = "Delay reason") String reason,
            @Out(description = "Delay completed") boolean completed
    ) {}

    public DelayNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        long delayMs = params.delayMs() == null ? 0 : params.delayMs();
        String reason = params.reason();

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
}
