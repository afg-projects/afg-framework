package io.github.afgprojects.framework.core.duplicatesubmit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解
 * <p>
 * 标注在方法上，自动在方法执行前进行去重检查，在指定间隔内拒绝重复请求。
 * 支持 SpEL 表达式动态生成去重键。
 * </p>
 *
 * <pre>{@code
 * // 基本用法：3秒内不允许重复提交
 * @DuplicateSubmit
 * public Result<Order> createOrder(OrderRequest request) { ... }
 *
 * // 自定义间隔和消息
 * @DuplicateSubmit(interval = 5000, message = "订单正在处理中，请勿重复提交")
 * public Result<Order> createOrder(OrderRequest request) { ... }
 *
 * // 使用 SpEL 表达式动态生成去重键
 * @DuplicateSubmit(key = "#userId + ':' + #request.orderId")
 * public Result<Order> submitOrder(String userId, OrderRequest request) { ... }
 *
 * // 自定义前缀
 * @DuplicateSubmit(prefix = "order-submit", key = "#orderId", interval = 10000)
 * public Result<Void> processOrder(String orderId) { ... }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DuplicateSubmit {

    /**
     * 去重键
     * <p>
     * 支持 SpEL 表达式，可以使用方法参数。
     * 例如：#userId、#user.id、#p0（第一个参数）
     * <p>
     * 默认为空，使用 userId + method + URI 组合作为去重键。
     *
     * @return 去重键
     */
    String key() default "";

    /**
     * 去重间隔（毫秒）
     * <p>
     * 在间隔时间内，相同 key 的重复请求将被拒绝。
     *
     * @return 去重间隔（毫秒）
     */
    long interval() default 3000;

    /**
     * 键前缀
     * <p>
     * 最终的去重键为：prefix + ":" + key
     *
     * @return 键前缀
     */
    String prefix() default "duplicate-submit";

    /**
     * 提示消息
     * <p>
     * 重复提交被拦截时返回的错误消息。
     *
     * @return 提示消息
     */
    String message() default "请勿重复提交";
}
