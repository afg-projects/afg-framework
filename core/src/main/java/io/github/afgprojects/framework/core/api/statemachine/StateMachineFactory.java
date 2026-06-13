package io.github.afgprojects.framework.core.api.statemachine;

import io.github.afgprojects.framework.core.statemachine.StateMachineDefinition;

/**
 * 状态机工厂接口
 * <p>
 * 状态机的统一管理入口，提供状态机定义的注册、查找和实例创建。
 * 框架在启动时通过 {@link io.github.afgprojects.framework.core.statemachine.StateMachineScanner}
 * 自动扫描所有 {@code @StateMachine} 注解的枚举并注册到工厂。
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private StateMachineFactory factory;
 *
 * // 按状态枚举类型查找定义
 * StateMachineDefinition<OrderStatus> def = factory.getDefinition(OrderStatus.class);
 *
 * // 按名称查找定义
 * StateMachineDefinition<OrderStatus> def = factory.getDefinition("OrderStatus");
 *
 * // 创建状态机实例
 * StateMachineInstance<Order, OrderStatus> sm = factory.create(OrderStatus.class);
 *
 * // 手动注册定义
 * factory.register(definition);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface StateMachineFactory {

    /**
     * 按状态枚举类型查找状态机定义
     *
     * @param stateType 状态枚举类
     * @param <S>       状态枚举类型
     * @return 状态机定义，如果不存在返回 {@code null}
     */
    <S extends Enum<S>> StateMachineDefinition<S> getDefinition(Class<S> stateType);

    /**
     * 按名称查找状态机定义
     *
     * @param name 状态机名称
     * @param <S>  状态枚举类型
     * @return 状态机定义，如果不存在返回 {@code null}
     */
    <S extends Enum<S>> StateMachineDefinition<S> getDefinition(String name);

    /**
     * 创建状态机实例
     * <p>
     * 根据状态枚举类型查找对应的状态机定义，创建可操作实体的状态机实例。
     * </p>
     *
     * @param stateType 状态枚举类
     * @param <T>       实体类型
     * @param <S>       状态枚举类型
     * @return 状态机实例
     * @throws io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException
     *         如果状态枚举类型未注册
     */
    <T, S extends Enum<S>> StateMachineInstance<T, S> create(Class<S> stateType);

    /**
     * 注册状态机定义
     * <p>
     * 将状态机定义注册到工厂，后续可通过 {@link #getDefinition} 和 {@link #create} 使用。
     * </p>
     *
     * @param definition 状态机定义
     */
    void register(StateMachineDefinition<?> definition);
}
