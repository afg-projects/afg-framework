package io.github.afgprojects.framework.core.api.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.statemachine.StateMachineDefinition;
import io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地内存状态机工厂实现
 * <p>
 * 使用 {@link ConcurrentHashMap} 存储所有状态机定义，支持按状态枚举类型和按名称查找。
 * 默认使用严格模式（非法转换抛出 {@link InvalidTransitionException}）。
 * </p>
 * <p>
 * 框架在启动时通过 {@link io.github.afgprojects.framework.core.statemachine.StateMachineScanner}
 * 自动扫描所有 {@code @StateMachine} 注解的枚举并注册到此工厂。
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class LocalStateMachineFactory implements StateMachineFactory {

    private final ConcurrentHashMap<Class<?>, StateMachineDefinition<?>> definitionsByType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StateMachineDefinition<?>> definitionsByName = new ConcurrentHashMap<>();
    private final boolean strictMode;

    /**
     * 构造函数（默认严格模式）
     */
    public LocalStateMachineFactory() {
        this.strictMode = true;
    }

    /**
     * 构造函数
     *
     * @param strictMode 是否启用严格模式（非法转换抛异常）
     */
    public LocalStateMachineFactory(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * 是否启用严格模式
     *
     * @return 严格模式标志
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Enum<S>> StateMachineDefinition<S> getDefinition(Class<S> stateType) {
        return (StateMachineDefinition<S>) definitionsByType.get(stateType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Enum<S>> StateMachineDefinition<S> getDefinition(String name) {
        return (StateMachineDefinition<S>) definitionsByName.get(name);
    }

    @Override
    public <T, S extends Enum<S>> StateMachineInstance<T, S> create(Class<S> stateType) {
        StateMachineDefinition<S> definition = getDefinition(stateType);
        if (definition == null) {
            throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                    new Object[]{"状态机定义", stateType.getSimpleName()});
        }
        return new DefaultStateMachineInstance<>(definition, strictMode);
    }

    @Override
    public void register(StateMachineDefinition<?> definition) {
        if (definition == null) {
            return;
        }
        definitionsByType.put(definition.getStateType(), definition);
        definitionsByName.put(definition.getName(), definition);
        log.info("注册状态机定义: name={}, stateType={}, entityType={}, transitions={}",
                definition.getName(),
                definition.getStateType().getSimpleName(),
                definition.getEntityType().getSimpleName(),
                definition.getTransitions().size());
    }
}
