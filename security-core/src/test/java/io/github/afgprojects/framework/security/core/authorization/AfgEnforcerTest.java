package io.github.afgprojects.framework.security.core.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;

class AfgEnforcerTest {

    @Nested
    @DisplayName("AfgEnforcer 接口测试")
    class AfgEnforcerInterfaceTests {

        @Test
        @DisplayName("应检查角色")
        void shouldCheckRole() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.hasRole("user-001", "ROLE_ADMIN")).isTrue();
            assertThat(enforcer.hasRole("user-001", "ROLE_USER")).isFalse();
        }

        @Test
        @DisplayName("应检查任意角色")
        void shouldCheckAnyRole() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.hasAnyRole("user-001", List.of("ROLE_USER", "ROLE_ADMIN"))).isTrue();
            assertThat(enforcer.hasAnyRole("user-001", List.of("ROLE_GUEST", "ROLE_USER"))).isFalse();
        }

        @Test
        @DisplayName("应检查所有角色")
        void shouldCheckAllRoles() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.hasAllRoles("user-001", List.of("ROLE_ADMIN"))).isTrue();
            assertThat(enforcer.hasAllRoles("user-001", List.of("ROLE_ADMIN", "ROLE_USER"))).isFalse();
        }

        @Test
        @DisplayName("应执行权限检查")
        void shouldEnforcePermission() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.enforce("user-001", "article", "read")).isTrue();
            assertThat(enforcer.enforce("user-001", "article", "delete")).isFalse();
        }

        @Test
        @DisplayName("应检查权限")
        void shouldCheckPermission() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.hasPermission("user-001", "article:read")).isTrue();
            assertThat(enforcer.hasPermission("user-001", "article:delete")).isFalse();
        }

        @Test
        @DisplayName("应检查资源操作权限")
        void shouldCheckResourceActionPermission() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.hasPermission("user-001", "article", "read")).isTrue();
        }

        @Test
        @DisplayName("应获取用户角色")
        void shouldGetRolesForUser() {
            AfgEnforcer enforcer = createTestEnforcer();
            List<String> roles = enforcer.getRolesForUser("user-001");

            assertThat(roles).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("应获取用户权限")
        void shouldGetPermissionsForUser() {
            AfgEnforcer enforcer = createTestEnforcer();
            List<String> permissions = enforcer.getPermissionsForUser("user-001");

            assertThat(permissions).contains("article:read");
        }

        @Test
        @DisplayName("应添加角色")
        void shouldAddRoleForUser() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.addRoleForUser("user-001", "ROLE_USER")).isTrue();
        }

        @Test
        @DisplayName("应删除角色")
        void shouldDeleteRoleForUser() {
            AfgEnforcer enforcer = createTestEnforcer();

            assertThat(enforcer.deleteRoleForUser("user-001", "ROLE_ADMIN")).isTrue();
        }
    }

    @Nested
    @DisplayName("AfgSecurityContext 接口测试")
    class AfgSecurityContextInterfaceTests {

        @Test
        @DisplayName("应获取认证信息")
        void shouldGetAuthentication() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.getAuthentication()).isNotNull();
            assertThat(context.getAuthentication().getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应判断是否已认证")
        void shouldCheckAuthenticated() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("未认证时应返回 false")
        void shouldReturnFalseWhenNotAuthenticated() {
            AfgSecurityContext context = createEmptySecurityContext();

            assertThat(context.isAuthenticated()).isFalse();
        }

        @Test
        @DisplayName("应获取当前用户")
        void shouldGetCurrentUser() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.getCurrentUser()).isNotNull();
            assertThat(context.getCurrentUser().getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应获取当前用户 ID")
        void shouldGetCurrentUserId() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.getCurrentUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应获取当前用户名")
        void shouldGetCurrentUsername() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.getCurrentUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应获取当前租户 ID")
        void shouldGetCurrentTenantId() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            assertThat(context.getCurrentTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("未认证时获取用户应抛出异常")
        void shouldThrowExceptionWhenNotAuthenticated() {
            AfgSecurityContext context = createEmptySecurityContext();

            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class, context::getRequiredCurrentUser);
        }

        @Test
        @DisplayName("未认证时获取用户 ID 应抛出异常")
        void shouldThrowExceptionWhenGetUserIdNotAuthenticated() {
            AfgSecurityContext context = createEmptySecurityContext();

            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class, context::getRequiredCurrentUserId);
        }

        @Test
        @DisplayName("应清除上下文")
        void shouldClearContext() {
            AfgSecurityContext context = createTestSecurityContext("user-001", "testuser");

            context.clear();

            assertThat(context.getAuthentication()).isNull();
            assertThat(context.isAuthenticated()).isFalse();
        }
    }

    // ========== 测试辅助方法 ==========

    private AfgEnforcer createTestEnforcer() {
        return new AfgEnforcer() {
            @Override
            public boolean hasRole(@NonNull String userId, @NonNull String role) {
                return "user-001".equals(userId) && "ROLE_ADMIN".equals(role);
            }

            @Override
            public boolean hasAnyRole(@NonNull String userId, @NonNull List<String> roles) {
                return roles.stream().anyMatch(role -> hasRole(userId, role));
            }

            @Override
            public boolean hasAllRoles(@NonNull String userId, @NonNull List<String> roles) {
                return roles.stream().allMatch(role -> hasRole(userId, role));
            }

            @Override
            public boolean enforce(@NonNull String subject, @NonNull String resource, @NonNull String action) {
                return "user-001".equals(subject) && "article".equals(resource) && "read".equals(action);
            }

            @Override
            public boolean enforce(
                    @NonNull String subject,
                    @NonNull String resource,
                    @NonNull String action,
                    @Nullable Map<String, String> context) {
                return enforce(subject, resource, action);
            }

            @Override
            public boolean hasPermission(@NonNull String userId, @NonNull String permission) {
                return "user-001".equals(userId) && "article:read".equals(permission);
            }

            @Override
            public boolean hasAnyPermission(@NonNull String userId, @NonNull List<String> permissions) {
                return permissions.stream().anyMatch(p -> hasPermission(userId, p));
            }

            @Override
            public @NonNull List<String> getRolesForUser(@NonNull String userId) {
                return "user-001".equals(userId) ? List.of("ROLE_ADMIN") : List.of();
            }

            @Override
            public @NonNull List<String> getPermissionsForUser(@NonNull String userId) {
                return "user-001".equals(userId) ? List.of("article:read", "article:write") : List.of();
            }

            @Override
            public boolean addRoleForUser(@NonNull String userId, @NonNull String role) {
                return true;
            }

            @Override
            public boolean deleteRoleForUser(@NonNull String userId, @NonNull String role) {
                return true;
            }

            @Override
            public boolean addPermissionForRole(@NonNull String role, @NonNull String permission) {
                return true;
            }

            @Override
            public boolean deletePermissionForRole(@NonNull String role, @NonNull String permission) {
                return true;
            }

            @Override
            public boolean deleteRolesForUser(@NonNull String userId) {
                return true;
            }

            @Override
            public boolean deleteRole(@NonNull String role) {
                return true;
            }

            @Override
            public void clear() {}
        };
    }

    private AfgSecurityContext createTestSecurityContext(String userId, String username) {
        return new AfgSecurityContext() {
            private AfgAuthentication authentication = createTestAuthentication(userId, username, "tenant-001");

            @Override
            public @Nullable AfgAuthentication getAuthentication() {
                return authentication;
            }

            @Override
            public void setAuthentication(@Nullable AfgAuthentication authentication) {
                this.authentication = authentication;
            }
        };
    }

    private AfgSecurityContext createEmptySecurityContext() {
        return new AfgSecurityContext() {
            private AfgAuthentication authentication = null;

            @Override
            public @Nullable AfgAuthentication getAuthentication() {
                return authentication;
            }

            @Override
            public void setAuthentication(@Nullable AfgAuthentication authentication) {
                this.authentication = authentication;
            }
        };
    }

    private AfgAuthentication createTestAuthentication(String userId, String username, String tenantId) {
        return new AfgAuthentication() {
            @Override
            public @NonNull AfgUserDetails getUserDetails() {
                return new AfgUserDetails() {
                    @Override
                    public @NonNull String getUserId() {
                        return userId;
                    }

                    @Override
                    public @NonNull String getUsername() {
                        return username;
                    }

                    @Override
                    public @Nullable String getTenantId() {
                        return tenantId;
                    }

                    @Override
                    public java.util.@NonNull Set<String> getRoles() {
                        return java.util.Set.of("ROLE_ADMIN");
                    }

                    @Override
                    public java.util.@NonNull Collection<? extends org.springframework.security.core.GrantedAuthority>
                            getAuthorities() {
                        return java.util.Set.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("article:read"));
                    }

                    @Override
                    public String getPassword() {
                        return "$2a$10$encrypted";
                    }
                };
            }

            @Override
            public java.util.@NonNull Collection<? extends org.springframework.security.core.GrantedAuthority>
                    getAuthorities() {
                return getUserDetails().getAuthorities();
            }

            @Override
            public @Nullable Object getCredentials() {
                return null;
            }

            @Override
            public @Nullable Object getDetails() {
                return null;
            }
        };
    }
}
