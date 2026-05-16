package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * JDBC 角色权限存储实现。
 *
 * <p>基于关系型数据库的持久化存储实现，适用于生产环境。
 *
 * <h3>表结构要求</h3>
 * <pre>{@code
 * CREATE TABLE auth_user_role (
 *     id BIGINT PRIMARY KEY,
 *     user_id VARCHAR(64) NOT NULL,
 *     role_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     UNIQUE(user_id, role_id, tenant_id)
 * );
 *
 * CREATE TABLE auth_role_permission (
 *     id BIGINT PRIMARY KEY,
 *     role_id VARCHAR(64) NOT NULL,
 *     permission VARCHAR(128) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     UNIQUE(role_id, permission, tenant_id)
 * );
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcRolePermissionStorage implements RolePermissionStorage {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRolePermissionStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @NonNull
    public Set<String> findRolesByUserId(@NonNull String userId) {
        String sql = "SELECT role_id FROM auth_user_role WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, userId));
    }

    @Override
    @NonNull
    public Set<String> findPermissionsByRole(@NonNull String role) {
        String sql = "SELECT permission FROM auth_role_permission WHERE role_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, role));
    }

    @Override
    @NonNull
    public Set<String> findPermissionsByUserId(@NonNull String userId) {
        String sql = """
            SELECT rp.permission
            FROM auth_role_permission rp
            INNER JOIN auth_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = ?
            """;
        return new HashSet<>(jdbcTemplate.queryForList(sql, String.class, userId));
    }

    @Override
    public void grantRole(@NonNull String userId, @NonNull String role) {
        String sql = "INSERT INTO auth_user_role (id, user_id, role_id) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM auth_user_role), ?, ?)";
        jdbcTemplate.update(sql, userId, role);
        log.debug("Granted role: userId={}, role={}", userId, role);
    }

    @Override
    public void revokeRole(@NonNull String userId, @NonNull String role) {
        String sql = "DELETE FROM auth_user_role WHERE user_id = ? AND role_id = ?";
        int rows = jdbcTemplate.update(sql, userId, role);
        log.debug("Revoked role: userId={}, role={}, rows={}", userId, role, rows);
    }

    @Override
    public void grantPermission(@NonNull String role, @NonNull String permission) {
        String sql = "INSERT INTO auth_role_permission (id, role_id, permission) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM auth_role_permission), ?, ?)";
        jdbcTemplate.update(sql, role, permission);
        log.debug("Granted permission: role={}, permission={}", role, permission);
    }

    @Override
    public void revokePermission(@NonNull String role, @NonNull String permission) {
        String sql = "DELETE FROM auth_role_permission WHERE role_id = ? AND permission = ?";
        int rows = jdbcTemplate.update(sql, role, permission);
        log.debug("Revoked permission: role={}, permission={}, rows={}", role, permission, rows);
    }

    @Override
    public boolean hasRole(@NonNull String userId, @NonNull String role) {
        String sql = "SELECT COUNT(*) FROM auth_user_role WHERE user_id = ? AND role_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, role);
        return count != null && count > 0;
    }

    @Override
    public boolean hasPermission(@NonNull String role, @NonNull String permission) {
        String sql = "SELECT COUNT(*) FROM auth_role_permission WHERE role_id = ? AND permission = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, role, permission);
        return count != null && count > 0;
    }
}
