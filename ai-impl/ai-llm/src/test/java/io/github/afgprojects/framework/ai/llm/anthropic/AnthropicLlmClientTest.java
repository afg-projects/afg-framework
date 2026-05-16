package io.github.afgprojects.framework.ai.llm.anthropic;

import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AnthropicLlmClient 单元测试
 *
 * <p>注意：由于 Spring AI 客户端在构造时会初始化 HTTP 客户端，
 * 实际 API 调用测试需要真实的 API Key 和网络环境。
 * 这里主要测试配置相关逻辑。
 *
 * <p>集成测试应使用真实的 API Key 在专门的测试环境中进行。
 */
class AnthropicLlmClientTest {

    @Test
    @DisplayName("LlmConfig 配置正确性")
    void config_correctness() {
        LlmConfig config = LlmConfig.of("sk-ant-test-key", "claude-3-opus");

        assertThat(config.apiKey()).isEqualTo("sk-ant-test-key");
        assertThat(config.model()).isEqualTo("claude-3-opus");
        assertThat(config.timeout()).isEqualTo(LlmConfig.DEFAULT_TIMEOUT);
    }

    @Test
    @DisplayName("LlmConfig withBaseUrl 正确性")
    void config_withBaseUrl() {
        LlmConfig config = LlmConfig.of("sk-ant-test-key", "claude-3-opus")
                .withBaseUrl("https://custom.api");

        assertThat(config.baseUrl()).isEqualTo("https://custom.api");
    }

    @Test
    @DisplayName("LlmConfig withTimeout 正确性")
    void config_withTimeout() {
        LlmConfig config = LlmConfig.of("sk-ant-test-key", "claude-3-opus")
                .withTimeout(java.time.Duration.ofSeconds(30));

        assertThat(config.timeout()).isEqualTo(java.time.Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("LlmConfig withModel 正确性")
    void config_withModel() {
        LlmConfig config = LlmConfig.of("key", "claude-3-opus")
                .withModel("claude-3-sonnet");

        assertThat(config.model()).isEqualTo("claude-3-sonnet");
    }
}