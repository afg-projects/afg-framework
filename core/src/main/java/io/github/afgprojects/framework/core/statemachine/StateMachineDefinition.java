package io.github.afgprojects.framework.core.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态机定义
 * <p>
 * 描述一个完整的状态机：名称、关联实体类型、状态枚举类型、所有合法转换和所有状态。
 * 提供 {@link #canTransit} 方法验证转换合法性，{@link #findTransitions} 方法查找可用转换。
 * </p>
 *
 * @param <S> 状态枚举类型
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateMachineDefinition<S extends Enum<S>> {

    /**
     * 状态机名称
     */
    private String name;

    /**
     * 关联的实体类
     */
    private Class<?> entityType;

    /**
     * 状态枚举类型
     */
    private Class<S> stateType;

    /**
     * 所有合法转换定义
     */
    private List<TransitionDefinition<S>> transitions;

    /**
     * 所有状态集合
     */
    private Set<S> states;

    /**
     * 验证从源状态到目标状态的转换是否合法
     *
     * @param from 源状态
     * @param to   目标状态
     * @return 如果存在合法转换返回 {@code true}
     */
    public boolean canTransit(S from, S to) {
        if (from == null || to == null) {
            return false;
        }
        return transitions.stream()
                .anyMatch(t -> t.getFrom() == from && t.getTo() == to);
    }

    /**
     * 查找从指定源状态出发的所有合法转换
     *
     * @param from 源状态
     * @return 从该状态出发的所有转换定义，如果没有则返回空列表
     */
    public List<TransitionDefinition<S>> findTransitions(S from) {
        if (from == null || transitions == null) {
            return Collections.emptyList();
        }
        return transitions.stream()
                .filter(t -> t.getFrom() == from)
                .collect(Collectors.toList());
    }

    /**
     * 查找从指定源状态可以到达的所有目标状态
     *
     * @param from 源状态
     * @return 可达的目标状态集合
     */
    public Set<S> findReachableStates(S from) {
        if (from == null || transitions == null) {
            return Collections.emptySet();
        }
        return transitions.stream()
                .filter(t -> t.getFrom() == from)
                .map(TransitionDefinition::getTo)
                .collect(Collectors.toSet());
    }

    /**
     * 查找指定事件的转换定义
     *
     * @param event 事件名
     * @return 匹配事件的转换定义列表
     */
    public List<TransitionDefinition<S>> findByEvent(String event) {
        if (event == null || transitions == null) {
            return Collections.emptyList();
        }
        return transitions.stream()
                .filter(t -> event.equals(t.getEvent()))
                .collect(Collectors.toList());
    }

    /**
     * Builder 模式构建 StateMachineDefinition
     *
     * @param <S> 状态枚举类型
     */
    public static <S extends Enum<S>> Builder<S> builder() {
        return new Builder<>();
    }

    /**
     * Builder
     *
     * @param <S> 状态枚举类型
     */
    @SuppressWarnings("unchecked")
    public static class Builder<S extends Enum<S>> {

        private String name;
        private Class<?> entityType;
        private Class<S> stateType;
        private final List<TransitionDefinition<S>> transitions = new ArrayList<>();
        private final Set<S> states = new HashSet<>();

        public Builder<S> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<S> entityType(Class<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * 设置状态枚举类型（带泛型）
         *
         * @param stateType 状态枚举类
         * @return Builder 实例
         */
        public Builder<S> stateType(Class<S> stateType) {
            this.stateType = stateType;
            return this;
        }

        /**
         * 设置状态枚举类型（原始类型）
         * <p>
         * 用于在编译时无法确定泛型类型的场景（如扫描器中动态发现枚举类）。
         * </p>
         *
         * @param stateType 状态枚举类
         * @return Builder 实例
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Builder<S> stateTypeRaw(Class<?> stateType) {
            this.stateType = (Class<S>) stateType;
            return this;
        }

        public Builder<S> transition(TransitionDefinition<S> transition) {
            this.transitions.add(transition);
            this.states.add(transition.getFrom());
            this.states.add(transition.getTo());
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<S> transitionRaw(TransitionDefinition<?> transition) {
            this.transitions.add((TransitionDefinition<S>) transition);
            this.states.add((S) transition.getFrom());
            this.states.add((S) transition.getTo());
            return this;
        }

        public Builder<S> transitions(List<TransitionDefinition<S>> transitions) {
            for (TransitionDefinition<S> t : transitions) {
                this.transitions.add(t);
                this.states.add(t.getFrom());
                this.states.add(t.getTo());
            }
            return this;
        }

        public Builder<S> state(S state) {
            this.states.add(state);
            return this;
        }

        /**
         * 添加状态（原始类型）
         * <p>
         * 用于在编译时无法确定泛型类型的场景。
         * </p>
         *
         * @param state 枚举常量
         * @return Builder 实例
         */
        @SuppressWarnings("unchecked")
        public Builder<S> stateRaw(Enum<?> state) {
            this.states.add((S) state);
            return this;
        }

        public StateMachineDefinition<S> build() {
            return new StateMachineDefinition<>(name, entityType, stateType,
                    new ArrayList<>(transitions), new HashSet<>(states));
        }
    }
}
