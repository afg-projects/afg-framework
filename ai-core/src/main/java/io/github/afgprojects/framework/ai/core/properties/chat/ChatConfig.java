package io.github.afgprojects.framework.ai.core.properties.chat;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 对话客户端配置。
 */
@Data
public class ChatConfig {

    /**
     * 是否启用对话客户端。
     */
    private boolean enabled = true;

    /**
     * 默认对话客户端名称。
     */
    private String defaultName = "default";

    /**
     * 默认系统提示词。
     */
    private @Nullable String defaultSystemPrompt;

    /**
     * 对话记忆配置。
     */
    private ChatMemoryConfig memory = new ChatMemoryConfig();
}
