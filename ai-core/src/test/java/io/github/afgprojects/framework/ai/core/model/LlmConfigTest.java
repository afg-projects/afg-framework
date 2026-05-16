package io.github.afgprojects.framework.ai.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LlmConfig 单元测试
 */
class LlmConfigTest {

    @Test
    @DisplayName("创建配置（仅模型）")
    void of_modelOnly() {
        LlmConfig config = LlmConfig.of("gpt-4");

        assertThat(config.model()).isEqualTo("gpt-4");
        assertThat(config.apiKey()).isNull();
        assertThat(config.baseUrl()).isNull();
        assertThat(config.timeout()).isEqualTo(LlmConfig.DEFAULT_TIMEOUT);
    }

    @Test
    @DisplayName("创建配置（API Key 和模型）")
    void of_apiKeyAndModel() {
        LlmConfig config = LlmConfig.of("sk-test", "gpt-4");

        assertThat(config.apiKey()).isEqualTo("sk-test");
        assertThat(config.model()).isEqualTo("gpt-4");
    }

    @Test
    @DisplayName("创建配置（全部参数）")
    void of_allParams() {
        Duration timeout = Duration.ofSeconds(30);
        LlmConfig config = LlmConfig.of("sk-test", "gpt-4", "https://api.example.com", timeout);

        assertThat(config.apiKey()).isEqualTo("sk-test");
        assertThat(config.model()).isEqualTo("gpt-4");
        assertThat(config.baseUrl()).isEqualTo("https://api.example.com");
        assertThat(config.timeout()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("空模型名抛出异常")
    void blankModel_throwsException() {
        assertThatThrownBy(() -> LlmConfig.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("model cannot be null");
    }

    @Test
    @DisplayName("withApiKey 创建新配置")
    void withApiKey() {
        LlmConfig config = LlmConfig.of("old-key", "gpt-4");
        LlmConfig newConfig = config.withApiKey("new-key");

        assertThat(newConfig.apiKey()).isEqualTo("new-key");
        assertThat(newConfig.model()).isEqualTo("gpt-4");
        assertThat(config.apiKey()).isEqualTo("old-key"); // 原配置不变
    }

    @Test
    @DisplayName("withModel 创建新配置")
    void withModel() {
        LlmConfig config = LlmConfig.of("key", "gpt-4");
        LlmConfig newConfig = config.withModel("gpt-3.5-turbo");

        assertThat(newConfig.model()).isEqualTo("gpt-3.5-turbo");
        assertThat(config.model()).isEqualTo("gpt-4");
    }

    @Test
    @DisplayName("withBaseUrl 创建新配置")
    void withBaseUrl() {
        LlmConfig config = LlmConfig.of("key", "gpt-4");
        LlmConfig newConfig = config.withBaseUrl("https://custom.api");

        assertThat(newConfig.baseUrl()).isEqualTo("https://custom.api");
    }

    @Test
    @DisplayName("withTimeout 创建新配置")
    void withTimeout() {
        LlmConfig config = LlmConfig.of("key", "gpt-4");
        Duration newTimeout = Duration.ofMinutes(5);
        LlmConfig newConfig = config.withTimeout(newTimeout);

        assertThat(newConfig.timeout()).isEqualTo(newTimeout);
    }

    @Test
    @DisplayName("默认超时时间为 60 秒")
    void defaultTimeout() {
        assertThat(LlmConfig.DEFAULT_TIMEOUT).isEqualTo(Duration.ofSeconds(60));
    }
}