package io.github.afgprojects.framework.governance.server.controller.service;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务注册控制器
 */
@Slf4j
@RestController
@RequestMapping("/console/service/registries")
@RequiredArgsConstructor
public class ServiceRegistryController {

    private final DataManager dataManager;

    /**
     * 获取所有服务列表
     */
    @GetMapping
    public List<ServiceRegistry> list() {
        return dataManager.entity(ServiceRegistry.class)
            .query()
            .where(Conditions.builder(ServiceRegistry.class)
                .eq(ServiceRegistry::getStatus, 1)
                .eq(ServiceRegistry::isDeleted, false)
                .build())
            .list();
    }

    /**
     * 根据ID获取服务
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRegistry> get(@PathVariable Long id) {
        return dataManager.findById(ServiceRegistry.class, id)
            .filter(s -> !s.isDeleted())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据编码获取服务
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ServiceRegistry> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ServiceRegistry.class, ServiceRegistry::getCode, code)
            .filter(s -> !s.isDeleted())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
