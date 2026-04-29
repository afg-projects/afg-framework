/**
 * 模型异常包。
 *
 * <p>提供业务异常和错误码定义。
 *
 * <p>核心类：
 * <ul>
 *   <li>{@link io.github.afgprojects.framework.core.model.exception.BusinessException} - 业务异常</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.exception.ErrorCode} - 错误码接口</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.exception.CommonErrorCode} - 通用错误码枚举</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.exception.ErrorCategory} - 错误分类枚举</li>
 *   <li>{@link io.github.afgprojects.framework.core.model.exception.ErrorCodeRange} - 错误码范围定义</li>
 * </ul>
 *
 * <h2>错误码设计</h2>
 * <p>错误码采用数字编码，便于分类和国际化：
 * <ul>
 *   <li>10000-19999: 通用错误码</li>
 *   <li>20000-29999: 认证授权错误码</li>
 *   <li>30000-39999: 业务错误码</li>
 *   <li>40000-49999: 存储错误码</li>
 *   <li>50000-59999: 任务调度错误码</li>
 *   <li>60000-69999: HTTP客户端错误码</li>
 *   <li>90000-99999: 系统错误码</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 定义模块错误码
 * public enum UserErrorCode implements ErrorCode {
 *     USER_NOT_FOUND(30001, "用户不存在", ErrorCategory.BUSINESS),
 *     USER_DISABLED(30002, "用户已禁用", ErrorCategory.BUSINESS);
 *
 *     private final int code;
 *     private final String message;
 *     private final ErrorCategory category;
 *
 *     // 构造函数、getter 方法...
 * }
 *
 * // 抛出业务异常
 * throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
 * throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "用户ID=123不存在");
 * }</pre>
 *
 * @since 1.0.0
 */
package io.github.afgprojects.framework.core.model.exception;
