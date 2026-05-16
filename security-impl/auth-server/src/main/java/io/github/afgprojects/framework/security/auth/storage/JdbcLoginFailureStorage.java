package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.storage.AfgLoginFailureStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 基于 JDBC 的登录失败存储。
 *
 * <p>将登录失败记录持久化到关系型数据库，支持：
 * <ul>
 *   <li>记录登录失败次数</li>
 *   <li>账户锁定状态管理</li>
 *   <li>失败记录查询</li>
 *   <li>账户解锁与重置</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_login_failure (
 *     user_id VARCHAR(64) NOT NULL UNIQUE,
 *     username VARCHAR(100) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     failure_count INT DEFAULT 1,
 *     last_ip VARCHAR(50),
 *     locked_until TIMESTAMP,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcLoginFailureStorage implements AfgLoginFailureStorage {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_login_failure";

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /** RowMapper 用于将 ResultSet 映射为 FailureRecord */
    private final RowMapper<FailureRecord> rowMapper = new FailureRecordRowMapper();

    /**
     * 构造函数，使用默认表名。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcLoginFailureStorage(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcLoginFailureStorage(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void recordFailure(
            @NonNull String userId,
            @NonNull String username,
            @Nullable String tenantId,
            @Nullable String ip
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 先尝试更新现有记录
        String updateSql = String.format(
                "UPDATE %s SET failure_count = failure_count + 1, last_ip = ?, updated_at = ? WHERE user_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(updateSql, ip, now, userId);
            if (rows > 0) {
                log.debug("Recorded login failure: userId={}, username={}, ip={}", userId, username, ip);
                return;
            }
        } catch (DataAccessException e) {
            log.error("Failed to update login failure record: userId={}", userId, e);
            throw e;
        }

        // 如果更新失败（记录不存在），则插入新记录
        String insertSql = String.format(
                "INSERT INTO %s (user_id, username, tenant_id, failure_count, last_ip, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 1, ?, ?, ?)",
                tableName
        );

        try {
            jdbcTemplate.update(insertSql, userId, username, tenantId, ip, now, now);
            log.debug("Created login failure record: userId={}, username={}, ip={}", userId, username, ip);
        } catch (DataAccessException e) {
            log.error("Failed to insert login failure record: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public int getFailureCount(@NonNull String userId) {
        String sql = String.format("SELECT failure_count FROM %s WHERE user_id = ?", tableName);

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        } catch (DataAccessException e) {
            log.error("Failed to get failure count: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public boolean isLocked(@NonNull String userId) {
        LocalDateTime lockedUntil = getLockedUntil(userId);
        if (lockedUntil == null) {
            return false;
        }
        return lockedUntil.isAfter(LocalDateTime.now());
    }

    @Override
    @Nullable
    public LocalDateTime getLockedUntil(@NonNull String userId) {
        String sql = String.format("SELECT locked_until FROM %s WHERE user_id = ?", tableName);

        try {
            return jdbcTemplate.queryForObject(sql, LocalDateTime.class, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error("Failed to get locked until: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public void unlock(@NonNull String userId) {
        String sql = String.format(
                "DELETE FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, userId);
            if (rows > 0) {
                log.debug("Unlocked user account: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to unlock user account: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public void reset(@NonNull String userId) {
        String sql = String.format(
                "DELETE FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, userId);
            if (rows > 0) {
                log.debug("Reset login failure record: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to reset login failure record: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取完整的失败记录。
     *
     * @param userId 用户 ID
     * @return 失败记录，如果不存在则返回 null
     */
    @Nullable
    public FailureRecord getFailureRecord(@NonNull String userId) {
        String sql = String.format(
                "SELECT failure_count, locked_until, last_ip FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error("Failed to get failure record: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 设置账户锁定截止时间。
     *
     * <p>当失败次数达到阈值时，调用此方法锁定账户。
     *
     * @param userId      用户 ID
     * @param lockedUntil 锁定截止时间
     */
    public void setLockedUntil(@NonNull String userId, @NonNull LocalDateTime lockedUntil) {
        String sql = String.format(
                "UPDATE %s SET locked_until = ?, updated_at = ? WHERE user_id = ?",
                tableName
        );

        try {
            jdbcTemplate.update(sql, lockedUntil, LocalDateTime.now(), userId);
            log.debug("Set account locked until: userId={}, lockedUntil={}", userId, lockedUntil);
        } catch (DataAccessException e) {
            log.error("Failed to set locked until: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 清理过期的锁定记录。
     *
     * <p>删除所有锁定时间已过的记录。
     *
     * @return 清理的记录数
     */
    public int cleanExpiredLocks() {
        String sql = String.format(
                "DELETE FROM %s WHERE locked_until IS NOT NULL AND locked_until < ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now());
            if (rows > 0) {
                log.info("Cleaned expired lock records: count={}", rows);
            }
            return rows;
        } catch (DataAccessException e) {
            log.error("Failed to clean expired lock records", e);
            throw e;
        }
    }

    /**
     * FailureRecord RowMapper 实现。
     */
    private static class FailureRecordRowMapper implements RowMapper<FailureRecord> {
        @Override
        public FailureRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FailureRecord(
                    rs.getInt("failure_count"),
                    rs.getObject("locked_until", LocalDateTime.class),
                    rs.getString("last_ip")
            );
        }
    }
}
