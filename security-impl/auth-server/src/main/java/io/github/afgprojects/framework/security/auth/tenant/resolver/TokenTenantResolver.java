package io.github.afgprojects.framework.security.auth.tenant.resolver;

import io.github.afgprojects.framework.security.auth.tenant.model.SimpleTenantContext;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 从 Bearer Token 解析租户的解析器。
 *
 * <p>从 HTTP 请求的 Authorization 头中提取 Bearer Token，
 * 然后通过 {@link TokenService} 提取租户 ID。
 *
 * <p>这是最可信的解析方式，因为 Token 由授权服务器签发。
 *
 * @since 1.0.0
 */
public class TokenTenantResolver implements TenantResolver {

    /**
     * 默认优先级。
     */
    private static final int DEFAULT_ORDER = 10;

    private final TokenService tokenService;
    private int order = DEFAULT_ORDER;

    /**
     * 构造 Token 租户解析器。
     *
     * @param tokenService Token 服务，不能为 null
     */
    public TokenTenantResolver(@NonNull TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }

        return resolveFromToken(token);
    }

    @Override
    @Nullable
    public TenantContext resolveFromToken(@NonNull String token) {
        String tenantId = tokenService.extractTenantId(token);
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return new SimpleTenantContext(tenantId);
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
