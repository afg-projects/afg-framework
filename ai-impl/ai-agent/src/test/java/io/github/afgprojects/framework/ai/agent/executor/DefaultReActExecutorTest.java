package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.planning.ReActResult;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultReActExecutor} 单元测试
 */
@DisplayName("DefaultReActExecutor")
class DefaultReActExecutorTest {

    private AfgChatClient chatClient;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        chatClient = mock(AfgChatClient.class);
        toolRegistry = new DefaultToolRegistry();
    }

    @Test
    @DisplayName("执行简单任务 - 直接返回 Final Answer")
    void shouldReturnFinalAnswerDirectly() {
        toolRegistry.register(createTestTool("test_tool", "A test tool"));

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(
                AiChatResponse.of("Thought: Simple question\nFinal Answer: 42")
        );

        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 5);
        ReActResult result = executor.execute("What is 6 * 7?");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("42");
    }

    @Test
    @DisplayName("执行工具调用任务 - 思考后调用工具")
    void shouldCallToolThenReturnAnswer() {
        toolRegistry.register(createTestTool("calculator", "Calculates math"));

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);

        // 第一次调用：返回工具调用
        // 第二次调用：返回最终答案
        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of(
                        "Thought: I need to calculate\nAction: calculator\nAction Input: {\"a\": 1, \"b\": 2}"))
                .thenReturn(AiChatResponse.of(
                        "Thought: Got the result\nFinal Answer: 3"));

        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 5);
        ReActResult result = executor.execute("Calculate 1 + 2");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("3");
    }

    @Test
    @DisplayName("达到最大步数限制 - 返回失败")
    void shouldReturnFailureWhenMaxStepsReached() {
        toolRegistry.register(createTestTool("tool1", "Tool 1"));

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);

        // 永远不返回 Final Answer
        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of("Thought: Still thinking\nAction: tool1\nAction Input: {}"));

        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 2);
        ReActResult result = executor.execute("Impossible task");

        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("工具不存在 - 返回错误观察结果")
    void shouldReturnErrorWhenToolNotFound() {
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);

        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of(
                        "Thought: Need to use missing tool\nAction: missing_tool\nAction Input: {}"))
                .thenReturn(AiChatResponse.of(
                        "Thought: Tool was not found\nFinal Answer: I cannot find the required tool"));

        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 5);
        ReActResult result = executor.execute("Use missing tool");

        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("空内容响应 - 继续迭代")
    void shouldContinueOnEmptyResponse() {
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);

        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of(""))
                .thenReturn(AiChatResponse.of("Thought: Retry\nFinal Answer: Success"));

        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 5);
        ReActResult result = executor.execute("Retry test");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).isEqualTo("Success");
    }

    // ── 辅助方法 ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, String> createTestTool(String name, String description) {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return name;
            }

            @Override
            public @NonNull String description() {
                return description;
            }

            @Override
            public @NonNull String inputSchema() {
                return "{}";
            }

            @Override
            public @NonNull String execute(Map<String, Object> input) {
                return "tool-result";
            }
        };
    }
}