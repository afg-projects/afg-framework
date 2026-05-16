package io.github.afgprojects.framework.apt.entity;

/**
 * 通用字段来源
 * <p>
 * 用于标识字段的注册来源，决定优先级。
 * <p>
 * 优先级规则：FRAMEWORK > CONFIG > ANNOTATION
 */
enum FieldSource {
    /**
     * 框架内置字段
     * <p>
     * 不可被覆盖，保证核心特性稳定。
     */
    FRAMEWORK,

    /**
     * 配置文件注册字段
     * <p>
     * 项目级别，可覆盖注解声明。
     */
    CONFIG,

    /**
     * 注解声明字段
     * <p>
     * 模块级别，优先级最低。
     */
    ANNOTATION
}
