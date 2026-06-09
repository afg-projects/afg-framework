package io.github.afgprojects.framework.security.core.token;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT Claims 提取工具类。
 *
 * <p>提供从 JWT Claims Map 中提取标准字段的通用方法，
 * 消除 auth-server 和 resource-server 中重复的解析逻辑。
 *
 * <p>设计为无状态工具类，通过 {@link JwtClaimsConfig} 配置 claim 名称映射。
 * 不依赖具体的 JWT 解析库（Nimbus JOSE / Spring OAuth2），
 * 仅操作 {@code Map<String, Object>} 形式的 claims，
 * 因此可同时被 auth-server 和 resource-server 使用。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用默认配置
 * JwtClaimsExtractor extractor = new JwtClaimsExtractor();
 * String userId = extractor.extractUserId(claims);
 *
 * // 使用自定义配置
 * JwtClaimsConfig config = new JwtClaimsConfig();
 * config.setTenantIdClaim("custom_tenant");
 * JwtClaimsExtractor extractor = new JwtClaimsExtractor(config);
 *
 * // 从 HttpServletRequest 提取 Bearer Token
 * String token = JwtClaimsExtractor.extractBearerToken(request);
 * }</pre>
 *
 * @since 1.0.0
 */
public class JwtClaimsExtractor {

    private final JwtClaimsConfig claimsConfig;

    /**
     * 使用默认配置创建提取器。
     */
    public JwtClaimsExtractor() {
        this(new JwtClaimsConfig());
    }

    /**
     * 使用指定配置创建提取器。
     *
     * @param claimsConfig JWT Claims 配置，如果为 null 则使用默认配置
     */
    public JwtClaimsExtractor(@Nullable JwtClaimsConfig claimsConfig) {
        this.claimsConfig = claimsConfig != null ? claimsConfig : new JwtClaimsConfig();
    }

    /**
     * 从 claims Map 提取用户 ID。
     *
     * @param claims JWT Claims Map
     * @return 用户 ID，如果不存在则返回 null
     */
    @Nullable
    public String extractUserId(@NonNull Map<String, Object> claims) {
        return getClaimAsString(claims, claimsConfig.getUserIdClaim());
    }

    /**
     * 从 claims Map 提取用户名。
     *
     * @param claims JWT Claims Map
     * @return 用户名，如果不存在则返回 null
     */
    @Nullable
    public String extractUsername(@NonNull Map<String, Object> claims) {
        return getClaimAsString(claims, claimsConfig.getUsernameClaim());
    }

    /**
     * 从 claims Map 提取租户 ID。
     *
     * @param claims JWT Claims Map
     * @return 租户 ID，如果不存在则返回 null
     */
    @Nullable
    public String extractTenantId(@NonNull Map<String, Object> claims) {
        return getClaimAsString(claims, claimsConfig.getTenantIdClaim());
    }

    /**
     * 从 claims Map 提取角色集合。
     *
     * @param claims JWT Claims Map
     * @return 角色集合，如果不存在则返回空集合
     */
    @NonNull
    public Set<String> extractRoles(@NonNull Map<String, Object> claims) {
        return getClaimAsStringSet(claims, claimsConfig.getRolesClaim());
    }

    /**
     * 从 claims Map 提取权限集合。
     *
     * @param claims JWT Claims Map
     * @return 权限集合，如果不存在则返回空集合
     */
    @NonNull
    public Set<String> extractPermissions(@NonNull Map<String, Object> claims) {
        return getClaimAsStringSet(claims, claimsConfig.getPermissionsClaim());
    }

    /**
     * 从 claims Map 提取 Token 类型。
     *
     * @param claims JWT Claims Map
     * @return Token 类型（如 "access"、"refresh"），如果不存在则返回 null
     */
    @Nullable
    public String extractTokenType(@NonNull Map<String, Object> claims) {
        return getClaimAsString(claims, claimsConfig.getTokenTypeClaim());
    }

    /**
     * 从 claims Map 提取 Issuer。
     *
     * @param claims JWT Claims Map
     * @return Issuer，如果不存在则返回 null
     */
    @Nullable
    public String extractIssuer(@NonNull Map<String, Object> claims) {
        return getClaimAsString(claims, claimsConfig.getIssuerClaim());
    }

    /**
     * 从 HttpServletRequest 提取 Bearer Token。
     *
     * <p>从 Authorization 请求头中提取 Bearer Token 值。
     *
     * @param request HTTP 请求
     * @return Bearer Token 值，如果不存在则返回 null
     */
    @Nullable
    public static String extractBearerToken(@NonNull HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        return extractBearerToken(authorizationHeader);
    }

    /**
     * 从 Authorization header 提取 Bearer Token。
     *
     * <p>支持 "Bearer {token}" 格式的 Authorization 头。
     *
     * @param authorizationHeader Authorization 请求头值
     * @return Bearer Token 值，如果格式不正确则返回 null
     */
    @Nullable
    public static String extractBearerToken(@Nullable String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取当前使用的 Claims 配置。
     *
     * @return JWT Claims 配置
     */
    @NonNull
    public JwtClaimsConfig getClaimsConfig() {
        return claimsConfig;
    }

    /**
     * 从 claims 中获取字符串值。
     *
     * @param claims    JWT Claims Map
     * @param claimName claim 名称
     * @return 字符串值，如果不存在则返回 null
     */
    @Nullable
    private String getClaimAsString(@NonNull Map<String, Object> claims, @NonNull String claimName) {
        Object value = claims.get(claimName);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    /**
     * 从 claims 中获取字符串集合。
     *
     * <p>支持以下类型的值：
     * <ul>
     *   <li>{@code Collection<?>} - 逐个转换为字符串</li>
     *   <li>{@code String} - 逗号分隔后转为集合</li>
     * </ul>
     *
     * @param claims    JWT Claims Map
     * @param claimName claim 名称
     * @return 字符串集合，如果不存在则返回空集合
     */
    @NonNull
    private Set<String> getClaimAsStringSet(@NonNull Map<String, Object> claims, @NonNull String claimName) {
        Object value = claims.get(claimName);
        if (value == null) {
            return Set.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        }
        if (value instanceof String str) {
            // 支持逗号分隔的字符串
            if (str.isBlank()) {
                return Set.of();
            }
            return Set.of(str.split(","));
        }
        return Set.of();
    }
}
