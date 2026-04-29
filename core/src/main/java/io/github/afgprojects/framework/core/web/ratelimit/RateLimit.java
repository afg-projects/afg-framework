package io.github.afgprojects.framework.core.web.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * <p>
 * 标注在方法上，实现声明式限流控制。
 * 基于 Redisson RRateLimiter 实现分布式令牌桶限流。
 * </p>
 *
 * <pre>{@code
 * // 基于IP限流，每秒10个请求
 * @RateLimit(key = "api.query", rate = 10, dimension = RateLimitDimension.IP)
 * public Result<User> queryUser(String id) { ... }
 *
 * // 基于用户限流，每秒5个请求，突发容量10
 * @RateLimit(key = "api.update", rate = 5, burst = 10, dimension = RateLimitDimension.USER)
 * public Result<Void> updateUser(User user) { ... }
 *
 * // 接口级别限流，限流后调用fallback方法
 * @RateLimit(key = "api.upload", rate = 2, dimension = RateLimitDimension.API, fallbackMethod = "uploadFallback")
 * public Result<Void> upload(File file) { ... }
 *
 * // 多级限流：同时限制IP和用户
 * @RateLimit(key = "api.search", rate = 100, dimension = RateLimitDimension.IP)
 * @RateLimit(key = "api.search", rate = 50, dimension = RateLimitDimension.USER)
 * public Result<List<Item>> search(String keyword) { ... }
 *
 * // 使用滑动窗口算法
 * @RateLimit(key = "api.order", rate = 10, algorithm = RateLimitAlgorithm.SLIDING_WINDOW)
 * public Result<Order> createOrder(OrderRequest request) { ... }
 *
 * // 使用 SpEL 表达式动态 key
 * @RateLimit(key = "'order:' + #orderId", rate = 5, dimension = RateLimitDimension.USER)
 * public Result<Order> getOrder(Long orderId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /**
     * 限流 key 前缀
     * <p>
     * 最终的 key 格式为: rateLimit:{key}:{dimension}:{dimensionValue}
     * 例如: rateLimit:api.query:ip:192.168.1.1
     * 支持 SpEL 表达式，使用 #参数名 引用方法参数
     * </p>
     *
     * @return 限流 key 前缀
     */
    String key();

    /**
     * 每秒请求数（令牌生成速率）
     * <p>
     * 表示每秒向令牌桶添加的令牌数量
     * </p>
     *
     * @return 每秒请求数
     */
    long rate() default 10;

    /**
     * 突发容量（令牌桶最大容量）
     * <p>
     * 允许短时间内爆发的最大请求数。
     * 默认值 0 表示使用 rate * 2 作为突发容量
     * </p>
     *
     * @return 突发容量
     */
    long burst() default 0;

    /**
     * 限流维度
     * <p>
     * 支持按 IP、用户、租户或接口进行限流
     * </p>
     *
     * @return 限流维度
     */
    RateLimitDimension dimension() default RateLimitDimension.IP;

    /**
     * 限流算法
     * <p>
     * 默认使用令牌桶算法，可选择滑动窗口算法
     * </p>
     *
     * @return 限流算法
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 时间窗口大小（秒）
     * <p>
     * 仅对滑动窗口算法有效，表示滑动窗口的时间范围
     * </p>
     *
     * @return 时间窗口大小
     */
    long windowSize() default 1;

    /**
     * 限流后回退方法名称
     * <p>
     * 当触发限流时，调用此方法作为降级处理。
     * 回退方法必须与原方法具有相同的方法签名。
     * 如果为空，则直接抛出限流异常。
     * </p>
     *
     * @return 回退方法名称
     */
    String fallbackMethod() default "";

    /**
     * 限流失败消息
     * <p>
     * 自定义限流失败时的错误消息
     * </p>
     *
     * @return 限流失败消息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否在响应头中返回限流信息
     * <p>
     * 包括 X-RateLimit-Limit、X-RateLimit-Remaining、X-RateLimit-Reset
     * </p>
     *
     * @return 是否返回响应头
     */
    boolean responseHeaders() default true;
}
