/**
 * Web 安全增强包。
 *
 * <p>提供安全相关的功能，包括安全上下文、权限控制、输入过滤等。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.AfgSecurityContext} - 安全上下文接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.AfgPrincipal} - 用户主体信息</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.AfgEnforcer} - Casbin 权限执行器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.AfgSecurityConfigurer} - 安全配置器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.AfgSecurityContextBridge} - 安全上下文桥接器</li>
 * </ul>
 *
 * <p>子包：
 * <ul>
 *   <li>{@link io.github.afgprojects.core.web.security.filter} - 安全过滤器</li>
 *   <li>{@link io.github.afgprojects.core.web.security.sanitizer} - 输入净化器</li>
 *   <li>{@link io.github.afgprojects.core.web.security.util} - 安全工具类</li>
 *   <li>{@link io.github.afgprojects.core.web.security.autoconfigure} - 安全自动配置</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.security;
