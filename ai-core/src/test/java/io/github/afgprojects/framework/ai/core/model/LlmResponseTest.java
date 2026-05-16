package io.github.afgprojects.framework.ai.core.model;

import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LlmResponse 单元测试
 */
class LlmResponseTest {

    @Test
    @DisplayName("创建响应（仅内容）")
    void of_contentOnly() {
        LlmResponse response = LlmResponse.of("Hello!");

        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.toolCalls()).isEmpty();
        assertThat(response.tokenUsage()).isNull();
        assertThat(response.finishReason()).isNull();
    }

    @Test
    @DisplayName("创建响应（内容和完成原因）")
    void of_contentAndFinishReason() {
        LlmResponse response = LlmResponse.of("Hello!", LlmResponse.FinishReason.STOP);

        assertThat(response.content()).isEqualTo("Hello!");
        assertThat(response.finishReason()).isEqualTo(LlmResponse.FinishReason.STOP);
    }

    @Test
    @DisplayName("创建响应（内容和 Token 使用量）")
    void of_contentAndTokenUsage() {
        TokenUsage usage = new TokenUsage(10, 5, 15);
        LlmResponse response = LlmResponse.of("Hello!", usage);

        assertThat(response.tokenUsage()).isEqualTo(usage);
    }

    @Test
    @DisplayName("创建工具调用响应")
    void ofToolCalls() {
        ToolCall call = new ToolCall("call-1", "tool", Map.of());
        LlmResponse response = LlmResponse.ofToolCalls(List.of(call));

        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.hasToolCalls()).isTrue();
        assertThat(response.finishReason()).isEqualTo(LlmResponse.FinishReason.TOOL_CALL);
    }

    @Test
    @DisplayName("hasContent 返回正确结果")
    void hasContent() {
        LlmResponse withContent = LlmResponse.of("Hello");
        LlmResponse withoutContent = LlmResponse.of(null);
        LlmResponse blankContent = LlmResponse.of("  ");

        assertThat(withContent.hasContent()).isTrue();
        assertThat(withoutContent.hasContent()).isFalse();
        assertThat(blankContent.hasContent()).isFalse();
    }

    @Test
    @DisplayName("hasTokenUsage 返回正确结果")
    void hasTokenUsage() {
        LlmResponse withUsage = LlmResponse.of("Hello", new TokenUsage(1, 1, 2));
        LlmResponse withoutUsage = LlmResponse.of("Hello");
        LlmResponse emptyUsage = LlmResponse.of("Hello", new TokenUsage(0, 0, 0));

        assertThat(withUsage.hasTokenUsage()).isTrue();
        assertThat(withoutUsage.hasTokenUsage()).isFalse();
        assertThat(emptyUsage.hasTokenUsage()).isFalse();
    }

    @Test
    @DisplayName("withContent 创建新响应")
    void withContent() {
        LlmResponse response = LlmResponse.of("Hello");
        LlmResponse newResponse = response.withContent("Hi");

        assertThat(newResponse.content()).isEqualTo("Hi");
        assertThat(response.content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("withToolCalls 创建新响应")
    void withToolCalls() {
        LlmResponse response = LlmResponse.of("Hello");
        ToolCall call = new ToolCall("call-1", "tool", Map.of());
        LlmResponse newResponse = response.withToolCalls(List.of(call));

        assertThat(newResponse.hasToolCalls()).isTrue();
        assertThat(response.hasToolCalls()).isFalse();
    }
}