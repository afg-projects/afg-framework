package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LlmRequest 单元测试
 */
class LlmRequestTest {

    @Test
    @DisplayName("创建请求（仅消息）")
    void of_messagesOnly() {
        List<Message> messages = List.of(Message.user("Hello"));
        LlmRequest request = LlmRequest.of(messages);

        assertThat(request.messages()).isEqualTo(messages);
        assertThat(request.systemPrompt()).isNull();
        assertThat(request.tools()).isEmpty();
        assertThat(request.options()).isEmpty();
    }

    @Test
    @DisplayName("创建请求（用户消息）")
    void ofUserMessage() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");

        assertThat(request.messages()).hasSize(1);
        assertThat(request.messages().get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("withSystemPrompt 创建新请求")
    void withSystemPrompt() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmRequest newRequest = request.withSystemPrompt("You are helpful");

        assertThat(newRequest.systemPrompt()).isEqualTo("You are helpful");
        assertThat(request.systemPrompt()).isNull();
    }

    @Test
    @DisplayName("withMessage 创建新请求")
    void withMessage() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmRequest newRequest = request.withMessage(Message.assistant("Hi!"));

        assertThat(newRequest.messages()).hasSize(2);
    }

    @Test
    @DisplayName("withTool 创建新请求")
    void withTool() {
        ToolDefinition tool = ToolDefinition.of("test_tool", "Test");
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmRequest newRequest = request.withTool(tool);

        assertThat(newRequest.tools()).hasSize(1);
        assertThat(newRequest.hasTools()).isTrue();
    }

    @Test
    @DisplayName("withOption 创建新请求")
    void withOption() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");
        LlmRequest newRequest = request.withOption("temperature", 0.7);

        assertThat(newRequest.getOption("temperature")).isEqualTo(0.7);
    }

    @Test
    @DisplayName("hasSystemPrompt 返回正确结果")
    void hasSystemPrompt() {
        LlmRequest withPrompt = LlmRequest.ofUserMessage("Hello").withSystemPrompt("System");
        LlmRequest withoutPrompt = LlmRequest.ofUserMessage("Hello");

        assertThat(withPrompt.hasSystemPrompt()).isTrue();
        assertThat(withoutPrompt.hasSystemPrompt()).isFalse();
    }

    @Test
    @DisplayName("getOption 带默认值")
    void getOption_withDefault() {
        LlmRequest request = LlmRequest.ofUserMessage("Hello");

        assertThat(request.getOption("nonexistent", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("Builder 创建请求")
    void builder() {
        LlmRequest request = LlmRequest.builder()
                .systemPrompt("System")
                .addMessage(Message.user("Hello"))
                .addTool(ToolDefinition.of("tool", "Test"))
                .option("temperature", 0.7)
                .build();

        assertThat(request.systemPrompt()).isEqualTo("System");
        assertThat(request.messages()).hasSize(1);
        assertThat(request.tools()).hasSize(1);
        assertThat(request.getOption("temperature")).isEqualTo(0.7);
    }
}