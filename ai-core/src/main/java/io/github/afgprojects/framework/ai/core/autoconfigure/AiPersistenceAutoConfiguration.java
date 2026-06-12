package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.persistence.DefaultMessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.DefaultSessionStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 持久化自动配置。
 *
 * <p>配置前缀：{@code afg.ai.persistence}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiCoreAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiPersistenceAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PersistenceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SessionStore defaultSessionStore(AfgAiProperties properties) {
            return new DefaultSessionStore(properties.getPersistence().getSession().getMaxSessionsPerUser());
        }

        @Bean
        @ConditionalOnMissingBean
        public MessageHistoryStore defaultMessageHistoryStore(AfgAiProperties properties) {
            return new DefaultMessageHistoryStore(properties.getPersistence().getMessageHistory().getMaxMessagesPerSession());
        }
    }
}
