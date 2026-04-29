package io.github.afgprojects.framework.core.web.health;

import io.github.afgprojects.framework.core.module.ModuleState;

/**
 * 模块状态提供者接口
 * 模块可实现此接口以提供健康状态信息
 *
 * @since 1.0.0
 */
public interface ModuleStatusProvider {

    /**
     * 获取模块当前状态
     *
     * @return 模块状态
     */
    ModuleState getState();

    /**
     * 获取模块健康详情信息
     *
     * @return 健康详情，可包含自定义信息
     */
    default String getHealthDetail() {
        return getState().name();
    }
}
