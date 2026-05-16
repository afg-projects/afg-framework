package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 基于 JDBC 的验证码存储。
 *
 * <p>将验证码持久化到关系型数据库，支持：
 * <ul>
 *   <li>验证码保存与查询</li>
 *   <li>TTL 过期机制</li>
 *   <li>验证码删除</li>
 *   <li>过期验证码清理</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_captcha (
 *     captcha_key VARCHAR(100) NOT NULL UNIQUE,
 *     captcha_value VARCHAR(50) NOT NULL,
 *     expires_at TIMESTAMP NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     INDEX idx_expires_at (expires_at)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcCaptchaStorage implements AfgCaptchaStorage {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_captcha";

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /**
     * 构造函数，使用默认表名。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcCaptchaStorage(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcCaptchaStorage(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void save(@NonNull String key, @NonNull String value, @NonNull Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);

        String sql = String.format(
                "INSERT INTO %s (captcha_key, captcha_value, expires_at, created_at) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE captcha_value = ?, expires_at = ?, created_at = ?",
                tableName
        );

        try {
            jdbcTemplate.update(
                    sql,
                    key,
                    value,
                    Timestamp.valueOf(expiresAt),
                    Timestamp.valueOf(now),
                    value,
                    Timestamp.valueOf(expiresAt),
                    Timestamp.valueOf(now)
            );
            log.debug("Saved captcha: key={}, ttl={}s", key, ttl.getSeconds());
        } catch (DataAccessException e) {
            log.error("Failed to save captcha: key={}", key, e);
            throw e;
        }
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        // 先清理过期数据，再查询
        deleteExpired();

        String sql = String.format(
                "SELECT captcha_value FROM %s WHERE captcha_key = ? AND expires_at > ?",
                tableName
        );

        try {
            String value = jdbcTemplate.queryForObject(
                    sql,
                    String.class,
                    key,
                    Timestamp.valueOf(LocalDateTime.now())
            );
            return value;
        } catch (EmptyResultDataAccessException e) {
            log.debug("Captcha not found or expired: key={}", key);
            return null;
        } catch (DataAccessException e) {
            log.error("Failed to get captcha: key={}", key, e);
            throw e;
        }
    }

    @Override
    public void delete(@NonNull String key) {
        String sql = String.format("DELETE FROM %s WHERE captcha_key = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, key);
            if (rows > 0) {
                log.debug("Deleted captcha: key={}", key);
            }
        } catch (DataAccessException e) {
            log.error("Failed to delete captcha: key={}", key, e);
            throw e;
        }
    }

    @Override
    public boolean exists(@NonNull String key) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE captcha_key = ? AND expires_at > ?",
                tableName
        );

        try {
            Integer count = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    key,
                    Timestamp.valueOf(LocalDateTime.now())
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.error("Failed to check captcha existence: key={}", key, e);
            throw e;
        }
    }

    /**
     * 清理所有过期的验证码。
     *
     * @return 删除的记录数
     */
    public int deleteExpired() {
        String sql = String.format("DELETE FROM %s WHERE expires_at < ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()));
            if (rows > 0) {
                log.debug("Deleted expired captchas: count={}", rows);
            }
            return rows;
        } catch (DataAccessException e) {
            log.error("Failed to delete expired captchas", e);
            throw e;
        }
    }
}
