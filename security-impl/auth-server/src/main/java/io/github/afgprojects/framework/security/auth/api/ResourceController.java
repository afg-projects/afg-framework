package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.auth.permission.entity.SecResource;
import io.github.afgprojects.framework.security.auth.permission.service.JdbcResourceService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源管理 API。
 */
@RestController
@RequestMapping("/resources")
public class ResourceController {

    private final JdbcResourceService resourceService;

    public ResourceController(JdbcResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public SecResource create(@RequestBody SecResource resource) {
        return resourceService.create(resource);
    }

    @PutMapping("/{id}")
    public SecResource update(@PathVariable String id, @RequestBody SecResource resource) {
        resource.setId(id);
        return resourceService.update(resource);
    }

    @GetMapping("/{id}")
    public SecResource getById(@PathVariable String id) {
        return resourceService.findById(id).orElse(null);
    }

    @GetMapping
    public List<SecResource> list(@RequestParam @Nullable String tenantId) {
        return resourceService.findAll(tenantId);
    }

    @GetMapping("/tree")
    public List<SecResource> tree(@RequestParam @Nullable String tenantId) {
        return resourceService.getResourceTree(tenantId);
    }

    @GetMapping("/type/{type}")
    public List<SecResource> listByType(@PathVariable String type, @RequestParam @Nullable String tenantId) {
        return resourceService.findByType(type, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        resourceService.delete(id);
    }
}
