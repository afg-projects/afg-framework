/**
 * 响应结果包。
 *
 * <p>提供统一的响应结果封装。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.Result} - 统一响应包装（code, message, data, traceId, requestId）</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.Results} - Result 工厂类，自动填充 traceId/requestId</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.PageData} - 分页数据包装</li>
 * </ul>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.model.result;
