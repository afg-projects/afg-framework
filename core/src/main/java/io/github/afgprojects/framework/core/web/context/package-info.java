/**
 * 请求上下文包。
 *
 * <p>提供请求级别的上下文管理，包括 traceId、requestId、userId、tenantId 等信息的传递。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.web.context.RequestContext} - 请求上下文数据类</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder} - 请求上下文持有者</li>
 *   <li>{@link io.github.afgprojects.framework.core.web.context.RequestContextFilter} - 请求上下文过滤器</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.web.context;
