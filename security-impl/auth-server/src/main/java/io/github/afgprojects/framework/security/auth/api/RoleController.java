package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.security.auth.permission.entity.SecRole;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色管理 API。
 *
 * <p>注意：此类通过 @Bean 注册而非组件扫描，因此需要显式包含
 * 模块 contextPath 前缀（/auth-api），不会被 ModuleWebAutoConfiguration
 * 自动添加。
 */
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final JdbcRoleService roleService;

    public RoleController(JdbcRoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public Result<SecRole> create(@RequestBody SecRole role) {
        return Result.success(roleService.create(role));
    }

    @PutMapping("/{id}")
    public Result<SecRole> update(@PathVariable Long id, @RequestBody SecRole role) {
        role.setId(id);
        return Result.success(roleService.update(role));
    }

    @GetMapping("/{id}")
    public Result<SecRole> getById(@PathVariable Long id) {
        return Result.success(roleService.findById(id).orElse(null));
    }

    @GetMapping
    public Result<List<SecRole>> list(@RequestParam @Nullable String tenantId) {
        return Result.success(roleService.findAll(tenantId));
    }

    @PostMapping("/{id}/permissions")
    public Result<Boolean> setPermissions(@PathVariable Long id, @RequestBody Set<Long> permissionIds, @RequestParam @Nullable String tenantId) {
        roleService.setRolePermissions(id, permissionIds, tenantId);
        return Result.success(true);
    }

    @GetMapping("/{id}/permissions")
    public Result<Set<Long>> getPermissions(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissions(id));
    }

    @PostMapping("/{id}/parent/{parentId}")
    public Result<Boolean> setParent(@PathVariable Long id, @PathVariable Long parentId, @RequestParam @Nullable String tenantId) {
        roleService.setParentRole(id, parentId, tenantId);
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success(true);
    }
}
