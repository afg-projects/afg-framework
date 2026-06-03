package io.github.afgprojects.framework.ai.core.properties.persistence;

import lombok.Data;

/**
 * 消息历史配置。
 */
@Data
public class MessageHistoryConfig {

    /**
     * 每会话最大消息数。
     */
    private int maxMessagesPerSession = 1000;

    /**
     * 消息历史表名。
     */
    private String tableName = "ai_message_history";
}
