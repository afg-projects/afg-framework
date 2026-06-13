package io.github.afgprojects.framework.core.statemachine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态转换定义
 * <p>
 * 描述一个合法的状态转换规则：从源状态（from）到目标状态（to），
 * 由特定事件（event）触发，对应枚举类中的某个方法（methodName）。
 * </p>
 *
 * @param <S> 状态枚举类型
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitionDefinition<S extends Enum<S>> {

    /**
     * 源状态
     */
    private S from;

    /**
     * 目标状态
     */
    private S to;

    /**
     * 触发事件名
     */
    private String event;

    /**
     * 对应的枚举类方法名
     */
    private String methodName;
}
