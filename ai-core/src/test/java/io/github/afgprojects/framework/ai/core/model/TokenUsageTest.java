package io.github.afgprojects.framework.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenUsage 单元测试
 */
class TokenUsageTest {

    @Test
    @DisplayName("创建 Token 使用量")
    void create() {
        TokenUsage usage = new TokenUsage(100, 50, 150);

        assertThat(usage.promptTokens()).isEqualTo(100);
        assertThat(usage.completionTokens()).isEqualTo(50);
        assertThat(usage.totalTokens()).isEqualTo(150);
    }

    @Test
    @DisplayName("isEmpty 返回正确结果")
    void isEmpty() {
        TokenUsage empty = new TokenUsage(0, 0, 0);
        TokenUsage notEmpty = new TokenUsage(1, 1, 2);

        assertThat(empty.isEmpty()).isTrue();
        assertThat(notEmpty.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("of 静态方法")
    void of() {
        TokenUsage usage = TokenUsage.of(100, 50);

        assertThat(usage.promptTokens()).isEqualTo(100);
        assertThat(usage.completionTokens()).isEqualTo(50);
        assertThat(usage.totalTokens()).isEqualTo(150);
    }

    @Test
    @DisplayName("empty 静态方法")
    void empty() {
        TokenUsage usage = TokenUsage.empty();

        assertThat(usage.promptTokens()).isZero();
        assertThat(usage.completionTokens()).isZero();
        assertThat(usage.totalTokens()).isZero();
    }

    @Test
    @DisplayName("add 方法")
    void add() {
        TokenUsage usage1 = new TokenUsage(100, 50, 150);
        TokenUsage usage2 = new TokenUsage(10, 5, 15);

        TokenUsage result = usage1.add(usage2);

        assertThat(result.promptTokens()).isEqualTo(110);
        assertThat(result.completionTokens()).isEqualTo(55);
        assertThat(result.totalTokens()).isEqualTo(165);
    }
}