package io.github.afgprojects.framework.ai.agent.tool.audit;

import io.github.afgprojects.framework.ai.core.tool.ToolAuditLogger;
import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 基于 JDBC 的工具审计日志实现。
 *
 * <p>将工具调用审计日志存储到数据库表中。
 *
 * <p>表结构：
 * <pre>
 * CREATE TABLE ai_tool_audit (
 *     id VARCHAR(64) PRIMARY KEY,
 *     call_id VARCHAR(64) NOT NULL,
 *     tool_name VARCHAR(100) NOT NULL,
 *     user_id VARCHAR(64),
 *     tenant_id VARCHAR(64),
 *     session_id VARCHAR(64),
 *     arguments TEXT,
 *     output TEXT,
 *     error TEXT,
 *     status VARCHAR(20) NOT NULL,
 *     start_time TIMESTAMP NOT NULL,
 *     end_time TIMESTAMP,
 *     duration_ms BIGINT,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 * </pre>
 *
 * @since 1.0.0
 */
public class JdbcToolAuditLogger implements ToolAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(JdbcToolAuditLogger.class);

    private final JdbcClient jdbcClient;

    /**
     * 创建 JDBC 审计日志器。
     *
     * @param jdbcClient JDBC 客户端
     */
    public JdbcToolAuditLogger(@NonNull JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public @NonNull String logStart(
            @NonNull String callId,
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        String recordId = UUID.randomUUID().toString().replace("-", "");
        Instant startTime = Instant.now();

        try {
            jdbcClient.sql("""
                INSERT INTO ai_tool_audit
                (id, call_id, tool_name, user_id, tenant_id, session_id, arguments, status, start_time)
                VALUES
                (:id, :callId, :toolName, :userId, :tenantId, :sessionId, :arguments, :status, :startTime)
                """)
                .param("id", recordId)
                .param("callId", callId)
                .param("toolName", toolName)
                .param("userId", context.getUserId())
                .param("tenantId", context.getTenantId())
                .param("sessionId", context.getSessionId())
                .param("arguments", serializeArguments(arguments))
                .param("status", ToolCallStatus.STARTED.name())
                .param("startTime", startTime)
                .update();

            log.debug("Tool audit start logged: recordId={}, toolName={}, userId={}",
                recordId, toolName, context.getUserId());
        } catch (Exception e) {
            log.error("Failed to log tool audit start: {}", e.getMessage());
            // 不抛出异常，避免影响工具执行
        }

        return recordId;
    }

    @Override
    public void logSuccess(
            @NonNull String recordId,
            @Nullable Object output,
            @NonNull Duration duration) {
        Instant endTime = Instant.now();

        try {
            jdbcClient.sql("""
                UPDATE ai_tool_audit
                SET output = :output,
                    status = :status,
                    end_time = :endTime,
                    duration_ms = :durationMs
                WHERE id = :id
                """)
                .param("id", recordId)
                .param("output", serializeOutput(output))
                .param("status", ToolCallStatus.SUCCESS.name())
                .param("endTime", endTime)
                .param("durationMs", duration.toMillis())
                .update();

            log.debug("Tool audit success logged: recordId={}, durationMs={}",
                recordId, duration.toMillis());
        } catch (Exception e) {
            log.error("Failed to log tool audit success: {}", e.getMessage());
        }
    }

    @Override
    public void logFailure(
            @NonNull String recordId,
            @NonNull String error,
            @NonNull Duration duration) {
        Instant endTime = Instant.now();

        try {
            jdbcClient.sql("""
                UPDATE ai_tool_audit
                SET error = :error,
                    status = :status,
                    end_time = :endTime,
                    duration_ms = :durationMs
                WHERE id = :id
                """)
                .param("id", recordId)
                .param("error", error)
                .param("status", ToolCallStatus.FAILURE.name())
                .param("endTime", endTime)
                .param("durationMs", duration.toMillis())
                .update();

            log.debug("Tool audit failure logged: recordId={}, error={}", recordId, error);
        } catch (Exception e) {
            log.error("Failed to log tool audit failure: {}", e.getMessage());
        }
    }

    @Override
    public void logPermissionDenied(
            @NonNull String callId,
            @NonNull String toolName,
            @NonNull String reason,
            @NonNull ToolContext context) {
        String recordId = UUID.randomUUID().toString().replace("-", "");
        Instant startTime = Instant.now();

        try {
            jdbcClient.sql("""
                INSERT INTO ai_tool_audit
                (id, call_id, tool_name, user_id, tenant_id, session_id, error, status, start_time, end_time, duration_ms)
                VALUES
                (:id, :callId, :toolName, :userId, :tenantId, :sessionId, :error, :status, :startTime, :endTime, :durationMs)
                """)
                .param("id", recordId)
                .param("callId", callId)
                .param("toolName", toolName)
                .param("userId", context.getUserId())
                .param("tenantId", context.getTenantId())
                .param("sessionId", context.getSessionId())
                .param("error", "Permission denied: " + reason)
                .param("status", ToolCallStatus.PERMISSION_DENIED.name())
                .param("startTime", startTime)
                .param("endTime", startTime)
                .param("durationMs", 0L)
                .update();

            log.debug("Tool audit permission denied logged: recordId={}, toolName={}, reason={}",
                recordId, toolName, reason);
        } catch (Exception e) {
            log.error("Failed to log tool audit permission denied: {}", e.getMessage());
        }
    }

    @Override
    public @NonNull List<ToolAuditEntry> query(@NonNull ToolAuditQuery query) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, call_id, tool_name, user_id, tenant_id, session_id,
                   arguments, output, error, status, start_time, end_time, duration_ms
            FROM ai_tool_audit
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (query.getUserId() != null) {
            sql.append(" AND user_id = ?");
            params.add(query.getUserId());
        }
        if (query.getTenantId() != null) {
            sql.append(" AND tenant_id = ?");
            params.add(query.getTenantId());
        }
        if (query.getToolName() != null) {
            sql.append(" AND tool_name = ?");
            params.add(query.getToolName());
        }
        if (query.getStatus() != null) {
            sql.append(" AND status = ?");
            params.add(query.getStatus().name());
        }
        if (query.getStartTimeFrom() != null) {
            sql.append(" AND start_time >= ?");
            params.add(query.getStartTimeFrom());
        }
        if (query.getStartTimeTo() != null) {
            sql.append(" AND start_time <= ?");
            params.add(query.getStartTimeTo());
        }

        sql.append(" ORDER BY start_time DESC");
        sql.append(" LIMIT ? OFFSET ?");
        params.add(query.getSize());
        params.add(query.getPage() * query.getSize());

        try {
            return jdbcClient.sql(sql.toString())
                .params(params.toArray())
                .query(this::mapToEntry)
                .list();
        } catch (Exception e) {
            log.error("Failed to query tool audit logs: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 序列化参数。
     */
    private @Nullable String serializeArguments(@NonNull Map<String, Object> arguments) {
        try {
            // 移除内部参数
            Map<String, Object> filtered = new HashMap<>(arguments);
            filtered.remove("__tool__");
            return toJson(filtered);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 序列化输出。
     */
    private @Nullable String serializeOutput(@Nullable Object output) {
        if (output == null) {
            return null;
        }
        try {
            return toJson(output);
        } catch (Exception e) {
            return output.toString();
        }
    }

    /**
     * 转换为 JSON。
     */
    private String toJson(Object obj) {
        // 简单实现，实际应使用 Jackson 或 Gson
        if (obj instanceof Map) {
            return obj.toString();
        }
        return obj.toString();
    }

    /**
     * 映射到 ToolAuditEntry。
     */
    private ToolAuditEntry mapToEntry(ResultSet rs, int rowNum) throws SQLException {
        return new DefaultToolAuditEntry(
            rs.getString("id"),
            rs.getString("call_id"),
            rs.getString("tool_name"),
            rs.getString("user_id"),
            rs.getString("tenant_id"),
            rs.getString("session_id"),
            parseArguments(rs.getString("arguments")),
            rs.getString("output"),
            rs.getString("error"),
            ToolCallStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toInstant() : Instant.now(),
            rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toInstant() : null,
            rs.getLong("duration_ms")
        );
    }

    /**
     * 解析参数。
     */
    private @NonNull Map<String, Object> parseArguments(@Nullable String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        // 简单实现，实际应使用 Jackson 或 Gson
        return Map.of("raw", arguments);
    }

    /**
     * 默认审计日志条目。
     */
    public record DefaultToolAuditEntry(
        @NonNull String id,
        @NonNull String callId,
        @NonNull String toolName,
        @Nullable String userId,
        @Nullable String tenantId,
        @Nullable String sessionId,
        @NonNull Map<String, Object> arguments,
        @Nullable Object output,
        @Nullable String error,
        @NonNull ToolCallStatus status,
        @NonNull Instant startTime,
        @Nullable Instant endTime,
        long durationMs
    ) implements ToolAuditEntry {

        @Override
        public @NonNull String getId() {
            return id;
        }

        @Override
        public @NonNull String getCallId() {
            return callId;
        }

        @Override
        public @NonNull String getToolName() {
            return toolName;
        }

        @Override
        public @Nullable String getUserId() {
            return userId;
        }

        @Override
        public @Nullable String getTenantId() {
            return tenantId;
        }

        @Override
        public @Nullable String getSessionId() {
            return sessionId;
        }

        @Override
        public @NonNull Map<String, Object> getArguments() {
            return arguments;
        }

        @Override
        public @Nullable Object getOutput() {
            return output;
        }

        @Override
        public @Nullable String getError() {
            return error;
        }

        @Override
        public @NonNull ToolCallStatus getStatus() {
            return status;
        }

        @Override
        public @NonNull Instant getStartTime() {
            return startTime;
        }

        @Override
        public @Nullable Instant getEndTime() {
            return endTime;
        }

        @Override
        public long getDurationMs() {
            return durationMs;
        }
    }

    /**
     * 默认审计日志查询条件。
     */
    public static class DefaultToolAuditQuery implements ToolAuditQuery {
        private String userId;
        private String tenantId;
        private String toolName;
        private ToolCallStatus status;
        private Instant startTimeFrom;
        private Instant startTimeTo;
        private int page = 0;
        private int size = 20;

        public DefaultToolAuditQuery userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        public DefaultToolAuditQuery tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public DefaultToolAuditQuery toolName(@Nullable String toolName) {
            this.toolName = toolName;
            return this;
        }

        public DefaultToolAuditQuery status(@Nullable ToolCallStatus status) {
            this.status = status;
            return this;
        }

        public DefaultToolAuditQuery startTimeFrom(@Nullable Instant startTimeFrom) {
            this.startTimeFrom = startTimeFrom;
            return this;
        }

        public DefaultToolAuditQuery startTimeTo(@Nullable Instant startTimeTo) {
            this.startTimeTo = startTimeTo;
            return this;
        }

        public DefaultToolAuditQuery page(int page) {
            this.page = page;
            return this;
        }

        public DefaultToolAuditQuery size(int size) {
            this.size = size;
            return this;
        }

        @Override
        public @Nullable String getUserId() { return userId; }
        @Override
        public @Nullable String getTenantId() { return tenantId; }
        @Override
        public @Nullable String getToolName() { return toolName; }
        @Override
        public @Nullable ToolCallStatus getStatus() { return status; }
        @Override
        public @Nullable Instant getStartTimeFrom() { return startTimeFrom; }
        @Override
        public @Nullable Instant getStartTimeTo() { return startTimeTo; }
        @Override
        public int getPage() { return page; }
        @Override
        public int getSize() { return size; }
    }
}