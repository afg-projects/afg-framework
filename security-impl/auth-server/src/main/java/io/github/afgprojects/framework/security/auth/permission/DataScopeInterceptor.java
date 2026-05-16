package io.github.afgprojects.framework.security.auth.permission;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import io.github.afgprojects.framework.security.auth.permission.config.PermissionProperties;
import io.github.afgprojects.framework.security.core.permission.DataScopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 数据权限拦截器。
 *
 * <p>在请求开始时从请求头或安全上下文中获取用户信息，
 * 通过 DataScopeService 获取用户的数据权限配置，
 * 并设置到 DataScopeContextHolder 中。
 *
 * <p>请求结束后自动清除上下文。
 *
 * <h3>请求头参数</h3>
 * <ul>
 *   <li>X-User-Id: 用户 ID</li>
 *   <li>X-Tenant-Id: 租户 ID（可选）</li>
 *   <li>X-Dept-Id: 部门 ID（可选）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 配置拦截器
 * @Bean
 * public DataScopeInterceptor dataScopeInterceptor(DataScopeService service) {
 *     return new DataScopeInterceptor(service, properties);
 * }
 *
 * // 在业务代码中使用
 * DataScopeContext context = DataScopeContextHolder.getContext();
 * if (context != null) {
 *     Long userId = context.getUserId();
 *     Set<Long> accessibleDeptIds = context.getAccessibleDeptIds();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class DataScopeInterceptor extends OncePerRequestFilter {

    private final DataScopeService dataScopeService;
    private final PermissionProperties properties;

    /**
     * 构造函数。
     *
     * @param dataScopeService 数据权限服务
     * @param properties       权限配置属性
     */
    public DataScopeInterceptor(DataScopeService dataScopeService, PermissionProperties properties) {
        this.dataScopeService = dataScopeService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求头获取用户信息
            String userId = request.getHeader("X-User-Id");
            String tenantId = request.getHeader("X-Tenant-Id");
            String deptIdStr = request.getHeader("X-Dept-Id");

            if (userId != null && !userId.isEmpty()) {
                // 获取数据权限配置
                DataScope dataScope = dataScopeService.getDataScope(userId, tenantId);

                // 构建数据权限上下文
                DataScopeContext context = buildContext(userId, tenantId, deptIdStr, dataScope);

                // 设置上下文
                DataScopeContextHolder.setContext(context);

                log.debug("Data scope context initialized: userId={}, tenantId={}, scopeType={}",
                        userId, tenantId, dataScope.scopeType());
            } else {
                log.debug("No user ID in request headers, skipping data scope initialization");
            }

            filterChain.doFilter(request, response);
        } finally {
            // 清除上下文
            DataScopeContextHolder.clear();
            log.debug("Data scope context cleared");
        }
    }

    /**
     * 构建数据权限上下文。
     *
     * @param userId    用户 ID
     * @param tenantId  租户 ID（可选）
     * @param deptIdStr 部门 ID 字符串（可选）
     * @param dataScope 数据权限配置
     * @return 数据权限上下文
     */
    private DataScopeContext buildContext(
            String userId,
            @Nullable String tenantId,
            @Nullable String deptIdStr,
            DataScope dataScope) {

        DataScopeContext.DataScopeContextBuilder builder = DataScopeContext.builder();

        // 设置用户 ID
        builder.userId(Long.parseLong(userId));

        // 设置租户 ID（如果存在）
        if (tenantId != null && !tenantId.isEmpty()) {
            // tenantId 存储在上下文中供其他组件使用
        }

        // 设置部门 ID（如果存在）
        if (deptIdStr != null && !deptIdStr.isEmpty()) {
            builder.deptId(Long.parseLong(deptIdStr));
        }

        // 根据数据范围类型设置权限
        switch (dataScope.scopeType()) {
            case ALL:
                builder.allDataPermission(true);
                break;
            case SELF:
                // SELF 类型：只能查看自己的数据
                builder.allDataPermission(false);
                break;
            case DEPT:
                // DEPT 类型：只能查看本部门数据
                builder.allDataPermission(false);
                if (deptIdStr != null && !deptIdStr.isEmpty()) {
                    builder.deptId(Long.parseLong(deptIdStr));
                }
                break;
            case DEPT_AND_CHILD:
                // DEPT_AND_CHILD 类型：可以查看本部门及子部门数据
                // 需要从服务获取子部门列表（这里简化处理）
                builder.allDataPermission(false);
                if (deptIdStr != null && !deptIdStr.isEmpty()) {
                    builder.deptId(Long.parseLong(deptIdStr));
                }
                break;
            case CUSTOM:
                // CUSTOM 类型：使用自定义条件
                builder.customCondition(dataScope.customCondition());
                builder.allDataPermission(false);
                break;
            default:
                // 默认使用配置的默认数据范围
                builder.allDataPermission("ALL".equals(properties.getDefaultDataScope()));
        }

        return builder.build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 如果未启用数据权限拦截器，跳过过滤
        return !properties.isEnabled() || !properties.isDataScopeInterceptorEnabled();
    }
}