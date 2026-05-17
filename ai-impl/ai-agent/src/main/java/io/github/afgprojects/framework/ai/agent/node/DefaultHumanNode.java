package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.multiagent.human.HumanDecision;
import io.github.afgprojects.framework.ai.core.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.multiagent.node.HumanNode;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 默认人机交互节点实现
 *
 * <p>提供人机交互节点的标准实现，支持审批、输入、审核等交互类型。
 */
public class DefaultHumanNode implements HumanNode {

    private final String id;
    private final HumanInteraction humanInteraction;
    private final InteractionType type;
    private final String prompt;
    private final String contentKey;
    private final String resultKey;
    private final Duration timeout;
    private final TimeoutAction timeoutAction;

    /**
     * 创建人机交互节点
     *
     * @param id               节点ID
     * @param humanInteraction 人机交互服务
     * @param type             交互类型
     * @param prompt           提示信息
     * @param contentKey       内容来源 key（可为 null）
     * @param resultKey        结果写入 key
     * @param timeout          超时时间
     * @param timeoutAction    超时行为
     */
    public DefaultHumanNode(
            String id,
            HumanInteraction humanInteraction,
            InteractionType type,
            String prompt,
            @Nullable String contentKey,
            String resultKey,
            Duration timeout,
            TimeoutAction timeoutAction
    ) {
        this.id = id;
        this.humanInteraction = humanInteraction;
        this.type = type;
        this.prompt = prompt;
        this.contentKey = contentKey;
        this.resultKey = resultKey;
        this.timeout = timeout;
        this.timeoutAction = timeoutAction;
    }

    @Override
    @NonNull
    public String getId() {
        return id;
    }

    @Override
    @NonNull
    public NodeResult execute(@NonNull WorkflowState state) {
        String interactionId = UUID.randomUUID().toString();
        Object content = contentKey != null ? state.get(contentKey) : null;

        try {
            HumanDecision decision = humanInteraction
                    .requestApproval(interactionId, prompt, content, timeout)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            WorkflowState updatedState = state.with(resultKey, decision);

            if (decision == HumanDecision.APPROVED) {
                return NodeResult.success(updatedState);
            } else {
                return NodeResult.failure(updatedState, "Human rejected: " + decision);
            }
        } catch (Exception e) {
            return handleTimeout(state);
        }
    }

    @Override
    @NonNull
    public NodeType getType() {
        return NodeType.HUMAN;
    }

    @Override
    @NonNull
    public InteractionType getInteractionType() {
        return type;
    }

    @Override
    @NonNull
    public String getPrompt() {
        return prompt;
    }

    @Override
    @Nullable
    public String getContentKey() {
        return contentKey;
    }

    @Override
    @NonNull
    public String getResultKey() {
        return resultKey;
    }

    @Override
    @NonNull
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    @NonNull
    public TimeoutAction getTimeoutAction() {
        return timeoutAction;
    }

    private NodeResult handleTimeout(WorkflowState state) {
        return switch (timeoutAction) {
            case APPROVE -> NodeResult.success(state.with(resultKey, HumanDecision.APPROVED));
            case REJECT -> NodeResult.failure(state, "Timeout: rejected by default");
            case RETRY -> NodeResult.success(state); // 由执行器处理重试
        };
    }
}
