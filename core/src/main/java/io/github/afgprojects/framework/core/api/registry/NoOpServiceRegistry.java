package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 服务注册实现
 * <p>
 * 空操作降级实现，所有注册/注销/状态更新操作被忽略。
 * 适用于未配置 Nacos/Consul 等注册中心的场景。
 * <p>
 * 不由 AutoConfiguration 自动注册，仅在需要手动降级时使用。
 * 服务注册通常由 governance-client 或 Nacos/Consul 模块提供。
 *
 * @since 1.0.0
 */
public class NoOpServiceRegistry implements ServiceRegistry {

    @Override
    public void register(@NonNull ServiceInstance instance) {
        // no-op
    }

    @Override
    public void deregister(@NonNull ServiceInstance instance) {
        // no-op
    }

    @Override
    public void updateStatus(
            @NonNull String serviceId,
            @NonNull String instanceId,
            ServiceInstance.@NonNull Status status) {
        // no-op
    }

    @Override
    @NonNull
    public String getRegistryName() {
        return "noop";
    }

    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public void close() {
        // no-op
    }
}
