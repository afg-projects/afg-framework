package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.persistence.DefaultMessageHistoryStore;
import io.github.afgprojects.framework.ai.persistence.DefaultSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 持久化模块自动配置
 *
 * <p>配置会话存储、消息历史存储。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     persistence:
 *       enabled: true
 *       session:
 *         enabled: true
 *         max-sessions-per-user: 100
 *         default-expires-in-seconds: 3600
 *       message-history:
 *         enabled: true
 *         max-messages-per-session: 1000
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass({SessionStore.class, MessageHistoryStore.class})
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PersistenceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PersistenceAutoConfiguration.class);

    /**
     * 配置会话存储
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.persistence.session", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(SessionStore.class)
    public SessionStore sessionStore(AiConfigurationProperties properties) {
        AiConfigurationProperties.SessionConfig config = properties.getPersistence().getSession();

        log.info("Creating default session store: maxSessionsPerUser={}, defaultExpiresInSeconds={}",
                config.getMaxSessionsPerUser(), config.getDefaultExpiresInSeconds());

        return new DefaultSessionStore(config.getMaxSessionsPerUser());
    }

    /**
     * 配置消息历史存储
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.persistence.message-history", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(MessageHistoryStore.class)
    public MessageHistoryStore messageHistoryStore(AiConfigurationProperties properties) {
        AiConfigurationProperties.MessageHistoryConfig config = properties.getPersistence().getMessageHistory();

        log.info("Creating default message history store: maxMessagesPerSession={}",
                config.getMaxMessagesPerSession());

        return new DefaultMessageHistoryStore(config.getMaxMessagesPerSession());
    }
}