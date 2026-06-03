package io.github.afgprojects.framework.security.core.tenant;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 从请求头解析租户的解析器。
 *
 * <p>从指定的 HTTP 请求头中解析租户 ID。
 * 默认请求头名称为 X-Tenant-Id。
 *
 * @since 1.0.0
 */
public class HeaderTenantResolver implements TenantResolver {

    /**
     * 默认租户请求头名称。
     */
    public static final String DEFAULT_HEADER_NAME = "X-Tenant-Id";

    /**
     * 默认优先级。
     */
    private static final int DEFAULT_ORDER = 200;

    private final String headerName;
    private int order = DEFAULT_ORDER;

    /**
     * 使用默认请求头名称创建解析器。
     */
    public HeaderTenantResolver() {
        this(DEFAULT_HEADER_NAME);
    }

    /**
     * 使用指定请求头名称创建解析器。
     *
     * @param headerName 请求头名称
     */
    public HeaderTenantResolver(@Nullable String headerName) {
        this.headerName = (headerName != null && !headerName.isBlank()) ? headerName : DEFAULT_HEADER_NAME;
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        String tenantId = request.getHeader(headerName);
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return new SimpleTenantContext(tenantId.trim());
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
     * 获取请求头名称。
     *
     * @return 请求头名称
     */
    public String getHeaderName() {
        return headerName;
    }
}
