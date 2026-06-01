package io.github.afgprojects.framework.ai.persistence.autoconfigure;

import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.persistence.JdbcMessageHistoryStore;
import io.github.afgprojects.framework.ai.persistence.JdbcSessionStore;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for JDBC-based AI persistence.
 *
 * <p>Configures JDBC-backed session storage and message history storage
 * when a JdbcDataManager is available.
 *
 * @see PersistenceProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PersistenceProperties.class)
@ConditionalOnClass(JdbcDataManager.class)
@ConditionalOnBean(JdbcDataManager.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "type", havingValue = "jdbc")
public class JdbcPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SessionStore jdbcSessionStore(JdbcDataManager dataManager, PersistenceProperties properties) {
        String tableName = properties.getSession().getTableName();
        log.info("Creating JdbcSessionStore with tableName={}", tableName);
        return new JdbcSessionStore(dataManager, tableName);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageHistoryStore jdbcMessageHistoryStore(JdbcDataManager dataManager, PersistenceProperties properties) {
        String tableName = properties.getMessageHistory().getTableName();
        log.info("Creating JdbcMessageHistoryStore with tableName={}", tableName);
        return new JdbcMessageHistoryStore(dataManager, tableName);
    }
}
