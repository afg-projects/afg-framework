package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanDecision;
import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Human approval node - pauses execution for human approval.
 *
 * <p>Pauses the workflow until a human approves or rejects the proposed action.
 * The node outputs the approval decision (approved/rejected) along with
 * optional comments from the approver. The output anchor is "approved" or
 * "rejected", enabling the DAG engine to route to different downstream nodes.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link HumanInteraction} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node auto-approves when no interaction
 * service is configured.</p>
 */
@Slf4j
public class HumanApprovalNode extends AbstractWorkflowNode<HumanApprovalNode.Params> {

    public static final String TYPE = "human-approval";

    /** Strongly-typed parameters for {@link HumanApprovalNode}. */
    public record Params(
            @Param(displayName = "Title", description = "Title of the approval request", defaultValue = "Approval Required")
            String title,
            @Param(displayName = "Description", description = "Description of what needs approval")
            String description,
            @Param(displayName = "Assignee", description = "User or role assigned to approve")
            String assignee,
            @Param(displayName = "Timeout (ms)", description = "Timeout for the approval in milliseconds", defaultValue = "0")
            Long timeoutMs
    ) {
        /** Effective title, defaulting to "Approval Required". */
        public String effectiveTitle() {
            return title == null || title.isBlank() ? "Approval Required" : title;
        }

        /** Effective timeout in milliseconds, defaulting to 0 (no timeout). */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 0L : timeoutMs;
        }
    }

    /** Output descriptor for {@link HumanApprovalNode}. */
    public record Output(
            @Out(description = "Whether approved") boolean approved,
            @Out(description = "Decision name") String decision,
            @Out(description = "Comments") String comments
    ) {}

    private final HumanInteraction humanInteraction;

    public HumanApprovalNode(String nodeId, HumanInteraction humanInteraction) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = humanInteraction;
    }

    public HumanApprovalNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.humanInteraction = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    public Set<String> getSourceAnchors() {
        return Set.of("approved", "rejected");
    }

    @Override
    public Set<String> getTargetAnchors() {
        return Set.of("input");
    }

    @Override
    protected NodeResult doExecuteRich(ExecutionContext context, Params params) {
        String title = params.effectiveTitle();
        String description = params.description();
        String assignee = params.assignee();

        log.debug("HumanApprovalNode [{}] requesting approval: {}", getNodeId(), title);

        if (humanInteraction == null) {
            // No human interaction available - auto-approve (default "output" anchor)
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("approved", true);
            result.put("comments", "Auto-approved (no HumanInteraction configured)");
            result.put("autoApproved", true);
            return NodeResult.of(result);
        }

        // Request human approval
        String prompt = title;
        Object content = description != null ? Map.of("description", description, "assignee", assignee != null ? assignee : "") : null;
        long timeoutMs = params.effectiveTimeoutMs();
        Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);

        HumanDecision decision = awaitInteraction(() -> humanInteraction.requestApproval(
                context.getWorkflowId(), prompt, content, timeout).get());

        boolean approved = decision == HumanDecision.APPROVED;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approved", approved);
        result.put("decision", decision.name());

        String anchor = approved ? "approved" : "rejected";
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
