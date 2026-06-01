package io.github.afgprojects.framework.ai.persistence.autoconfigure;

import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.persistence.DefaultMessageHistoryStore;
import io.github.afgprojects.framework.ai.persistence.DefaultSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for AI persistence support.
 *
 * <p>Configures session storage and message history storage.
 *
 * @see PersistenceProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PersistenceProperties.class)
@ConditionalOnClass(DefaultSessionStore.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SessionStore sessionStore(PersistenceProperties properties) {
        log.info("Creating DefaultSessionStore with maxSessionsPerUser={}",
                properties.getSession().getMaxSessionsPerUser());
        return new DefaultSessionStore(properties.getSession().getMaxSessionsPerUser());
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageHistoryStore messageHistoryStore(PersistenceProperties properties) {
        log.info("Creating DefaultMessageHistoryStore with maxMessagesPerSession={}",
                properties.getMessageHistory().getMaxMessagesPerSession());
        return new DefaultMessageHistoryStore(properties.getMessageHistory().getMaxMessagesPerSession());
    }
}
