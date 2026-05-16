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
import java.util.Optional;

/**
 * 基于 JDBC 的登录失败存储。
 *
 * <p>将登录失败记录持久化到关系型数据库，支持：
 * <ul>
 *   <li>记录登录失败次数</li>
 *   <li>账户锁定管理</li>
 *   <li>失败记录重置</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_login_failure (
 *     user_id VARCHAR(64) PRIMARY KEY,
 *     username VARCHAR(128) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     failure_count INT NOT NULL DEFAULT 0,
 *     locked_until TIMESTAMP,
 *     last_failure_ip VARCHAR(64),
 *     last_failure_at TIMESTAMP,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP NOT NULL,
 *     INDEX idx_username (username),
 *     INDEX idx_locked_until (locked_until)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcLoginFailureStorage implements AfgLoginFailureStorage {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_login_failure";

    /** 默认锁定阈值 */
    private static final int DEFAULT_LOCK_THRESHOLD = 5;

    /** 默认锁定时长（分钟） */
    private static final int DEFAULT_LOCK_DURATION_MINUTES = 30;

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final int lockThreshold;
    private final int lockDurationMinutes;

    /** RowMapper 用于将 ResultSet 映射为 FailureRecord */
    private final RowMapper<FailureRecord> rowMapper = new FailureRecordRowMapper();

    /**
     * 构造函数，使用默认表名和锁定策略。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcLoginFailureStorage(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME, DEFAULT_LOCK_THRESHOLD, DEFAULT_LOCK_DURATION_MINUTES);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcLoginFailureStorage(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this(jdbcTemplate, tableName, DEFAULT_LOCK_THRESHOLD, DEFAULT_LOCK_DURATION_MINUTES);
    }

    /**
     * 构造函数，使用自定义表名和锁定策略。
     *
     * @param jdbcTemplate        JDBC 模板
     * @param tableName           表名
     * @param lockThreshold       锁定阈值（失败多少次后锁定）
     * @param lockDurationMinutes 锁定时长（分钟）
     */
    public JdbcLoginFailureStorage(
            @NonNull JdbcTemplate jdbcTemplate,
            @NonNull String tableName,
            int lockThreshold,
            int lockDurationMinutes
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.lockThreshold = lockThreshold;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Override
    public void recordFailure(
            @NonNull String userId,
            @NonNull String username,
            @Nullable String tenantId,
            @Nullable String ip
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 先检查是否存在记录
        String checkSql = String.format("SELECT COUNT(*) FROM %s WHERE user_id = ?", tableName);
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);
        boolean exists = count != null && count > 0;

        if (exists) {
            // 更新现有记录
            String updateSql = String.format(
                    "UPDATE %s SET failure_count = failure_count + 1, last_failure_ip = ?, last_failure_at = ?, "
                            + "updated_at = ? WHERE user_id = ?",
                    tableName
            );
            jdbcTemplate.update(updateSql, ip, now, now, userId);
        } else {
            // 插入新记录
            String insertSql = String.format(
                    "INSERT INTO %s (user_id, username, tenant_id, failure_count, last_failure_ip, last_failure_at, "
                            + "created_at, updated_at) VALUES (?, ?, ?, 1, ?, ?, ?, ?)",
                    tableName
            );
            jdbcTemplate.update(insertSql, userId, username, tenantId, ip, now, now, now);
        }

        // 检查是否需要锁定
        int failureCount = getFailureCount(userId);
        if (failureCount >= lockThreshold) {
            LocalDateTime lockedUntil = now.plusMinutes(lockDurationMinutes);
            String lockSql = String.format(
                    "UPDATE %s SET locked_until = ?, updated_at = ? WHERE user_id = ?",
                    tableName
            );
            jdbcTemplate.update(lockSql, lockedUntil, now, userId);
            log.info("Account locked: userId={}, failureCount={}, lockedUntil={}", userId, failureCount, lockedUntil);
        }

        log.debug("Recorded login failure: userId={}, username={}, failureCount={}", userId, username, failureCount);
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
        String sql = String.format(
                "SELECT locked_until FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            LocalDateTime lockedUntil = jdbcTemplate.queryForObject(sql, LocalDateTime.class, userId);
            if (lockedUntil == null) {
                return false;
            }
            return lockedUntil.isAfter(LocalDateTime.now());
        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (DataAccessException e) {
            log.error("Failed to check lock status: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    @Nullable
    public LocalDateTime getLockedUntil(@NonNull String userId) {
        String sql = String.format(
                "SELECT locked_until FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            LocalDateTime lockedUntil = jdbcTemplate.queryForObject(sql, LocalDateTime.class, userId);
            if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                return lockedUntil;
            }
            return null;
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
                "UPDATE %s SET failure_count = 0, locked_until = NULL, updated_at = ? WHERE user_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), userId);
            if (rows > 0) {
                log.info("Unlocked account: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to unlock account: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public void reset(@NonNull String userId) {
        String sql = String.format(
                "UPDATE %s SET failure_count = 0, locked_until = NULL, updated_at = ? WHERE user_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), userId);
            if (rows > 0) {
                log.debug("Reset failure count: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to reset failure count: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取失败记录详情。
     *
     * @param userId 用户 ID
     * @return 失败记录，如果不存在则返回空
     */
    @NonNull
    public Optional<FailureRecord> getFailureRecord(@NonNull String userId) {
        String sql = String.format(
                "SELECT failure_count, locked_until, last_failure_ip FROM %s WHERE user_id = ?",
                tableName
        );

        try {
            FailureRecord record = jdbcTemplate.queryForObject(sql, rowMapper, userId);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to get failure record: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 删除用户的失败记录。
     *
     * @param userId 用户 ID
     */
    public void delete(@NonNull String userId) {
        String sql = String.format("DELETE FROM %s WHERE user_id = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, userId);
            if (rows > 0) {
                log.debug("Deleted failure record: userId={}", userId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to delete failure record: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 清理过期的锁定记录。
     *
     * <p>将已过期的锁定状态清除，但保留失败记录。
     *
     * @return 更新的记录数
     */
    public int clearExpiredLocks() {
        String sql = String.format(
                "UPDATE %s SET locked_until = NULL, updated_at = ? WHERE locked_until IS NOT NULL AND locked_until < ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), LocalDateTime.now());
            if (rows > 0) {
                log.info("Cleared expired locks: count={}", rows);
            }
            return rows;
        } catch (DataAccessException e) {
            log.error("Failed to clear expired locks", e);
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
                    rs.getString("last_failure_ip")
            );
        }
    }
}
