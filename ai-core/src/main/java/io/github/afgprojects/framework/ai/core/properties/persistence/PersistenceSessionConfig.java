package io.github.afgprojects.framework.ai.core.properties.persistence;

import lombok.Data;

/**
 * Persistence 会话存储配置。
 */
@Data
public class PersistenceSessionConfig {

    /**
     * Database table name for sessions.
     */
    private String tableName = "ai_session";

    /**
     * Maximum number of concurrent sessions per user.
     */
    private int maxSessionsPerUser = 100;

    /**
     * Default session expiration time in seconds.
     */
    private long defaultExpiresInSeconds = 3600;
}
