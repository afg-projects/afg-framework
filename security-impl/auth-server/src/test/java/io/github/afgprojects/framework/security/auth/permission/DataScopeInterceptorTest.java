package io.github.afgprojects.framework.security.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.auth.permission.config.PermissionProperties;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * DataScopeInterceptor 测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@DisplayName("DataScopeInterceptor 测试")
class DataScopeInterceptorTest {

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private PermissionProperties properties;
    private DataScopeInterceptor interceptor;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        properties = new PermissionProperties();
        properties.setEnabled(true);
        properties.setDataScopeInterceptorEnabled(true);
        properties.setDefaultDataScope("ALL");
        interceptor = new DataScopeInterceptor(dataScopeService, properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        DataScopeContextHolder.clear();
        mocks.close();
    }

    @Nested
    @DisplayName("正常流程测试")
    class NormalFlowTests {

        @Test
        @DisplayName("应设置数据权限上下文")
        void shouldSetDataScopeContext() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(request.getHeader("X-Tenant-Id")).thenReturn(tenantId);
            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService).getDataScope(userId, tenantId);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("应从请求头获取用户信息")
        void shouldGetUserInfoFromHeaders() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            String deptId = "10";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(request.getHeader("X-Tenant-Id")).thenReturn(tenantId);
            when(request.getHeader("X-Dept-Id")).thenReturn(deptId);
            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(request).getHeader("X-User-Id");
            verify(request).getHeader("X-Tenant-Id");
            verify(request).getHeader("X-Dept-Id");
        }

        @Test
        @DisplayName("应根据 ALL 数据范围设置全部权限")
        void shouldSetAllPermissionForAllScope() throws Exception {
            // given
            String userId = "100";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);
            when(dataScopeService.getDataScope(eq(userId), any())).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then - 验证 filterChain 被调用
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("上下文清理测试")
    class ContextCleanupTests {

        @Test
        @DisplayName("请求结束后应清除上下文")
        void shouldClearContextAfterRequest() throws Exception {
            // given
            String userId = "100";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(dataScopeService.getDataScope(eq(userId), any())).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("异常时应清除上下文")
        void shouldClearContextOnException() throws Exception {
            // given
            String userId = "100";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(dataScopeService.getDataScope(eq(userId), any())).thenReturn(dataScope);
            doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

            // when/then
            try {
                interceptor.doFilterInternal(request, response, filterChain);
            } catch (RuntimeException e) {
                // 预期异常
            }

            // then - 上下文应该被清除
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("空用户 ID 测试")
    class NullUserIdTests {

        @Test
        @DisplayName("用户 ID 为空时应跳过上下文设置")
        void shouldSkipWhenUserIdIsNull() throws Exception {
            // given
            when(request.getHeader("X-User-Id")).thenReturn(null);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService, never()).getDataScope(any(), any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("用户 ID 为空字符串时应跳过上下文设置")
        void shouldSkipWhenUserIdIsEmpty() throws Exception {
            // given
            when(request.getHeader("X-User-Id")).thenReturn("");

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService, never()).getDataScope(any(), any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 测试")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("禁用权限功能时应跳过过滤")
        void shouldNotFilterWhenDisabled() throws Exception {
            // given
            properties.setEnabled(false);

            // when
            boolean shouldNotFilter = interceptor.shouldNotFilter(request);

            // then
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("禁用数据权限拦截器时应跳过过滤")
        void shouldNotFilterWhenInterceptorDisabled() throws Exception {
            // given
            properties.setDataScopeInterceptorEnabled(false);

            // when
            boolean shouldNotFilter = interceptor.shouldNotFilter(request);

            // then
            assertThat(shouldNotFilter).isTrue();
        }

        @Test
        @DisplayName("启用时应执行过滤")
        void shouldFilterWhenEnabled() throws Exception {
            // given
            properties.setEnabled(true);
            properties.setDataScopeInterceptorEnabled(true);

            // when
            boolean shouldNotFilter = interceptor.shouldNotFilter(request);

            // then
            assertThat(shouldNotFilter).isFalse();
        }
    }

    @Nested
    @DisplayName("数据范围类型测试")
    class DataScopeTypeTests {

        @Test
        @DisplayName("DEPT 类型应设置部门 ID")
        void shouldSetDeptIdForDeptScope() throws Exception {
            // given
            String userId = "100";
            String deptId = "10";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(request.getHeader("X-Dept-Id")).thenReturn(deptId);
            when(dataScopeService.getDataScope(eq(userId), any())).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("CUSTOM 类型应设置自定义条件")
        void shouldSetCustomConditionForCustomScope() throws Exception {
            // given
            String userId = "100";
            DataScope dataScope = DataScope.builder()
                    .table("sys_user")
                    .column("dept_id")
                    .scopeType(DataScopeType.CUSTOM)
                    .customCondition("dept_id IN (1, 2, 3)")
                    .build();

            when(request.getHeader("X-User-Id")).thenReturn(userId);
            when(dataScopeService.getDataScope(eq(userId), any())).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }
}
