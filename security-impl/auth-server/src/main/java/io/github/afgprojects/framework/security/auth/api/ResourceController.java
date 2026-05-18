package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.permission.entity.SecResource;
import io.github.afgprojects.framework.security.permission.service.JdbcResourceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final JdbcResourceService resourceService;

    @PostMapping
    public SecResource create(@RequestBody SecResource resource) {
        return resourceService.create(resource);
    }

    @PutMapping("/{id}")
    public SecResource update(@PathVariable Long id, @RequestBody SecResource resource) {
        resource.setId(id);
        return resourceService.update(resource);
    }

    @GetMapping("/{id}")
    public SecResource getById(@PathVariable Long id) {
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
    public void delete(@PathVariable Long id) {
        resourceService.delete(id);
    }
}
