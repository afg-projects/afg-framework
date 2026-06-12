package io.github.afgprojects.framework.core.api.registry;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 服务发现实现
 * <p>
 * 空操作降级实现，所有查询返回空列表/空 Optional，监听器操作被忽略。
 * 适用于未配置 Nacos/Consul 等注册中心的场景。
 * <p>
 * 不由 AutoConfiguration 自动注册，仅在需要手动降级时使用。
 * 服务发现通常由 governance-client 或 Nacos/Consul 模块提供。
 *
 * @since 1.0.0
 */
public class NoOpServiceDiscovery implements ServiceDiscovery {

    @Override
    @NonNull
    public List<ServiceInstance> getInstances(@NonNull String serviceId) {
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public Optional<ServiceInstance> getInstance(@NonNull String serviceId) {
        return Optional.empty();
    }

    @Override
    @NonNull
    public Optional<ServiceInstance> getInstance(
            @NonNull String serviceId,
            @NonNull LoadBalanceStrategy strategy) {
        return Optional.empty();
    }

    @Override
    @NonNull
    public List<String> getServices() {
        return Collections.emptyList();
    }

    @Override
    public void addListener(
            @NonNull String serviceId,
            @NonNull ServiceInstanceListener listener) {
        // no-op
    }

    @Override
    public void removeListener(
            @NonNull String serviceId,
            @NonNull ServiceInstanceListener listener) {
        // no-op
    }

    @Override
    @NonNull
    public String getDiscoveryName() {
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
