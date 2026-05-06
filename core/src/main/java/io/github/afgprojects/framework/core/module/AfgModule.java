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
    String moduleId();

    /**
     * 获取模块显示名称
     *
     * @return 模块名称
     */
    String moduleName();

    /**
     * 获取依赖的模块ID列表
     *
     * @return 依赖模块ID列表，无依赖返回空列表
     */
    default List<String> dependencies() {
        return Collections.emptyList();
    }

    /**
     * 获取模块基础包名
     * 用于自动为该包下的 Controller 添加 contextPath 前缀
     *
     * @return 基础包名，如 "io.github.afgprojects.auth"
     */
    default String basePackage() {
        return "";
    }

    /**
     * 获取模块上下文路径
     * 用于 Web MVC 路径映射前缀
     *
     * @return 上下文路径，如 "/api/auth"
     */
    default String contextPath() {
        return "";
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
