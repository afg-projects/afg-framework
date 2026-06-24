package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.auth.permission.entity.SecRole;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 用户权限管理 API。
 */
@RestController
@RequestMapping("/user-permissions")
public class UserPermissionController {

    private final RbacService rbacService;
    private final JdbcRoleService roleService;

    public UserPermissionController(RbacService rbacService, JdbcRoleService roleService) {
        this.rbacService = rbacService;
        this.roleService = roleService;
    }

    @GetMapping("/{userId}/permissions")
    public Set<String> getPermissions(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return rbacService.getPermissions(userId, tenantId);
    }

    @GetMapping("/{userId}/roles")
    public Set<String> getRoles(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return rbacService.getRoles(userId, tenantId);
    }

    @GetMapping("/{userId}/roles/detail")
    public List<SecRole> getRoleDetails(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return roleService.getUserRoles(userId, tenantId);
    }

    @GetMapping("/{userId}/has-permission")
    public boolean hasPermission(@PathVariable String userId, @RequestParam String permission, @RequestParam @Nullable String tenantId) {
        return rbacService.hasPermission(userId, permission, tenantId);
    }

    @GetMapping("/{userId}/has-role")
    public boolean hasRole(@PathVariable String userId, @RequestParam String role, @RequestParam @Nullable String tenantId) {
        return rbacService.hasRole(userId, role, tenantId);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public void assignRole(@PathVariable String userId, @PathVariable String roleId, @RequestParam @Nullable String tenantId) {
        roleService.assignRoleToUser(userId, roleId, tenantId);
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public void removeRole(@PathVariable String userId, @PathVariable String roleId, @RequestParam @Nullable String tenantId) {
        roleService.removeRoleFromUser(userId, roleId, tenantId);
    }
}
