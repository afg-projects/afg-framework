package io.github.afgprojects.framework.core.web.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextProvider;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.core.security.datascope.DataScopeProperties;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * DataScopeContextFilter 单元测试
 */
@DisplayName("DataScopeContextFilter 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataScopeContextFilterTest {

    @Mock
    private DataScopeProperties properties;

    @Mock
    private DataScopeContextProvider contextProvider;

    @Mock
    private AfgSecurityContextBridge securityContextBridge;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private DataScopeContextFilter filter;

    @BeforeEach
    void setUp() {
        DataScopeContextHolder.clear();
        when(properties.isEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("基本过滤测试")
    class BasicFilterTests {

        @Test
        @DisplayName("应该初始化和清除上下文")
        void shouldInitializeAndClearContext() throws ServletException, IOException {
            // given
            filter = new DataScopeContextFilter(properties, null, null);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            // 上下文应该在 finally 块后被清除
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("应该使用自定义上下文提供者")
        void shouldUseCustomContextProvider() throws ServletException, IOException {
            // given
            DataScopeContext customContext = DataScopeContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .build();
            when(contextProvider.provide(any())).thenReturn(customContext);
            filter = new DataScopeContextFilter(properties, contextProvider, null);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(contextProvider).provide(request);
        }

        @Test
        @DisplayName("上下文提供者返回 null 时应该回退到默认构建")
        void shouldFallbackWhenProviderReturnsNull() throws ServletException, IOException {
            // given
            when(contextProvider.provide(any())).thenReturn(null);
            filter = new DataScopeContextFilter(properties, contextProvider, null);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(contextProvider).provide(request);
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 测试")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("禁用时应该跳过过滤")
        void shouldSkipWhenDisabled() throws ServletException, IOException {
            // given
            when(properties.isEnabled()).thenReturn(false);
            filter = new DataScopeContextFilter(properties, null, null);

            // when
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // then
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("启用时不应该跳过过滤")
        void shouldNotSkipWhenEnabled() throws ServletException, IOException {
            // given
            when(properties.isEnabled()).thenReturn(true);
            filter = new DataScopeContextFilter(properties, null, null);

            // when
            boolean shouldNotFilter = filter.shouldNotFilter(request);

            // then
            assertThat(shouldNotFilter).isFalse();
        }
    }

    @Nested
    @DisplayName("安全上下文桥接测试")
    class SecurityContextBridgeTests {

        @Test
        @DisplayName("有安全上下文桥接器时应该使用")
        void shouldUseSecurityContextBridge() throws ServletException, IOException {
            // given
            filter = new DataScopeContextFilter(properties, null, securityContextBridge);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }
}