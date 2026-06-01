package io.github.afgprojects.framework.ai.chat.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ChatClient 配置属性
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     chat:
 *       enabled: true
 *       default-system-prompt: "You are a helpful assistant."
 *       memory:
 *         enabled: true
 *         max-messages: 20
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.chat")
public class ChatClientProperties {

    private boolean enabled = true;
    private String defaultName;
    private String defaultSystemPrompt;
    private MemoryConfig memory = new MemoryConfig();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDefaultName() { return defaultName; }
    public void setDefaultName(String defaultName) { this.defaultName = defaultName; }
    public String getDefaultSystemPrompt() { return defaultSystemPrompt; }
    public void setDefaultSystemPrompt(String defaultSystemPrompt) { this.defaultSystemPrompt = defaultSystemPrompt; }
    public MemoryConfig getMemory() { return memory; }
    public void setMemory(MemoryConfig memory) { this.memory = memory; }

    public static class MemoryConfig {
        private boolean enabled = true;
        private int maxMessages = 20;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    }
}
