package io.github.afgprojects.framework.ai.llm.openai;

import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAiLlmClient 单元测试
 *
 * <p>注意：由于 Spring AI 客户端在构造时会初始化 HTTP 客户端，
 * 实际 API 调用测试需要真实的 API Key 和网络环境。
 * 这里主要测试配置相关逻辑。
 *
 * <p>集成测试应使用真实的 API Key 在专门的测试环境中进行。
 */
class OpenAiLlmClientTest {

    @Test
    @DisplayName("LlmConfig 配置正确性")
    void config_correctness() {
        LlmConfig config = LlmConfig.of("sk-test-key", "gpt-4");

        assertThat(config.apiKey()).isEqualTo("sk-test-key");
        assertThat(config.model()).isEqualTo("gpt-4");
        assertThat(config.timeout()).isEqualTo(LlmConfig.DEFAULT_TIMEOUT);
    }

    @Test
    @DisplayName("LlmConfig withBaseUrl 正确性")
    void config_withBaseUrl() {
        LlmConfig config = LlmConfig.of("sk-test-key", "gpt-4")
                .withBaseUrl("https://custom.api");

        assertThat(config.baseUrl()).isEqualTo("https://custom.api");
    }

    @Test
    @DisplayName("LlmConfig withTimeout 正确性")
    void config_withTimeout() {
        LlmConfig config = LlmConfig.of("sk-test-key", "gpt-4")
                .withTimeout(java.time.Duration.ofSeconds(30));

        assertThat(config.timeout()).isEqualTo(java.time.Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("LlmConfig withApiKey 正确性")
    void config_withApiKey() {
        LlmConfig config = LlmConfig.of("old-key", "gpt-4")
                .withApiKey("new-key");

        assertThat(config.apiKey()).isEqualTo("new-key");
    }

    @Test
    @DisplayName("LlmConfig withModel 正确性")
    void config_withModel() {
        LlmConfig config = LlmConfig.of("key", "gpt-4")
                .withModel("gpt-3.5-turbo");

        assertThat(config.model()).isEqualTo("gpt-3.5-turbo");
    }
}