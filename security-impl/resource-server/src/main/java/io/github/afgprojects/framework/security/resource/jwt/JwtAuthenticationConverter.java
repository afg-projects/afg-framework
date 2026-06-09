package io.github.afgprojects.framework.security.resource.jwt;

import io.github.afgprojects.framework.security.core.authentication.AfgAuthentication;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.token.JwtClaimsConfig;
import io.github.afgprojects.framework.security.core.token.JwtClaimsExtractor;
import io.github.afgprojects.framework.security.resource.properties.jwt.ResourceSecurityJwtProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JWT 认证转换器。
 *
 * <p>将 Spring Security 的 JwtAuthenticationToken 转换为 AfgAuthentication。
 *
 * <p>使用 {@link JwtClaimsExtractor} 从 JWT Claims 中提取用户信息，
 * Claim 名称映射由 {@link JwtClaimsConfig} 统一管理。
 *
 * <p>使用示例：
 * <pre>{@code
 * JwtAuthenticationConverter converter = new JwtAuthenticationConverter(jwtConfig);
 * AfgAuthentication afgAuth = converter.convert(jwtAuthenticationToken);
 * }</pre>
 *
 * @since 1.0.0
 */
public class JwtAuthenticationConverter {

    private final JwtClaimsExtractor claimsExtractor;

    /**
     * 使用指定 JWT 配置属性构造转换器。
     *
     * @param jwtConfig JWT 配置属性（用于构建 Claims 提取器）
     */
    public JwtAuthenticationConverter(@NonNull ResourceSecurityJwtProperties jwtConfig) {
        JwtClaimsConfig claimsConfig = new JwtClaimsConfig();
        claimsConfig.setUserIdClaim(jwtConfig.getUserIdClaim());
        claimsConfig.setUsernameClaim(jwtConfig.getUsernameClaim());
        claimsConfig.setRolesClaim(jwtConfig.getRolesClaim());
        claimsConfig.setPermissionsClaim(jwtConfig.getPermissionsClaim());
        claimsConfig.setTenantIdClaim(jwtConfig.getTenantIdClaim());
        this.claimsExtractor = new JwtClaimsExtractor(claimsConfig);
    }

    /**
     * 使用指定 Claims 提取器构造转换器。
     *
     * @param claimsExtractor JWT Claims 提取器
     */
    public JwtAuthenticationConverter(@NonNull JwtClaimsExtractor claimsExtractor) {
        this.claimsExtractor = claimsExtractor;
    }

    /**
     * 转换 JWT 认证令牌。
     *
     * @param jwtToken JWT 认证令牌
     * @return AFG 认证令牌
     */
    @NonNull
    public AfgAuthentication convert(@NonNull JwtAuthenticationToken jwtToken) {
        AfgUserDetails userDetails = convertToUserDetails(jwtToken);
        return new JwtAfgAuthentication(jwtToken, userDetails);
    }

    /**
     * 从 JWT Token 提取用户详情。
     *
     * @param jwtToken JWT 认证令牌
     * @return 用户详情
     */
    @NonNull
    public AfgUserDetails convertToUserDetails(@NonNull JwtAuthenticationToken jwtToken) {
        Map<String, Object> claims = jwtToken.getToken().getClaims();

        String userId = claimsExtractor.extractUserId(claims);
        String username = claimsExtractor.extractUsername(claims);
        String tenantId = claimsExtractor.extractTenantId(claims);

        Set<String> roles = claimsExtractor.extractRoles(claims);
        Set<String> permissions = claimsExtractor.extractPermissions(claims);

        return new JwtUserDetails(
                userId,
                username != null ? username : userId,
                tenantId,
                roles,
                permissions,
                jwtToken.getAuthorities()
        );
    }

    /**
     * 基于 JWT 的用户详情实现。
     */
    private static class JwtUserDetails implements AfgUserDetails {

        private final String userId;
        private final String username;
        private final String tenantId;
        private final Set<String> roles;
        private final Set<String> permissions;
        private final Collection<? extends GrantedAuthority> authorities;

        JwtUserDetails(
                String userId,
                String username,
                String tenantId,
                Set<String> roles,
                Set<String> permissions,
                Collection<? extends GrantedAuthority> authorities) {
            this.userId = userId;
            this.username = username;
            this.tenantId = tenantId;
            this.roles = roles != null ? roles : Set.of();
            this.permissions = permissions != null ? permissions : Set.of();
            this.authorities = authorities;
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
            // 合并 JWT 权限和角色/权限
            Set<GrantedAuthority> allAuthorities = new HashSet<>(authorities);
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
     * 基于 JWT 的 AFG 认证实现。
     */
    private static class JwtAfgAuthentication implements AfgAuthentication {

        private final JwtAuthenticationToken jwtToken;
        private final AfgUserDetails userDetails;

        JwtAfgAuthentication(JwtAuthenticationToken jwtToken, AfgUserDetails userDetails) {
            this.jwtToken = jwtToken;
            this.userDetails = userDetails;
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
        public Object getCredentials() {
            return jwtToken.getCredentials();
        }

        @Override
        public Object getDetails() {
            return jwtToken.getDetails();
        }

        @Override
        public boolean isAuthenticated() {
            return jwtToken.isAuthenticated();
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            jwtToken.setAuthenticated(isAuthenticated);
        }

        @Override
        public String getName() {
            return userDetails.getUsername();
        }
    }
}