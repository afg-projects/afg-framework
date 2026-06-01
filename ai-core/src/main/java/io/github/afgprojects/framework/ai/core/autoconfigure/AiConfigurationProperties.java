package io.github.afgprojects.framework.ai.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块全局配置属性
 *
 * <p>LLM Provider 配置全部走 Spring AI 原生：
 * <pre>{@code
 * spring:
 *   ai:
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       chat:
 *         options:
 *           model: gpt-4
 *     anthropic:
 *       api-key: ${ANTHROPIC_API_KEY}
 *       chat:
 *         options:
 *           model: claude-sonnet-4-20250514
 *     ollama:
 *       base-url: http://localhost:11434
 *       chat:
 *         options:
 *           model: llama3
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai")
public class AiConfigurationProperties {

    private boolean enabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}