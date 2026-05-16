package io.github.afgprojects.framework.security.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.security.core.permission.RolePermissionStorage;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

/**
 * JDBC 角色权限存储测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class JdbcRolePermissionStorageTest {

    private JdbcTemplate jdbcTemplate;
    private RolePermissionStorage storage;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_role_permission;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建表
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_user_role");
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_role_permission");

        jdbcTemplate.execute("""
            CREATE TABLE auth_user_role (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id VARCHAR(64) NOT NULL,
                role_id VARCHAR(64) NOT NULL,
                tenant_id VARCHAR(64),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE auth_role_permission (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                role_id VARCHAR(64) NOT NULL,
                permission VARCHAR(128) NOT NULL,
                tenant_id VARCHAR(64),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
            )
            """);

        storage = new JdbcRolePermissionStorage(jdbcTemplate);
    }

    @Nested
    @DisplayName("角色查询测试")
    class RoleQueryTests {

        @Test
        @DisplayName("应获取用户的所有角色")
        void shouldGetUserRoles() {
            // given - 预置数据
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (1, 'user-001', 'ADMIN')");
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (2, 'user-001', 'USER')");

            // when
            Set<String> roles = storage.findRolesByUserId("user-001");

            // then
            assertThat(roles).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("用户无角色时应返回空集合")
        void shouldReturnEmptyWhenNoRoles() {
            // when
            Set<String> roles = storage.findRolesByUserId("user-no-roles");

            // then
            assertThat(roles).isEmpty();
        }
    }

    @Nested
    @DisplayName("权限查询测试")
    class PermissionQueryTests {

        @Test
        @DisplayName("应获取角色的所有权限")
        void shouldGetRolePermissions() {
            // given
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (1, 'ADMIN', 'user:read')");
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (2, 'ADMIN', 'user:write')");

            // when
            Set<String> permissions = storage.findPermissionsByRole("ADMIN");

            // then
            assertThat(permissions).containsExactlyInAnyOrder("user:read", "user:write");
        }

        @Test
        @DisplayName("角色无权限时应返回空集合")
        void shouldReturnEmptyWhenNoPermissions() {
            // when
            Set<String> permissions = storage.findPermissionsByRole("NO_PERMISSION_ROLE");

            // then
            assertThat(permissions).isEmpty();
        }

        @Test
        @DisplayName("应获取用户的所有权限（通过角色关联）")
        void shouldGetUserPermissions() {
            // given
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (1, 'user-001', 'ADMIN')");
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (2, 'user-001', 'USER')");
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (1, 'ADMIN', 'user:read')");
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (2, 'ADMIN', 'user:write')");
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (3, 'USER', 'profile:read')");

            // when
            Set<String> permissions = storage.findPermissionsByUserId("user-001");

            // then
            assertThat(permissions).containsExactlyInAnyOrder("user:read", "user:write", "profile:read");
        }
    }

    @Nested
    @DisplayName("角色授权测试")
    class RoleGrantTests {

        @Test
        @DisplayName("应授予用户角色")
        void shouldGrantRole() {
            // when
            storage.grantRole("user-001", "ADMIN");

            // then
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, "user-001", "ADMIN");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应撤销用户角色")
        void shouldRevokeRole() {
            // given
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (1, 'user-001', 'ADMIN')");

            // when
            storage.revokeRole("user-001", "ADMIN");

            // then
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, "user-001", "ADMIN");
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("权限授权测试")
    class PermissionGrantTests {

        @Test
        @DisplayName("应授予角色权限")
        void shouldGrantPermission() {
            // when
            storage.grantPermission("ADMIN", "user:delete");

            // then
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_role_permission WHERE role_id = ? AND permission = ?",
                Integer.class, "ADMIN", "user:delete");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应撤销角色权限")
        void shouldRevokePermission() {
            // given
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (1, 'ADMIN', 'user:delete')");

            // when
            storage.revokePermission("ADMIN", "user:delete");

            // then
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_role_permission WHERE role_id = ? AND permission = ?",
                Integer.class, "ADMIN", "user:delete");
            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("角色检查测试")
    class RoleCheckTests {

        @Test
        @DisplayName("应检查用户是否具有角色")
        void shouldCheckUserRole() {
            // given
            jdbcTemplate.update("INSERT INTO auth_user_role (id, user_id, role_id) VALUES (1, 'user-001', 'ADMIN')");

            // when & then
            assertThat(storage.hasRole("user-001", "ADMIN")).isTrue();
            assertThat(storage.hasRole("user-001", "USER")).isFalse();
        }
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionCheckTests {

        @Test
        @DisplayName("应检查角色是否具有权限")
        void shouldCheckRolePermission() {
            // given
            jdbcTemplate.update("INSERT INTO auth_role_permission (id, role_id, permission) VALUES (1, 'ADMIN', 'user:read')");

            // when & then
            assertThat(storage.hasPermission("ADMIN", "user:read")).isTrue();
            assertThat(storage.hasPermission("ADMIN", "user:delete")).isFalse();
        }
    }
}
