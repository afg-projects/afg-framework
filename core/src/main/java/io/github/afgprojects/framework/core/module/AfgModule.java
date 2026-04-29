package io.github.afgprojects.framework.core.module;

import java.util.Collections;
import java.util.List;

import io.github.afgprojects.framework.core.web.security.AfgSecurityConfiguration;

/**
 * AFG 模块接口
 * 所有 AFG 模块必须实现此接口
 */
public interface AfgModule {

    /**
     * 获取模块唯一标识
     *
     * @return 模块ID
     */
    String getModuleId();

    /**
     * 获取模块显示名称
     *
     * @return 模块名称
     */
    String getModuleName();

    /**
     * 获取依赖的模块ID列表
     *
     * @return 依赖模块ID列表，无依赖返回空列表
     */
    default List<String> getDependencies() {
        return Collections.emptyList();
    }

    /**
     * 模块注册时的初始化回调
     *
     * @param context 模块上下文
     */
    default void onRegister(ModuleContext context) {
        // 默认空实现，子类可覆盖
    }

    /**
     * 模块安全配置回调
     *
     * @param config 安全配置收集器
     */
    default void configureSecurity(AfgSecurityConfiguration config) {
        // 默认空实现，子类可覆盖
    }
}
