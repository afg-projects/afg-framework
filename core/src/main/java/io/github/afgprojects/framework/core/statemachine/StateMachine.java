package io.github.afgprojects.framework.core.statemachine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 状态机注解
 * <p>
 * 标记枚举类为状态机定义，声明关联的实体类和状态机名称。
 * 框架在启动时扫描所有 {@code @StateMachine} 注解的枚举，结合 {@link Transition} 注解
 * 构建状态转换图，由 {@link io.github.afgprojects.framework.core.api.statemachine.StateMachineFactory} 统一管理。
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
 *     @Transition(from = "CONFIRMED", to = "CANCELLED")
 *     public void cancel(Order order) { ... }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StateMachine {

    /**
     * 关联的实体类
     * <p>
     * 实体类必须包含状态字段的 getter/setter（如 {@code getStatus()}/{@code setStatus()}），
     * 以便状态机实例读取和更新实体状态。
     * </p>
     *
     * @return 实体类
     */
    Class<?> entity();

    /**
     * 状态机名称
     * <p>
     * 默认使用枚举类的简单名称。用于通过名称查找状态机定义。
     * </p>
     *
     * @return 状态机名称，默认为空（使用枚举类名）
     */
    String name() default "";
}
