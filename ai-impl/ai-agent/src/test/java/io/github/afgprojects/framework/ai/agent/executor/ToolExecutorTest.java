package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ToolExecutor 单元测试
 */
class ToolExecutorTest {

    private ToolRegistry toolRegistry;
    private LlmClient llmClient;
    private ToolExecutor executor;

    @BeforeEach
    void setUp() {
        toolRegistry = new DefaultToolRegistry();
        llmClient = mock(LlmClient.class);
        executor = new ToolExecutor(toolRegistry, llmClient, 5, 30000L);
    }

    @Test
    @DisplayName("创建执行器")
    void create_executor() {
        ToolExecutor newExecutor = new ToolExecutor(toolRegistry, llmClient, 10, 60000L);

        assertThat(newExecutor).isNotNull();
    }

    @Test
    @DisplayName("执行无工具调用的请求")
    void executeWithTools_noToolCalls_returnsResponse() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmResponse expectedResponse = LlmResponse.of("Hello! How can I help you?");

        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expectedResponse);

        LlmResponse response = executor.executeWithTools(request);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(response.hasToolCalls()).isFalse();
    }

    @Test
    @DisplayName("执行带工具调用的请求")
    void executeWithTools_withToolCalls_executesTool() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("test_tool", "Test tool");
        toolRegistry.register(testTool);

        // 模拟第一次响应（有工具调用）
        ToolCall toolCall = new ToolCall("call-1", "test_tool", Map.of("input", "test"));
        LlmResponse toolCallResponse = new LlmResponse(
                null,
                List.of(toolCall),
                null,
                LlmResponse.FinishReason.TOOL_CALL
        );

        // 模拟第二次响应（最终答案）
        LlmResponse finalResponse = LlmResponse.of("Tool executed successfully");

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        LlmRequest request = LlmRequest.ofUserMessage("Execute the tool");
        LlmResponse response = executor.executeWithTools(request);

        assertThat(response).isEqualTo(finalResponse);
    }

    @Test
    @DisplayName("工具不存在时返回错误结果")
    void executeWithTools_toolNotFound_returnsError() {
        // 不注册任何工具

        // 模拟响应（调用不存在的工具）
        ToolCall toolCall = new ToolCall("call-1", "nonexistent_tool", Map.of());
        LlmResponse toolCallResponse = new LlmResponse(
                null,
                List.of(toolCall),
                null,
                LlmResponse.FinishReason.TOOL_CALL
        );

        LlmResponse finalResponse = LlmResponse.of("Tool not found");

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        LlmRequest request = LlmRequest.ofUserMessage("Execute");
        LlmResponse response = executor.executeWithTools(request);

        assertThat(response).isEqualTo(finalResponse);
    }

    @Test
    @DisplayName("达到最大迭代次数")
    void executeWithTools_maxIterations_returnsLastResponse() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("test_tool", "Test tool");
        toolRegistry.register(testTool);

        // 模拟持续返回工具调用（不会终止）
        ToolCall toolCall = new ToolCall("call-1", "test_tool", Map.of("input", "test"));
        LlmResponse toolCallResponse = new LlmResponse(
                null,
                List.of(toolCall),
                null,
                LlmResponse.FinishReason.TOOL_CALL
        );

        LlmResponse finalResponse = LlmResponse.of("Final answer");

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(toolCallResponse)
                .thenReturn(toolCallResponse)
                .thenReturn(toolCallResponse)
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        LlmRequest request = LlmRequest.ofUserMessage("Execute");
        LlmResponse response = executor.executeWithTools(request);

        // 达到最大迭代次数后返回
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("异步执行工具调用")
    void executeWithToolsAsync_returnsCompletableFuture() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmResponse expectedResponse = LlmResponse.of("Hello!");

        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expectedResponse);

        var future = executor.executeWithToolsAsync(request);

        assertThat(future).isNotNull();
        LlmResponse response = future.join();
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("多工具调用并行执行")
    void executeWithTools_multipleToolCalls_executesAll() {
        // 注册两个工具
        Tool<Map<String, Object>, Object> tool1 = createTestTool("tool1", "Tool 1");
        Tool<Map<String, Object>, Object> tool2 = createTestTool("tool2", "Tool 2");
        toolRegistry.register(tool1);
        toolRegistry.register(tool2);

        // 模拟响应（同时调用两个工具）
        ToolCall call1 = new ToolCall("call-1", "tool1", Map.of("input", "a"));
        ToolCall call2 = new ToolCall("call-2", "tool2", Map.of("input", "b"));
        LlmResponse toolCallResponse = new LlmResponse(
                null,
                List.of(call1, call2),
                null,
                LlmResponse.FinishReason.TOOL_CALL
        );

        LlmResponse finalResponse = LlmResponse.of("Both tools executed");

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(toolCallResponse)
                .thenReturn(finalResponse);

        LlmRequest request = LlmRequest.ofUserMessage("Execute both");
        LlmResponse response = executor.executeWithTools(request);

        assertThat(response).isEqualTo(finalResponse);
    }

    @SuppressWarnings("unchecked")
    private Tool<Map<String, Object>, Object> createTestTool(String name, String description) {
        return new Tool<>() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public Object execute(Map<String, Object> input) {
                return "Result from " + name + ": " + input.get("input");
            }
        };
    }
}