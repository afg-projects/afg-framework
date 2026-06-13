package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanDecision;
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
 * Human approval node - pauses execution for human approval.
 *
 * <p>Pauses the workflow until a human approves or rejects the proposed action.
 * The node outputs the approval decision (approved/rejected) along with
 * optional comments from the approver.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code title} (optional) - title of the approval request</li>
 *   <li>{@code description} (optional) - description of what needs approval</li>
 *   <li>{@code timeoutMs} (optional) - timeout for the approval, defaults to 0 (no timeout)</li>
 *   <li>{@code assignee} (optional) - user or role assigned to approve</li>
 * </ul>
 */
@Slf4j
public class HumanApprovalNode implements WorkflowNode {

    public static final String TYPE = "human-approval";

    private final String nodeId;
    private final HumanInteraction humanInteraction;

    public HumanApprovalNode(String nodeId, HumanInteraction humanInteraction) {
        this.nodeId = nodeId;
        this.humanInteraction = humanInteraction;
    }

    public HumanApprovalNode(String nodeId) {
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
            String title = getParam(params, "title", "Approval Required");
            String description = getParam(params, "description", null);
            String assignee = getParam(params, "assignee", null);

            log.debug("HumanApprovalNode [{}] requesting approval: {}", nodeId, title);

            if (humanInteraction == null) {
                // No human interaction available - auto-approve
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("approved", true);
                result.put("comments", "Auto-approved (no HumanInteraction configured)");
                result.put("autoApproved", true);
                long duration = System.currentTimeMillis() - startTime;
                return NodeOutput.of(result).withDuration(duration);
            }

            // Request human approval
            String prompt = title;
            Object content = description != null ? Map.of("description", description, "assignee", assignee != null ? assignee : "") : null;
            long timeoutMs = getLongParam(params, "timeoutMs", 0L);
            Duration timeout = timeoutMs > 0 ? Duration.ofMillis(timeoutMs) : Duration.ofHours(24);

            HumanDecision decision = humanInteraction.requestApproval(
                    context.getWorkflowId(), prompt, content, timeout).get();

            boolean approved = decision == HumanDecision.APPROVED;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("approved", approved);
            result.put("decision", decision.name());
            String anchor = approved ? "approved" : "rejected";

            long duration = System.currentTimeMillis() - startTime;
            return new NodeOutput(result, anchor, 0, 0, duration);

        } catch (Exception e) {
            log.error("HumanApprovalNode [{}] execution failed", nodeId, e);
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

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.longValue();
        return Long.parseLong(value.toString());
    }
}
