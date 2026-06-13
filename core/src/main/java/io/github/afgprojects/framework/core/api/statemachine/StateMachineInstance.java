package io.github.afgprojects.framework.core.api.statemachine;

import java.util.List;

import io.github.afgprojects.framework.core.statemachine.StateMachineDefinition;

/**
 * 状态机实例接口
 * <p>
 * 提供对特定实体的状态机操作：读取当前状态、验证转换合法性、执行状态转换、
 * 查询可用转换。每个实例绑定一个状态机定义（{@link StateMachineDefinition}），
 * 可操作任意关联实体类型 {@code T} 的实例。
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private StateMachineFactory factory;
 *
 * StateMachineInstance<Order, OrderStatus> sm = factory.create(OrderStatus.class);
 *
 * // 查询当前状态
 * OrderStatus current = sm.getCurrentState(order);
 *
 * // 验证转换合法性
 * if (sm.canTransit(order, OrderStatus.CONFIRMED)) {
 *     sm.transit(order, OrderStatus.CONFIRMED);
 * }
 *
 * // 查询可用转换
 * List<OrderStatus> available = sm.getAvailableTransitions(order);
 * }</pre>
 *
 * @param <T> 实体类型
 * @param <S> 状态枚举类型
 * @since 1.0.0
 */
public interface StateMachineInstance<T, S extends Enum<S>> {

    /**
     * 获取实体的当前状态
     *
     * @param entity 实体实例
     * @return 当前状态
     */
    S getCurrentState(T entity);

    /**
     * 将实体从当前状态转换到目标状态
     * <p>
     * 如果转换不合法（当前状态到目标状态不存在合法转换），根据严格模式配置：
     * <ul>
     *   <li>严格模式（默认）：抛出 {@link io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException}</li>
     *   <li>非严格模式：静默忽略，不更新状态</li>
     * </ul>
     * </p>
     *
     * @param entity       实体实例
     * @param targetState  目标状态
     */
    void transit(T entity, S targetState);

    /**
     * 验证从实体当前状态到目标状态的转换是否合法
     *
     * @param entity      实体实例
     * @param targetState 目标状态
     * @return 如果转换合法返回 {@code true}
     */
    boolean canTransit(T entity, S targetState);

    /**
     * 获取实体当前状态下可用的目标状态列表
     *
     * @param entity 实体实例
     * @return 可达的目标状态列表
     */
    List<S> getAvailableTransitions(T entity);

    /**
     * 获取状态机定义
     *
     * @return 状态机定义
     */
    StateMachineDefinition<S> getDefinition();
}
