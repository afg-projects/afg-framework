/**
 * 模块异常包。
 *
 * <p>提供模块系统相关的异常类，用于模块注册、依赖解析等操作的错误处理。
 *
 * <p>异常类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.module.exception.ModuleException} - 模块异常基类</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.exception.ModuleNotFoundException} - 模块未找到异常</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.exception.ModuleDuplicateException} - 模块重复异常</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.exception.ModuleCircularDependencyException} - 模块循环依赖异常</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.module.exception;
