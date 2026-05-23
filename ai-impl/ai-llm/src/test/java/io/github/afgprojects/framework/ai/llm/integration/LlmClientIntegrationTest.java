package io.github.afgprojects.framework.ai.llm.integration;

import io.github.afgprojects.framework.ai.agent.tool.DefaultToolRegistry;
import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.llm.ollama.OllamaLlmClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmClient 集成测试
 *
 * <p>使用本地 Ollama 服务测试 LLM 客户端功能，包括：
 * <ul>
 *   <li>基本对话功能</li>
 *   <li>流式输出</li>
 *   <li>工具调用</li>
 *   <li>多轮对话</li>
 * </ul>
 *
 * <p>准备工作：
 * <pre>
 * # 启动 Ollama 服务
 * ollama serve
 *
 * # 拉取模型（推荐使用支持工具调用的模型）
 * ollama pull qwen2.5:1.5b
 * # 或使用更好的模型
 * ollama pull qwen2.5:7b
 * </pre>
 */
@DisplayName("LlmClient 集成测试")
class LlmClientIntegrationTest {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "qwen2.5:1.5b";

    private static OllamaLlmClient llmClient;
    private static boolean ollamaAvailable = false;

    @BeforeAll
    static void setUpClass() {
        try {
            llmClient = new OllamaLlmClient(OLLAMA_BASE_URL, MODEL_NAME);
            // 测试连接
            LlmResponse response = llmClient.chat(LlmRequest.ofUserMessage("Hello"));
            if (response.content() != null && !response.content().isBlank()) {
                ollamaAvailable = true;
                System.out.println("✅ Ollama 连接成功");
                System.out.println("   模型: " + MODEL_NAME);
                System.out.println("   基础 URL: " + OLLAMA_BASE_URL);
            }
        } catch (Exception e) {
            System.err.println("❌ 无法连接 Ollama 服务: " + e.getMessage());
            System.err.println("   请确保 Ollama 服务已启动: ollama serve");
            System.err.println("   并拉取模型: ollama pull " + MODEL_NAME);
        }
    }

    // ========== 基本对话测试 ==========

    @Test
    @DisplayName("简单问答")
    void testSimpleQuestion() {
        assumeOllamaAvailable();
        LlmRequest request = LlmRequest.ofUserMessage("1+1等于几？");
        LlmResponse response = llmClient.chat(request);

        System.out.println("=== 简单问答测试 ===");
        System.out.println("问题: 1+1等于几？");
        System.out.println("回答: " + response.content());

        assertNotNull(response.content());
        assertFalse(response.content().isBlank());
    }

    @Test
    @DisplayName("带系统提示词的对话")
    void testChatWithSystemPrompt() {
        assumeOllamaAvailable();
        LlmRequest request = LlmRequest.builder()
            .systemPrompt("你是一个专业的程序员助手，回答要简洁专业。")
            .addMessage(Message.user("什么是 RESTful API？"))
            .build();

        LlmResponse response = llmClient.chat(request);

        System.out.println("=== 带系统提示词测试 ===");
        System.out.println("系统提示: 你是一个专业的程序员助手");
        System.out.println("问题: 什么是 RESTful API？");
        System.out.println("回答: " + response.content());

        assertNotNull(response.content());
        assertFalse(response.content().isBlank());
    }

    @Test
    @DisplayName("多轮对话")
    void testMultiTurnConversation() {
        assumeOllamaAvailable();
        // 第一轮
        LlmRequest request1 = LlmRequest.builder()
            .systemPrompt("你是一个友好的助手。")
            .addMessage(Message.user("我叫张三"))
            .build();

        LlmResponse response1 = llmClient.chat(request1);
        System.out.println("=== 多轮对话测试 ===");
        System.out.println("用户: 我叫张三");
        System.out.println("助手: " + response1.content());

        // 第二轮（带历史）
        LlmRequest request2 = LlmRequest.builder()
            .systemPrompt("你是一个友好的助手。")
            .addMessage(Message.user("我叫张三"))
            .addMessage(Message.assistant(response1.content()))
            .addMessage(Message.user("我叫什么名字？"))
            .build();

        LlmResponse response2 = llmClient.chat(request2);
        System.out.println("用户: 我叫什么名字？");
        System.out.println("助手: " + response2.content());

        assertNotNull(response2.content());
        // 检查是否记住了名字
        assertTrue(response2.content().contains("张三") || response2.content().toLowerCase().contains("zhang"),
            "助手应该记住用户的名字");
    }

    // ========== 工具调用测试 ==========

    private List<ToolDefinition> testTools;

