package io.github.afgprojects.framework.security.core.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AfgUserDetailsTest {

    @Nested
    @DisplayName("AfgUserDetails 接口测试")
    class AfgUserDetailsInterfaceTests {

        @Test
        @DisplayName("应获取用户 ID")
        void shouldGetUserId() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应获取用户名")
        void shouldGetUsername() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应获取租户 ID")
        void shouldGetTenantId() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应获取角色集合")
        void shouldGetRoles() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("应获取权限集合")
        void shouldGetAuthorities() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("read");
        }

        @Test
        @DisplayName("默认显示名称应返回用户名")
        void shouldReturnUsernameAsDisplayName() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.getDisplayName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("默认应不是管理员")
        void shouldNotBeAdminByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("默认账号应未过期")
        void shouldBeAccountNonExpiredByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("默认账号应未锁定")
        void shouldBeAccountNonLockedByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("默认凭证应未过期")
        void shouldBeCredentialsNonExpiredByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("默认应启用")
        void shouldBeEnabledByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");

            assertThat(userDetails.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("AfgAuthentication 接口测试")
    class AfgAuthenticationInterfaceTests {

        @Test
        @DisplayName("应获取用户详情")
        void shouldGetUserDetails() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getUserDetails()).isSameAs(userDetails);
        }

        @Test
        @DisplayName("应从用户详情获取用户 ID")
        void shouldGetUserIdFromUserDetails() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("应从用户详情获取用户名")
        void shouldGetNameFromUserDetails() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应从用户详情获取租户 ID")
        void shouldGetTenantIdFromUserDetails() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应从用户详情获取角色")
        void shouldGetRolesFromUserDetails() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("默认应已认证")
        void shouldBeAuthenticatedByDefault() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("principal 应返回用户详情")
        void shouldReturnUserDetailsAsPrincipal() {
            AfgUserDetails userDetails = createTestUserDetails("user-001", "testuser", "tenant-001");
            AfgAuthentication auth = createTestAuthentication(userDetails);

            assertThat(auth.getPrincipal()).isSameAs(userDetails);
        }
    }

    @Nested
    @DisplayName("AfgUserDetailsService 接口测试")
    class AfgUserDetailsServiceInterfaceTests {

        @Test
        @DisplayName("应根据用户名加载用户")
        void shouldLoadUserByUsername() {
            AfgUserDetailsService service = createTestUserDetailsService();
            AfgUserDetails userDetails = service.loadUserByUsername("testuser");

            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("应根据用户 ID 加载用户")
        void shouldLoadUserByUserId() {
            AfgUserDetailsService service = createTestUserDetailsService();
            AfgUserDetails userDetails = service.loadUserByUserId("user-001");

            assertThat(userDetails.getUserId()).isEqualTo("user-001");
        }

        @Test
        @DisplayName("默认应根据用户名加载用户（忽略租户）")
        void shouldLoadUserByUsernameAndTenantDefault() {
            AfgUserDetailsService service = createTestUserDetailsService();
            AfgUserDetails userDetails = service.loadUserByUsernameAndTenant("testuser", "tenant-001");

            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("默认手机号登录应抛出异常")
        void shouldThrowExceptionForMobileLoginByDefault() {
            AfgUserDetailsService service = createTestUserDetailsService();

            org.junit.jupiter.api.Assertions.assertThrows(
                    UsernameNotFoundException.class, () -> service.loadUserByMobile("13800138000"));
        }

        @Test
        @DisplayName("默认邮箱登录应抛出异常")
        void shouldThrowExceptionForEmailLoginByDefault() {
            AfgUserDetailsService service = createTestUserDetailsService();

            org.junit.jupiter.api.Assertions.assertThrows(
                    UsernameNotFoundException.class, () -> service.loadUserByEmail("test@example.com"));
        }
    }

    // ========== 测试辅助方法 ==========

    private AfgUserDetails createTestUserDetails(String userId, String username, String tenantId) {
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
            public @NonNull Set<String> getRoles() {
                return Set.of("ROLE_USER");
            }

            @Override
            public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
                return Set.of(new SimpleGrantedAuthority("read"));
            }

            @Override
            public String getPassword() {
                return "$2a$10$encrypted";
            }
        };
    }

    private AfgAuthentication createTestAuthentication(AfgUserDetails userDetails) {
        return new AfgAuthentication() {
            @Override
            public @NonNull AfgUserDetails getUserDetails() {
                return userDetails;
            }

            @Override
            public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
                return userDetails.getAuthorities();
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

    private AfgUserDetailsService createTestUserDetailsService() {
        return new AfgUserDetailsService() {
            @Override
            public @NonNull AfgUserDetails loadUserByUsername(@NonNull String username)
                    throws UsernameNotFoundException {
                if ("testuser".equals(username)) {
                    return createTestUserDetails("user-001", username, "tenant-001");
                }
                throw new UsernameNotFoundException("User not found: " + username);
            }

            @Override
            public @NonNull AfgUserDetails loadUserByUserId(@NonNull String userId)
                    throws UsernameNotFoundException {
                if ("user-001".equals(userId)) {
                    return createTestUserDetails(userId, "testuser", "tenant-001");
                }
                throw new UsernameNotFoundException("User not found: " + userId);
            }
        };
    }
}
