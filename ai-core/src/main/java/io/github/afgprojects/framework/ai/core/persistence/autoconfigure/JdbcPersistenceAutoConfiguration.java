package io.github.afgprojects.framework.ai.core.persistence.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.persistence.JdbcMessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.JdbcSessionStore;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for JDBC-based AI persistence.
 *
 * <p>Configures JDBC-backed session storage and message history storage
 * when a DataManager is available.
 *
 * @see PersistenceProperties
 */
@Slf4j
@AutoConfiguration(after = PersistenceAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.data.jdbc.autoconfigure.DataManagerAutoConfiguration")
@EnableConfigurationProperties(PersistenceProperties.class)
@ConditionalOnBean(DataManager.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SessionStore jdbcSessionStore(DataManager dataManager, PersistenceProperties properties) {
        String tableName = properties.getSession().getTableName();
        log.info("Creating JdbcSessionStore with tableName={}", tableName);
        return new JdbcSessionStore(dataManager, tableName);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageHistoryStore jdbcMessageHistoryStore(DataManager dataManager, PersistenceProperties properties) {
        String tableName = properties.getMessageHistory().getTableName();
        log.info("Creating JdbcMessageHistoryStore with tableName={}", tableName);
        return new JdbcMessageHistoryStore(dataManager, tableName);
    }
}
