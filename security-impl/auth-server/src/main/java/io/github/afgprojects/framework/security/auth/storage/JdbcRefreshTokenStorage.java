package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.auth.entity.AuthRefreshToken;
import io.github.afgprojects.framework.security.core.storage.AfgRefreshTokenStorage;

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
 * 基于 JDBC 的 Refresh Token 存储。
 *
 * <p>将 Refresh Token 持久化到关系型数据库，支持：
 * <ul>
 *   <li>Token 保存与查询</li>
 *   <li>Token 撤销（删除）</li>
 *   <li>按用户删除所有 Token</li>
 *   <li>清理过期 Token</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_refresh_token (
 *     token_id VARCHAR(64) PRIMARY KEY,
 *     token_hash VARCHAR(128) NOT NULL UNIQUE,
 *     user_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     client_id VARCHAR(128),
 *     device_id VARCHAR(128),
 *     expires_at TIMESTAMP NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id),
 *     INDEX idx_token_hash (token_hash),
 *     INDEX idx_expires_at (expires_at)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcRefreshTokenStorage implements AfgRefreshTokenStorage {
    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_refresh_token";

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /** RowMapper 用于将 ResultSet 映射为 RefreshTokenInfo */
    private final RowMapper<RefreshTokenInfo> rowMapper = new RefreshTokenInfoRowMapper();

    /**
     * 构造函数，使用默认表名。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcRefreshTokenStorage(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcRefreshTokenStorage(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void save(
            @NonNull String tokenId,
            @NonNull String tokenHash,
            @NonNull String userId,
            @Nullable String tenantId,
            @Nullable String clientId,
            @Nullable String deviceId,
            @NonNull LocalDateTime expiresAt
    ) {
        LocalDateTime createdAt = LocalDateTime.now();

        String sql = String.format(
                "INSERT INTO %s (token_id, token_hash, user_id, tenant_id, client_id, device_id, expires_at, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tableName
        );

        try {
            jdbcTemplate.update(
                    sql,
                    tokenId,
                    tokenHash,
                    userId,
                    tenantId,
                    clientId,
                    deviceId,
                    expiresAt,
                    createdAt
            );
            log.debug("Saved refresh token: tokenId={}, userId={}", tokenId, userId);
        } catch (DataAccessException e) {
            log.error("Failed to save refresh token: tokenId={}, userId={}", tokenId, userId, e);
            throw e;
        }
    }

    @Override
    @NonNull
    public Optional<RefreshTokenInfo> findByTokenHash(@NonNull String tokenHash) {
        String sql = String.format(
                "SELECT token_id, token_hash, user_id, tenant_id, client_id, device_id, expires_at, created_at "
                        + "FROM %s WHERE token_hash = ?",
                tableName
        );

        try {
            RefreshTokenInfo info = jdbcTemplate.queryForObject(sql, rowMapper, tokenHash);
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find refresh token by hash", e);
            throw e;
        }
    }

    @Override
    @NonNull
    public Optional<RefreshTokenInfo> findByTokenId(@NonNull String tokenId) {
        String sql = String.format(
                "SELECT token_id, token_hash, user_id, tenant_id, client_id, device_id, expires_at, created_at "
                        + "FROM %s WHERE token_id = ?",
                tableName
        );

        try {
            RefreshTokenInfo info = jdbcTemplate.queryForObject(sql, rowMapper, tokenId);
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find refresh token by id", e);
            throw e;
        }
    }

    @Override
    public void delete(@NonNull String tokenId) {
        String sql = String.format("DELETE FROM %s WHERE token_id = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, tokenId);
            if (rows > 0) {
                log.debug("Deleted refresh token: tokenId={}", tokenId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to delete refresh token: tokenId={}", tokenId, e);
            throw e;
        }
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        String sql = String.format("DELETE FROM %s WHERE user_id = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, userId);
            log.debug("Deleted all refresh tokens for user: userId={}, count={}", userId, rows);
        } catch (DataAccessException e) {
            log.error("Failed to delete refresh tokens by user: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public int deleteExpired() {
        String sql = String.format("DELETE FROM %s WHERE expires_at < ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, LocalDateTime.now());
            if (rows > 0) {
                log.info("Deleted expired refresh tokens: count={}", rows);
            }
            return rows;
        } catch (DataAccessException e) {
            log.error("Failed to delete expired refresh tokens", e);
            throw e;
        }
    }

    /**
     * RefreshTokenInfo RowMapper 实现。
     */
    private static class RefreshTokenInfoRowMapper implements RowMapper<RefreshTokenInfo> {
        @Override
        public RefreshTokenInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RefreshTokenInfo(
                    rs.getString("token_id"),
                    rs.getString("token_hash"),
                    rs.getString("user_id"),
                    rs.getString("tenant_id"),
                    rs.getString("client_id"),
                    rs.getString("device_id"),
                    rs.getObject("expires_at", LocalDateTime.class),
                    rs.getObject("created_at", LocalDateTime.class)
            );
        }
    }
}
