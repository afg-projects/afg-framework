/**
 * AFG 平台 OAuth2 授权服务器模块根包。
 *
 * <p>提供 OAuth2 授权服务器实现，包括：
 * <ul>
 *   <li>授权码模式（Authorization Code）</li>
 *   <li>客户端凭证模式（Client Credentials）</li>
 *   <li>刷新令牌模式（Refresh Token）</li>
 *   <li>PKCE 扩展支持</li>
 * </ul>
 *
 * <p>主要子包：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.auth.autoconfigure} - 配置属性和自动配置</li>
 *   <li>{@link io.github.afgprojects.framework.security.auth.token} - Token 处理</li>
 *   <li>{@link io.github.afgprojects.framework.security.auth.oauth2} - OAuth2 实现</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.auth;
