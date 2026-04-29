package io.github.afgprojects.framework.integration.jdbc.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 数据库审计日志配置属性
 * <p>
 * 配置项前缀: afg.audit.database
 * </p>
 *
 * <pre>
 * afg:
 *   audit:
 *     storage-type: database
 *     database:
 *       table-name: audit_log
 *       async-enabled: true
 *       batch-size: 100
 *       flush-interval-ms: 5000
 *       queue-capacity: 10000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.audit.database")
public class DatabaseAuditLogProperties {

    /** 表名 */
    private String tableName = "audit_log";

    /** 是否启用异步写入 */
    private boolean asyncEnabled = true;

    /** 批量写入大小 */
    private int batchSize = 100;

    /** 刷新间隔（毫秒） */
    private long flushIntervalMs = 5000;

    /** 队列容量 */
    private int queueCapacity = 10000;
}
