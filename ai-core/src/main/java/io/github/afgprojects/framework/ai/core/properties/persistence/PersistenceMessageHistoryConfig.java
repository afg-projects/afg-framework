package io.github.afgprojects.framework.ai.core.properties.persistence;

import lombok.Data;

/**
 * Persistence 消息历史配置。
 */
@Data
public class PersistenceMessageHistoryConfig {

    /**
     * Database table name for message history.
     */
    private String tableName = "ai_message_history";

    /**
     * Maximum number of messages retained per session.
     */
    private int maxMessagesPerSession = 1000;
}
