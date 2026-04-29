package io.github.afgprojects.framework.integration.jdbc.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.afgprojects.framework.core.audit.AuditLog;
import io.github.afgprojects.framework.core.audit.AuditLogStorage;

/**
 * 基于数据库的审计日志存储
 * <p>
 * 将审计日志持久化到数据库，支持异步批量写入以提升性能
 * </p>
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>支持同步/异步两种写入模式</li>
 *   <li>异步模式下支持批量写入，减少数据库压力</li>
 *   <li>支持自定义表名</li>
 *   <li>存储失败不影响业务流程</li>
 * </ul>
 *
 * <h3>表结构</h3>
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
 * </pre>
 */
public class DatabaseAuditLogStorage implements AuditLogStorage {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAuditLogStorage.class);

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "audit_log";

    /** 默认批量大小 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** 默认刷新间隔（毫秒） */
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 5000;

    /** 默认队列容量 */
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseAuditLogProperties properties;

    /** 异步写入队列 */
    private final BlockingQueue<AuditLogRecord> queue;

    /** 批量写入缓冲区 */
    private final List<AuditLogRecord> buffer;

    /** 调度器 */
    private final ScheduledExecutorService scheduler;

    /** 关闭标志 */
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /** 表名 */
    private final String tableName;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC 模板
     * @param properties    配置属性
     */
    public DatabaseAuditLogStorage(
            @NonNull JdbcTemplate jdbcTemplate,
            @NonNull DatabaseAuditLogProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.tableName = properties.getTableName() != null ? properties.getTableName() : DEFAULT_TABLE_NAME;

        if (properties.isAsyncEnabled()) {
            this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
            this.buffer = new ArrayList<>(properties.getBatchSize());
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "audit-log-writer");
                t.setDaemon(true);
                return t;
            });
            startAsyncWriter();
        } else {
            this.queue = null;
            this.buffer = null;
            this.scheduler = null;
        }
    }

    /**
     * 启动异步写入线程
     */
    private void startAsyncWriter() {
        long flushInterval = properties.getFlushIntervalMs() > 0
                ? properties.getFlushIntervalMs()
                : DEFAULT_FLUSH_INTERVAL_MS;

        scheduler.scheduleWithFixedDelay(
                this::flushBuffer,
                flushInterval,
                flushInterval,
                TimeUnit.MILLISECONDS);

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public void save(@NonNull AuditLog auditLog) {
        if (shutdown.get()) {
            log.warn("Audit log storage is shutting down, discard log: id={}", auditLog.id());
            return;
        }

        AuditLogRecord record = AuditLogRecord.from(auditLog);

        if (properties.isAsyncEnabled()) {
            saveAsync(record);
        } else {
            saveSync(record);
        }
    }

    /**
     * 异步保存审计日志
     *
     * @param record 审计日志记录
     */
    private void saveAsync(AuditLogRecord record) {
        if (!queue.offer(record)) {
            // 队列满，降级为同步写入
            log.warn("Audit log queue is full, fallback to sync save: id={}", record.id());
            saveSync(record);
        }
    }

    /**
     * 同步保存审计日志
     *
     * @param record 审计日志记录
     */
    private void saveSync(AuditLogRecord record) {
        try {
            insertBatch(List.of(record));
            log.debug("Audit log saved: id={}, operation={}", record.id(), record.operation());
        } catch (DataAccessException e) {
            log.error("Failed to save audit log: id={}, operation={}", record.id(), record.operation(), e);
        }
    }

    /**
     * 刷新缓冲区到数据库
     */
    private void flushBuffer() {
        if (queue == null || queue.isEmpty()) {
            return;
        }

        // 从队列取出数据到缓冲区
        queue.drainTo(buffer, properties.getBatchSize());

        if (buffer.isEmpty()) {
            return;
        }

        try {
            insertBatch(buffer);
            log.debug("Flushed {} audit logs to database", buffer.size());
        } catch (DataAccessException e) {
            log.error("Failed to flush audit logs batch, count={}", buffer.size(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 批量插入审计日志
     *
     * @param records 审计日志记录列表
     */
    private void insertBatch(List<AuditLogRecord> records) {
        String sql = buildInsertSql();

        List<Object[]> batchArgs = records.stream()
                .map(this::toSqlParams)
                .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 构建 INSERT SQL 语句
     *
     * @return SQL 语句
     */
    private String buildInsertSql() {
        return String.format(
                "INSERT INTO %s (id, trace_id, request_id, user_id, username, tenant_id, "
                        + "module, operation, target, class_name, method_name, args, old_value, new_value, "
                        + "result, error_message, client_ip, timestamp, duration_ms) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tableName);
    }

    /**
     * 将审计日志记录转换为 SQL 参数
     *
     * @param record 审计日志记录
     * @return SQL 参数数组
     */
    private Object[] toSqlParams(AuditLogRecord record) {
        return new Object[]{
                record.id(),
                record.traceId(),
                record.requestId(),
                record.userId(),
                record.username(),
                record.tenantId(),
                record.module(),
                record.operation(),
                record.target(),
                record.className(),
                record.methodName(),
                record.args(),
                record.oldValue(),
                record.newValue(),
                record.result(),
                record.errorMessage(),
                record.clientIp(),
                record.timestamp(),
                record.durationMs()
        };
    }

    /**
     * 关闭存储器，刷新剩余数据
     */
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        log.info("Shutting down audit log storage, flushing remaining logs...");

        // 刷新剩余数据
        while (queue != null && !queue.isEmpty()) {
            flushBuffer();
        }

        // 关闭调度器
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Audit log storage shutdown complete");
    }

    /**
     * 获取当前队列大小（仅用于监控）
     *
     * @return 队列大小
     */
    public int getQueueSize() {
        return queue != null ? queue.size() : 0;
    }
}