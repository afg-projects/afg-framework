package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 服务发现接口。
 *
 * <p>提供服务实例发现能力，支持从注册中心获取服务实例列表，
 * 并监听服务实例变化。
 *
 * <p>使用示例：
 * <pre>{@code
 * ServiceDiscovery discovery = ...;
 *
 * // 获取所有实例
 * List<ServiceInstance> instances = discovery.getInstances("user-service");
 *
 * // 获取单个实例（自动负载均衡）
 * Optional<ServiceInstance> instance = discovery.getInstance("user-service");
 *
 * // 监听服务变化
 * discovery.addListener("user-service", event -> {
 *     System.out.println("Service changed: " + event.changeType());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ServiceDiscovery extends AutoCloseable {

    /**
     * 获取服务实例列表。
     *
     * @param serviceId 服务 ID
     * @return 服务实例列表（只包含 UP 状态的实例）
     */
    @NonNull
    List<ServiceInstance> getInstances(@NonNull String serviceId);

    /**
     * 获取单个服务实例（默认负载均衡策略）。
     *
     * <p>使用默认的负载均衡策略（RANDOM）选择一个实例。
     *
     * @param serviceId 服务 ID
     * @return 服务实例，不存在返回 empty
     */
    @NonNull
    Optional<ServiceInstance> getInstance(@NonNull String serviceId);

    /**
     * 获取单个服务实例（指定负载均衡策略）。
     *
     * @param serviceId 服务 ID
     * @param strategy  负载均衡策略
     * @return 服务实例，不存在返回 empty
     */
    @NonNull
    Optional<ServiceInstance> getInstance(
        @NonNull String serviceId,
        @NonNull LoadBalanceStrategy strategy
    );

    /**
     * 获取所有服务名称。
     *
     * @return 服务名称列表
     */
    @NonNull
    List<String> getServices();

    /**
     * 添加服务实例变化监听器。
     *
     * @param serviceId 服务 ID
     * @param listener  监听器
     */
    void addListener(
        @NonNull String serviceId,
        @NonNull ServiceInstanceListener listener
    );

    /**
     * 移除服务实例变化监听器。
     *
     * @param serviceId 服务 ID
     * @param listener  监听器
     */
    void removeListener(
        @NonNull String serviceId,
        @NonNull ServiceInstanceListener listener
    );

    /**
     * 获取发现中心名称。
     *
     * @return 发现中心名称（如 "nacos", "consul"）
     */
    @NonNull
    String getDiscoveryName();

    /**
     * 健康检查。
     *
     * @return 如果连接正常返回 true
     */
    boolean isHealthy();

    /**
     * 关闭发现中心连接。
     */
    @Override
    void close();

    /**
     * 负载均衡策略。
     */
    enum LoadBalanceStrategy {
        /**
         * 随机选择。
         */
        RANDOM,

        /**
         * 轮询选择。
         */
        ROUND_ROBIN,

        /**
         * 加权随机（根据实例权重）。
         */
        WEIGHTED_RANDOM,

        /**
         * 最少连接（选择负载最低的实例）。
         */
        LEAST_CONNECTIONS
    }
}