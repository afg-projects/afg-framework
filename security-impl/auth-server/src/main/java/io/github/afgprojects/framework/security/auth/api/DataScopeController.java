package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.auth.datascope.entity.SecDataScope;
import io.github.afgprojects.framework.security.auth.datascope.service.JdbcDataScopeService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 数据范围管理 API。
 */
@RestController
@RequestMapping("/data-scopes")
public class DataScopeController {

    private final JdbcDataScopeService dataScopeService;

    public DataScopeController(JdbcDataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    @PostMapping
    public SecDataScope create(@RequestBody SecDataScope dataScope) {
        return dataScopeService.create(dataScope);
    }

    @GetMapping("/{id}")
    public SecDataScope getById(@PathVariable String id) {
        return dataScopeService.findById(id).orElse(null);
    }

    @GetMapping
    public List<SecDataScope> list(@RequestParam @Nullable String tenantId) {
        return dataScopeService.findAll(tenantId);
    }

    @GetMapping("/user/{userId}")
    public List<SecDataScope> userScopes(@PathVariable String userId, @RequestParam @Nullable String tenantId) {
        return dataScopeService.getUserDataScopes(userId, tenantId);
    }

    @PostMapping("/user/{userId}")
    public void setUserScopes(@PathVariable String userId, @RequestBody Set<String> scopeIds, @RequestParam @Nullable String tenantId) {
        dataScopeService.setUserDataScopes(userId, scopeIds, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        dataScopeService.delete(id);
    }
}
