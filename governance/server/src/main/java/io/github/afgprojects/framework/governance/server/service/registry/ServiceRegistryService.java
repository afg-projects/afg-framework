package io.github.afgprojects.framework.governance.server.service.registry;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceInstance;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 服务注册服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryService {

    private final DataManager dataManager;

    /**
     * 注册或更新服务实例
     *
     * @param serviceName 服务名称
     * @param instanceId  实例ID
     * @param host        主机地址
     * @param port        端口号
     * @param protocol    协议
     * @param weight      权重
     * @param metadata    元数据
     * @return 注册的实例
     */
    @Transactional
    public ServiceInstance register(String serviceName, String instanceId, String host, int port,
                                    String protocol, int weight, String metadata) {
        // 查找或创建服务注册记录
        ServiceRegistry registry = dataManager.findOneByField(ServiceRegistry.class, ServiceRegistry::getName, serviceName)
                .orElseGet(() -> {
                    ServiceRegistry newRegistry = new ServiceRegistry();
                    newRegistry.setCode(serviceName.toLowerCase().replace(" ", "-"));
                    newRegistry.setName(serviceName);
                    newRegistry.setStatus(1);
                    return dataManager.save(ServiceRegistry.class, newRegistry);
                });

        // 查找或创建实例
        ServiceInstance instance = dataManager.findOneByField(ServiceInstance.class, ServiceInstance::getInstanceId, instanceId)
                .orElseGet(() -> {
                    ServiceInstance newInstance = new ServiceInstance();
                    newInstance.setInstanceId(instanceId);
                    newInstance.setServiceId(registry.getId());
                    return newInstance;
                });

        // 更新实例信息
        instance.setServiceId(registry.getId());
        instance.setHost(host);
        instance.setPort(port);
        instance.setProtocol(protocol);
        instance.setWeight(weight);
        instance.setMetadata(metadata);
        instance.setStatus(1);
        instance.setLastHeartbeat(Instant.now());

        ServiceInstance saved = dataManager.save(ServiceInstance.class, instance);
        log.info("Service instance registered: {} [{}:{}] weight={}", instanceId, host, port, weight);
        return saved;
    }

    /**
     * 注销服务实例（软删除）
     *
     * @param instanceId 实例ID
     */
    @Transactional
    public void deregister(String instanceId) {
        dataManager.findOneByField(ServiceInstance.class, ServiceInstance::getInstanceId, instanceId)
                .ifPresent(instance -> {
                    instance.setStatus(0);
                    dataManager.save(ServiceInstance.class, instance);
                    log.info("Service instance deregistered: {}", instanceId);
                });
    }

    /**
     * 发现服务的所有健康实例
     *
     * @param serviceName 服务名称
     * @return 健康实例列表
     */
    public List<ServiceInstance> discover(String serviceName) {
        // 查找服务注册记录
        Optional<ServiceRegistry> registryOpt = dataManager.findOneByField(
                ServiceRegistry.class, ServiceRegistry::getName, serviceName);

        if (registryOpt.isEmpty()) {
            log.debug("Service not found: {}", serviceName);
            return List.of();
        }

        String serviceId = registryOpt.get().getId();

        // 查询所有在线实例
        return dataManager.entity(ServiceInstance.class)
                .query()
                .where(builder(ServiceInstance.class)
                        .eq(ServiceInstance::getServiceId, serviceId)
                        .eq(ServiceInstance::getStatus, 1)
                        .build())
                .list();
    }

    /**
     * 发送心跳
     *
     * @param instanceId 实例ID
     */
    @Transactional
    public void heartbeat(String instanceId) {
        dataManager.findOneByField(ServiceInstance.class, ServiceInstance::getInstanceId, instanceId)
                .ifPresent(instance -> {
                    instance.setLastHeartbeat(Instant.now());
                    dataManager.save(ServiceInstance.class, instance);
                    log.debug("Heartbeat received: {}", instanceId);
                });
    }
}
