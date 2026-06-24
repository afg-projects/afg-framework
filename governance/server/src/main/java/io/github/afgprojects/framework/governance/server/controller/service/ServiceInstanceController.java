package io.github.afgprojects.framework.governance.server.controller.service;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceInstance;
import io.github.afgprojects.framework.governance.server.service.registry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 服务实例控制器
 */
@Slf4j
@RestController
@RequestMapping("/console/service/instances")
@RequiredArgsConstructor
public class ServiceInstanceController {

    private final DataManager dataManager;
    private final ServiceRegistryService serviceRegistryService;

    /**
     * 获取实例列表
     *
     * @param serviceId 可选的服务ID参数
     */
    @GetMapping
    public Result<List<ServiceInstance>> list(@RequestParam(required = false) String serviceId) {
        var builder = Conditions.builder(ServiceInstance.class)
            .eq(ServiceInstance::isDeleted, false);

        if (serviceId != null) {
            builder.eq(ServiceInstance::getServiceId, serviceId);
        }

        return Result.success(dataManager.entity(ServiceInstance.class)
            .query()
            .where(builder.build())
            .list());
    }

    /**
     * 根据ID获取实例
     */
    @GetMapping("/{id}")
    public Result<ServiceInstance> get(@PathVariable String id) {
        return dataManager.findById(ServiceInstance.class, id)
            .filter(i -> !i.isDeleted())
            .map(Result::success)
            .orElse(Result.fail(404, "Service instance not found: " + id));
    }

    /**
     * 更新实例权重
     */
    @PutMapping("/{id}/weight")
    @Transactional
    public Result<ServiceInstance> updateWeight(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        ServiceInstance instance = dataManager.findById(ServiceInstance.class, id)
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Service instance not found: " + id));

        Integer weight = body.get("weight");
        if (weight == null || weight < 0 || weight > 1000) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "Weight must be between 0 and 1000");
        }

        instance.setWeight(weight);
        ServiceInstance saved = dataManager.save(ServiceInstance.class, instance);
        log.info("Updated instance {} weight to {}", instance.getInstanceId(), weight);
        return Result.success(saved);
    }

    /**
     * 注销实例
     */
    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> deregister(@PathVariable String id) {
        ServiceInstance instance = dataManager.findById(ServiceInstance.class, id)
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Service instance not found: " + id));

        serviceRegistryService.deregister(instance.getInstanceId());
        log.info("Deregistered instance: {}", instance.getInstanceId());
        return Result.success(null);
    }
}
