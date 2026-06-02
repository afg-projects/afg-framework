package io.github.afgprojects.framework.ai.agent;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.planning.ReActResult;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.agent.executor.DefaultReActExecutor;
import io.github.afgprojects.framework.ai.agent.executor.ToolExecutor;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI Agent 集成测试
 *
 * <p>使用 Mock AfgChatClient 测试 Agent 功能。
 */
class AgentIntegrationTest {

    private AfgChatClient chatClient;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        chatClient = mock(AfgChatClient.class);
        toolRegistry = new DefaultToolRegistry();
    }

    @Test
    @DisplayName("Tool 执行器集成测试")
    void toolExecutorIntegration() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> calculator = createCalculatorTool();
        Tool<Map<String, Object>, Object> greeter = createGreeterTool();
        toolRegistry.register(calculator);
        toolRegistry.register(greeter);

        // Mock AfgChatClient 响应
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(anyString())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(AiChatResponse.of("我已经完成了计算和问候。"));

        // 创建 Tool 执行器
        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);

        // 发送请求
        AiChatResponse response = executor.executeWithTools("请计算 10 + 20 并向用户打招呼");

        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        System.out.println("Response: " + response.content());
    }

    @Test
    @DisplayName("ReAct 推理集成测试")
    void reActIntegration() {
        // 注册工具
        Tool<Map<String, Object>, Object> calculator = createCalculatorTool();
        toolRegistry.register(calculator);

        // Mock LLM 响应 - 模拟 ReAct 推理过程
        String thought1 = "Thought: I need to calculate 15 + 27\nAction: calculator\nAction Input: {\"a\": 15, \"b\": 27, \"operation\": \"add\"}";
        String thought2 = "Thought: I have the result\nFinal Answer: 15 + 27 = 42.0";

        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(anyString())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of(thought1))
                .thenReturn(AiChatResponse.of(thought2));

        // 创建 ReAct 执行器
        DefaultReActExecutor executor = new DefaultReActExecutor(chatClient, toolRegistry, 5);

        // 执行任务
        ReActResult result = executor.execute("请计算 15 + 27 等于多少？");

        assertThat(result).isNotNull();
        assertThat(result.hasSteps()).isTrue();

        System.out.println("Success: " + result.success());
        System.out.println("Answer: " + result.answer());
        System.out.println("Steps: " + result.stepCount());
    }

    @Test
    @DisplayName("多工具协作测试")
    void multiToolCollaboration() {
        // 注册多个工具
        toolRegistry.register(createCalculatorTool());
        toolRegistry.register(createGreeterTool());
        toolRegistry.register(createTimeTool());

        // Mock AfgChatClient 响应
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(anyString())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList())).thenReturn(AiChatResponse.of("我已经完成了所有任务。"));

        // 创建执行器
        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);

        // 发送请求
        AiChatResponse response = executor.executeWithTools("请告诉我时间，计算 5 * 8，然后向张三打招呼");

        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        System.out.println("Response: " + response.content());
    }

    @Test
    @DisplayName("工具注册表测试")
    void toolRegistryTest() {
        Tool<Map<String, Object>, Object> tool1 = createCalculatorTool();
        Tool<Map<String, Object>, Object> tool2 = createGreeterTool();

        toolRegistry.register(tool1);
        toolRegistry.register(tool2);

        assertThat(toolRegistry.size()).isEqualTo(2);
        assertThat(toolRegistry.exists("calculator")).isTrue();
        assertThat(toolRegistry.exists("greeter")).isTrue();

        // 获取所有工具
        Collection<Tool<?, ?>> allTools = toolRegistry.getAllTools();
        assertThat(allTools).hasSize(2);

        System.out.println("Registered tools:");
        for (Tool<?, ?> tool : allTools) {
            System.out.println("  - " + tool.name() + ": " + tool.description());
        }
    }

    @Test
    @DisplayName("工具执行测试")
    void toolExecution() {
        toolRegistry.register(createCalculatorTool());

        // 获取工具并执行
        var toolOpt = toolRegistry.getTool("calculator");
        assertThat(toolOpt).isPresent();

        @SuppressWarnings("unchecked")
        Tool<Map<String, Object>, Object> tool = (Tool<Map<String, Object>, Object>) toolOpt.get();

        Map<String, Object> input = Map.of("a", 10, "b", 5, "operation", "multiply");
        Object result = tool.execute(input);

        assertThat(result).isEqualTo(50.0);
        System.out.println("10 * 5 = " + result);
    }

    @Test
    @DisplayName("工具链测试")
    void toolChain() {
        toolRegistry.register(createCalculatorTool());
        toolRegistry.register(createGreeterTool());

        // 模拟多步骤工具调用
        AfgChatClient clientWithPrompt = mock(AfgChatClient.class);
        when(chatClient.withSystemPrompt(anyString())).thenReturn(clientWithPrompt);
        when(clientWithPrompt.chat(anyList()))
                .thenReturn(AiChatResponse.of("第一步完成"))
                .thenReturn(AiChatResponse.of("第二步完成"))
                .thenReturn(AiChatResponse.of("所有步骤完成"));

        ToolExecutor executor = new ToolExecutor(toolRegistry, chatClient, 5, 30000L);

        AiChatResponse response = executor.executeWithTools("执行多个任务");

        assertThat(response).isNotNull();
        System.out.println("Final response: " + response.content());
    }

    // ==================== 测试工具 ====================

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, Object> createCalculatorTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "calculator";
            }

            @Override
            public @NonNull String description() {
                return "执行简单的数学计算。输入两个数字和操作符（add/subtract/multiply/divide）。";
            }

            @Override
            public @NonNull String inputSchema() {
                return """
                    {
                        "type": "object",
                        "properties": {
                            "a": {"type": "number"},
                            "b": {"type": "number"},
                            "operation": {"type": "string"}
                        }
                    }
                    """;
            }

            @Override
            public Object execute(Map<String, Object> input) {
                double a = ((Number) input.get("a")).doubleValue();
                double b = ((Number) input.get("b")).doubleValue();
                String op = (String) input.get("operation");

                return switch (op.toLowerCase()) {
                    case "add" -> a + b;
                    case "subtract" -> a - b;
                    case "multiply" -> a * b;
                    case "divide" -> b != 0 ? a / b : "Error: Division by zero";
                    default -> "Error: Unknown operation";
                };
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, Object> createGreeterTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "greeter";
            }

            @Override
            public @NonNull String description() {
                return "向用户打招呼。输入用户名字。";
            }

            @Override
            public @NonNull String inputSchema() {
                return "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
            }

            @Override
            public Object execute(Map<String, Object> input) {
                String name = (String) input.get("name");
                return "你好，" + name + "！很高兴见到你！";
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, Object> createTimeTool() {
        return new Tool<>() {
            @Override
            public @NonNull String name() {
                return "current_time";
            }

            @Override
            public @NonNull String description() {
                return "获取当前时间。";
            }

            @Override
            public @NonNull String inputSchema() {
                return "{\"type\": \"object\"}";
            }

            @Override
            public Object execute(Map<String, Object> input) {
                return java.time.LocalDateTime.now().toString();
            }
        };
    }
}