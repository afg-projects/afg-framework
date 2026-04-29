package io.github.afgprojects.framework.core.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 追踪注解
 * <p>
 * 标记需要追踪的方法，切面将自动创建 Span 并记录执行信息。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Traced(operationName = "user.query", kind = SpanKind.SERVER)
 * public User getUser(String userId) {
 *     // 业务逻辑
 * }
 *
 * @Traced(operationName = "external.api.call", kind = SpanKind.CLIENT)
 * public Response callExternalApi(Request request) {
 *     // 外部调用
 * }
 * }</pre>
 *
 * @see SpanKind
 * @see TracedAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {

    /**
     * 操作名称
     * <p>
     * 如果为空，自动使用 "className.methodName" 格式
     * </p>
     *
     * @return 操作名称
     */
    String operationName() default "";

    /**
     * Span 类型
     * <p>
     * 用于区分不同的调用场景
     * </p>
     *
     * @return Span 类型
     */
    SpanKind kind() default SpanKind.INTERNAL;

    /**
     * 是否记录参数
     * <p>
     * 开启后会将方法参数作为 Span 标签记录
     * 注意：敏感参数应避免记录
     * </p>
     *
     * @return 是否记录参数
     */
    boolean logParameters() default false;

    /**
     * 是否记录返回值
     * <p>
     * 开启后会将方法返回值作为 Span 事件记录
     * 注意：敏感数据应避免记录
     * </p>
     *
     * @return 是否记录返回值
     */
    boolean logResult() default false;

    /**
     * 异常记录级别
     * <p>
     * 控制异常信息的记录详细程度
     * </p>
     *
     * @return 异常记录级别
     */
    ExceptionLogLevel exceptionLogLevel() default ExceptionLogLevel.MESSAGE;
}
