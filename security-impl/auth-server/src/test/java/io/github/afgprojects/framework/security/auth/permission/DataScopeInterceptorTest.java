package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

/**
 * DataScopeInterceptor 测试。
 *
 * <p>测试从 Spring Security Context 获取用户信息的安全实现。
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

    @Mock
    private AfgAuthentication afgAuthentication;

    @Mock
    private AfgUserDetails userDetails;

    private AuthSecurityProperties.PermissionConfig properties;
    private DataScopeInterceptor interceptor;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        properties = new AuthSecurityProperties.PermissionConfig();
        properties.setEnabled(true);
        properties.setDataScopeInterceptorEnabled(true);
        properties.setDefaultDataScope("ALL");
        interceptor = new DataScopeInterceptor(dataScopeService, properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        DataScopeContextHolder.clear();
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Nested
    @DisplayName("正常流程测试")
    class NormalFlowTests {

        @Test
        @DisplayName("应从 SecurityContext 获取用户信息并设置数据权限上下文")
        void shouldSetDataScopeContextFromSecurityContext() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            // 设置 SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(tenantId);
            when(afgAuthentication.isAdmin()).thenReturn(false);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService).getDataScope(userId, tenantId);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("管理员应设置全部数据权限上下文")
        void shouldSetAllPermissionForAdmin() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            // 设置 SecurityContext - 管理员
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(tenantId);
            when(afgAuthentication.isAdmin()).thenReturn(true);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then - 管理员会调用 dataScopeService 但上下文设置 allDataPermission=true
            verify(dataScopeService).getDataScope(userId, tenantId);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("应根据 DEPT 数据范围获取可访问部门")
        void shouldGetAccessibleDeptIdsForDeptScope() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.DEPT);
            Set<Long> accessibleDeptIds = Set.of(1L, 2L, 3L);

            // 设置 SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(tenantId);
            when(afgAuthentication.isAdmin()).thenReturn(false);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);
            when(dataScopeService.getAccessibleDeptIds(userId, tenantId)).thenReturn(accessibleDeptIds);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService).getAccessibleDeptIds(userId, tenantId);
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

            // 设置 SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(null);
            when(afgAuthentication.isAdmin()).thenReturn(false);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

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

            // 设置 SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(null);
            when(afgAuthentication.isAdmin()).thenReturn(false);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

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
    @DisplayName("未认证用户测试")
    class UnauthenticatedUserTests {

        @Test
        @DisplayName("无认证用户时应跳过上下文设置")
        void shouldSkipWhenNoAuthentication() throws Exception {
            // given - 空 SecurityContext
            SecurityContextHolder.clearContext();

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService, never()).getDataScope(any(), any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("匿名用户时应跳过上下文设置")
        void shouldSkipForAnonymousUser() throws Exception {
            // given - 匿名认证
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            securityContext.setAuthentication(anonymousToken);
            SecurityContextHolder.setContext(securityContext);

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then
            verify(dataScopeService, never()).getDataScope(any(), any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("非 AfgAuthentication 类型时应跳过上下文设置")
        void shouldSkipForNonAfgAuthentication() throws Exception {
            // given - 非 AfgAuthentication
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            Authentication otherAuth = mock(Authentication.class);
            when(otherAuth.isAuthenticated()).thenReturn(true);
            securityContext.setAuthentication(otherAuth);
            SecurityContextHolder.setContext(securityContext);

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
    @DisplayName("安全测试")
    class SecurityTests {

        @Test
        @DisplayName("不应从请求头获取用户信息")
        void shouldNotGetUserInfoFromHeaders() throws Exception {
            // given
            String userId = "100";
            String tenantId = "tenant-001";
            DataScope dataScope = DataScope.of("sys_user", "dept_id", DataScopeType.ALL);

            // 设置 SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            when(afgAuthentication.isAuthenticated()).thenReturn(true);
            when(afgAuthentication.getUserDetails()).thenReturn(userDetails);
            when(userDetails.getUserId()).thenReturn(userId);
            when(userDetails.getTenantId()).thenReturn(tenantId);
            when(afgAuthentication.isAdmin()).thenReturn(false);
            securityContext.setAuthentication(afgAuthentication);
            SecurityContextHolder.setContext(securityContext);

            when(dataScopeService.getDataScope(userId, tenantId)).thenReturn(dataScope);

            // 设置请求头（这些应该被忽略）
            when(request.getHeader("X-User-Id")).thenReturn("malicious-user");
            when(request.getHeader("X-Tenant-Id")).thenReturn("malicious-tenant");

            // when
            interceptor.doFilterInternal(request, response, filterChain);

            // then - 应该使用 SecurityContext 中的用户信息，而不是请求头
            verify(dataScopeService).getDataScope(userId, tenantId);
            verify(request, never()).getHeader("X-User-Id");
            verify(request, never()).getHeader("X-Tenant-Id");
        }
    }
}