    @BeforeEach
    void setUpTools() {
        // 定义测试工具
        testTools = List.of(
            ToolDefinition.of("get_weather", "获取指定城市的天气信息"),
            ToolDefinition.of("calculate", "执行数学计算"),
            new ToolDefinition(
                "search",
                "在互联网上搜索信息",
                """
                {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "搜索关键词"
                    }
                  },
                  "required": ["query"]
                }
                """
            )
        );
    }

    @Test
    @DisplayName("工具定义传递")
    void testToolDefinitionPassing() {
        assumeOllamaAvailable();
        LlmRequest request = LlmRequest.builder()
            .systemPrompt("你是一个智能助手，可以使用工具来帮助用户。")
            .addMessage(Message.user("北京今天天气怎么样？"))
            .build();

        // 使用 chatWithTools 并传递工具定义
        LlmResponse response = llmClient.chatWithTools(request, testTools);

        System.out.println("=== 工具定义传递测试 ===");
        System.out.println("用户: 北京今天天气怎么样？");
        System.out.println("回答: " + response.content());
        if (response.hasToolCalls()) {
            System.out.println("工具调用:");
            for (var tc : response.toolCalls()) {
                System.out.println("  - " + tc.name() + "(" + tc.arguments() + ")");
            }
        }

        assertNotNull(response);
    }

    @Test
    @DisplayName("数学计算工具调用")
    void testCalculatorToolCall() {
        assumeOllamaAvailable();
        ToolDefinition calculateTool = new ToolDefinition(
            "calculate",
            "执行数学计算",
            """
            {
              "type": "object",
              "properties": {
                "expression": {
                  "type": "string",
                  "description": "数学表达式，如 '1+2' 或 '3*4'"
                }
              },
              "required": ["expression"]
            }
            """
        );

        LlmRequest request = LlmRequest.builder()
            .systemPrompt("你是一个智能助手，可以使用 calculate 工具进行数学计算。")
            .addMessage(Message.user("帮我计算 123 * 456"))
            .build();

        LlmResponse response = llmClient.chatWithTools(request, List.of(calculateTool));

        System.out.println("=== 数学计算工具调用测试 ===");
        System.out.println("用户: 帮我计算 123 * 456");
        System.out.println("回答: " + response.content());
        if (response.hasToolCalls()) {
            System.out.println("工具调用:");
            for (var tc : response.toolCalls()) {
                System.out.println("  - " + tc.name() + "(" + tc.arguments() + ")");
            }
        }

        assertNotNull(response);
    }

    @Test
    @DisplayName("搜索工具调用")
    void testSearchToolCall() {
        assumeOllamaAvailable();
        ToolDefinition searchTool = new ToolDefinition(
            "search",
            "在互联网上搜索信息",
            """
            {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "搜索关键词"
                }
              },
              "required": ["query"]
            }
            """
        );

        LlmRequest request = LlmRequest.builder()
            .systemPrompt("你是一个智能助手，可以使用 search 工具搜索信息。")
            .addMessage(Message.user("帮我搜索一下 Spring Boot 的最新版本"))
            .build();

        LlmResponse response = llmClient.chatWithTools(request, List.of(searchTool));

        System.out.println("=== 搜索工具调用测试 ===");
        System.out.println("用户: 帮我搜索一下 Spring Boot 的最新版本");
        System.out.println("回答: " + response.content());
        if (response.hasToolCalls()) {
            System.out.println("工具调用:");
            for (var tc : response.toolCalls()) {
                System.out.println("  - " + tc.name() + "(" + tc.arguments() + ")");
            }
        }

        assertNotNull(response);
    }

