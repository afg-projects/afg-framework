package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * HeaderTenantResolver 测试类。
 *
 * @since 1.0.0
 */
class HeaderTenantResolverTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造应使用 X-Tenant-Id")
        void shouldUseDefaultHeaderName() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            assertThat(resolver.getHeaderName()).isEqualTo("X-Tenant-Id");
        }

        @Test
        @DisplayName("自定义请求头名称")
        void shouldUseCustomHeaderName() {
            HeaderTenantResolver resolver = new HeaderTenantResolver("X-Custom-Tenant");
            assertThat(resolver.getHeaderName()).isEqualTo("X-Custom-Tenant");
        }

        @Test
        @DisplayName("null 请求头名称应使用默认值")
        void shouldUseDefaultWhenHeaderNameIsNull() {
            HeaderTenantResolver resolver = new HeaderTenantResolver(null);
            assertThat(resolver.getHeaderName()).isEqualTo("X-Tenant-Id");
        }
    }

    @Nested
    @DisplayName("resolve 测试")
    class ResolveTests {

        @Test
        @DisplayName("请求头有值时应解析成功")
        void shouldResolveWhenHeaderHasValue() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-123");

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("请求头为 null 时应返回 null")
        void shouldReturnNullWhenHeaderIsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("请求头为空字符串时应返回 null")
        void shouldReturnNullWhenHeaderIsEmpty() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn("");

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("请求头为空白字符串时应返回 null")
        void shouldReturnNullWhenHeaderIsBlank() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn("   ");

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("应 trim 请求头值")
        void shouldTrimHeaderValue() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Tenant-Id")).thenReturn("  tenant-123  ");

            HeaderTenantResolver resolver = new HeaderTenantResolver();
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-123");
        }

        @Test
        @DisplayName("使用自定义请求头名称解析")
        void shouldResolveWithCustomHeaderName() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Custom-Tenant")).thenReturn("tenant-456");

            HeaderTenantResolver resolver = new HeaderTenantResolver("X-Custom-Tenant");
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-456");
        }
    }

    @Nested
    @DisplayName("order 测试")
    class OrderTests {

        @Test
        @DisplayName("默认 order 应为 200")
        void shouldDefaultOrderTo200() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            assertThat(resolver.getOrder()).isEqualTo(200);
        }

        @Test
        @DisplayName("设置 order")
        void shouldSetOrder() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            resolver.setOrder(100);
            assertThat(resolver.getOrder()).isEqualTo(100);
        }
    }
}