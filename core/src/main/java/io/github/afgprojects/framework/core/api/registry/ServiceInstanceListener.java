package io.github.afgprojects.framework.core.api.registry;

import org.jspecify.annotations.NonNull;

/**
 * 服务实例变化监听器。
 *
 * <p>当服务实例列表发生变化时回调此接口。
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ServiceInstanceListener {

    /**
     * 服务实例变化回调。
     *
     * @param event 变化事件
     */
    void onChange(@NonNull ServiceInstanceEvent event);
}