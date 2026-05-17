package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;

/**
 * 服务注册接口。
 *
 * <p>提供服务实例注册能力，用于将服务实例信息注册到注册中心。
 * 支持多种注册中心实现（Nacos、Consul、Eureka 等）。
 *
 * <p>使用示例：
 * <pre>{@code
 * ServiceRegistry registry = ...;
 *
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("user-service")
 *     .host("192.168.1.100")
 *     .port(8080)
 *     .metadata("version", "1.0.0")
 *     .build();
 *
 * // 注册服务
 * registry.register(instance);
 *
 * // 注销服务
 * registry.deregister(instance);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ServiceRegistry extends AutoCloseable {

    /**
     * 注册服务实例。
     *
     * @param instance 服务实例信息
     * @throws ServiceRegistryException 注册失败
     */
    void register(@NonNull ServiceInstance instance);

    /**
     * 注销服务实例。
     *
     * @param instance 服务实例信息
     */
    void deregister(@NonNull ServiceInstance instance);

    /**
     * 更新服务实例状态。
     *
     * @param serviceId   服务 ID
     * @param instanceId  实例 ID
     * @param status      实例状态
     */
    void updateStatus(
        @NonNull String serviceId,
        @NonNull String instanceId,
        ServiceInstance.@NonNull Status status
    );

    /**
     * 获取注册中心名称。
     *
     * @return 注册中心名称（如 "nacos", "consul", "eureka"）
     */
    @NonNull
    String getRegistryName();

    /**
     * 健康检查。
     *
     * @return 如果连接正常返回 true
     */
    boolean isHealthy();

    /**
     * 关闭注册中心连接。
     */
    @Override
    void close();
}