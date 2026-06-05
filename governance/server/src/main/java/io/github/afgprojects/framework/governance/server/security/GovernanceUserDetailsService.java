package io.github.afgprojects.framework.governance.server.security;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.governance.server.entity.user.GovRole;
import io.github.afgprojects.framework.governance.server.entity.user.GovUser;
import io.github.afgprojects.framework.governance.server.entity.user.GovUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 治理中心用户详情服务
 * <p>
 * 实现 AfgUserDetailsService 接口，提供用户认证信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernanceUserDetailsService implements AfgUserDetailsService {

    private final DataManager dataManager;

    @Override
    @NonNull
    public AfgUserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        GovUser user = dataManager.findOneByField(GovUser.class, GovUser::getUsername, username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.isDeleted() || user.getStatus() != 1) {
            throw new UsernameNotFoundException("User is disabled: " + username);
        }

        return buildUserDetails(user);
    }

    @Override
    @NonNull
    public AfgUserDetails loadUserByUserId(@NonNull String userId) throws UsernameNotFoundException {
        Long id = Long.parseLong(userId);
        GovUser user = dataManager.findById(GovUser.class, id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        if (user.isDeleted() || user.getStatus() != 1) {
            throw new UsernameNotFoundException("User is disabled: " + userId);
        }

        return buildUserDetails(user);
    }

    @Override
    @NonNull
    public AfgUserDetails loadUserByMobile(@NonNull String mobile) throws UsernameNotFoundException {
        GovUser user = dataManager.findOneByField(GovUser.class, GovUser::getMobile, mobile)
            .orElseThrow(() -> new UsernameNotFoundException("User not found by mobile: " + mobile));

        if (user.isDeleted() || user.getStatus() != 1) {
            throw new UsernameNotFoundException("User is disabled: " + mobile);
        }

        return buildUserDetails(user);
    }

    @Override
    @NonNull
    public AfgUserDetails loadUserByEmail(@NonNull String email) throws UsernameNotFoundException {
        GovUser user = dataManager.findOneByField(GovUser.class, GovUser::getEmail, email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found by email: " + email));

        if (user.isDeleted() || user.getStatus() != 1) {
            throw new UsernameNotFoundException("User is disabled: " + email);
        }

        return buildUserDetails(user);
    }

    /**
     * 构建 AfgUserDetails
     */
    private AfgUserDetails buildUserDetails(GovUser user) {
        // 获取用户角色
        Set<String> roles = new HashSet<>();

        // 获取权限（角色对应的权限）
        Collection<GrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());

        return new GovernanceUserDetails(
            String.valueOf(user.getId()),
            user.getUsername(),
            user.getPassword(),
            user.getRealName(),
            user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null,
            roles,
            authorities,
            user.getUserType(),
            user.getStatus() == 1
        );
    }

    /**
     * 获取用户角色
     */
    private Set<String> getUserRoles(Long userId) {
        List<GovUserRole> userRoles = dataManager.findAllByField(GovUserRole.class, GovUserRole::getUserId, userId);

        if (userRoles.isEmpty()) {
            return Set.of("USER");
        }

        Set<String> roles = new HashSet<>();
        for (GovUserRole ur : userRoles) {
            dataManager.findById(GovRole.class, ur.getRoleId())
                .filter(r -> !r.isDeleted() && r.getStatus() == 1)
                .ifPresent(r -> roles.add(r.getCode()));
        }

        // 默认角色
        if (roles.isEmpty()) {
            roles.add("USER");
        }

        return roles;
    }
}
