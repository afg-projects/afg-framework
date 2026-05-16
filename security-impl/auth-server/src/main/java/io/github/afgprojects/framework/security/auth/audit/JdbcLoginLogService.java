package io.github.afgprojects.framework.security.auth.audit;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.core.audit.LoginLogService;
import io.github.afgprojects.framework.security.core.audit.model.LoginLog;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
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
    public void recordLogin(@NonNull LoginLog loginLog) {
        String id = UUID.randomUUID().toString();

        List<Object> params = new ArrayList<>();
        params.add(id);
        params.add(loginLog.userId());
        params.add(loginLog.username());
        params.add(loginLog.tenantId());
        params.add(loginLog.ip());
        params.add(loginLog.deviceId());
        params.add(loginLog.deviceName());
        params.add(loginLog.browser());
        params.add(loginLog.os());
        params.add(loginLog.location());
        params.add(loginLog.result());
        params.add(loginLog.failReason());
        params.add(Timestamp.from(loginLog.loginTime()));
        params.add(loginLog.logoutTime() != null ? Timestamp.from(loginLog.logoutTime()) : null);

        dataManager.executeUpdate(INSERT_SQL, params);

        log.debug("Recorded login log: username={}, result={}", loginLog.username(), loginLog.result());
    }

    @Override
    public void recordLogout(@NonNull String userId, @Nullable String tenantId, @NonNull String ip) {
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(java.time.Instant.now()));
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
}
