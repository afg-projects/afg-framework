package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.exception.ToolException;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ToolExecutor} 单元测试
 */
@DisplayName("ToolExecutor")
class ToolExecutorTest {

    private ToolRegistry toolRegistry;
    private AfgChatClient chatClient;

    @BeforeEach
    void setUp() {
        toolRegistry = new DefaultToolRegistry();
        chatClient = mock(AfgChatClient.class);
    }

    @Test
    @DisplayName("executeWithTools - 应正确执行带工具调用的对话")
    void shouldExecuteWithTools() {
        // 注册测试工具
        toolRegistry.register(createCalculatorTool());

        // Mock AfgChatClient
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(AiChatResponse.of("10 + 20 = 30"));

        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);
        AiChatResponse response = executor.executeWithTools("Calculate 10 + 20");

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("10 + 20 = 30");
    }

    @Test
    @DisplayName("executeWithTools - 带系统提示")
    void shouldExecuteWithSystemPrompt() {
        toolRegistry.register(createCalculatorTool());

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(AiChatResponse.of("Result: 42"));

        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);

        List<AiMessage> messages = List.of(AiMessage.user("What is 6*7?"));
        AiChatResponse response = executor.executeWithTools("You are a math assistant", messages);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Result: 42");
    }

    @Test
    @DisplayName("executeTool - 应正确执行单个工具")
    void shouldExecuteSingleTool() {
        toolRegistry.register(createCalculatorTool());

        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);
        Object result = executor.executeTool("calculator", Map.of("a", 3, "b", 4, "operation", "add"));

        assertThat(result).isEqualTo(7.0);
    }

    @Test
    @DisplayName("executeTool - 工具不存在应抛异常")
    void shouldThrowWhenToolNotFound() {
        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);

        assertThatThrownBy(() -> executor.executeTool("nonexistent", Map.of()))
                .isInstanceOf(ToolException.class);
    }

    @Test
    @DisplayName("executeWithToolsAsync - 应异步执行")
    void shouldExecuteAsync() throws Exception {
        toolRegistry.register(createCalculatorTool());

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(any())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(AiChatResponse.of("Async result"));

        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);
        AiChatResponse response = executor.executeWithToolsAsync("Test async").get();

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Async result");
    }

    // ── 辅助方法 ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, Object> createCalculatorTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "calculator";
            }

            @Override
            public @NonNull String description() {
                return "Performs calculations";
            }

            @Override
            public @NonNull String inputSchema() {
                return "{}";
            }

            @Override
            public Object execute(Map<String, Object> input) {
                double a = ((Number) input.get("a")).doubleValue();
                double b = ((Number) input.get("b")).doubleValue();
                String op = (String) input.get("operation");
                return switch (op) {
                    case "add" -> a + b;
                    case "subtract" -> a - b;
                    case "multiply" -> a * b;
                    default -> 0;
                };
            }
        };
    }
}