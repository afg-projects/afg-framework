/**
 * 安全过滤器包。
 *
 * <p>提供安全相关的过滤器，包括 XSS 防护、SQL 注入防护等。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.AbstractSecurityFilter} - 安全过滤器基类</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.SecurityHeaderFilter} - 安全头过滤器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.XssFilter} - XSS 过滤器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.SqlInjectionFilter} - SQL 注入过滤器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.XssChecker} - XSS 检查器</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.security.filter.SqlInjectionChecker} - SQL 注入检查器</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.security.filter;
