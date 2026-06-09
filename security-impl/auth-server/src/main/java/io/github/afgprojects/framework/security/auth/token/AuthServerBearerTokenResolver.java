package io.github.afgprojects.framework.security.auth.token;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.login.TokenService;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auth Server Bearer Token 解析器。
 *
 * <p>从 HTTP 请求中提取 Bearer Token，使用 {@link TokenService} 验证 Token，
 * 然后使用 {@link JwtClaimsExtractor} 从解析后的 claims 提取用户信息，
 * 构建 {@link AfgAuthentication} 填充到 {@link SecurityContext}。
 *
 * <p>此解析器用于 auth-server 模块自身的端点认证（如 /auth-api/auth/user-info、
 * /auth-api/oauth2/authorize），不依赖 resource-server 模块的 JWT 验证机制。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从 Authorization 请求头提取 Bearer Token</li>
 *   <li>使用 TokenService 验证 Token 有效性</li>
 *   <li>使用 TokenService 提取用户信息（userId、username、roles、permissions、tenantId）</li>
 *   <li>构建 AfgAuthentication 并填充到 SecurityContext</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Slf4j
public class AuthServerBearerTokenResolver {

    private final TokenService tokenService;
    private final JwtClaimsExtractor claimsExtractor;

    /**
     * 使用默认 Claims 配置创建解析器。
     *
     * @param tokenService Token 服务
     */
    public AuthServerBearerTokenResolver(@NonNull TokenService tokenService) {
        this(tokenService, new JwtClaimsExtractor());
    }

    /**
     * 使用指定 Claims 提取器创建解析器。
     *
     * @param tokenService     Token 服务
     * @param claimsExtractor  JWT Claims 提取器
     */
    public AuthServerBearerTokenResolver(@NonNull TokenService tokenService,
                                          @NonNull JwtClaimsExtractor claimsExtractor) {
        this.tokenService = tokenService;
        this.claimsExtractor = claimsExtractor;
    }

    /**
     * 从 HTTP 请求解析 Bearer Token 并填充 SecurityContext。
     *
     * <p>如果请求中包含有效的 Bearer Token，则构建 AfgAuthentication
     * 并设置到 SecurityContextHolder。
     *
     * @param request HTTP 请求
     * @return 解析后的 AfgAuthentication，如果 Token 无效或不存在则返回 null
     */
    @Nullable
    public AfgAuthentication resolve(@NonNull HttpServletRequest request) {
        String token = JwtClaimsExtractor.extractBearerToken(request);
        if (token == null) {
            return null;
        }

        return resolveFromToken(token);
    }

    /**
     * 从 Token 字符串解析并构建 AfgAuthentication。
     *
     * @param token Bearer Token 值
     * @return 解析后的 AfgAuthentication，如果 Token 无效则返回 null
     */
    @Nullable
    public AfgAuthentication resolveFromToken(@NonNull String token) {
        try {
            if (!tokenService.validateAccessToken(token)) {
                log.debug("Bearer token validation failed");
                return null;
            }

            String userId = tokenService.extractUserId(token);
            String username = tokenService.extractUsername(token);
            Set<String> roles = tokenService.extractRoles(token);
            Set<String> permissions = tokenService.extractPermissions(token);
            String tenantId = tokenService.extractTenantId(token);

            if (userId == null) {
                log.debug("Bearer token does not contain user ID");
                return null;
            }

            AfgUserDetails userDetails = new BearerTokenUserDetails(
                    userId,
                    username != null ? username : userId,
                    tenantId,
                    roles,
                    permissions);

            AfgAuthentication authentication = new BearerTokenAuthentication(userDetails, token);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            log.debug("Authenticated user from Bearer token: userId={}, username={}", userId, username);
            return authentication;

        } catch (Exception e) {
            log.debug("Failed to resolve Bearer token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 基于 Bearer Token 的用户详情实现。
     */
    private static class BearerTokenUserDetails implements AfgUserDetails {

        private final String userId;
        private final String username;
        private final String tenantId;
        private final Set<String> roles;
        private final Set<String> permissions;

        BearerTokenUserDetails(String userId, String username, String tenantId,
                                Set<String> roles, Set<String> permissions) {
            this.userId = userId;
            this.username = username;
            this.tenantId = tenantId;
            this.roles = roles != null ? roles : Set.of();
            this.permissions = permissions != null ? permissions : Set.of();
        }

        @Override
        @NonNull
        public String getUserId() {
            return userId;
        }

        @Override
        @NonNull
        public String getUsername() {
            return username;
        }

        @Override
        @Nullable
        public String getTenantId() {
            return tenantId;
        }

        @Override
        @NonNull
        public Set<String> getRoles() {
            return roles;
        }

        @Override
        @NonNull
        public Collection<? extends GrantedAuthority> getAuthorities() {
            Set<GrantedAuthority> allAuthorities = new HashSet<>();
            roles.forEach(role -> allAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            permissions.forEach(perm -> allAuthorities.add(new SimpleGrantedAuthority(perm)));
            return List.copyOf(allAuthorities);
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    /**
     * 基于 Bearer Token 的 AFG 认证实现。
     */
    private static class BearerTokenAuthentication implements AfgAuthentication {

        private final AfgUserDetails userDetails;
        private final String token;

        BearerTokenAuthentication(AfgUserDetails userDetails, String token) {
            this.userDetails = userDetails;
            this.token = token;
        }

        @Override
        @NonNull
        public AfgUserDetails getUserDetails() {
            return userDetails;
        }

        @Override
        @NonNull
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return userDetails.getAuthorities();
        }

        @Override
        @Nullable
        public Object getCredentials() {
            return token;
        }

        @Override
        @Nullable
        public Object getDetails() {
            return null;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            throw new IllegalArgumentException("Cannot set authenticated on BearerTokenAuthentication");
        }

        @Override
        @NonNull
        public String getName() {
            return userDetails.getUsername();
        }
    }
}
