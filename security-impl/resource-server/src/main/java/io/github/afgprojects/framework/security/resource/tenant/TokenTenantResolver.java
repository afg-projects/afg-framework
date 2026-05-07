package io.github.afgprojects.framework.security.resource.tenant;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

/**
 * 从 JWT Token 解析租户的解析器。
 *
 * <p>从当前认证的 JWT Token 中提取租户 ID。
 * 默认从 tenant_id claim 中获取租户 ID。
 *
 * <p>这是最可信的解析方式，因为 Token 由授权服务器签发。
 *
 * @since 1.0.0
 */
public class TokenTenantResolver implements TenantResolver {

    /**
     * 默认租户 ID claim 名称。
     */
    public static final String DEFAULT_TENANT_ID_CLAIM = "tenant_id";

    private final String tenantIdClaim;
    private int order = TenantResolveStrategy.TOKEN.getOrder();

    /**
     * 使用默认 claim 名称创建解析器。
     */
    public TokenTenantResolver() {
        this(DEFAULT_TENANT_ID_CLAIM);
    }

    /**
     * 使用指定 claim 名称创建解析器。
     *
     * @param tenantIdClaim 租户 ID claim 名称
     */
    public TokenTenantResolver(@NonNull String tenantIdClaim) {
        this.tenantIdClaim = tenantIdClaim != null ? tenantIdClaim : DEFAULT_TENANT_ID_CLAIM;
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            return resolveFromJwtToken(jwtToken);
        }
        return null;
    }

    @Override
    @Nullable
    public TenantContext resolveFromToken(@NonNull String token) {
        // 此方法需要解析 JWT Token 字符串
        // 通常通过 resolve(HttpServletRequest) 方法解析，因为 JWT 已在 SecurityContext 中
        // 如果需要直接解析 Token 字符串，可以注入 JwtDecoder
        return null;
    }

    /**
     * 从 JwtAuthenticationToken 解析租户上下文。
     *
     * @param jwtToken JWT 认证令牌
     * @return 租户上下文，如果无法解析则返回 null
     */
    @Nullable
    private TenantContext resolveFromJwtToken(@NonNull JwtAuthenticationToken jwtToken) {
        Map<String, Object> claims = jwtToken.getToken().getClaims();
        Object tenantIdObj = claims.get(tenantIdClaim);

        if (tenantIdObj == null) {
            return null;
        }

        String tenantId = String.valueOf(tenantIdObj);
        if (tenantId.isBlank()) {
            return null;
        }

        // 构建租户上下文，从 claims 中获取更多信息
        DefaultTenantContext context = new DefaultTenantContext(tenantId);

        // 可选：获取租户编码
        Object tenantCodeObj = claims.get("tenant_code");
        if (tenantCodeObj != null) {
            context.addAttribute("tenantCode", String.valueOf(tenantCodeObj));
        }

        // 可选：获取租户名称
        Object tenantNameObj = claims.get("tenant_name");
        if (tenantNameObj != null) {
            context.addAttribute("tenantName", String.valueOf(tenantNameObj));
        }

        return context;
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
     * 获取租户 ID claim 名称。
     *
     * @return claim 名称
     */
    public String getTenantIdClaim() {
        return tenantIdClaim;
    }
}