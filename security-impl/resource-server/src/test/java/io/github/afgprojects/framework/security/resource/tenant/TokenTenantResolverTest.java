package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * TokenTenantResolver 测试类。
 *
 * @since 1.0.0
 */
class TokenTenantResolverTest {

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造应使用 tenant_id claim")
        void shouldUseDefaultTenantIdClaim() {
            TokenTenantResolver resolver = new TokenTenantResolver();
            assertThat(resolver.getTenantIdClaim()).isEqualTo("tenant_id");
        }

        @Test
        @DisplayName("自定义 tenantId claim")
        void shouldUseCustomTenantIdClaim() {
            TokenTenantResolver resolver = new TokenTenantResolver("custom_tenant");
            assertThat(resolver.getTenantIdClaim()).isEqualTo("custom_tenant");
        }

        @Test
        @DisplayName("null claim 名称应使用默认值")
        void shouldUseDefaultWhenClaimIsNull() {
            TokenTenantResolver resolver = new TokenTenantResolver(null);
            assertThat(resolver.getTenantIdClaim()).isEqualTo("tenant_id");
        }
    }

    @Nested
    @DisplayName("resolve 测试")
    class ResolveTests {

        @Test
        @DisplayName("SecurityContext 有 JwtAuthenticationToken 时应解析成功")
        void shouldResolveWhenJwtTokenPresent() {
            // Setup JWT with tenant_id claim
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("tenant_id", "tenant-123")
                    .claim("sub", "user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            // Setup SecurityContext
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtToken);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("JWT 中无 tenant_id claim 时应返回 null")
        void shouldReturnNullWhenNoTenantIdClaim() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("sub", "user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtToken);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("SecurityContext 为空时应返回 null")
        void shouldReturnNullWhenSecurityContextEmpty() {
            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Authentication 不是 JwtAuthenticationToken 时应返回 null")
        void shouldReturnNullWhenNotJwtToken() {
            Authentication auth = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("tenant_id 为空字符串时应返回 null")
        void shouldReturnNullWhenTenantIdIsBlank() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("tenant_id", "")
                    .claim("sub", "user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtToken);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("应解析 tenant_code 和 tenant_name")
        void shouldResolveTenantCodeAndName() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("tenant_id", "tenant-123")
                    .claim("tenant_code", "acme")
                    .claim("tenant_name", "ACME Corp")
                    .claim("sub", "user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtToken);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
            assertThat(context.getAttributes())
                    .containsEntry("tenantCode", "acme")
                    .containsEntry("tenantName", "ACME Corp");
        }

        @Test
        @DisplayName("使用自定义 claim 名称解析")
        void shouldResolveWithCustomClaimName() {
            Jwt jwt = Jwt.withTokenValue("token-value")
                    .header("alg", "RS256")
                    .claim("custom_tenant", "tenant-456")
                    .claim("sub", "user-1")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(jwtToken);
            SecurityContextHolder.setContext(securityContext);

            HttpServletRequest request = mock(HttpServletRequest.class);

            TokenTenantResolver resolver = new TokenTenantResolver("custom_tenant");
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-456");
        }
    }

    @Nested
    @DisplayName("resolveFromToken 测试")
    class ResolveFromTokenTests {

        @Test
        @DisplayName("resolveFromToken 应返回 null（需要 JwtDecoder）")
        void shouldReturnNullForResolveFromToken() {
            TokenTenantResolver resolver = new TokenTenantResolver();
            TenantContext context = resolver.resolveFromToken("some-token");
            assertThat(context).isNull();
        }
    }

    @Nested
    @DisplayName("order 测试")
    class OrderTests {

        @Test
        @DisplayName("默认 order 应为 100")
        void shouldDefaultOrderTo100() {
            TokenTenantResolver resolver = new TokenTenantResolver();
            assertThat(resolver.getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("设置 order")
        void shouldSetOrder() {
            TokenTenantResolver resolver = new TokenTenantResolver();
            resolver.setOrder(50);
            assertThat(resolver.getOrder()).isEqualTo(50);
        }
    }
}