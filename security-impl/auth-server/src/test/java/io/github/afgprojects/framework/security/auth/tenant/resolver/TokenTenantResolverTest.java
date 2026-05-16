package io.github.afgprojects.framework.security.auth.tenant.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.afgprojects.framework.security.auth.tenant.model.SimpleTenantContext;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TokenTenantResolver 测试类。
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TokenTenantResolverTest {

    @Mock
    private TokenService tokenService;

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 TokenService 构造")
        void shouldCreateWithTokenService() {
            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            assertThat(resolver.getOrder()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("resolve 测试")
    class ResolveTests {

        @Test
        @DisplayName("从 Bearer Token 解析租户成功")
        void shouldResolveFromBearerToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(tokenService.extractTenantId("valid-token")).thenReturn("tenant-001");

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("无 Authorization 头时返回 null")
        void shouldReturnNullWhenNoAuthHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Authorization 头不是 Bearer 时返回 null")
        void shouldReturnNullWhenNotBearer() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Token 中无租户信息时返回 null")
        void shouldReturnNullWhenNoTenantInToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(tokenService.extractTenantId("valid-token")).thenReturn(null);

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Bearer 后无 Token 时返回 null")
        void shouldReturnNullWhenNoTokenAfterBearer() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Authorization 头格式错误时返回 null")
        void shouldReturnNullWhenMalformedAuthHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer");

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }
    }

    @Nested
    @DisplayName("resolveFromToken 测试")
    class ResolveFromTokenTests {

        @Test
        @DisplayName("从 Token 字符串解析租户成功")
        void shouldResolveFromTokenString() {
            when(tokenService.extractTenantId("valid-token")).thenReturn("tenant-002");

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolveFromToken("valid-token");

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("Token 无租户信息时返回 null")
        void shouldReturnNullWhenNoTenantInTokenString() {
            when(tokenService.extractTenantId("valid-token")).thenReturn(null);

            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            TenantContext context = resolver.resolveFromToken("valid-token");

            assertThat(context).isNull();
        }
    }

    @Nested
    @DisplayName("order 测试")
    class OrderTests {

        @Test
        @DisplayName("默认 order 为 10")
        void shouldDefaultOrderTo10() {
            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            assertThat(resolver.getOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("设置 order")
        void shouldSetOrder() {
            TokenTenantResolver resolver = new TokenTenantResolver(tokenService);
            resolver.setOrder(5);
            assertThat(resolver.getOrder()).isEqualTo(5);
        }
    }
}
