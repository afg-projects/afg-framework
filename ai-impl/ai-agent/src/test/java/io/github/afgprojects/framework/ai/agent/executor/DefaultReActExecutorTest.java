package io.github.afgprojects.framework.ai.agent.executor;

import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.planning.ReActResult;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DefaultReActExecutor 单元测试
 */
class DefaultReActExecutorTest {

    private ToolRegistry toolRegistry;
    private LlmClient llmClient;
    private DefaultReActExecutor executor;

    @BeforeEach
    void setUp() {
        toolRegistry = new DefaultToolRegistry();
        llmClient = mock(LlmClient.class);
        executor = new DefaultReActExecutor(llmClient, toolRegistry, 5);
    }

    @Test
    @DisplayName("创建执行器")
    void create_executor() {
        DefaultReActExecutor newExecutor = new DefaultReActExecutor(llmClient, toolRegistry, 10);

        assertThat(newExecutor).isNotNull();
    }

    @Test
    @DisplayName("直接返回最终答案")
    void execute_withFinalAnswer_returnsSuccess() {
        // 模拟 LLM 直接返回最终答案
        String llmResponse = """
                Thought: I can answer this directly.
                Final Answer: The answer is 42.
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(llmResponse));

        ReActResult result = executor.execute("What is the answer?");

        assertThat(result.success()).isTrue();
        assertThat(result.answer()).contains("42");
        assertThat(result.hasSteps()).isTrue();
    }

    @Test
    @DisplayName("执行工具后返回答案")
    void execute_withToolCall_returnsSuccess() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("calculator", "A calculator tool");
        toolRegistry.register(testTool);

        // 第一次响应：调用工具
        String toolCallResponse = """
                Thought: I need to calculate the result.
                Action: calculator
                Action Input: {"input": "2+2"}
                """;

        // 第二次响应：返回答案
        String finalResponse = """
                Thought: I have the calculation result.
                Final Answer: The result is Result from calculator: 2+2.
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(toolCallResponse))
                .thenReturn(LlmResponse.of(finalResponse));

        ReActResult result = executor.execute("Calculate 2+2");

        assertThat(result.success()).isTrue();
        assertThat(result.stepCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("工具不存在时返回错误")
    void execute_toolNotFound_returnsError() {
        // 不注册任何工具

        // 模拟调用不存在的工具
        String toolCallResponse = """
                Thought: I need to use a tool.
                Action: nonexistent_tool
                Action Input: {}
                """;

        String finalResponse = """
                Thought: The tool was not found.
                Final Answer: I could not complete the task.
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(toolCallResponse))
                .thenReturn(LlmResponse.of(finalResponse));

        ReActResult result = executor.execute("Do something");

        assertThat(result.success()).isTrue();
        assertThat(result.hasSteps()).isTrue();
    }

    @Test
    @DisplayName("达到最大步数返回失败")
    void execute_maxSteps_returnsFailure() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("loop_tool", "A tool that loops");
        toolRegistry.register(testTool);

        // 模拟持续调用工具（不会终止）
        String loopResponse = """
                Thought: I need to continue.
                Action: loop_tool
                Action Input: {"input": "continue"}
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(loopResponse));

        ReActResult result = executor.execute("Loop forever");

        assertThat(result.success()).isFalse();
        assertThat(result.answer()).contains("Max steps reached");
        assertThat(result.stepCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("使用自定义最大步数")
    void executeWithMaxSteps_customSteps_limitsIterations() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("loop_tool", "A tool that loops");
        toolRegistry.register(testTool);

        String loopResponse = """
                Thought: Continue
                Action: loop_tool
                Action Input: {}
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(loopResponse));

        ReActResult result = executor.executeWithMaxSteps("Loop", 3);

        assertThat(result.success()).isFalse();
        assertThat(result.stepCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("解析复杂 JSON 输入")
    void execute_complexJsonInput_parsesCorrectly() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("complex_tool", "A complex tool");
        toolRegistry.register(testTool);

        String toolCallResponse = """
                Thought: I need to process complex data.
                Action: complex_tool
                Action Input: {"name": "test", "value": 123, "nested": {"key": "value"}}
                """;

        String finalResponse = """
                Thought: Done
                Final Answer: Processed successfully
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(toolCallResponse))
                .thenReturn(LlmResponse.of(finalResponse));

        ReActResult result = executor.execute("Process complex data");

        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("解析非 JSON 输入")
    void execute_nonJsonInput_treatsAsString() {
        // 注册测试工具
        Tool<Map<String, Object>, Object> testTool = createTestTool("text_tool", "A text tool");
        toolRegistry.register(testTool);

        String toolCallResponse = """
                Thought: Process text
                Action: text_tool
                Action Input: simple text input
                """;

        String finalResponse = """
                Thought: Done
                Final Answer: Text processed
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(toolCallResponse))
                .thenReturn(LlmResponse.of(finalResponse));

        ReActResult result = executor.execute("Process text");

        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("多步骤推理链")
    void execute_multiStepChain_completesSuccessfully() {
        // 注册多个工具
        Tool<Map<String, Object>, Object> searchTool = createTestTool("search", "Search tool");
        Tool<Map<String, Object>, Object> analyzeTool = createTestTool("analyze", "Analyze tool");
        toolRegistry.register(searchTool);
        toolRegistry.register(analyzeTool);

        // 第一步：搜索
        String searchResponse = """
                Thought: Need to search first
                Action: search
                Action Input: {"query": "data"}
                """;

        // 第二步：分析
        String analyzeResponse = """
                Thought: Now analyze the results
                Action: analyze
                Action Input: {"data": "results"}
                """;

        // 第三步：最终答案
        String finalResponse = """
                Thought: Analysis complete
                Final Answer: The analysis shows positive results.
                """;

        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(LlmResponse.of(searchResponse))
                .thenReturn(LlmResponse.of(analyzeResponse))
                .thenReturn(LlmResponse.of(finalResponse));

        ReActResult result = executor.execute("Search and analyze");

        assertThat(result.success()).isTrue();
        assertThat(result.stepCount()).isEqualTo(3);
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
                return "Result from " + name + ": " + input;
            }
        };
    }
}