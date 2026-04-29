package io.github.afgprojects.framework.core.module;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

/**
 * 模块状态变更事件
 * 当模块状态发生变化时发布此事件
 */
public class ModuleStateEvent extends ApplicationEvent {

    private final String moduleId;
    private final ModuleState previousState;
    private final ModuleState newState;
    private final Instant timestamp;
    private final @Nullable String reason;

    /**
     * 创建模块状态变更事件
     *
     * @param source        事件源
     * @param moduleId      模块ID
     * @param previousState 之前的状态
     * @param newState      新状态
     * @param reason        变更原因（可选）
     */
    public ModuleStateEvent(
            @NonNull Object source,
            @NonNull String moduleId,
            @NonNull ModuleState previousState,
            @NonNull ModuleState newState,
            @Nullable String reason) {
        super(source);
        this.moduleId = moduleId;
        this.previousState = previousState;
        this.newState = newState;
        this.timestamp = Instant.now();
        this.reason = reason;
    }

    /**
     * 获取模块ID
     */
    @NonNull public String getModuleId() {
        return moduleId;
    }

    /**
     * 获取之前的状态
     */
    @NonNull public ModuleState getPreviousState() {
        return previousState;
    }

    /**
     * 获取新状态
     */
    @NonNull public ModuleState getNewState() {
        return newState;
    }

    /**
     * 获取事件时间戳
     */
    @NonNull public Instant getEventTimestamp() {
        return timestamp;
    }

    /**
     * 获取变更原因
     */
    @Nullable public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ModuleStateEvent{" + "moduleId='"
                + moduleId + '\'' + ", previousState="
                + previousState + ", newState="
                + newState + ", timestamp="
                + timestamp + ", reason='"
                + reason + '\'' + '}';
    }
}
