package io.github.afgprojects.framework.core.statemachine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 状态转换注解
 * <p>
 * 标记枚举类中的方法为合法的状态转换规则，声明源状态和目标状态。
 * 与 {@link StateMachine} 注解配合使用，框架在启动时解析所有 {@code @Transition} 注解
 * 构建状态转换图。
 * </p>
 * <p>
 * 注意：由于 Java 注解限制，{@code from} 和 {@code to} 使用字符串指定状态名称，
 * 对应枚举常量的名称（{@link Enum#name()}）。
 * </p>
 *
 * <pre>{@code
 * @StateMachine(entity = Order.class)
 * public enum OrderStatus {
 *     PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;
 *
 *     @Transition(from = "PENDING", to = "CONFIRMED")
 *     public void confirm(Order order) { ... }
 *
 *     // 支持多源状态
 *     @Transition(from = {"CONFIRMED", "SHIPPED"}, to = "CANCELLED")
 *     public void cancel(Order order) { ... }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transition {

    /**
     * 源状态名称
     * <p>
     * 可以指定多个源状态名称，表示从任意一个源状态都可以转换到目标状态。
     * 状态名称必须与枚举常量的 {@link Enum#name()} 匹配。
     * </p>
     *
     * @return 源状态名称数组
     */
    String[] from();

    /**
     * 目标状态名称
     * <p>
     * 状态名称必须与枚举常量的 {@link Enum#name()} 匹配。
     * </p>
     *
     * @return 目标状态名称
     */
    String to();

    /**
     * 触发事件名
     * <p>
     * 默认使用方法名。事件名用于标识转换的触发条件，便于日志记录和审计。
     * </p>
     *
     * @return 事件名，默认为空（使用方法名）
     */
    String event() default "";
}
