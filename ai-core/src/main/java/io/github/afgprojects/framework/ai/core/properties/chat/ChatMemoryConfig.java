package io.github.afgprojects.framework.ai.core.properties.chat;

import lombok.Data;

/**
 * 对话记忆配置。
 */
@Data
public class ChatMemoryConfig {

    /**
     * 最大保留消息数。
     */
    private int maxMessages = 20;
}
