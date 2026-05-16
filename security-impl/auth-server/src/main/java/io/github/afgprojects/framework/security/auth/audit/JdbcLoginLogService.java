package io.github.afgprojects.framework.security.auth.audit;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.audit.model.LoginLog;
import io.github.afgprojects.framework.security.auth.audit.model.LoginResult;
import io.github.afgprojects.framework.security.core.audit.LoginLogService;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 JDBC 的登录日志服务实现。
 *
 * <p>使用 {@link JdbcDataManager} 将登录日志持久化到数据库。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class JdbcLoginLogService implements LoginLogService {

    private static final String TABLE_NAME = "auth_login_log";

    private static final String INSERT_SQL = """
            INSERT INTO %s
            (id, user_id, username, tenant_id, ip, device_id, device_name, browser, os, location, result, fail_reason, login_time, logout_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(TABLE_NAME);

    // NOTE: ORDER BY ... LIMIT 1 is MySQL-specific syntax. For database portability,
    // consider using a subquery or ROW_NUMBER() window function for PostgreSQL/Oracle.
    private static final String UPDATE_LOGOUT_SQL = """
            UPDATE %s
            SET logout_time = ?
            WHERE user_id = ?
            AND (tenant_id = ? OR (? IS NULL AND tenant_id IS NULL))
            AND logout_time IS NULL
            ORDER BY login_time DESC
            LIMIT 1
            """.formatted(TABLE_NAME);

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM %s".formatted(TABLE_NAME);

    private static final String SELECT_SQL = "SELECT id, user_id, username, tenant_id, ip, device_id, device_name, browser, os, location, result, fail_reason, login_time, logout_time FROM %s".formatted(TABLE_NAME);

    private final JdbcDataManager dataManager;

    /**
     * 创建 JdbcLoginLogService 实例。
     *
     * @param dataManager JDBC 数据管理器，永不为 null
     */
    public JdbcLoginLogService(@NonNull JdbcDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void recordLogin(@NonNull LoginLogInfo loginLog) {
        String id = UUID.randomUUID().toString();

        List<Object> params = new ArrayList<>();
        params.add(id);
        params.add(loginLog.getUserId());
        params.add(loginLog.getUsername());
        params.add(loginLog.getTenantId());
        params.add(loginLog.getIp());
        params.add(loginLog.getDeviceId());
        params.add(loginLog.getDeviceName());
        params.add(loginLog.getBrowser());
        params.add(loginLog.getOs());
        params.add(loginLog.getLocation());
        params.add(loginLog.getResult());
        params.add(loginLog.getFailReason());
        params.add(loginLog.getLoginTime() != null ? Timestamp.from(loginLog.getLoginTime()) : Timestamp.from(Instant.now()));
        params.add(loginLog.getLogoutTime() != null ? Timestamp.from(loginLog.getLogoutTime()) : null);

        dataManager.executeUpdate(INSERT_SQL, params);

        log.debug("Recorded login log: username={}, result={}", loginLog.getUsername(), loginLog.getResult());
    }

    @Override
    public void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip) {
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(Instant.now()));
        params.add(userId);
        params.add(tenantId);
        params.add(tenantId);

        int updated = dataManager.executeUpdate(UPDATE_LOGOUT_SQL, params);

        if (updated > 0) {
            log.debug("Recorded logout for user: userId={}, tenantId={}, ip={}", userId, tenantId, ip);
        } else {
            log.debug("No active login session found for user: userId={}, tenantId={}, ip={}", userId, tenantId, ip);
        }
    }

    @Override
    public Page<LoginLogInfo> queryLogs(@NonNull LoginLogQuery query, @NonNull Pageable pageable) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 构建查询条件
        if (query.getUserId() != null) {
            whereClause.append(" AND user_id = ?");
            params.add(query.getUserId());
        }
        if (query.getUsername() != null) {
            whereClause.append(" AND username LIKE ?");
            params.add("%" + query.getUsername() + "%");
        }
        if (query.getTenantId() != null) {
            whereClause.append(" AND tenant_id = ?");
            params.add(query.getTenantId());
        }
        if (query.getIp() != null) {
            whereClause.append(" AND ip = ?");
            params.add(query.getIp());
        }
        if (query.getResult() != null) {
            whereClause.append(" AND result = ?");
            params.add(query.getResult());
        }
        if (query.getStartTime() != null) {
            whereClause.append(" AND login_time >= ?");
            params.add(Timestamp.from(query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            whereClause.append(" AND login_time <= ?");
            params.add(Timestamp.from(query.getEndTime()));
        }

        // Count query
        String countSql = COUNT_SQL + whereClause;
        Long total = dataManager.getJdbcClient()
                .sql(countSql)
                .params(params)
                .query(Long.class)
                .single();

        // Build ORDER BY clause
        StringBuilder orderByClause = new StringBuilder(" ORDER BY ");
        if (pageable.getSort().isSorted()) {
            List<String> orderParts = new ArrayList<>();
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                orderParts.add(order.getProperty() + " " + order.getDirection().name());
            }
            orderByClause.append(String.join(", ", orderParts));
        } else {
            orderByClause.append("login_time DESC");
        }

        // Data query with pagination (H2/MySQL syntax)
        String dataSql = SELECT_SQL + whereClause + orderByClause + " LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageable.getPageSize());
        dataParams.add(pageable.getOffset());

        List<LoginLogInfo> logs = dataManager.queryForList(dataSql, dataParams, new LoginLogRowMapper());

        return new PageImpl<>(logs, pageable, total != null ? total : 0);
    }

    /**
     * 登录日志行映射器。
     */
    private static class LoginLogRowMapper implements org.springframework.jdbc.core.RowMapper<LoginLogInfo> {
        @Override
        public LoginLogInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return LoginLog.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getString("user_id"))
                    .username(rs.getString("username"))
                    .tenantId(rs.getString("tenant_id"))
                    .ip(rs.getString("ip"))
                    .deviceId(rs.getString("device_id"))
                    .deviceName(rs.getString("device_name"))
                    .browser(rs.getString("browser"))
                    .os(rs.getString("os"))
                    .location(rs.getString("location"))
                    .result(rs.getString("result") != null ? LoginResult.valueOf(rs.getString("result")) : null)
                    .failReason(rs.getString("fail_reason"))
                    .loginTime(rs.getTimestamp("login_time") != null ?
                            rs.getTimestamp("login_time").toInstant() : null)
                    .logoutTime(rs.getTimestamp("logout_time") != null ?
                            rs.getTimestamp("logout_time").toInstant() : null)
                    .build();
        }
    }
}