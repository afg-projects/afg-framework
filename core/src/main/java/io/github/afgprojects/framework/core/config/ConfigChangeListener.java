package io.github.afgprojects.framework.core.config;

/**
 * 配置变更监听接口
 * 实现此接口以响应配置变更
 */
@FunctionalInterface
public interface ConfigChangeListener {

    /**
     * 配置变更回调
     *
     * @param event 配置变更事件，包含旧值、新值和差异
     */
    void onConfigChange(ConfigChangeEvent event);
}
