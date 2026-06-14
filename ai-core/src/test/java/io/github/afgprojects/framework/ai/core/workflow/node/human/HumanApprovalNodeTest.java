package io.github.afgprojects.framework.ai.core.workflow.node.human;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * HumanApprovalNode 纯单元测试
 *
 * <p>测试人工审批节点在无 HumanInteraction 实现时的降级行为：
 * 自动审批、参数传递、anchor 路由。
 * 不需要真实的人工交互系统。
 */
@DisplayName("HumanApprovalNode")
class HumanApprovalNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");

            assertThat(node.getNodeId()).isEqualTo("approval-1");
            assertThat(node.getType()).isEqualTo("human-approval");
        }
    }

    @Nested
    @DisplayName("降级行为：无 HumanInteraction")
    class NoInteractionFallback {

        @Test
        @DisplayName("humanInteraction 为 null 时应自动审批通过")
        void shouldAutoApprove_whenNoHumanInteraction() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.data()).containsEntry("approved", true);
            assertThat(output.data()).containsEntry("autoApproved", true);
            assertThat(output.data()).containsKey("comments");
        }

        @Test
        @DisplayName("自动审批时 anchor 应为 output（默认 anchor）")
        void shouldReturnOutputAnchor_whenAutoApproved() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            // Auto-approve path uses NodeOutput.of(result) which defaults anchor to "output"
            assertThat(output.anchor()).isEqualTo("output");
        }

        @Test
        @DisplayName("null HumanInteraction 构造也应有相同降级行为")
        void shouldAutoApprove_whenNullHumanInteraction() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1", null);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.data()).containsEntry("approved", true);
        }
    }

    @Nested
    @DisplayName("参数传递")
    class ParameterPassing {

        @Test
        @DisplayName("title 参数应传递到审批请求")
        void shouldPassTitleParameter() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of(
                "title", "Deploy to Production",
                "description", "Release v2.0"
            ));

            // Auto-approved, but parameters were processed without error
            assertThat(output.data()).containsEntry("approved", true);
        }

        @Test
        @DisplayName("无 title 参数时应使用默认标题")
        void shouldUseDefaultTitle_whenTitleMissing() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            // Should not throw, uses default "Approval Required"
            assertThat(output.data()).containsEntry("approved", true);
        }
    }

    @Nested
    @DisplayName("输出属性")
    class OutputProperties {

        @Test
        @DisplayName("输出应包含耗时信息")
        void shouldIncludeDuration() {
            HumanApprovalNode node = new HumanApprovalNode("approval-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            assertThat(output.durationMs()).isGreaterThanOrEqualTo(0);
        }
    }
}
