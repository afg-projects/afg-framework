/**
 * 数据模型包。
 *
 * <p>提供统一的数据模型，包括响应结果、分页数据、实体基类等。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.Result} - 统一响应包装</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.Results} - Result 工厂类</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.result.PageData} - 分页数据包装</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.entity.BaseEntity} - 实体基类</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.entity.TenantEntity} - 租户实体基类</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用 Results 工厂类，自动填充 traceId/requestId
 * return Results.success(data);
 * return Results.fail(ErrorCode.USER_NOT_FOUND);
 * }</pre>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.model;
