package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;

/**
 * 租户解析策略枚举。
 *
 * <p>定义不同的租户解析方式，用于配置租户解析链。
 *
 * <p>推荐优先级顺序：
 * <ol>
 *   <li>TOKEN - 从 JWT Token 解析（最高优先级，最可信）</li>
 *   <li>HEADER - 从请求头解析</li>
 *   <li>DOMAIN - 从子域名解析</li>
 *   <li>DEFAULT - 使用默认租户 ID</li>
 * </ol>
 *
 * @since 1.0.0
 */
public enum TenantResolveStrategy {

    /**
     * 从 JWT Token 解析租户 ID。
     *
     * <p>从 Token 的 tenant_id claim 中获取租户 ID。
     * 这是最可信的方式，因为 Token 由授权服务器签发。
     */
    TOKEN(100, "Resolve tenant from JWT token"),

    /**
     * 从请求头解析租户 ID。
     *
     * <p>默认请求头名称：X-Tenant-Id
     */
    HEADER(200, "Resolve tenant from request header"),

    /**
     * 从子域名解析租户编码。
     *
     * <p>例如：tenant1.example.com 中的 tenant1 作为租户编码。
     */
    DOMAIN(300, "Resolve tenant from subdomain"),

    /**
     * 使用默认租户 ID。
     *
     * <p>作为兜底策略，返回配置的默认租户 ID。
     */
    DEFAULT(400, "Use default tenant ID");

    private final int order;
    private final String description;

    TenantResolveStrategy(int order, @NonNull String description) {
        this.order = order;
        this.description = description;
    }

    /**
     * 获取解析策略的优先级顺序。
     *
     * <p>数值越小优先级越高。
     *
     * @return 优先级顺序
     */
    public int getOrder() {
        return order;
    }

    /**
     * 获取策略描述。
     *
     * @return 描述信息
     */
    @NonNull
    public String getDescription() {
        return description;
    }
}
