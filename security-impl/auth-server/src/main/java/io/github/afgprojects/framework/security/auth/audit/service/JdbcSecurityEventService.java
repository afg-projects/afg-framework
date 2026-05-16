package io.github.afgprojects.framework.security.auth.audit.service;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.audit.model.SecurityEvent;
import io.github.afgprojects.framework.security.auth.audit.model.SecurityEventType;
import io.github.afgprojects.framework.security.core.audit.SecurityEventService;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 JDBC 的安全事件服务实现。
 *
 * <p>使用 {@link JdbcDataManager} 将安全事件持久化到数据库。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class JdbcSecurityEventService implements SecurityEventService {

    private static final String TABLE_NAME = "auth_security_event";

    private static final String INSERT_SQL = """
            INSERT INTO %s
            (event_type, user_id, tenant_id, ip, details, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.formatted(TABLE_NAME);

    private static final String SELECT_RECENT_SQL = """
            SELECT id, event_type, user_id, tenant_id, ip, details, created_at
            FROM %s
            WHERE created_at >= ?
            ORDER BY created_at DESC
            """.formatted(TABLE_NAME);

    private static final String SELECT_BY_TYPE_SQL = """
            SELECT id, event_type, user_id, tenant_id, ip, details, created_at
            FROM %s
            WHERE event_type = ? AND created_at >= ?
            ORDER BY created_at DESC
            """.formatted(TABLE_NAME);

    private final JdbcDataManager dataManager;

    /**
     * 创建 JdbcSecurityEventService 实例。
     *
     * @param dataManager JDBC 数据管理器，永不为 null
     */
    public JdbcSecurityEventService(@NonNull JdbcDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void recordEvent(@NonNull SecurityEventInfo event) {
        List<Object> params = new ArrayList<>();
        params.add(event.getEventType());
        params.add(event.getUserId());
        params.add(event.getTenantId());
        params.add(event.getIp());
        params.add(event.getDetails());
        params.add(event.getCreatedAt() != null ? Timestamp.from(event.getCreatedAt()) : Timestamp.from(Instant.now()));

        dataManager.executeUpdate(INSERT_SQL, params);

        log.debug("Recorded security event: type={}, userId={}, ip={}",
                event.getEventType(), event.getUserId(), event.getIp());
    }

    @Override
    public List<SecurityEventInfo> getRecentEvents(@NonNull Duration duration) {
        Instant startTime = Instant.now().minus(duration);

        return dataManager.queryForList(SELECT_RECENT_SQL,
                List.of(Timestamp.from(startTime)),
                new SecurityEventRowMapper());
    }

    @Override
    public List<SecurityEventInfo> getEventsByType(@NonNull String eventType, @NonNull Duration duration) {
        Instant startTime = Instant.now().minus(duration);

        return dataManager.queryForList(SELECT_BY_TYPE_SQL,
                List.of(eventType, Timestamp.from(startTime)),
                new SecurityEventRowMapper());
    }

    /**
     * 安全事件行映射器。
     */
    private static class SecurityEventRowMapper implements org.springframework.jdbc.core.RowMapper<SecurityEventInfo> {
        @Override
        public SecurityEventInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SecurityEvent.builder()
                    .id(rs.getLong("id"))
                    .eventType(SecurityEventType.valueOf(rs.getString("event_type")))
                    .userId(rs.getString("user_id"))
                    .tenantId(rs.getString("tenant_id"))
                    .ip(rs.getString("ip"))
                    .details(rs.getString("details"))
                    .createdAt(rs.getTimestamp("created_at") != null ?
                            rs.getTimestamp("created_at").toInstant() : null)
                    .build();
        }
    }
}