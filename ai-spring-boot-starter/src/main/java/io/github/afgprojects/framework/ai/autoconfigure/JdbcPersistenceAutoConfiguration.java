package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.persistence.JdbcMessageHistoryStore;
import io.github.afgprojects.framework.ai.persistence.JdbcSessionStore;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * JDBC 持久化自动配置
 *
 * <p>当 classpath 上存在 JdbcDataManager 且配置 type=jdbc 时激活。
 * 独立于 PersistenceAutoConfiguration 以避免类加载失败。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = {AiAutoConfiguration.class, PersistenceAutoConfiguration.class})
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass(JdbcDataManager.class)
@ConditionalOnBean(JdbcDataManager.class)
@ConditionalOnProperty(prefix = "afg.ai.persistence", name = "type", havingValue = "jdbc")
public class JdbcPersistenceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JdbcPersistenceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(SessionStore.class)
    public SessionStore jdbcSessionStore(
            @NonNull JdbcDataManager dataManager,
            @NonNull AiConfigurationProperties properties) {

        String tableName = properties.getPersistence().getSession().getTableName();
        log.info("Creating JDBC session store with DataManager: table={}", tableName);

        return new JdbcSessionStore(dataManager, tableName);
    }

    @Bean
    @ConditionalOnMissingBean(MessageHistoryStore.class)
    public MessageHistoryStore jdbcMessageHistoryStore(
            @NonNull JdbcDataManager dataManager,
            @NonNull AiConfigurationProperties properties) {

        String tableName = properties.getPersistence().getMessageHistory().getTableName();
        log.info("Creating JDBC message history store with DataManager: table={}", tableName);

        return new JdbcMessageHistoryStore(dataManager, tableName);
    }
}