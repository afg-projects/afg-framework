package io.github.afgprojects.framework.core.statemachine.exception;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

/**
 * 非法状态转换异常
 * <p>
 * 当状态机执行不合法的状态转换时抛出（严格模式）。
 * 包含源状态、目标状态和状态机名称信息，便于问题定位。
 * </p>
 *
 * @since 1.0.0
 */
public class InvalidTransitionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final String stateMachineName;
    private final String fromState;
    private final String toState;

    /**
     * 构造非法状态转换异常
     *
     * @param stateMachineName 状态机名称
     * @param fromState        源状态
     * @param toState          目标状态
     */
    public InvalidTransitionException(String stateMachineName, Enum<?> fromState, Enum<?> toState) {
        super(CommonErrorCode.FAIL,
                String.format("非法状态转换: 状态机[%s] 不支持从 [%s] 转换到 [%s]",
                        stateMachineName, fromState, toState));
        this.stateMachineName = stateMachineName;
        this.fromState = fromState != null ? fromState.name() : null;
        this.toState = toState != null ? toState.name() : null;
    }

    /**
     * 构造非法状态转换异常（使用自定义消息）
     *
     * @param stateMachineName 状态机名称
     * @param fromState        源状态
     * @param toState          目标状态
     * @param message          自定义消息
     */
    public InvalidTransitionException(String stateMachineName, Enum<?> fromState, Enum<?> toState, String message) {
        super(CommonErrorCode.FAIL, message);
        this.stateMachineName = stateMachineName;
        this.fromState = fromState != null ? fromState.name() : null;
        this.toState = toState != null ? toState.name() : null;
    }

    /**
     * 获取状态机名称
     *
     * @return 状态机名称
     */
    public String getStateMachineName() {
        return stateMachineName;
    }

    /**
     * 获取源状态名称
     *
     * @return 源状态名称
     */
    public String getFromState() {
        return fromState;
    }

    /**
     * 获取目标状态名称
     *
     * @return 目标状态名称
     */
    public String getToState() {
        return toState;
    }
}
