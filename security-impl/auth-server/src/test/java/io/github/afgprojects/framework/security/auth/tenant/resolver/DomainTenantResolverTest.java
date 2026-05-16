package io.github.afgprojects.framework.security.auth.tenant.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.afgprojects.framework.security.core.tenant.Tenant;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

/**
 * DomainTenantResolver 测试类。
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DomainTenantResolverTest {

    @Mock
    private AfgTenantService tenantService;

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用 tenantService 和 domainMappings 构造")
        void shouldCreateWithTenantServiceAndMappings() {
            Map<String, String> mappings = Map.of("tenant1.example.com", "tenant-001");
            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            assertThat(resolver.getOrder()).isEqualTo(30);
        }

        @Test
        @DisplayName("使用空 mappings 构造")
        void shouldCreateWithEmptyMappings() {
            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            assertThat(resolver.getOrder()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("resolve 测试")
    class ResolveTests {

        @Test
        @DisplayName("从精确域名匹配解析租户成功")
        void shouldResolveFromExactDomainMatch() {
            Map<String, String> mappings = Map.of("tenant1.example.com", "tenant-001");
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("tenant1.example.com");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("从通配符域名匹配解析租户成功")
        void shouldResolveFromWildcardDomainMatch() {
            Map<String, String> mappings = Map.of("*.example.com", "tenant-wildcard");
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("any.example.com");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-wildcard");
        }

        @Test
        @DisplayName("通过 AfgTenantService 解析域名成功")
        void shouldResolveViaTenantService() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("custom.example.com");

            Tenant tenant = mock(Tenant.class);
            when(tenant.getTenantId()).thenReturn("tenant-custom");
            when(tenantService.resolveByDomain("custom.example.com")).thenReturn(tenant);

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-custom");
        }

        @Test
        @DisplayName("优先使用 domainMappings 而非 tenantService")
        void shouldPreferDomainMappingsOverTenantService() {
            Map<String, String> mappings = Map.of("tenant1.example.com", "tenant-mapping");
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("tenant1.example.com");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-mapping");
        }

        @Test
        @DisplayName("无 Host 头时返回 null")
        void shouldReturnNullWhenNoHostHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn(null);

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Host 头为空时返回 null")
        void shouldReturnNullWhenHostHeaderEmpty() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("域名不匹配且 tenantService 返回 null 时返回 null")
        void shouldReturnNullWhenNoMatchAndTenantServiceReturnsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("unknown.example.com");
            when(tenantService.resolveByDomain("unknown.example.com")).thenReturn(null);

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNull();
        }

        @Test
        @DisplayName("Host 包含端口号时应正确处理")
        void shouldHandleHostWithPort() {
            Map<String, String> mappings = Map.of("tenant1.example.com", "tenant-001");
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("tenant1.example.com:8080");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("通配符匹配多级子域名")
        void shouldMatchMultiLevelSubdomainWithWildcard() {
            Map<String, String> mappings = Map.of("*.example.com", "tenant-wildcard");
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Host")).thenReturn("deep.sub.example.com");

            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, mappings);
            TenantContext context = resolver.resolve(request);

            assertThat(context).isNotNull();
            assertThat(context.getTenantId()).isEqualTo("tenant-wildcard");
        }
    }

    @Nested
    @DisplayName("order 测试")
    class OrderTests {

        @Test
        @DisplayName("默认 order 为 30")
        void shouldDefaultOrderTo30() {
            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            assertThat(resolver.getOrder()).isEqualTo(30);
        }

        @Test
        @DisplayName("设置 order")
        void shouldSetOrder() {
            DomainTenantResolver resolver = new DomainTenantResolver(tenantService, Map.of());
            resolver.setOrder(25);
            assertThat(resolver.getOrder()).isEqualTo(25);
        }
    }
}
