package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 权限查询 API（供资源服务器调用）。
 */
@RestController
@RequestMapping("/internal/permissions")
@RequiredArgsConstructor
public class PermissionQueryController {

    private final RbacService rbacService;

    /**
     * 检查用户是否具有指定权限。
     */
    @GetMapping("/check")
    public boolean hasPermission(
            @RequestParam String userId,
            @RequestParam String permission,
            @RequestParam @Nullable String tenantId) {
        return rbacService.hasPermission(userId, permission, tenantId);
    }

    /**
     * 检查用户是否具有指定角色。
     */
    @GetMapping("/check-role")
    public boolean hasRole(
            @RequestParam String userId,
            @RequestParam String role,
            @RequestParam @Nullable String tenantId) {
        return rbacService.hasRole(userId, role, tenantId);
    }

    /**
     * 获取用户的所有权限。
     */
    @GetMapping("/{userId}")
    public Set<String> getPermissions(
            @PathVariable String userId,
            @RequestParam @Nullable String tenantId) {
        return rbacService.getPermissions(userId, tenantId);
    }

    /**
     * 获取用户的所有角色。
     */
    @GetMapping("/{userId}/roles")
    public Set<String> getRoles(
            @PathVariable String userId,
            @RequestParam @Nullable String tenantId) {
        return rbacService.getRoles(userId, tenantId);
    }
}