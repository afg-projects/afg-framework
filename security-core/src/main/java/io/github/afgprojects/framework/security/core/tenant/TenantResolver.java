package io.github.afgprojects.framework.security.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 租户解析器接口。
 *
 * <p>从请求或 Token 中解析租户信息。
 *
 * <p>实现类可以支持多种解析策略，如：
 * <ul>
 *   <li>从请求头解析（X-Tenant-Id）</li>
 *   <li>从 JWT Token 解析（tenant_id claim）</li>
 *   <li>从子域名解析（tenant.example.com）</li>
 *   <li>从 URL 路径解析（/{tenant}/api/...）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface TenantResolver {

    /**
     * 从 HTTP 请求解析租户上下文。
     *
     * @param request HTTP 请求，永不为 null
     * @return 租户上下文，如果无法解析则返回 null
     */
    @Nullable
    TenantContext resolve(@NonNull HttpServletRequest request);

    /**
     * 从 Token 解析租户上下文。
     *
     * @param token Token 字符串，永不为 null
     * @return 租户上下文，如果无法解析则返回 null
     */
    @Nullable
    default TenantContext resolveFromToken(@NonNull String token) {
        return null;
    }

    /**
     * 获取解析器优先级。
     *
     * <p>数值越小优先级越高。默认优先级为 100。
     *
     * @return 优先级
     */
    default int getOrder() {
        return 100;
    }
}