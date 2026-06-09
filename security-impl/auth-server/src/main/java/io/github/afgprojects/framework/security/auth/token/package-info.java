/**
 * Token 处理包。
 *
 * <p>提供 Token 生成、验证和解析：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.auth.token.DefaultTokenService} - 默认 Token 服务（基于 Nimbus JOSE JWT）</li>
 *   <li>{@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenResolver} - Auth Server Bearer Token 解析器</li>
 *   <li>{@link io.github.afgprojects.framework.security.auth.token.AuthServerBearerTokenFilter} - Auth Server Bearer Token 认证过滤器</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.auth.token;
