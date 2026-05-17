package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 服务实例变化事件。
 *
 * @param serviceId       服务 ID
 * @param changeType      变化类型
 * @param instances       实例列表（REFRESH 时使用）
 * @param addedInstance   新增的实例（ADD 时使用）
 * @param removedInstance 移除的实例（REMOVE 时使用）
 * @since 1.0.0
 */
public record ServiceInstanceEvent(
    @NonNull String serviceId,
    @NonNull ChangeType changeType,
    @Nullable List<ServiceInstance> instances,
    @Nullable ServiceInstance addedInstance,
    @Nullable ServiceInstance removedInstance
) {
    /**
     * 变化类型枚举。
     */
    public enum ChangeType {
        /**
         * 实例列表刷新（全量更新）。
         */
        REFRESH,

        /**
         * 新增实例。
         */
        ADD,

        /**
         * 移除实例。
         */
        REMOVE,

        /**
         * 实例状态变更。
         */
        STATUS_CHANGE
    }

    /**
     * 创建刷新事件。
     *
     * @param serviceId 服务 ID
     * @param instances 实例列表
     * @return 事件实例
     */
    public static @NonNull ServiceInstanceEvent refresh(
            @NonNull String serviceId,
            @NonNull List<ServiceInstance> instances) {
        return new ServiceInstanceEvent(serviceId, ChangeType.REFRESH, instances, null, null);
    }

    /**
     * 创建新增实例事件。
     *
     * @param serviceId 服务 ID
     * @param instance  新增的实例
     * @return 事件实例
     */
    public static @NonNull ServiceInstanceEvent add(
            @NonNull String serviceId,
            @NonNull ServiceInstance instance) {
        return new ServiceInstanceEvent(serviceId, ChangeType.ADD, null, instance, null);
    }

    /**
     * 创建移除实例事件。
     *
     * @param serviceId 服务 ID
     * @param instance  移除的实例
     * @return 事件实例
     */
    public static @NonNull ServiceInstanceEvent remove(
            @NonNull String serviceId,
            @NonNull ServiceInstance instance) {
        return new ServiceInstanceEvent(serviceId, ChangeType.REMOVE, null, null, instance);
    }

    /**
     * 创建状态变更事件。
     *
     * @param serviceId 服务 ID
     * @param instance  状态变更的实例
     * @return 事件实例
     */
    public static @NonNull ServiceInstanceEvent statusChange(
            @NonNull String serviceId,
            @NonNull ServiceInstance instance) {
        return new ServiceInstanceEvent(serviceId, ChangeType.STATUS_CHANGE, null, instance, null);
    }
}