package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.security.datascope.entity.SecDataScope;
import io.github.afgprojects.framework.security.datascope.service.JdbcDataScopeService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/data-scopes")
@RequiredArgsConstructor
public class DataScopeController {

    private final JdbcDataScopeService dataScopeService;

    @PostMapping
    public SecDataScope create(@RequestBody SecDataScope dataScope) {
        return dataScopeService.create(dataScope);
    }

    @GetMapping("/{id}")
    public SecDataScope getById(@PathVariable Long id) {
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
    public void setUserScopes(@PathVariable String userId, @RequestBody Set<Long> scopeIds, @RequestParam @Nullable String tenantId) {
        dataScopeService.setUserDataScopes(userId, scopeIds, tenantId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dataScopeService.delete(id);
    }
}
