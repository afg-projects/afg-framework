package io.github.afgprojects.framework.ai.persistence.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Persistence configuration properties.
 *
 * <p>Prefix: {@code afg.ai.persistence}
 */
@Data
@ConfigurationProperties(prefix = "afg.ai.persistence")
public class PersistenceProperties {

    /**
     * Whether persistence support is enabled.
     */
    private boolean enabled = true;

    /**
     * Session storage configuration.
     */
    private SessionConfig session = new SessionConfig();

    /**
     * Message history storage configuration.
     */
    private MessageHistoryConfig messageHistory = new MessageHistoryConfig();

    @Data
    public static class SessionConfig {

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

    @Data
    public static class MessageHistoryConfig {

        /**
         * Database table name for message history.
         */
        private String tableName = "ai_message_history";

        /**
         * Maximum number of messages retained per session.
         */
        private int maxMessagesPerSession = 1000;
    }
}
