package io.github.afgprojects.framework.security.auth.tenant.resolver;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties.TenantConfig;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DefaultTenantResolver 测试类。
 *
 * @since 1.0.0
 */
class DefaultTenantResolverTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 AuthSecurityProperties.TenantConfig 构造")
        void shouldCreateWithTenantProperties() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            assertThat(resolver.getOrder()).isEqualTo(1000);
        }

        @Test
        @DisplayName("使用自定义默认租户 ID 构造")
        void shouldCreateWithCustomDefaultTenant() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            properties.setDefaultTenant("custom-default");
            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            assertThat(resolver.getOrder()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("resolve 测试")
    class ResolveTests {

        @Test
        @DisplayName("返回默认租户 ID")
        void shouldReturnDefaultTenantId() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            HttpServletRequest request = mock(HttpServletRequest.class);

            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("default");
            assertThat(context.isDefault()).isTrue();
        }

        @Test
        @DisplayName("返回自定义默认租户 ID")
        void shouldReturnCustomDefaultTenantId() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            properties.setDefaultTenant("custom-default");
            HttpServletRequest request = mock(HttpServletRequest.class);

            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("custom-default");
            assertThat(context.isDefault()).isTrue();
        }

        @Test
        @DisplayName("无论请求内容如何都返回默认租户")
        void shouldAlwaysReturnDefaultTenant() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            HttpServletRequest request = mock(HttpServletRequest.class);

            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("order 测试")
    class OrderTests {

        @Test
        @DisplayName("默认 order 为 1000")
        void shouldDefaultOrderTo1000() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            assertThat(resolver.getOrder()).isEqualTo(1000);
        }

        @Test
        @DisplayName("设置 order")
        void shouldSetOrder() {
            AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
            DefaultTenantResolver resolver = new DefaultTenantResolver(properties);
            resolver.setOrder(999);
            assertThat(resolver.getOrder()).isEqualTo(999);
        }
    }
}