package io.github.afgprojects.framework.governance.server.controller.service;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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
    public Result<List<ServiceRegistry>> list() {
        return Result.success(dataManager.entity(ServiceRegistry.class)
            .query()
            .where(Conditions.builder(ServiceRegistry.class)
                .eq(ServiceRegistry::getStatus, 1)
                .eq(ServiceRegistry::isDeleted, false)
                .build())
            .list());
    }

    /**
     * 根据ID获取服务
     */
    @GetMapping("/{id}")
    public Result<ServiceRegistry> get(@PathVariable Long id) {
        return dataManager.findById(ServiceRegistry.class, id)
            .filter(s -> !s.isDeleted())
            .map(Result::success)
            .orElse(Result.fail(404, "Service not found: " + id));
    }

    /**
     * 根据编码获取服务
     */
    @GetMapping("/code/{code}")
    public Result<ServiceRegistry> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ServiceRegistry.class, ServiceRegistry::getCode, code)
            .filter(s -> !s.isDeleted())
            .map(Result::success)
            .orElse(Result.fail(404, "Service not found for code: " + code));
    }
}
