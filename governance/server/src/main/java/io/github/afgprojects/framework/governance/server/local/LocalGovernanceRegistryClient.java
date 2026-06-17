package io.github.afgprojects.framework.governance.server.local;

import io.github.afgprojects.framework.governance.server.entity.service.ServiceInstance;
import io.github.afgprojects.framework.governance.server.service.registry.ServiceRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地模式服务注册发现客户端
 * <p>
 * 当 governance-server 和 governance-client 在同一个 JVM 时，
 * 通过 Spring Bean 直接调用 {@link ServiceRegistryService} 替代 gRPC 通信。
 * 无需心跳（同 JVM），服务实例始终可用。
 * <p>
 * 实现 {@link SmartLifecycle} 接口，支持在 Spring 容器启动时自动注册服务实例。
 *
 * @author afg-projects
 */
@Slf4j
public class LocalGovernanceRegistryClient implements SmartLifecycle {

    private final ServiceRegistryService registryService;
    private final GovernanceLocalProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 当前实例ID
     */
    private volatile String instanceId;

    public LocalGovernanceRegistryClient(ServiceRegistryService registryService,
                                         GovernanceLocalProperties properties) {
        this.registryService = registryService;
        this.properties = properties;
    }

    // ==================== 服务注册 ====================

    /**
     * 注册服务实例
     *
     * @param host 主机地址
     * @param port 端口号
     * @return 注册的实例
     */
    public ServiceInstance register(String host, int port) {
        this.instanceId = java.util.UUID.randomUUID().toString();
        String serviceName = resolveServiceName();
        int weight = 100;
        String metadata = "";

        log.info("Registering service instance via local mode: serviceName={}, instanceId={}, host={}:{}",
                serviceName, instanceId, host, port);

        ServiceInstance instance = registryService.register(
                serviceName, instanceId, host, port, "http", weight, metadata);
        log.info("Service instance registered via local mode: {}", instanceId);
        return instance;
    }

    /**
     * 注销服务实例
     */
    public void deregister() {
        if (instanceId != null) {
            log.info("Deregistering service instance via local mode: {}", instanceId);
            registryService.deregister(instanceId);
            instanceId = null;
        }
    }

    // ==================== 服务发现 ====================

    /**
     * 发现服务的所有健康实例
     *
     * @param serviceName 服务名称
     * @return 健康实例列表
     */
    public List<ServiceInstance> discover(String serviceName) {
        return registryService.discover(serviceName);
    }

    /**
     * 获取一个健康实例（简单轮询负载均衡）
     *
     * @param serviceName 服务名称
     * @return 实例信息
     */
    public Optional<ServiceInstance> selectOneInstance(String serviceName) {
        List<ServiceInstance> instances = discover(serviceName);
        if (instances.isEmpty()) {
            return Optional.empty();
        }

        int index = Math.abs(roundRobinCounter.getAndIncrement()) % instances.size();
        return Optional.of(instances.get(index));
    }

    // ==================== SmartLifecycle ====================

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            if (properties.isAutoRegister()) {
                try {
                    String host = resolveHost();
                    int port = resolvePort();
                    register(host, port);
                } catch (Exception e) {
                    log.error("Failed to auto-register service via local mode", e);
                }
            }
            log.info("LocalGovernanceRegistryClient started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            deregister();
            log.info("LocalGovernanceRegistryClient stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        // 在其他 SmartLifecycle 组件之后启动，确保服务已准备就绪
        return Integer.MAX_VALUE - 100;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isAutoRegister();
    }

    // ==================== 状态查询 ====================

    /**
     * 获取当前实例ID
     *
     * @return 实例ID，未注册时返回 null
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 是否已注册
     *
     * @return 是否已注册
     */
    public boolean isRegistered() {
        return instanceId != null;
    }

    // ==================== 私有辅助方法 ====================

    private String resolveServiceName() {
        String name = properties.getServiceName();
        return (name != null && !name.isBlank()) ? name : "unknown";
    }

    private String resolveHost() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private int resolvePort() {
        // 本地模式下默认使用 8080，实际端口由应用配置决定
        return 8080;
    }
}
