package io.github.afgprojects.framework.security.resource.tenant;

import io.github.afgprojects.framework.security.core.tenant.DefaultTenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.extern.slf4j.Slf4j;

/**
 * 从 JWT Token 解析租户的解析器。
 *
 * <p>从 Spring Security 上下文中提取 JWT Token，
 * 然后从 Token 的 claim 中获取租户 ID。
 *
 * <p>这是最可信的解析方式，因为 Token 由授权服务器签发。
 *
 * @since 1.0.0
 */
@Slf4j
public class TokenTenantResolver implements TenantResolver {

    /**
     * 默认租户 ID claim 名称。
     */
    private static final String DEFAULT_TENANT_ID_CLAIM = "tenant_id";

    /**
     * 默认优先级。
     */
    private static final int DEFAULT_ORDER = 100;

    private final String tenantIdClaim;
    private int order = DEFAULT_ORDER;

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
    public TokenTenantResolver(@Nullable String tenantIdClaim) {
        this.tenantIdClaim = (tenantIdClaim != null && !tenantIdClaim.isBlank())
                ? tenantIdClaim
                : DEFAULT_TENANT_ID_CLAIM;
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return resolveFromJwt(jwt);
        }

        return null;
    }

    /**
     * 从 JWT 中解析租户上下文。
     *
     * @param jwt JWT Token
     * @return 租户上下文，如果无法解析则返回 null
     */
    @Nullable
    private TenantContext resolveFromJwt(@NonNull Jwt jwt) {
        String tenantId = jwt.getClaimAsString(tenantIdClaim);
        if (tenantId == null || tenantId.isBlank()) {
            log.debug("JWT 中不包含租户 ID claim: {}", tenantIdClaim);
            return null;
        }

        log.debug("从 JWT 解析租户: tenantId={}", tenantId);
        return new DefaultTenantContext(tenantId);
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
