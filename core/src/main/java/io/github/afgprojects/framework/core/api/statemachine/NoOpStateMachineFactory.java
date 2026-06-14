package io.github.afgprojects.framework.core.api.statemachine;

import java.util.List;

import io.github.afgprojects.framework.core.statemachine.StateMachineDefinition;

/**
 * NoOp 状态机工厂实现
 * <p>
 * 本地降级实现，所有操作均为空操作或返回安全默认值。
 * {@code getDefinition} 返回空的默认定义（name="noop", states=empty, transitions=empty），
 * 避免调用方 NPE 风险；
 * {@code create} 返回 NoOp 状态机实例；
 * {@code register} 静默忽略。
 * </p>
 * <p>
 * 由 {@code StateMachineAutoConfiguration} 在状态机功能禁用或无其他实现时自动注册。
 * 适用于不需要状态机功能的场景。
 * </p>
 *
 * @since 1.0.0
 */
public class NoOpStateMachineFactory implements StateMachineFactory {

    @Override
    public <S extends Enum<S>> StateMachineDefinition<S> getDefinition(Class<S> stateType) {
        return createEmptyDefinition(stateType.getSimpleName());
    }

    @Override
    public <S extends Enum<S>> StateMachineDefinition<S> getDefinition(String name) {
        return createEmptyDefinition(name);
    }

    @SuppressWarnings("unchecked")
    private <S extends Enum<S>> StateMachineDefinition<S> createEmptyDefinition(String name) {
        return (StateMachineDefinition<S>) StateMachineDefinition.builder()
                .name(name != null ? name : "noop")
                .build();
    }

    @Override
    public <T, S extends Enum<S>> StateMachineInstance<T, S> create(Class<S> stateType) {
        return new NoOpStateMachineInstance<>();
    }

    @Override
    public void register(StateMachineDefinition<?> definition) {
        // no-op: 静默忽略注册
    }

    /**
     * NoOp 状态机实例
     * <p>
     * 所有操作均为空操作或返回默认值。
     * </p>
     */
    private static class NoOpStateMachineInstance<T, S extends Enum<S>> implements StateMachineInstance<T, S> {

        @Override
        public S getCurrentState(T entity) {
            return null;
        }

        @Override
        public void transit(T entity, S targetState) {
            // no-op: 静默忽略转换
        }

        @Override
        public boolean canTransit(T entity, S targetState) {
            return false;
        }

        @Override
        public List<S> getAvailableTransitions(T entity) {
            return List.of();
        }

        @Override
        public StateMachineDefinition<S> getDefinition() {
            return createEmptyDefinition();
        }

        @SuppressWarnings("unchecked")
        private StateMachineDefinition<S> createEmptyDefinition() {
            return (StateMachineDefinition<S>) StateMachineDefinition.builder()
                    .name("noop")
                    .build();
        }
    }
}
