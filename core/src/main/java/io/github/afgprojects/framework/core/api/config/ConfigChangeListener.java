package io.github.afgprojects.framework.core.api.config;

import org.jspecify.annotations.NonNull;

/**
 * 配置变更监听器
 */
@FunctionalInterface
public interface ConfigChangeListener {

    /**
     * 配置变更回调
     *
     * @param event 配置变更事件
     */
    void onChange(@NonNull ConfigChangeEvent event);
}