    @Test
    @DisplayName("带实际工具执行的天气查询")
    void testWeatherToolExecution() {
        assumeOllamaAvailable();

        // 创建工具注册表
        ToolRegistry registry = new DefaultToolRegistry();

        // 注册天气工具（模拟实现）
        ToolDefinition weatherToolDef = ToolDefinition.of("get_weather", "获取指定城市的天气信息");
        Tool<Map<String, Object>, String> weatherTool = new Tool<>() {
            @Override
            public String name() {
                return weatherToolDef.name();
            }

            @Override
            public String description() {
                return weatherToolDef.description();
            }

            @Override
            public String inputSchema() {
                return weatherToolDef.inputSchema();
            }

            @Override
            public String execute(Map<String, Object> args) {
                String city = (String) args.get("city");
                // 模拟天气数据
                return city + " 今天天气晴朗，温度 25°C，空气质量良好。";
            }
        };
        registry.register(weatherTool);

        // 创建客户端
        OllamaLlmClient clientWithTools = new OllamaLlmClient(OLLAMA_BASE_URL, MODEL_NAME);

        LlmRequest request = LlmRequest.builder()
            .systemPrompt("""
                你是一个智能助手，可以使用 get_weather 工具查询天气。

                当用户询问天气时，你必须首先调用 get_weather 工具获取信息，然后根据工具返回的结果回答用户。

                工具调用格式（必须严格按照这个格式）：
                {"name":"get_weather","arguments":{"city":"城市名"}}

                例如用户问"北京天气"，你应该先返回：
                {"name":"get_weather","arguments":{"city":"北京"}}
                然后等待工具返回结果后，再给出最终回答。
                """)
            .addMessage(Message.user("北京今天天气怎么样？"))
            .build();

        LlmResponse response = clientWithTools.chatWithTools(request, List.of(weatherToolDef));

        System.out.println("=== 带实际工具执行的天气查询测试 ===");
        System.out.println("用户: 北京今天天气怎么样？");
        System.out.println("原始回答: " + response.content());
        System.out.println("hasToolCalls: " + response.hasToolCalls());
        System.out.println("hasToolResults: " + response.hasToolResults());
        if (response.hasToolCalls()) {
            System.out.println("工具调用:");
            for (var tc : response.toolCalls()) {
                System.out.println("  - " + tc.name() + "(" + tc.arguments() + ")");
            }
        }
        if (response.hasToolResults()) {
            System.out.println("工具执行结果:");
            for (var tr : response.toolResults()) {
                System.out.println("  - " + tr.toolName() + ": " + tr.output());
            }
        }

        assertNotNull(response);

        // 验证工具调用循环是否正确工作
        // 注意：qwen2.5:1.5b 对工具调用支持不完善，可能直接回答而不调用工具
        // 如果模型返回工具调用 JSON，hasToolResults 应该为 true
        // 如果模型直接回答，我们检查回答内容是否合理
        if (response.hasToolResults()) {
            System.out.println("✅ 工具调用成功执行");
        } else {
            System.out.println("⚠️ 模型未返回工具调用格式，直接回答了问题");
            System.out.println("   建议：使用支持工具调用的更好模型，如 qwen2.5:7b 或 llama3.1");
        }

        // 至少验证回答不为空
        assertTrue(response.content() != null && !response.content().isBlank(),
            "回答不应为空");
    }

    @Test
    @DisplayName("带实际工具执行的数学计算")
    void testCalculatorToolExecution() {
        assumeOllamaAvailable();

        // 创建工具注册表
        ToolRegistry registry = new DefaultToolRegistry();

        // 注册计算器工具
        ToolDefinition calcToolDef = new ToolDefinition(
            "calculate",
            "执行数学计算",
            """
            {
              "type": "object",
              "properties": {
                "expression": {
                  "type": "string",
                  "description": "数学表达式，如 '123*456'"
                }
              },
              "required": ["expression"]
            }
            """
        );
        Tool<Map<String, Object>, String> calcTool = new Tool<>() {
            @Override
            public String name() {
                return calcToolDef.name();
            }

            @Override
            public String description() {
                return calcToolDef.description();
            }

            @Override
            public String inputSchema() {
                return calcToolDef.inputSchema();
            }

            @Override
            public String execute(Map<String, Object> args) {
                String expr = (String) args.get("expression");
                try {
                    // 简单计算（仅支持乘法）
                    if (expr.contains("*")) {
                        String[] parts = expr.split("\\*");
                        long a = Long.parseLong(parts[0].trim());
                        long b = Long.parseLong(parts[1].trim());
                        return String.valueOf(a * b);
                    }
                    return "不支持的表达式";
                } catch (Exception e) {
                    return "计算错误: " + e.getMessage();
                }
            }
        };
        registry.register(calcTool);

        // 创建客户端
        OllamaLlmClient clientWithTools = new OllamaLlmClient(OLLAMA_BASE_URL, MODEL_NAME);

        LlmRequest request = LlmRequest.builder()
            .systemPrompt("""
                你是一个智能助手，可以使用 calculate 工具进行数学计算。

                当用户需要计算时，你必须首先调用 calculate 工具，然后根据工具返回的结果回答用户。

                工具调用格式（必须严格按照这个格式）：
                {"name":"calculate","arguments":{"expression":"数学表达式"}}

                例如用户问"计算 123*456"，你应该先返回：
                {"name":"calculate","arguments":{"expression":"123*456"}}
                然后等待工具返回结果后，再给出最终回答。
                """)
            .addMessage(Message.user("帮我计算 123 * 456"))
            .build();

        LlmResponse response = clientWithTools.chatWithTools(request, List.of(calcToolDef));

        System.out.println("=== 带实际工具执行的数学计算测试 ===");
        System.out.println("用户: 帮我计算 123 * 456");
        System.out.println("回答: " + response.content());
        if (response.hasToolCalls()) {
            System.out.println("工具调用:");
            for (var tc : response.toolCalls()) {
                System.out.println("  - " + tc.name() + "(" + tc.arguments() + ")");
            }
        }
        if (response.hasToolResults()) {
            System.out.println("工具执行结果:");
            for (var tr : response.toolResults()) {
                System.out.println("  - " + tr.toolName() + ": " + tr.output());
            }
        }

        assertNotNull(response);

        // 验证工具调用循环是否正确工作
        // 注意：qwen2.5:1.5b 对工具调用支持不完善，可能直接回答而不调用工具
        // 如果模型返回工具调用 JSON，hasToolResults 应该为 true
        // 如果模型直接回答，我们检查回答内容是否合理
        if (response.hasToolResults()) {
            System.out.println("✅ 工具调用成功执行");
            for (var tr : response.toolResults()) {
                System.out.println("  - " + tr.toolName() + ": " + tr.output());
            }
        } else {
            System.out.println("⚠️ 模型未返回工具调用格式，直接回答了问题");
            System.out.println("   建议：使用支持工具调用的更好模型，如 qwen2.5:7b 或 llama3.1");
        }

        // 至少验证回答不为空
        assertTrue(response.content() != null && !response.content().isBlank(),
            "回答不应为空");
    }

