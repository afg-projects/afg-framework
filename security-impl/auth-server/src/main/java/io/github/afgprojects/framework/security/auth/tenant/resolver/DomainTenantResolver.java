package io.github.afgprojects.framework.security.auth.tenant.resolver;

import io.github.afgprojects.framework.security.auth.tenant.model.SimpleTenantContext;
import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.Tenant;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 从域名解析租户的解析器。
 *
 * <p>从 HTTP 请求的 Host 头中解析域名，然后通过以下方式解析租户：
 * <ol>
 *   <li>首先查找 domainMappings 配置的精确匹配</li>
 *   <li>然后查找 domainMappings 配置的通配符匹配（如 *.example.com）</li>
 *   <li>最后调用 AfgTenantService.resolveByDomain() 方法</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class DomainTenantResolver implements TenantResolver {

    /**
     * 默认优先级。
     */
    private static final int DEFAULT_ORDER = 30;

    private final AfgTenantService tenantService;
    private final Map<String, String> domainMappings;
    private int order = DEFAULT_ORDER;

    /**
     * 构造域名租户解析器。
     *
     * @param tenantService 租户服务，用于通过域名查找租户
     * @param domainMappings 域名到租户 ID 的映射，支持通配符（如 *.example.com）
     */
    public DomainTenantResolver(
            @NonNull AfgTenantService tenantService,
            @NonNull Map<String, String> domainMappings) {
        this.tenantService = tenantService;
        this.domainMappings = domainMappings != null ? domainMappings : Map.of();
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            return null;
        }

        // 移除端口号
        String domain = extractDomain(host);
        if (domain.isEmpty()) {
            return null;
        }

        // 1. 首先尝试精确匹配
        String tenantId = domainMappings.get(domain);
        if (tenantId != null) {
            return new SimpleTenantContext(tenantId);
        }

        // 2. 尝试通配符匹配
        tenantId = matchWildcard(domain);
        if (tenantId != null) {
            return new SimpleTenantContext(tenantId);
        }

        // 3. 通过 tenantService 解析
        Tenant tenant = tenantService.resolveByDomain(domain);
        if (tenant != null) {
            return new SimpleTenantContext(tenant.getTenantId());
        }

        return null;
    }

    /**
     * 从 Host 头中提取域名（移除端口号）。
     *
     * @param host Host 头值
     * @return 域名，如果无效则返回空字符串
     */
    private String extractDomain(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        int portIndex = host.indexOf(':');
        return portIndex > 0 ? host.substring(0, portIndex).trim() : host.trim();
    }

    /**
     * 通配符域名匹配。
     *
     * <p>支持 *.example.com 格式的通配符，匹配任意子域名。
     *
     * @param domain 请求的域名
     * @return 匹配的租户 ID，如果没有匹配则返回 null
     */
    @Nullable
    private String matchWildcard(String domain) {
        for (Map.Entry<String, String> entry : domainMappings.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.startsWith("*.")) {
                String suffix = pattern.substring(1); // .example.com
                if (domain.endsWith(suffix) || domain.equals(pattern.substring(2))) {
                    return entry.getValue();
                }
            }
        }
        return null;
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
}
