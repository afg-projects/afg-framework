package io.github.afgprojects.framework.ai.chat;

import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用本地 Ollama 测试 AfgChatClient 功能
 *
 * <p>设置 TEST_OLLAMA=true 启用测试，OLLAMA_BASE_URL 配置 Ollama 地址。
 */
@EnabledIfEnvironmentVariable(named = "TEST_OLLAMA", matches = "true")
class OllamaIntegrationTest {

    private static AfgChatClient afgChatClient;

    @BeforeAll
    static void setUp() {
        String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

        var ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
        var chatOptions = OllamaChatOptions.builder()
            .model("qwen2.5:3b")
            .temperature(0.7)
            .build();
        var chatModel = OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(chatOptions)
            .build();

        var chatClient = ChatClient.builder(chatModel).build();
        afgChatClient = new DefaultAfgChatClient(chatClient);
    }

    @org.junit.jupiter.api.Test
    void testSimpleChat() {
        AiChatResponse response = afgChatClient.chat("你好，请用一句话介绍你自己。");

        assertThat(response).isNotNull();
        assertThat(response.hasContent()).isTrue();
        assertThat(response.content()).isNotBlank();

        System.out.println("=== 同步对话 ===");
        System.out.println(response.content());

        if (response.metadata() != null) {
            System.out.println("模型: " + response.metadata().model());
            System.out.println("完成原因: " + response.metadata().finishReason());
        }
    }

    @org.junit.jupiter.api.Test
    void testChatWithAiMessage() {
        AiMessage message = AiMessage.user("请用一句话说明 Java 的优点。");
        AiChatResponse response = afgChatClient.chat(message);

        assertThat(response).isNotNull();
        assertThat(response.hasContent()).isTrue();

        System.out.println("=== AiMessage 对话 ===");
        System.out.println(response.content());
    }

    @org.junit.jupiter.api.Test
    void testChatWithMessageList() {
        List<AiMessage> messages = List.of(
            AiMessage.system("你是一个编程助手，回答要简洁。"),
            AiMessage.user("什么是 Spring AI？")
        );

        AiChatResponse response = afgChatClient.chat(messages);
        assertThat(response).isNotNull();
        assertThat(response.hasContent()).isTrue();

        System.out.println("=== 多轮对话 ===");
        System.out.println(response.content());
    }

    @org.junit.jupiter.api.Test
    void testChatStream() {
        Flux<String> stream = afgChatClient.chatStream("请用三句话描述春天的景色。");

        StringBuilder sb = new StringBuilder();
        StepVerifier.create(stream)
            .thenConsumeWhile(content -> {
                sb.append(content);
                return true;
            })
            .verifyComplete();

        String result = sb.toString();
        assertThat(result).isNotBlank();

        System.out.println("=== 流式对话 ===");
        System.out.println(result);
    }

    @org.junit.jupiter.api.Test
    void testWithSystemPrompt() {
        AfgChatClient clientWithPrompt = afgChatClient.withSystemPrompt(
            "你是一个翻译助手，只能将中文翻译成英文，不要解释。");

        AiChatResponse response = clientWithPrompt.chat("你好");
        assertThat(response).isNotNull();
        assertThat(response.hasContent()).isTrue();

        System.out.println("=== 系统提示词 ===");
        System.out.println(response.content());
    }

    @org.junit.jupiter.api.Test
    void testPromptBuilder() {
        AiChatResponse response = afgChatClient.prompt("1+1等于几？")
            .systemPrompt("你是一个数学老师，回答要简洁。")
            .call();

        assertThat(response).isNotNull();
        assertThat(response.hasContent()).isTrue();

        System.out.println("=== 构建器调用 ===");
        System.out.println(response.content());
    }
}