package io.github.afgprojects.framework.core.web.context;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.afgprojects.framework.core.security.datascope.DataScopeContext;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextHolder;
import io.github.afgprojects.framework.core.security.datascope.DataScopeContextProvider;
import io.github.afgprojects.framework.core.security.datascope.DataScopeProperties;
import io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 数据权限上下文过滤器
 * <p>
 * 在请求开始时初始化数据权限上下文，请求结束时清除。
 * 从安全上下文中提取用户 ID、部门 ID 等信息，用于数据权限判断。
 * <p>
 * 该过滤器应在安全过滤器之后执行，确保安全上下文已经建立。
 */
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class DataScopeContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DataScopeContextFilter.class);

    private final DataScopeProperties properties;
    private final @Nullable DataScopeContextProvider contextProvider;
    private final @Nullable AfgSecurityContextBridge securityContextBridge;

    /**
     * 创建数据权限上下文过滤器
     *
     * @param properties          数据权限配置属性
     * @param contextProvider     数据权限上下文提供者（可选，用于自定义上下文构建）
     * @param securityContextBridge 安全上下文桥接器（可选，用于从安全上下文提取信息）
     */
    public DataScopeContextFilter(
            DataScopeProperties properties,
            @Nullable DataScopeContextProvider contextProvider,
            @Nullable AfgSecurityContextBridge securityContextBridge) {
        this.properties = properties;
        this.contextProvider = contextProvider;
        this.securityContextBridge = securityContextBridge;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 初始化数据权限上下文
            DataScopeContext context = buildContext(request);
            DataScopeContextHolder.setContext(context);

            log.debug("Data scope context initialized: userId={}, deptId={}",
                    context.getUserId(), context.getDeptId());

            filterChain.doFilter(request, response);
        } finally {
            // 清除上下文
            DataScopeContextHolder.clear();
            log.debug("Data scope context cleared");
        }
    }

    /**
     * 构建数据权限上下文
     */
    private DataScopeContext buildContext(HttpServletRequest request) {
        // 如果有自定义提供者，优先使用
        if (contextProvider != null) {
            DataScopeContext context = contextProvider.provide(request);
            if (context != null) {
                return context;
            }
        }

        // 否则从安全上下文构建
        return buildFromSecurityContext();
    }

    /**
     * 从安全上下文构建数据权限上下文
     */
    private DataScopeContext buildFromSecurityContext() {
        DataScopeContext.DataScopeContextBuilder builder = DataScopeContext.builder();

        // 尝试从请求上下文获取用户信息
        Long userId = AfgRequestContextHolder.getUserId();
        String username = AfgRequestContextHolder.getUsername();
        Long tenantId = AfgRequestContextHolder.getTenantId();

        if (userId != null) {
            builder.userId(userId);
        }

        // 如果提供了安全上下文桥接器，尝试获取更多信息
        if (securityContextBridge != null) {
            // 安全上下文通常由 Spring Security 等框架提供
            // 这里简化处理，实际使用时可能需要从 SecurityContextHolder 获取
        }

        return builder.build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 如果未启用数据权限，跳过过滤
        return !properties.isEnabled();
    }
}
