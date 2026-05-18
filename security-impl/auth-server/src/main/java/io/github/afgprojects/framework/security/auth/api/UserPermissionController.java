package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.core.permission.RbacService;
import io.github.afgprojects.framework.security.permission.entity.SecRole;
import io.github.afgprojects.framework.security.permission.service.JdbcRoleService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/user-permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final RbacService rbacService;
    private final JdbcRoleService roleService;

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
    public void assignRole(@PathVariable String userId, @PathVariable Long roleId, @RequestParam @Nullable String tenantId) {
        roleService.assignRoleToUser(userId, roleId, tenantId);
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public void removeRole(@PathVariable String userId, @PathVariable Long roleId, @RequestParam @Nullable String tenantId) {
        roleService.removeRoleFromUser(userId, roleId, tenantId);
    }
}
