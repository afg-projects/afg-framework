package io.github.afgprojects.framework.security.auth;

import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

/**
 * 认证服务器模块配置。
 *
 * <p>提供完整的认证授权功能，包括：
 * <ul>
 *   <li>多种登录方式（用户名密码、手机号验证码、邮箱验证码）</li>
 *   <li>JWT Token 生成和验证</li>
 *   <li>验证码生成和验证</li>
 *   <li>登录失败锁定</li>
 *   <li>密码强度校验</li>
 *   <li>IP 限制</li>
 *   <li>设备绑定</li>
 * </ul>
 *
 * <p>模块 ID: auth-server
 * <p>Context Path: /auth-api
 *
 * @since 1.0.0
 */
@AfgModuleAnnotation(name = "认证服务器模块")
public class AuthServerModuleConfig {
}
