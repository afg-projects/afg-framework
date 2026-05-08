package io.github.afgprojects.framework.security.resource.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * JwtAuthenticationConverter 测试类。
 *
 * @since 1.0.0
 */
class JwtAuthenticationConverterTest {

    private JwtResourceProperties properties;
    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        properties = new JwtResourceProperties();
        converter = new JwtAuthenticationConverter(properties);
    }

    @Nested
    @DisplayName("convert 测试")
    class ConvertTests {

        @Test
        @DisplayName("应正确转换 JWT 到 AfgAuthentication")
        void shouldConvertJwtToAfgAuthentication() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("preferred_username", "testuser")
                    .claim("tenant_id", "tenant-123")
                    .claim("roles", List.of("admin", "user"))
                    .claim("permissions", List.of("read", "write"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
            jwtToken.setAuthenticated(true);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth).isNotNull();
            assertThat(afgAuth.getUserId()).isEqualTo("user-123");
            assertThat(afgAuth.getName()).isEqualTo("testuser");
            assertThat(afgAuth.getTenantId()).isEqualTo("tenant-123");
            assertThat(afgAuth.getRoles()).containsExactlyInAnyOrder("admin", "user");
            assertThat(afgAuth.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("JWT 无 username claim 时应使用 userId")
        void shouldUseUserIdWhenNoUsernameClaim() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getName()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("JWT 无 tenant_id 时应返回 null")
        void shouldReturnNullTenantIdWhenNotPresent() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getTenantId()).isNull();
        }

        @Test
        @DisplayName("roles 为空时应返回空集合")
        void shouldReturnEmptyRolesWhenNotPresent() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("roles 为逗号分隔字符串时应正确解析")
        void shouldParseCommaSeparatedRoles() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("roles", "admin,user,manager")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getRoles()).containsExactlyInAnyOrder("admin", "user", "manager");
        }

        @Test
        @DisplayName("JwtAuthenticationToken 为 null 时应抛出异常")
        void shouldThrowExceptionWhenJwtTokenIsNull() {
            assertThatThrownBy(() -> converter.convert(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("convertToUserDetails 测试")
    class ConvertToUserDetailsTests {

        @Test
        @DisplayName("应正确转换 JWT 到 AfgUserDetails")
        void shouldConvertJwtToUserDetails() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("preferred_username", "testuser")
                    .claim("tenant_id", "tenant-123")
                    .claim("roles", List.of("admin"))
                    .claim("permissions", List.of("read", "write"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgUserDetails userDetails = converter.convertToUserDetails(jwtToken);

            assertThat(userDetails.getUserId()).isEqualTo("user-123");
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getTenantId()).isEqualTo("tenant-123");
            assertThat(userDetails.getRoles()).containsExactly("admin");
        }

        @Test
        @DisplayName("UserDetails 的账户状态应全部为 true")
        void userDetailsAccountStatusShouldBeTrue() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgUserDetails userDetails = converter.convertToUserDetails(jwtToken);

            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.getPassword()).isNull();
        }
    }

    @Nested
    @DisplayName("自定义 claim 名称测试")
    class CustomClaimNameTests {

        @Test
        @DisplayName("使用自定义 userId claim")
        void shouldUseCustomUserIdClaim() {
            properties.setUserIdClaim("user_id");

            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("user_id", "custom-user-123")
                    .claim("sub", "default-sub")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getUserId()).isEqualTo("custom-user-123");
        }

        @Test
        @DisplayName("使用自定义 tenantId claim")
        void shouldUseCustomTenantIdClaim() {
            properties.setTenantIdClaim("custom_tenant");

            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("custom_tenant", "tenant-custom")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getTenantId()).isEqualTo("tenant-custom");
        }

        @Test
        @DisplayName("使用自定义 roles claim")
        void shouldUseCustomRolesClaim() {
            properties.setRolesClaim("authorities");

            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("authorities", List.of("ROLE_ADMIN", "ROLE_USER"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }
    }

    @Nested
    @DisplayName("权限合并测试")
    class AuthorityMergeTests {

        @Test
        @DisplayName("应合并 JWT 权限和角色")
        void shouldMergeJwtAuthoritiesAndRoles() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .claim("roles", List.of("admin", "user"))
                    .claim("permissions", List.of("read", "write"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority("SCOPE_api")));

            AfgAuthentication afgAuth = converter.convert(jwtToken);
            Collection<? extends GrantedAuthority> authorities = afgAuth.getAuthorities();

            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder(
                            "SCOPE_api",
                            "ROLE_admin",
                            "ROLE_user",
                            "read",
                            "write");
        }
    }

    @Nested
    @DisplayName("AfgAuthentication 委托测试")
    class AfgAuthenticationDelegateTests {

        @Test
        @DisplayName("getCredentials 应返回 JWT token 的 credentials")
        void shouldReturnJwtCredentials() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
            jwtToken.setAuthenticated(true);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getCredentials()).isEqualTo(jwtToken.getCredentials());
        }

        @Test
        @DisplayName("getDetails 应返回 JWT token 的 details")
        void shouldReturnJwtDetails() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
            jwtToken.setAuthenticated(true);
            jwtToken.setDetails("test-details");

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getDetails()).isEqualTo("test-details");
        }

        @Test
        @DisplayName("setAuthenticated 应委托给 JWT token")
        void shouldDelegateSetAuthenticated() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
            jwtToken.setAuthenticated(true);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.isAuthenticated()).isTrue();

            afgAuth.setAuthenticated(false);
            assertThat(afgAuth.isAuthenticated()).isFalse();
        }

        @Test
        @DisplayName("getPrincipal 应返回 UserDetails")
        void shouldReturnUserDetailsAsPrincipal() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-123")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);
            jwtToken.setAuthenticated(true);

            AfgAuthentication afgAuth = converter.convert(jwtToken);

            assertThat(afgAuth.getPrincipal()).isInstanceOf(AfgUserDetails.class);
            assertThat(((AfgUserDetails) afgAuth.getPrincipal()).getUserId()).isEqualTo("user-123");
        }
    }
}