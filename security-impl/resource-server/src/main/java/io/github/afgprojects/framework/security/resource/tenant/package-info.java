/**
 * 租户解析组件。
 *
 * <p>提供租户解析的实现类，包括请求头解析、Token 解析和解析链。
 *
 * <p>主要类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.security.resource.tenant.TenantResolveStrategy} - 租户解析策略枚举</li>
 *   <li>{@link io.github.afgprojects.framework.security.resource.tenant.DefaultTenantContext} - 默认租户上下文实现</li>
 *   <li>{@link io.github.afgprojects.framework.security.resource.tenant.HeaderTenantResolver} - 请求头租户解析器</li>
 *   <li>{@link io.github.afgprojects.framework.security.resource.tenant.TokenTenantResolver} - Token 租户解析器</li>
 *   <li>{@link io.github.afgprojects.framework.security.resource.tenant.TenantResolverChain} - 租户解析链</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.security.resource.tenant;