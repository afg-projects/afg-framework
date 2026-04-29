package io.github.afgprojects.framework.integration.jdbc.autoconfigure;

import javax.sql.DataSource;

import io.github.afgprojects.framework.core.audit.AuditLogProperties;
import io.github.afgprojects.framework.core.audit.AuditLogStorage;
import io.github.afgprojects.framework.integration.jdbc.audit.DatabaseAuditLogProperties;
import io.github.afgprojects.framework.integration.jdbc.audit.DatabaseAuditLogStorage;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC 自动配置
 * <p>
 * 自动配置条件:
 * <ul>
 *   <li>存在 DataSource bean</li>
 *   <li>afg.audit.storage-type=database</li>
 * </ul>
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自动配置数据库审计日志存储</li>
 *   <li>支持异步批量写入</li>
 *   <li>支持自定义表名</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   audit:
 *     enabled: true
 *     storage-type: database
 *     database:
 *       table-name: audit_log
 *       async-enabled: true
 *       batch-size: 100
 *       flush-interval-ms: 5000
 *       queue-capacity: 10000
 * </pre>
 *
 * <h3>数据库表结构</h3>
 * <pre>
 * CREATE TABLE audit_log (
 *     id VARCHAR(36) PRIMARY KEY,
 *     trace_id VARCHAR(64),
 *     request_id VARCHAR(64),
 *     user_id BIGINT,
 *     username VARCHAR(128),
 *     tenant_id BIGINT,
 *     module VARCHAR(128),
 *     operation VARCHAR(256),
 *     target VARCHAR(512),
 *     class_name VARCHAR(256),
 *     method_name VARCHAR(128),
 *     args TEXT,
 *     old_value TEXT,
 *     new_value TEXT,
 *     result VARCHAR(16),
 *     error_message TEXT,
 *     client_ip VARCHAR(64),
 *     timestamp TIMESTAMP,
 *     duration_ms BIGINT
 * );
 *
 * -- 索引建议
 * CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
 * CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
 * CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
 * CREATE INDEX idx_audit_log_module ON audit_log(module);
 * CREATE INDEX idx_audit_log_trace_id ON audit_log(trace_id);
 * </pre>
 */
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties({
        AuditLogProperties.class,
        DatabaseAuditLogProperties.class
})
public class JdbcAutoConfiguration {

    /**
     * 配置 JdbcTemplate
     *
     * @param dataSource 数据源
     * @return JdbcTemplate 实例
     */
    @Bean
    @ConditionalOnMissingBean(JdbcTemplate.class)
    public JdbcTemplate jdbcTemplate(@NonNull DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 配置数据库审计日志存储
     * <p>
     * 条件：存在 DataSource Bean 且 storage-type=database
     * </p>
     *
     * @param jdbcTemplate    JDBC 模板
     * @param properties      审计日志配置
     * @param dbProperties    数据库审计日志配置
     * @return 数据库审计日志存储实例
     */
    @Bean
    @ConditionalOnMissingBean(AuditLogStorage.class)
    @ConditionalOnProperty(prefix = "afg.audit", name = "storage-type", havingValue = "database")
    public AuditLogStorage databaseAuditLogStorage(
            @NonNull JdbcTemplate jdbcTemplate,
            @NonNull AuditLogProperties properties,
            @NonNull DatabaseAuditLogProperties dbProperties) {
        return new DatabaseAuditLogStorage(jdbcTemplate, dbProperties);
    }
}