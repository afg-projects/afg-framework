package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.core.support.BaseUnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * BaggageInterceptor 单元测试
 */
@DisplayName("BaggageInterceptor 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaggageInterceptorTest extends BaseUnitTest {

    private BaggageInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        interceptor = new BaggageInterceptor();
    }

    @Nested
    @DisplayName("租户ID提取测试")
    class TenantIdExtractionTests {

        @Test
        @DisplayName("应该从 X-Tenant-Id 头提取租户ID")
        void shouldExtractTenantIdFromHeader() throws ServletException, java.io.IOException {
            // given
            when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-123");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

            // when
            interceptor.doFilter(request, response, filterChain);

            // then
            assertThat(BaggageContext.getTenantId()).isEqualTo("tenant-123");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("应该从 X-Baggage-tenantId 头提取租户ID")
        void shouldExtractTenantIdFromBaggageHeader() throws ServletException, java.io.IOException {
            // given
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);
            when(request.getHeader("X-Baggage-tenantId")).thenReturn("tenant-456");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

            // when
            interceptor.doFilter(request, response, filterChain);

            // then
            assertThat(BaggageContext.getTenantId()).isEqualTo("tenant-456");
        }
    }

    @Nested
    @DisplayName("用户ID提取测试")
    class UserIdExtractionTests {

        @Test
        @DisplayName("应该从 X-User-Id 头提取用户ID")
        void shouldExtractUserIdFromHeader() throws ServletException, java.io.IOException {
            // given
            when(request.getHeader("X-User-Id")).thenReturn("user-123");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

            // when
            interceptor.doFilter(request, response, filterChain);

            // then
            assertThat(BaggageContext.getUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("应该从 X-Baggage-userId 头提取用户ID")
        void shouldExtractUserIdFromBaggageHeader() throws ServletException, java.io.IOException {
            // given
            when(request.getHeader("X-User-Id")).thenReturn(null);
            when(request.getHeader("X-Baggage-userId")).thenReturn("user-456");
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

            // when
            interceptor.doFilter(request, response, filterChain);

            // then
            assertThat(BaggageContext.getUserId()).isEqualTo("user-456");
        }
    }

    @Nested
    @DisplayName("自定义 Baggage 提取测试")
    class CustomBaggageExtractionTests {

        @Test
        @DisplayName("应该提取 X-Baggage- 前缀的头")
        void shouldExtractCustomBaggageHeaders() throws ServletException, java.io.IOException {
            // given
            when(request.getHeaderNames()).thenReturn(
                    Collections.enumeration(List.of("X-Baggage-customField", "X-Other-Header"))
            );
            when(request.getHeader("X-Baggage-customField")).thenReturn("custom-value");

            // when
            interceptor.doFilter(request, response, filterChain);

            // then
            assertThat(BaggageContext.get("customField")).isEqualTo("custom-value");
        }
    }

    @Nested
    @DisplayName("空值处理测试")
    class NullValueHandlingTests {

        @Test
        @DisplayName("空 header 不应该设置值")
        void shouldNotSetNullValue() throws ServletException, java.io.IOException {
            // given
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);
            when(request.getHeader("X-Baggage-tenantId")).thenReturn(null);
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

            // when
            interceptor.doFilter(request, response, filterChain);

            // then - 不应该抛出异常
            verify(filterChain).doFilter(request, response);
        }
    }
}
