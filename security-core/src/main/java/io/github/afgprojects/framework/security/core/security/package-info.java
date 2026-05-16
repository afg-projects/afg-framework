/**
 * 安全策略核心接口。
 *
 * <p>提供登录安全相关的策略接口，包括：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.core.security.LoginFailureTracker} - 登录失败追踪</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.security.PasswordValidator} - 密码验证</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.security.IpRestrictionChecker} - IP 限制检查</li>
 *   <li>{@link io.github.afgprojects.framework.security.core.security.DeviceLimiter} - 设备限制</li>
 * </ul>
 *
 * <p>这些接口定义了安全策略的 SPI，业务系统或框架模块可以提供具体实现。
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.core.security;
