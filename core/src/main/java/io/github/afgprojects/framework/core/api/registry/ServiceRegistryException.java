package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.Nullable;

/**
 * 服务注册异常。
 *
 * @since 1.0.0
 */
public class ServiceRegistryException extends RuntimeException {

    private final String serviceId;
    private final String instanceId;

    /**
     * 创建异常。
     *
     * @param message 错误消息
     */
    public ServiceRegistryException(@Nullable String message) {
        super(message);
        this.serviceId = null;
        this.instanceId = null;
    }

    /**
     * 创建异常。
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public ServiceRegistryException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.serviceId = null;
        this.instanceId = null;
    }

    /**
     * 创建异常（带服务信息）。
     *
     * @param serviceId  服务 ID
     * @param instanceId 实例 ID
     * @param message    错误消息
     */
    public ServiceRegistryException(
            @Nullable String serviceId,
            @Nullable String instanceId,
            @Nullable String message) {
        super(message);
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }

    /**
     * 创建异常（带服务信息）。
     *
     * @param serviceId  服务 ID
     * @param instanceId 实例 ID
     * @param message    错误消息
     * @param cause      原因
     */
    public ServiceRegistryException(
            @Nullable String serviceId,
            @Nullable String instanceId,
            @Nullable String message,
            @Nullable Throwable cause) {
        super(message, cause);
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }

    /**
     * 获取服务 ID。
     *
     * @return 服务 ID
     */
    @Nullable
    public String getServiceId() {
        return serviceId;
    }

    /**
     * 获取实例 ID。
     *
     * @return 实例 ID
     */
    @Nullable
    public String getInstanceId() {
        return instanceId;
    }
}