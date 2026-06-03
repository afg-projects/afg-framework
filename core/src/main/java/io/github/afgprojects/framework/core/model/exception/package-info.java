/**
 * 模型异常包。
 *
 * <p>提供业务异常和错误码定义。
 *
 * <h2>错误码设计</h2>
 * <p>错误码采用数字编码，便于分类和国际化：
 * <ul>
 *   <li>10000-19999: 通用模块（核心错误码）</li>
 *   <li>20000-29999: 认证授权模块</li>
 *   <li>30000-39999: 业务模块</li>
 *   <li>40000-49999: 存储错误码</li>
 *   <li>50000-59999: 任务调度错误码</li>
 *   <li>60000-69999: HTTP客户端错误码</li>
 *   <li>90000-99999: 系统模块</li>
 * </ul>
 * <p>通用模块内部细分：
 * <ul>
 *   <li>10000-10099: 成功与通用错误</li>
 *   <li>10100-10199: 资源错误</li>
 *   <li>10200-10299: 请求错误</li>
 *   <li>10300-10399: 限流错误</li>
 *   <li>10400-10499: 认证授权错误（轻量级，完整认证在 20000+）</li>
 *   <li>11000-11999: 数据层错误</li>
 *   <li>12000-12999: 存储错误</li>
 *   <li>13000-13999: 任务错误</li>
 *   <li>14000-14999: HTTP客户端错误</li>
 *   <li>15000-15999: 模块错误</li>
 *   <li>16000-16999: 配置错误</li>
 *   <li>17000-17999: 功能开关错误</li>
 *   <li>19000-19999: 系统错误</li>
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