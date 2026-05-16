package io.github.afgprojects.framework.ai.llm.ollama;

import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.memory.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OllamaLlmClient 集成测试
 *
 * <p>使用本地 Ollama 服务进行实际 API 调用测试。
 * 需要先启动 Ollama 服务并下载模型：
 * <pre>
 * ollama serve
 * ollama pull qwen2.5:1.5b
 * </pre>
 *
 * <p>测试默认跳过，需要设置环境变量 OLLAMA_AVAILABLE=true 启用。
 */
class OllamaLlmClientIntegrationTest {

    private static final String MODEL = "qwen2.5:1.5b";
    private static final String BASE_URL = "http://localhost:11434";

    @Test
    @DisplayName("简单对话测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void simpleChat() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        var request = LlmRequest.ofUserMessage("你好，请用一句话介绍你自己");
        var response = client.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
        assertThat(response.hasContent()).isTrue();

        System.out.println("Response: " + response.content());
    }

    @Test
    @DisplayName("多轮对话测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void multiTurnChat() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        // 第一轮
        var request1 = LlmRequest.ofUserMessage("我叫张三");
        var response1 = client.chat(request1);

        assertThat(response1.content()).isNotBlank();
        System.out.println("Turn 1: " + response1.content());

        // 第二轮（带历史）
        var request2 = LlmRequest.builder()
                .addMessage(Message.user("我叫张三"))
                .addMessage(Message.assistant(response1.content()))
                .addMessage(Message.user("你还记得我的名字吗？"))
                .build();

        var response2 = client.chat(request2);

        assertThat(response2.content()).isNotBlank();
        assertThat(response2.content()).containsIgnoringCase("张三");
        System.out.println("Turn 2: " + response2.content());
    }

    @Test
    @DisplayName("系统提示测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void systemPrompt() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        var request = LlmRequest.builder()
                .systemPrompt("你是一个专业的 Java 开发助手，回答问题时使用代码示例。")
                .addMessage(Message.user("如何创建一个不可变列表？"))
                .build();

        var response = client.chat(request);

        assertThat(response.content()).isNotBlank();
        assertThat(response.content()).containsIgnoringCase("List");
        System.out.println("Response: " + response.content());
    }

    @Test
    @DisplayName("流式响应测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void streamingChat() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        var request = LlmRequest.ofUserMessage("请写一首关于春天的短诗");

        StringBuilder fullResponse = new StringBuilder();
        client.chatStream(request)
                .doOnNext(resp -> {
                    if (resp.content() != null) {
                        fullResponse.append(resp.content());
                        System.out.print(resp.content());
                    }
                })
                .blockLast();

        assertThat(fullResponse.toString()).isNotBlank();
        System.out.println("\n\nFull response: " + fullResponse);
    }

    @Test
    @DisplayName("Token 使用量统计")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void tokenUsage() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        var request = LlmRequest.ofUserMessage("你好");
        var response = client.chat(request);

        assertThat(response.content()).isNotBlank();

        if (response.hasTokenUsage()) {
            var usage = response.tokenUsage();
            System.out.println("Prompt tokens: " + usage.promptTokens());
            System.out.println("Completion tokens: " + usage.completionTokens());
            System.out.println("Total tokens: " + usage.totalTokens());

            assertThat(usage.promptTokens()).isGreaterThan(0);
            assertThat(usage.totalTokens()).isGreaterThan(0);
        } else {
            System.out.println("Token usage not available for this model");
        }
    }

    @Test
    @DisplayName("配置测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void configTest() {
        var config = LlmConfig.of(MODEL)
                .withBaseUrl(BASE_URL)
                .withTimeout(java.time.Duration.ofSeconds(30));

        var client = new OllamaLlmClient(config);

        assertThat(client.getConfig().model()).isEqualTo(MODEL);
        assertThat(client.getConfig().baseUrl()).isEqualTo(BASE_URL);
    }

    @Test
    @DisplayName("长文本生成测试")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_AVAILABLE", matches = "true")
    void longTextGeneration() {
        var client = new OllamaLlmClient(BASE_URL, MODEL);

        var request = LlmRequest.builder()
                .systemPrompt("你是一个技术文档撰写助手。")
                .addMessage(Message.user("请简要介绍 Spring Boot 的核心特性，不超过 200 字。"))
                .build();

        var response = client.chat(request);

        assertThat(response.content()).isNotBlank();
        // 模型可能生成超过预期长度，只检查有内容即可
        System.out.println("Response length: " + response.content().length());
        System.out.println("Response: " + response.content());
    }
}