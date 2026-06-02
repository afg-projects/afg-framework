package io.github.afgprojects.framework.ai.langchain4j.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LangChain4j 适配模块配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     langchain4j:
 *       enabled: true
 *       default-chat-model: "default"
 *       default-embedding-model: "default"
 *       max-retries: 3
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.langchain4j")
public class Lc4jProperties {

    private boolean enabled = true;
    private String defaultChatModel = "default";
    private String defaultEmbeddingModel = "default";
    private int maxRetries = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultChatModel() { return defaultChatModel; }
    public void setDefaultChatModel(String defaultChatModel) { this.defaultChatModel = defaultChatModel; }

    public String getDefaultEmbeddingModel() { return defaultEmbeddingModel; }
    public void setDefaultEmbeddingModel(String defaultEmbeddingModel) { this.defaultEmbeddingModel = defaultEmbeddingModel; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
