package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;

import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 基于 JDBC 的 Token 黑名单存储。
 *
 * <p>将 Token 黑名单持久化到关系型数据库，支持：
 * <ul>
 *   <li>Token 加入黑名单</li>
 *   <li>检查 Token 是否在黑名单中</li>
 *   <li>将用户所有 Token 加入黑名单</li>
 *   <li>自动清理过期记录</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_token_blacklist (
 *     token_hash VARCHAR(128) NOT NULL UNIQUE,
 *     user_id VARCHAR(64) NOT NULL,
 *     reason VARCHAR(100),
 *     expires_at TIMESTAMP NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id),
 *     INDEX idx_expires_at (expires_at)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcTokenBlacklist implements AfgTokenBlacklist {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_token_blacklist";

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /**
     * 构造函数，使用默认表名。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcTokenBlacklist(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcTokenBlacklist(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void addToBlacklist(
            @NonNull String tokenHash,
            @NonNull String userId,
            @NonNull String reason,
            @NonNull Duration ttl
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);

        try {
            // 先检查是否存在记录
            String checkSql = String.format("SELECT COUNT(*) FROM %s WHERE token_hash = ?", tableName);
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tokenHash);
            boolean exists = count != null && count > 0;

            if (exists) {
                // 更新现有记录
                String updateSql = String.format(
                        "UPDATE %s SET user_id = ?, reason = ?, expires_at = ?, created_at = ? WHERE token_hash = ?",
                        tableName
                );
                jdbcTemplate.update(updateSql, userId, reason, expiresAt, now, tokenHash);
            } else {
                // 插入新记录
                String insertSql = String.format(
                        "INSERT INTO %s (token_hash, user_id, reason, expires_at, created_at) VALUES (?, ?, ?, ?, ?)",
                        tableName
                );
                jdbcTemplate.update(insertSql, tokenHash, userId, reason, expiresAt, now);
            }
            log.debug("Added token to blacklist: tokenHash={}, userId={}, reason={}", tokenHash, userId, reason);
        } catch (DataAccessException e) {
            log.error("Failed to add token to blacklist: tokenHash={}, userId={}", tokenHash, userId, e);
            throw e;
        }
    }

    @Override
    public boolean isBlacklisted(@NonNull String tokenHash) {
        // 先清理过期记录，再查询
        deleteExpired();

        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE token_hash = ?",
                tableName
        );

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenHash);
            boolean blacklisted = count != null && count > 0;
            if (blacklisted) {
                log.debug("Token is blacklisted: tokenHash={}", tokenHash);
            }
            return blacklisted;
        } catch (DataAccessException e) {
            log.error("Failed to check if token is blacklisted: tokenHash={}", tokenHash, e);
            throw e;
        }
    }

    @Override
    public void blacklistAllUserTokens(@NonNull String userId, @NonNull Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);
        String reason = "user_logout_all";

        // 使用一个特殊的 token_hash 前缀来标记用户级别的黑名单
        // 当检查 token 时，需要同时检查用户级别的黑名单
        String userBlacklistTokenHash = "user_all:" + userId;

        try {
            // 先检查是否存在记录
            String checkSql = String.format("SELECT COUNT(*) FROM %s WHERE token_hash = ?", tableName);
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userBlacklistTokenHash);
            boolean exists = count != null && count > 0;

            if (exists) {
                // 更新现有记录
                String updateSql = String.format(
                        "UPDATE %s SET expires_at = ?, created_at = ? WHERE token_hash = ?",
                        tableName
                );
                jdbcTemplate.update(updateSql, expiresAt, now, userBlacklistTokenHash);
            } else {
                // 插入新记录
                String insertSql = String.format(
                        "INSERT INTO %s (token_hash, user_id, reason, expires_at, created_at) VALUES (?, ?, ?, ?, ?)",
                        tableName
                );
                jdbcTemplate.update(insertSql, userBlacklistTokenHash, userId, reason, expiresAt, now);
            }
            log.info("Blacklisted all tokens for user: userId={}, ttl={}", userId, ttl);
        } catch (DataAccessException e) {
            log.error("Failed to blacklist all tokens for user: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 删除过期的黑名单记录。
     *
     * <p>建议定期调用此方法或在查询时自动调用以清理过期数据。
     *
     * @return 删除的记录数
     */
    public int deleteExpired() {
        String sql = String.format("DELETE FROM %s WHERE expires_at < ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now());
            if (rows > 0) {
                log.debug("Deleted expired blacklist records: count={}", rows);
            }
            return rows;
        } catch (DataAccessException e) {
            log.error("Failed to delete expired blacklist records", e);
            throw e;
        }
    }

    /**
     * 检查用户是否被全局拉黑（所有 Token 都在黑名单中）。
     *
     * @param userId 用户 ID
     * @return 如果用户被全局拉黑则返回 true
     */
    public boolean isUserBlacklisted(@NonNull String userId) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE user_id = ? AND token_hash = ? AND expires_at > ?",
                tableName
        );

        try {
            String userBlacklistTokenHash = "user_all:" + userId;
            Integer count = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    userId,
                    userBlacklistTokenHash,
                    LocalDateTime.now()
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.error("Failed to check if user is blacklisted: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 检查 Token 是否在黑名单中（包括用户级别的黑名单检查）。
     *
     * <p>此方法会同时检查：
     * <ul>
     *   <li>单个 Token 是否在黑名单中</li>
     *   <li>用户是否被全局拉黑</li>
     * </ul>
     *
     * @param tokenHash Token 的哈希值
     * @param userId    用户 ID
     * @return 如果在黑名单中则返回 true
     */
    public boolean isBlacklistedWithUserCheck(@NonNull String tokenHash, @NonNull String userId) {
        // 先检查单个 Token
        if (isBlacklisted(tokenHash)) {
            return true;
        }
        // 再检查用户是否被全局拉黑
        return isUserBlacklisted(userId);
    }
}
