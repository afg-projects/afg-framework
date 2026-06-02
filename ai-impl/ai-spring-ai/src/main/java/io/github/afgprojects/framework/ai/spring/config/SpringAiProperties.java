package io.github.afgprojects.framework.ai.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI 适配模块配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     spring:
 *       enabled: true
 *       default-chat-model: "default"
 *       default-embedding-model: "default"
 *       observation-enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.spring")
public class SpringAiProperties {

    private boolean enabled = true;
    private String defaultChatModel = "default";
    private String defaultEmbeddingModel = "default";
    private boolean observationEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultChatModel() { return defaultChatModel; }
    public void setDefaultChatModel(String defaultChatModel) { this.defaultChatModel = defaultChatModel; }

    public String getDefaultEmbeddingModel() { return defaultEmbeddingModel; }
    public void setDefaultEmbeddingModel(String defaultEmbeddingModel) { this.defaultEmbeddingModel = defaultEmbeddingModel; }

    public boolean isObservationEnabled() { return observationEnabled; }
    public void setObservationEnabled(boolean observationEnabled) { this.observationEnabled = observationEnabled; }
}
