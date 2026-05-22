package io.github.afgprojects.framework.security.auth.casbin;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.casbin.jcasbin.main.Enforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.afgprojects.framework.security.core.permission.PermissionService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Casbin 权限服务测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class CasbinPermissionServiceTest {

    @TempDir
    Path tempDir;

    private Enforcer enforcer;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() throws Exception {
        // 创建临时模型文件
        Path modelPath = tempDir.resolve("model.conf");
        String modelText = """
                [request_definition]
                r = sub, obj, act

                [policy_definition]
                p = sub, obj, act

                [role_definition]
                g = _, _

                [policy_effect]
                e = some(where (p.eft == allow))

                [matchers]
                m = g(r.sub, p.sub) && r.obj == p.obj && r.act == p.act
                """;
        Files.writeString(modelPath, modelText);

        // 创建空策略文件
        Path policyPath = tempDir.resolve("policy.csv");
        Files.writeString(policyPath, "");

        enforcer = new Enforcer(modelPath.toString(), policyPath.toString());
        permissionService = new CasbinPermissionService(enforcer);

        // 初始化测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 添加角色
        enforcer.addRoleForUser("admin-user", "ADMIN");
        enforcer.addRoleForUser("normal-user", "USER");

        // 添加权限策略
        enforcer.addPolicy("ADMIN", "user", "read");
        enforcer.addPolicy("ADMIN", "user", "write");
        enforcer.addPolicy("ADMIN", "user", "delete");
        enforcer.addPolicy("USER", "user", "read");
    }

    @Nested
    @DisplayName("角色检查测试")
    class RoleCheckTests {

        @Test
        @DisplayName("应检查用户是否具有角色")
        void shouldCheckUserRole() {
            assertThat(permissionService.hasRole("admin-user", "ADMIN")).isTrue();
            assertThat(permissionService.hasRole("admin-user", "USER")).isFalse();
            assertThat(permissionService.hasRole("normal-user", "USER")).isTrue();
        }

        @Test
        @DisplayName("应获取用户所有角色")
        void shouldGetUserRoles() {
            Set<String> roles = permissionService.getRoles("admin-user");

            assertThat(roles).contains("ADMIN");
        }
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionCheckTests {

        @Test
        @DisplayName("应检查用户是否具有权限")
        void shouldCheckUserPermission() {
            assertThat(permissionService.hasPermission("admin-user", "user:read")).isTrue();
            assertThat(permissionService.hasPermission("admin-user", "user:delete")).isTrue();
            assertThat(permissionService.hasPermission("normal-user", "user:read")).isTrue();
            assertThat(permissionService.hasPermission("normal-user", "user:delete")).isFalse();
        }

        @Test
        @DisplayName("应检查用户是否具有任意权限")
        void shouldCheckAnyPermission() {
            assertThat(permissionService.hasAnyPermission("admin-user", Set.of("user:read", "user:delete"))).isTrue();
            assertThat(permissionService.hasAnyPermission("normal-user", Set.of("user:delete", "role:read"))).isFalse();
        }
    }

    @Nested
    @DisplayName("ABAC 检查测试")
    class AbacCheckTests {

        @Test
        @DisplayName("应检查用户对资源的操作权限")
        void shouldCheckResourcePermission() {
            assertThat(permissionService.check("admin-user", "read", "user")).isTrue();
            assertThat(permissionService.check("normal-user", "delete", "user")).isFalse();
        }
    }

    @Nested
    @DisplayName("角色授权测试")
    class RoleGrantTests {

        @Test
        @DisplayName("应授予用户角色")
        void shouldGrantRole() {
            permissionService.grantRole("new-user", "USER");

            assertThat(permissionService.hasRole("new-user", "USER")).isTrue();
        }

        @Test
        @DisplayName("应撤销用户角色")
        void shouldRevokeRole() {
            permissionService.revokeRole("admin-user", "ADMIN");

            assertThat(permissionService.hasRole("admin-user", "ADMIN")).isFalse();
        }
    }

    @Nested
    @DisplayName("权限授权测试")
    class PermissionGrantTests {

        @Test
        @DisplayName("应授予角色权限")
        void shouldGrantPermission() {
            enforcer.addPolicy("USER", "profile", "edit");

            assertThat(permissionService.hasPermission("normal-user", "profile:edit")).isTrue();
        }
    }
}