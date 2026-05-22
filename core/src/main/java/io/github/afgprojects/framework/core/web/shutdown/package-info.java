/**
 * 优雅关闭包。
 *
 * <p>提供应用优雅关闭的能力，支持自定义关闭顺序和钩子函数。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.shutdown.ShutdownHook} - 关闭钩子接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.shutdown.ShutdownOrder} - 关闭顺序注解</li>
 *   <li>{@link io.github.afgprojects.framework.core.config.AfgCoreProperties.ShutdownConfig} - 关闭配置属性（在 AfgCoreProperties 中）</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.shutdown;
