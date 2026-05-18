package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.permission.entity.SecPermission;
import io.github.afgprojects.framework.security.permission.service.JdbcResourceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final JdbcResourceService resourceService;

    @PostMapping
    public SecPermission create(@RequestBody SecPermission permission) {
        return resourceService.createPermission(permission);
    }

    @GetMapping("/{code}")
    public SecPermission getByCode(@PathVariable String code, @RequestParam @Nullable String tenantId) {
        return resourceService.findPermissionByCode(code, tenantId).orElse(null);
    }

    @GetMapping
    public List<SecPermission> list(@RequestParam @Nullable String tenantId) {
        return resourceService.findAllPermissions(tenantId);
    }

    @GetMapping("/resource/{resourceId}")
    public List<SecPermission> listByResource(@PathVariable Long resourceId, @RequestParam @Nullable String tenantId) {
        return resourceService.findPermissionsByResource(resourceId, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        resourceService.delete(id);
    }
}
