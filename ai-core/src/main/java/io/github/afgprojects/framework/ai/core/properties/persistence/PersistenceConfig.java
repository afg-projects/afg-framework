package io.github.afgprojects.framework.ai.core.properties.persistence;

import lombok.Data;

/**
 * 持久化配置。
 */
@Data
public class PersistenceConfig {

    /**
     * 是否启用持久化。
     */
    private boolean enabled = true;

    /**
     * 会话存储配置。
     */
    private SessionConfig session = new SessionConfig();

    /**
     * 消息历史配置。
     */
    private MessageHistoryConfig messageHistory = new MessageHistoryConfig();
}
