package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC 数据权限服务实现。
 *
 * <p>基于关系型数据库的持久化存储实现，适用于生产环境。
 *
 * <h3>表结构要求</h3>
 * <pre>{@code
 * CREATE TABLE auth_data_scope (
 *     id BIGINT PRIMARY KEY,
 *     user_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL',
 *     scope_value VARCHAR(256),
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     UNIQUE(user_id, tenant_id)
 * );
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcDataScopeService implements DataScopeService {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDataScopeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @NonNull
    public DataScope getDataScope(@NonNull String userId, @Nullable String tenantId) {
        String sql;
        Object[] args;

        if (tenantId != null) {
            sql = "SELECT scope_type, scope_value FROM auth_data_scope WHERE user_id = ? AND tenant_id = ?";
            args = new Object[]{userId, tenantId};
        } else {
            sql = "SELECT scope_type, scope_value FROM auth_data_scope WHERE user_id = ? AND tenant_id IS NULL";
            args = new Object[]{userId};
        }

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                DataScopeType type = DataScopeType.valueOf(rs.getString("scope_type"));
                String value = rs.getString("scope_value");
                return DataScope.builder()
                    .scopeType(type)
                    .customCondition(value)
                    .build();
            }
            return DataScope.builder()
                .scopeType(DataScopeType.ALL)
                .build();
        }, args);
    }

    @Override
    public void setDataScope(@NonNull String userId, @Nullable String tenantId, @NonNull DataScope scope) {
        // 先删除再插入（H2 不支持 MERGE 的复杂语法）
        removeDataScope(userId, tenantId);

        String sql;
        Object[] args;

        if (tenantId != null) {
            sql = "INSERT INTO auth_data_scope (id, user_id, tenant_id, scope_type, scope_value) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM auth_data_scope), ?, ?, ?, ?)";
            args = new Object[]{userId, tenantId, scope.scopeType().name(), scope.customCondition()};
        } else {
            sql = "INSERT INTO auth_data_scope (id, user_id, scope_type, scope_value) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM auth_data_scope), ?, ?, ?)";
            args = new Object[]{userId, scope.scopeType().name(), scope.customCondition()};
        }

        jdbcTemplate.update(sql, args);
        log.debug("Set data scope: userId={}, tenantId={}, scope={}", userId, tenantId, scope);
    }

    @Override
    public void removeDataScope(@NonNull String userId, @Nullable String tenantId) {
        String sql;
        Object[] args;

        if (tenantId != null) {
            sql = "DELETE FROM auth_data_scope WHERE user_id = ? AND tenant_id = ?";
            args = new Object[]{userId, tenantId};
        } else {
            sql = "DELETE FROM auth_data_scope WHERE user_id = ? AND tenant_id IS NULL";
            args = new Object[]{userId};
        }

        int rows = jdbcTemplate.update(sql, args);
        log.debug("Removed data scope: userId={}, tenantId={}, rows={}", userId, tenantId, rows);
    }
}
