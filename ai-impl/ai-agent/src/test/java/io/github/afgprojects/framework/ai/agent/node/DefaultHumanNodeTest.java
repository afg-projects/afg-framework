package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanDecision;
import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.HumanNode;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultHumanNodeTest {

    private HumanInteraction mockInteraction;
    private DefaultHumanNode humanNode;

    @BeforeEach
    void setUp() {
        mockInteraction = mock(HumanInteraction.class);
        humanNode = new DefaultHumanNode(
                "approve-1",
                mockInteraction,
                HumanNode.InteractionType.APPROVE,
                "请审批以下内容",
                "content",
                "approval_result",
                Duration.ofMinutes(30),
                HumanNode.TimeoutAction.REJECT
        );
    }

    @Test
    @DisplayName("获取节点ID")
    void getId_returnsId() {
        assertThat(humanNode.getId()).isEqualTo("approve-1");
    }

    @Test
    @DisplayName("获取节点类型")
    void getType_returnsHuman() {
        assertThat(humanNode.getType()).isEqualTo(NodeType.HUMAN);
    }

    @Test
    @DisplayName("获取交互类型")
    void getInteractionType_returnsApprove() {
        assertThat(humanNode.getInteractionType()).isEqualTo(HumanNode.InteractionType.APPROVE);
    }

    @Test
    @DisplayName("执行审批-批准")
    void execute_approved_returnsSuccess() {
        WorkflowState state = new WorkflowState()
                .with("content", "test content");

        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(HumanDecision.APPROVED));

        NodeResult result = humanNode.execute(state);

        assertThat(result.status().name()).isEqualTo("SUCCESS");
        HumanDecision decision = result.updatedState().get("approval_result");
        assertThat(decision).isEqualTo(HumanDecision.APPROVED);
    }

    @Test
    @DisplayName("执行审批-拒绝")
    void execute_rejected_returnsFailure() {
        WorkflowState state = new WorkflowState()
                .with("content", "test content");

        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(HumanDecision.REJECTED));

        NodeResult result = humanNode.execute(state);

        assertThat(result.status().name()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("获取超时配置")
    void getTimeout_returnsConfigured() {
        assertThat(humanNode.getTimeout()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("获取超时行为")
    void getTimeoutAction_returnsConfigured() {
        assertThat(humanNode.getTimeoutAction()).isEqualTo(HumanNode.TimeoutAction.REJECT);
    }

    @Test
    @DisplayName("获取提示信息")
    void getPrompt_returnsConfigured() {
        assertThat(humanNode.getPrompt()).isEqualTo("请审批以下内容");
    }

    @Test
    @DisplayName("获取内容来源key")
    void getContentKey_returnsConfigured() {
        assertThat(humanNode.getContentKey()).isEqualTo("content");
    }

    @Test
    @DisplayName("获取结果写入key")
    void getResultKey_returnsConfigured() {
        assertThat(humanNode.getResultKey()).isEqualTo("approval_result");
    }

    @Test
    @DisplayName("超时自动批准")
    void execute_timeoutWithApproveAction_returnsSuccess() {
        DefaultHumanNode nodeWithApproveTimeout = new DefaultHumanNode(
                "approve-2",
                mockInteraction,
                HumanNode.InteractionType.APPROVE,
                "请审批",
                null,
                "result",
                Duration.ofSeconds(1),
                HumanNode.TimeoutAction.APPROVE
        );

        CompletableFuture<HumanDecision> timeoutFuture = new CompletableFuture<>();
        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(timeoutFuture);

        WorkflowState state = new WorkflowState();
        NodeResult result = nodeWithApproveTimeout.execute(state);

        assertThat(result.status().name()).isEqualTo("SUCCESS");
        HumanDecision decision = result.updatedState().get("result");
        assertThat(decision).isEqualTo(HumanDecision.APPROVED);
    }

    @Test
    @DisplayName("超时自动拒绝")
    void execute_timeoutWithRejectAction_returnsFailure() {
        DefaultHumanNode nodeWithRejectTimeout = new DefaultHumanNode(
                "approve-3",
                mockInteraction,
                HumanNode.InteractionType.APPROVE,
                "请审批",
                null,
                "result",
                Duration.ofSeconds(1),
                HumanNode.TimeoutAction.REJECT
        );

        CompletableFuture<HumanDecision> timeoutFuture = new CompletableFuture<>();
        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(timeoutFuture);

        WorkflowState state = new WorkflowState();
        NodeResult result = nodeWithRejectTimeout.execute(state);

        assertThat(result.status().name()).isEqualTo("FAILURE");
        assertThat(result.errorMessage()).contains("Timeout");
    }

    @Test
    @DisplayName("超时重试")
    void execute_timeoutWithRetryAction_returnsSuccessForRetry() {
        DefaultHumanNode nodeWithRetryTimeout = new DefaultHumanNode(
                "approve-4",
                mockInteraction,
                HumanNode.InteractionType.APPROVE,
                "请审批",
                null,
                "result",
                Duration.ofSeconds(1),
                HumanNode.TimeoutAction.RETRY
        );

        CompletableFuture<HumanDecision> timeoutFuture = new CompletableFuture<>();
        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(timeoutFuture);

        WorkflowState state = new WorkflowState();
        NodeResult result = nodeWithRetryTimeout.execute(state);

        // RETRY 返回 SUCCESS，由执行器处理重试逻辑
        assertThat(result.status().name()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("取消决策")
    void execute_cancelled_returnsFailure() {
        WorkflowState state = new WorkflowState()
                .with("content", "test content");

        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(HumanDecision.CANCELLED));

        NodeResult result = humanNode.execute(state);

        assertThat(result.status().name()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("超时决策")
    void execute_timeoutDecision_returnsFailure() {
        WorkflowState state = new WorkflowState()
                .with("content", "test content");

        when(mockInteraction.requestApproval(any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(HumanDecision.TIMEOUT));

        NodeResult result = humanNode.execute(state);

        assertThat(result.status().name()).isEqualTo("FAILURE");
    }
}
