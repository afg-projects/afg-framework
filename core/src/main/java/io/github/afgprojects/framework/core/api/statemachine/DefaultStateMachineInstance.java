package io.github.afgprojects.framework.core.api.statemachine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.afgprojects.framework.core.statemachine.StateMachineDefinition;
import io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认状态机实例实现
 * <p>
 * 提供状态机的运行时操作：读取实体当前状态、验证转换合法性、执行状态转换、
 * 查询可用转换。通过反射调用实体的 getter/setter 方法操作状态字段。
 * </p>
 * <p>
 * 支持严格模式（非法转换抛出 {@link InvalidTransitionException}）和非严格模式（静默忽略）。
 * </p>
 *
 * @param <T> 实体类型
 * @param <S> 状态枚举类型
 * @since 1.0.0
 */
@Slf4j
public class DefaultStateMachineInstance<T, S extends Enum<S>> implements StateMachineInstance<T, S> {

    private final StateMachineDefinition<S> definition;
    private final boolean strictMode;

    /**
     * 构造函数
     *
     * @param definition 状态机定义
     * @param strictMode 是否启用严格模式
     */
    public DefaultStateMachineInstance(StateMachineDefinition<S> definition, boolean strictMode) {
        this.definition = definition;
        this.strictMode = strictMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S getCurrentState(T entity) {
        try {
            Method getter = findStateGetter(entity.getClass());
            return (S) getter.invoke(entity);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("无法读取实体状态: entityType=%s, stateType=%s",
                            entity.getClass().getSimpleName(), definition.getStateType().getSimpleName()),
                    e);
        }
    }

    @Override
    public void transit(T entity, S targetState) {
        S currentState = getCurrentState(entity);
        if (!definition.canTransit(currentState, targetState)) {
            if (strictMode) {
                throw new InvalidTransitionException(definition.getName(), currentState, targetState);
            }
            log.debug("非法状态转换被忽略: stateMachine={}, from={}, to={}",
                    definition.getName(), currentState, targetState);
            return;
        }
        try {
            Method setter = findStateSetter(entity.getClass());
            setter.invoke(entity, targetState);
            log.debug("状态转换成功: stateMachine={}, from={}, to={}",
                    definition.getName(), currentState, targetState);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("无法设置实体状态: entityType=%s, stateType=%s, targetState=%s",
                            entity.getClass().getSimpleName(), definition.getStateType().getSimpleName(), targetState),
                    e);
        }
    }

    @Override
    public boolean canTransit(T entity, S targetState) {
        S currentState = getCurrentState(entity);
        return definition.canTransit(currentState, targetState);
    }

    @Override
    public List<S> getAvailableTransitions(T entity) {
        S currentState = getCurrentState(entity);
        Set<S> reachableStates = definition.findReachableStates(currentState);
        return new ArrayList<>(reachableStates);
    }

    @Override
    public StateMachineDefinition<S> getDefinition() {
        return definition;
    }

    /**
     * 查找实体的状态 getter 方法
     * <p>
     * 按优先级尝试以下方法名：{@code getStatus}, {@code getState}
     * </p>
     */
    private Method findStateGetter(Class<?> entityClass) throws NoSuchMethodException {
        for (String methodName : new String[]{"getStatus", "getState"}) {
            try {
                Method method = entityClass.getMethod(methodName);
                if (definition.getStateType().isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 继续尝试下一个方法名
            }
        }
        throw new NoSuchMethodException(
                String.format("实体类 %s 未找到状态 getter 方法（getStatus/getState 返回 %s）",
                        entityClass.getSimpleName(), definition.getStateType().getSimpleName()));
    }

    /**
     * 查找实体的状态 setter 方法
     * <p>
     * 按优先级尝试以下方法名：{@code setStatus}, {@code setState}
     * </p>
     */
    private Method findStateSetter(Class<?> entityClass) throws NoSuchMethodException {
        for (String methodName : new String[]{"setStatus", "setState"}) {
            try {
                Method method = entityClass.getMethod(methodName, definition.getStateType());
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // 继续尝试下一个方法名
            }
        }
        throw new NoSuchMethodException(
                String.format("实体类 %s 未找到状态 setter 方法（setStatus/setState 参数类型 %s）",
                        entityClass.getSimpleName(), definition.getStateType().getSimpleName()));
    }
}
