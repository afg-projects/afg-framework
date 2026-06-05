package io.github.afgprojects.framework.security.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HeaderTenantResolver 测试
 */
@DisplayName("HeaderTenantResolver 测试")
class HeaderTenantResolverTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("应使用默认 header 名称创建")
        void shouldCreateWithDefaultHeaderName() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();

            assertThat(resolver.getHeaderName()).isEqualTo(HeaderTenantResolver.DEFAULT_HEADER_NAME);
            assertThat(resolver.getHeaderName()).isEqualTo("X-Tenant-Id");
        }

        @Test
        @DisplayName("应使用自定义 header 名称创建")
        void shouldCreateWithCustomHeaderName() {
            HeaderTenantResolver resolver = new HeaderTenantResolver("X-Custom-Tenant");

            assertThat(resolver.getHeaderName()).isEqualTo("X-Custom-Tenant");
        }

        @Test
        @DisplayName("null header 名称应使用默认值")
        void shouldUseDefaultWhenHeaderNameIsNull() {
            HeaderTenantResolver resolver = new HeaderTenantResolver(null);

            assertThat(resolver.getHeaderName()).isEqualTo(HeaderTenantResolver.DEFAULT_HEADER_NAME);
        }

        @Test
        @DisplayName("空白 header 名称应使用默认值")
        void shouldUseDefaultWhenHeaderNameIsBlank() {
            HeaderTenantResolver resolver = new HeaderTenantResolver("   ");

            assertThat(resolver.getHeaderName()).isEqualTo(HeaderTenantResolver.DEFAULT_HEADER_NAME);
        }

        @Test
        @DisplayName("空 header 名称应使用默认值")
        void shouldUseDefaultWhenHeaderNameIsEmpty() {
            HeaderTenantResolver resolver = new HeaderTenantResolver("");

            assertThat(resolver.getHeaderName()).isEqualTo(HeaderTenantResolver.DEFAULT_HEADER_NAME);
        }
    }

    @Nested
    @DisplayName("resolve 方法")
    class ResolveTests {

        @Test
        @DisplayName("应从 header 中解析租户上下文")
        void shouldResolveTenantContextFromHeader() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "tenant-001");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应从自定义 header 名称中解析租户上下文")
        void shouldResolveFromCustomHeaderName() {
            HeaderTenantResolver resolver = new HeaderTenantResolver("X-Custom-Tenant");
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Custom-Tenant", "custom-tenant");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("custom-tenant");
        }

        @Test
        @DisplayName("header 不存在时应返回 null")
        void shouldReturnNullWhenHeaderNotPresent() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("header 值为空字符串时应返回 null")
        void shouldReturnNullWhenHeaderIsEmpty() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("header 值为空白字符串时应返回 null")
        void shouldReturnNullWhenHeaderIsBlank() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "   ");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("应返回 SimpleTenantContext 实例")
        void shouldReturnSimpleTenantContext() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "tenant-001");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isInstanceOf(SimpleTenantContext.class);
        }

        @Test
        @DisplayName("应去除 header 值的前后空白")
        void shouldTrimHeaderValue() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", "  tenant-001  ");

            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("应返回默认优先级 200")
        void shouldReturnDefaultOrder() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();

            assertThat(resolver.getOrder()).isEqualTo(200);
        }

        @Test
        @DisplayName("应支持设置自定义优先级")
        void shouldSupportCustomOrder() {
            HeaderTenantResolver resolver = new HeaderTenantResolver();
            resolver.setOrder(100);

            assertThat(resolver.getOrder()).isEqualTo(100);
        }
    }
}
