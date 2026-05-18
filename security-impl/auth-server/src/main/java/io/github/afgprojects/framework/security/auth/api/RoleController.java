package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.permission.entity.SecRole;
import io.github.afgprojects.framework.security.permission.service.JdbcRoleService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final JdbcRoleService roleService;

    @PostMapping
    public SecRole create(@RequestBody SecRole role) {
        return roleService.create(role);
    }

    @PutMapping("/{id}")
    public SecRole update(@PathVariable Long id, @RequestBody SecRole role) {
        role.setId(id);
        return roleService.update(role);
    }

    @GetMapping("/{id}")
    public SecRole getById(@PathVariable Long id) {
        return roleService.findById(id).orElse(null);
    }

    @GetMapping
    public List<SecRole> list(@RequestParam @Nullable String tenantId) {
        return roleService.findAll(tenantId);
    }

    @PostMapping("/{id}/permissions")
    public void setPermissions(@PathVariable Long id, @RequestBody Set<Long> permissionIds, @RequestParam @Nullable String tenantId) {
        roleService.setRolePermissions(id, permissionIds, tenantId);
    }

    @GetMapping("/{id}/permissions")
    public Set<Long> getPermissions(@PathVariable Long id) {
        return roleService.getRolePermissions(id);
    }

    @PostMapping("/{id}/parent/{parentId}")
    public void setParent(@PathVariable Long id, @PathVariable Long parentId, @RequestParam @Nullable String tenantId) {
        roleService.setParentRole(id, parentId, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
