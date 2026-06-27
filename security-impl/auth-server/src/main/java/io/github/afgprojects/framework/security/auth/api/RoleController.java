package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.permission.entity.SecPermission;
import io.github.afgprojects.framework.security.auth.permission.entity.SecRole;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcRoleService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DataManager dataManager;

    public RoleController(JdbcRoleService roleService, DataManager dataManager) {
        this.roleService = roleService;
        this.dataManager = dataManager;
    }

    @PostMapping
    public Result<SecRole> create(@RequestBody SecRole role) {
        return Result.success(roleService.create(role));
    }

    @PutMapping("/{id}")
    public Result<SecRole> update(@PathVariable String id, @RequestBody SecRole role) {
        role.setId(id);
        return Result.success(roleService.update(role));
    }

    @GetMapping("/{id}")
    public Result<SecRole> getById(@PathVariable String id) {
        return Result.success(roleService.findById(id).orElse(null));
    }

    @GetMapping
    public Result<List<SecRole>> list(@RequestParam @Nullable String tenantId) {
        return Result.success(roleService.findAll(tenantId));
    }

    /**
     * 设置角色权限。
     *
     * <p>接收 permissionCodes（三段式权限码，如 "system:user:list"），
     * 内部查找对应的 sec_permission.id 后写入 sec_role_permission。
     * 同时双写到 sec_casbin_rule 并 reload Casbin 策略。
     *
     * @param id 角色 ID
     * @param request 权限设置请求，包含 permissionCodes 列表
     * @param tenantId 租户 ID
     */
    @PostMapping("/{id}/permissions")
    public Result<Boolean> setPermissions(@PathVariable String id,
                                          @RequestBody PermissionSetRequest request,
                                          @RequestParam @Nullable String tenantId) {
        Set<String> permissionIds = resolvePermissionIds(request.getPermissionCodes(), tenantId);
        roleService.setRolePermissions(id, permissionIds, tenantId);
        return Result.success(true);
    }

    /**
     * 获取角色权限。
     *
     * <p>返回 permission_code 而非 permission_id，便于前端使用。
     */
    @GetMapping("/{id}/permissions")
    public Result<PermissionListResponse> getPermissions(@PathVariable String id) {
        Set<String> permissionIds = roleService.getRolePermissions(id);

        List<PermissionInfo> permissions = permissionIds.stream()
            .map(pid -> dataManager.findById(SecPermission.class, pid))
            .filter(opt -> opt.isPresent())
            .map(opt -> {
                SecPermission perm = opt.get();
                return new PermissionInfo(perm.getId(), perm.getPermissionCode(), perm.getPermissionName());
            })
            .collect(Collectors.toList());

        return Result.success(new PermissionListResponse(id, permissions));
    }

    @PostMapping("/{id}/parent/{parentId}")
    public Result<Boolean> setParent(@PathVariable String id, @PathVariable String parentId, @RequestParam @Nullable String tenantId) {
        roleService.setParentRole(id, parentId, tenantId);
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable String id) {
        roleService.delete(id);
        return Result.success(true);
    }

    /**
     * 根据 permission_codes 查找对应的 sec_permission.id 集合。
     */
    private Set<String> resolvePermissionIds(Set<String> permissionCodes, @Nullable String tenantId) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> ids = new java.util.HashSet<>();
        for (String code : permissionCodes) {
            var condition = Conditions.builder(SecPermission.class)
                .eq(SecPermission::getPermissionCode, code);
            if (tenantId != null) {
                condition.eq(SecPermission::getTenantId, tenantId);
            }
            dataManager.findOne(SecPermission.class, condition.build())
                .ifPresent(perm -> ids.add(perm.getId()));
        }
        return ids;
    }

    /**
     * 权限设置请求
     */
    @lombok.Data
    public static class PermissionSetRequest {
        /** 权限码列表（三段式，如 "system:user:list"） */
        private Set<String> permissionCodes;
    }

    /**
     * 权限列表响应
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PermissionListResponse {
        private String roleId;
        private List<PermissionInfo> permissions;
    }

    /**
     * 权限信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PermissionInfo {
        private String id;
        private String permissionCode;
        private String permissionName;
    }
}
