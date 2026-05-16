package io.github.afgprojects.framework.security.auth.tenant.resolver;

import io.github.afgprojects.framework.security.auth.tenant.config.TenantProperties;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 默认租户解析器。
 *
 * <p>始终返回配置的默认租户 ID。
 * 作为租户解析链的兜底策略，优先级最低。
 *
 * @since 1.0.0
 */
public class DefaultTenantResolver implements TenantResolver {

    /**
     * 默认优先级（最低）。
     */
    private static final int DEFAULT_ORDER = 1000;

    private final String defaultTenantId;
    private int order = DEFAULT_ORDER;

    /**
     * 使用 TenantProperties 构造默认租户解析器。
     *
     * @param properties 租户配置属性
     */
    public DefaultTenantResolver(@NonNull TenantProperties properties) {
        this.defaultTenantId = properties.getDefaultTenant();
    }

    /**
     * 使用自定义默认租户 ID 构造解析器。
     *
     * @param defaultTenantId 默认租户 ID
     */
    public DefaultTenantResolver(@NonNull String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    @NonNull
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        return new DefaultTenantContext(defaultTenantId);
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * 设置解析器优先级。
     *
     * @param order 优先级
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * 获取默认租户 ID。
     *
     * @return 默认租户 ID
     */
    public String getDefaultTenantId() {
        return defaultTenantId;
    }
}
