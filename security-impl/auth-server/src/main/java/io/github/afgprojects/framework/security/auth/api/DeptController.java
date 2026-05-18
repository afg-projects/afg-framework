package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.datascope.entity.SecDept;
import io.github.afgprojects.framework.security.datascope.service.JdbcDeptService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final JdbcDeptService deptService;

    @PostMapping
    public SecDept create(@RequestBody SecDept dept) {
        return deptService.create(dept);
    }

    @PutMapping("/{id}")
    public SecDept update(@PathVariable Long id, @RequestBody SecDept dept) {
        dept.setId(id);
        return deptService.update(dept);
    }

    @GetMapping("/{id}")
    public SecDept getById(@PathVariable Long id) {
        return deptService.findById(id).orElse(null);
    }

    @GetMapping
    public List<SecDept> list(@RequestParam String tenantId) {
        return deptService.findAll(tenantId);
    }

    @GetMapping("/tree")
    public List<SecDept> tree(@RequestParam String tenantId) {
        return deptService.getDeptTree(tenantId);
    }

    @GetMapping("/user/{userId}")
    public List<SecDept> userDepts(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return deptService.getUserDepts(userId, tenantId);
    }

    @GetMapping("/user/{userId}/primary")
    public SecDept primaryDept(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return deptService.getPrimaryDept(userId, tenantId).orElse(null);
    }

    @PostMapping("/user/{userId}")
    public void setUserDept(@PathVariable String userId, @RequestParam Long deptId, @RequestParam @Nullable String tenantId, @RequestParam(defaultValue = "true") boolean isPrimary) {
        deptService.setUserDept(userId, deptId, tenantId, isPrimary);
    }

    @GetMapping("/{id}/children")
    public Set<Long> childIds(@PathVariable Long id, @RequestParam String tenantId) {
        return deptService.getChildDeptIds(id, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deptService.delete(id);
    }
}
