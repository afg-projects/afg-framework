package io.github.afgprojects.framework.governance.server.security;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * 治理中心用户详情实现
 */
@Data
public class GovernanceUserDetails implements AfgUserDetails {

    private final String userId;
    private final String username;
    private final String password;
    private final String displayName;
    private final String tenantId;
    private final Set<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String userType;
    private final boolean enabled;

    public GovernanceUserDetails(
            String userId,
            String username,
            String password,
            String displayName,
            String tenantId,
            Set<String> roles,
            Collection<? extends GrantedAuthority> authorities,
            String userType,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.tenantId = tenantId;
        this.roles = roles;
        this.authorities = authorities;
        this.userType = userType;
        this.enabled = enabled;
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
    public String getDisplayName() {
        return displayName;
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
        return authorities;
    }

    @Override
    @Nullable
    public String getUserType() {
        return userType;
    }

    @Override
    public boolean isAdmin() {
        return roles.contains("ADMIN") || roles.contains("SUPER_ADMIN");
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
}