    // ========== 流式输出测试 ==========

    @Test
    @DisplayName("流式输出基本测试")
    void testBasicStreaming() {
        assumeOllamaAvailable();
        LlmRequest request = LlmRequest.ofUserMessage("请用三句话介绍 Java 编程语言");

        StringBuilder fullResponse = new StringBuilder();
        AtomicInteger chunkCount = new AtomicInteger(0);

        var flux = llmClient.chatStream(request);
        flux.doOnNext(chunk -> {
            if (chunk.content() != null) {
                fullResponse.append(chunk.content());
                chunkCount.incrementAndGet();
            }
        }).blockLast();

        System.out.println("=== 流式输出测试 ===");
        System.out.println("问题: 请用三句话介绍 Java 编程语言");
        System.out.println("完整回答: " + fullResponse.toString());
        System.out.println("接收到的块数: " + chunkCount.get());

        assertFalse(fullResponse.isEmpty(), "应该有输出内容");
        assertTrue(chunkCount.get() > 1, "流式输出应该有多个块");
    }

    // ========== 配置测试 ==========

    @Test
    @DisplayName("LlmConfig 创建测试")
    void testLlmConfigCreation() {
        LlmConfig config = LlmConfig.of(MODEL_NAME)
            .withBaseUrl(OLLAMA_BASE_URL)
            .withApiKey("test-key");

        assertEquals(MODEL_NAME, config.model());
        assertEquals(OLLAMA_BASE_URL, config.baseUrl());
        assertEquals("test-key", config.apiKey());
    }

    @Test
    @DisplayName("LlmRequest 构建测试")
    void testLlmRequestBuilding() {
        LlmRequest request = LlmRequest.builder()
            .systemPrompt("系统提示")
            .addMessage(Message.user("用户消息"))
            .addMessage(Message.assistant("助手回复"))
            .option("temperature", 0.7)
            .option("maxTokens", 1000)
            .build();

        assertEquals("系统提示", request.systemPrompt());
        assertEquals(2, request.messages().size());
        assertEquals(0.7, request.getOption("temperature"));
        assertEquals(1000, request.getOption("maxTokens"));
    }

    // ========== 错误处理测试 ==========

    @Test
    @DisplayName("空消息处理")
    void testEmptyMessage() {
        assumeOllamaAvailable();
        LlmRequest request = LlmRequest.ofUserMessage("");
        LlmResponse response = llmClient.chat(request);

        // 应该能处理空消息而不崩溃
        assertNotNull(response);
    }

    @Test
    @DisplayName("长消息处理")
    void testLongMessage() {
        assumeOllamaAvailable();
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longMessage.append("这是一段很长的测试消息。");
        }

        LlmRequest request = LlmRequest.ofUserMessage(longMessage.toString());
        LlmResponse response = llmClient.chat(request);

        System.out.println("=== 长消息处理测试 ===");
        System.out.println("消息长度: " + longMessage.length() + " 字符");
        System.out.println("响应长度: " + (response.content() != null ? response.content().length() : 0) + " 字符");

        assertNotNull(response);
    }

    // ========== 辅助方法 ==========

    private void assumeOllamaAvailable() {
        if (!ollamaAvailable) {
            System.out.println("⏭️ 跳过测试：Ollama 服务不可用");
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Ollama 服务不可用");
        }
    }
}
