/**
 * 模块系统核心包。
 *
 * <p>提供模块注册、依赖解析、生命周期管理等功能，支持模块间的依赖注入和事件通信。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.module.AfgModule} - 模块接口，所有业务模块需实现</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.AfgModuleAnnotation} - 模块声明注解，自动注册为 Spring Component</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.ModuleRegistry} - 模块注册表，管理模块生命周期</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.ModuleDefinition} - 模块定义数据类</li>
 *   <li>{@link io.github.afgprojects.framework.core.module.ModuleContext} - 模块上下文，提供事件发布和模块间通信</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @AfgModuleAnnotation(id = "user-module", name = "用户模块", dependencies = {"base-module"})
 * public class UserModule implements AfgModule {
 *     @Override
 *     public void onRegister(ModuleContext context) {
 *         // 模块初始化逻辑
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.module;
