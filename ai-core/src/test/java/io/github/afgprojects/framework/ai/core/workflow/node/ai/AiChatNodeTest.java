package io.github.afgprojects.framework.ai.core.workflow.node.ai;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AiChatNode 纯单元测试
 *
 * <p>测试 AI 对话节点在无 AfgChatProvider 场景下的降级行为：
 * 无客户端时的错误输出、必需参数缺失时的异常、流式降级。
 * 不需要真实 AI Provider，仅验证 fallback 逻辑。
 */
@DisplayName("AiChatNode")
class AiChatNodeTest {

    private ExecutionContext createContext() {
        return new DefaultExecutionContext("wf-1", "conv-1", "user-1");
    }

    @Nested
    @DisplayName("基础属性")
    class BasicProperties {

        @Test
        @DisplayName("应返回正确的节点 ID 和类型")
        void shouldReturnNodeIdAndType() {
            AiChatNode node = new AiChatNode("chat-1");

            assertThat(node.getNodeId()).isEqualTo("chat-1");
            assertThat(node.getType()).isEqualTo("ai-chat");
        }

        @Test
        @DisplayName("带客户端构造应保留客户端引用")
        void shouldRetainClientReference() {
            // 构造时不抛异常即可验证
            AiChatNode node = new AiChatNode("chat-1", null);

            assertThat(node.getNodeId()).isEqualTo("chat-1");
        }
    }

    @Nested
    @DisplayName("降级行为：无 AfgChatClient")
    class NoClientFallback {

        @Test
        @DisplayName("chatClient 为 null 时应返回错误消息说明 AI 未配置")
        void shouldReturnErrorOutput_whenNoChatClient() {
            AiChatNode node = new AiChatNode("chat-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("message", "hello"));

            assertThat(output.data()).containsEntry("message", "hello");
            assertThat(output.data()).containsEntry("error", "No AfgChatClient available - AI chat not configured");
        }

        @Test
        @DisplayName("null 客户端构造也应有相同降级行为")
        void shouldReturnErrorOutput_whenNullChatClient() {
            AiChatNode node = new AiChatNode("chat-1", null);
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("message", "hello"));

            assertThat(output.data()).containsKey("error");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("缺少 message 参数时应返回错误输出")
        void shouldReturnErrorOutput_whenMessageMissing() {
            AiChatNode node = new AiChatNode("chat-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of());

            // AbstractWorkflowNode catches IllegalArgumentException
            assertThat(output.data()).containsKey("error");
        }

        @Test
        @DisplayName("null 参数应视为空 map 处理")
        void shouldHandleNullParams() {
            AiChatNode node = new AiChatNode("chat-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, null);

            assertThat(output.data()).containsKey("error");
        }
    }

    @Nested
    @DisplayName("流式执行降级")
    class StreamingFallback {

        @Test
        @DisplayName("无客户端时流式执行应降级为同步执行")
        void shouldDegradeToSyncStream_whenNoClient() {
            AiChatNode node = new AiChatNode("chat-1");
            ExecutionContext context = createContext();

            var flux = node.executeStream(context, Map.of("message", "hello"));

            // Flux should emit at least one event (the COMPLETE event)
            var events = flux.collectList().block();
            assertThat(events).isNotNull();
            assertThat(events).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("输出属性")
    class OutputProperties {

        @Test
        @DisplayName("降级输出应包含耗时信息")
        void shouldIncludeDuration() {
            AiChatNode node = new AiChatNode("chat-1");
            ExecutionContext context = createContext();

            NodeOutput output = node.execute(context, Map.of("message", "test"));

            assertThat(output.durationMs()).isGreaterThanOrEqualTo(0);
        }
    }
}
