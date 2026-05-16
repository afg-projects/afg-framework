package io.github.afgprojects.framework.security.auth.tenant.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.security.auth.tenant.resolver.TenantResolverChain;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * TenantFilter 测试类。
 *
 * @since 1.0.0
 */
class TenantFilterTest {

    @Mock
    private TenantResolverChain resolverChain;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private FilterChain filterChain;

    private TenantFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new TenantFilter(resolverChain, tenantContextHolder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("doFilterInternal - 正常流程")
    class NormalFlow {

        @Test
        @DisplayName("应该解析租户并设置到上下文")
        void shouldResolveTenantAndSetContext() throws Exception {
            // Given
            TenantContext mockContext = mock(TenantContext.class);
            when(mockContext.getTenantId()).thenReturn("tenant-123");
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenReturn(mockContext);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(resolverChain).resolve(any(HttpServletRequest.class));
            verify(tenantContextHolder).setTenantId("tenant-123");
            verify(tenantContextHolder).clear();
            verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("应该在 finally 块中清除租户上下文")
        void shouldClearContextInFinally() throws Exception {
            // Given
            TenantContext mockContext = mock(TenantContext.class);
            when(mockContext.getTenantId()).thenReturn("tenant-123");
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenReturn(mockContext);

            // 模拟 filterChain 抛出异常
            doThrow(new RuntimeException("Test exception"))
                    .when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When & Then
            try {
                filter.doFilter(request, response, filterChain);
            } catch (RuntimeException e) {
                // 预期的异常
            }

            // 验证即使抛出异常，也会清除上下文
            verify(tenantContextHolder).clear();
        }

        @Test
        @DisplayName("当解析器返回 null 时应该继续处理请求")
        void shouldContinueWhenResolverReturnsNull() throws Exception {
            // Given
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenReturn(null);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(tenantContextHolder, never()).setTenantId(any());
            verify(tenantContextHolder).clear();
            verify(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        }
    }

    @Nested
    @DisplayName("doFilterInternal - 异常处理")
    class ExceptionHandling {

        @Test
        @DisplayName("当 TenantException 发生时应该返回 400 JSON 响应")
        void shouldReturn400OnTenantException() throws Exception {
            // Given
            TenantException exception = TenantException.notFound("tenant-123");
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenThrow(exception);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            String responseBody = response.getContentAsString();
            assertThat(responseBody).contains("\"code\":20000");
            assertThat(responseBody).contains("\"message\"");

            // 不应该继续执行 filter chain
            verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("当 TenantException 发生时应该清除上下文")
        void shouldClearContextOnTenantException() throws Exception {
            // Given
            TenantException exception = TenantException.unresolved();
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenThrow(exception);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(tenantContextHolder).clear();
        }

        @Test
        @DisplayName("应该处理不同类型的 TenantException")
        void shouldHandleDifferentTenantExceptions() throws Exception {
            // Given
            TenantException exception = TenantException.disabled("tenant-123");
            when(resolverChain.resolve(any(HttpServletRequest.class))).thenThrow(exception);

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getStatus()).isEqualTo(400);
            String responseBody = response.getContentAsString();
            assertThat(responseBody).contains("\"code\":20001");
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("不应该过滤健康检查端点")
        void shouldNotFilterHealthEndpoint() throws Exception {
            request.setServletPath("/actuator/health");

            boolean shouldNotFilter = filter.shouldNotFilter(request);

            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("不应该过滤 Actuator 端点")
        void shouldNotFilterActuatorEndpoints() throws Exception {
            request.setServletPath("/actuator/info");

            boolean shouldNotFilter = filter.shouldNotFilter(request);

            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("应该过滤普通 API 端点")
        void shouldFilterApiEndpoints() throws Exception {
            request.setServletPath("/api/users");

            boolean shouldNotFilter = filter.shouldNotFilter(request);

            assertThat(shouldNotFilter).isFalse();
        }
    }

    @Nested
    @DisplayName("OncePerRequestFilter 特性")
    class OncePerRequestFilterFeature {

        @Test
        @DisplayName("应该继承 OncePerRequestFilter")
        void shouldExtendOncePerRequestFilter() {
            assertThat(filter).isInstanceOf(org.springframework.web.filter.OncePerRequestFilter.class);
        }
    }
}
