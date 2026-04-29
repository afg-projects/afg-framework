package io.github.afgprojects.framework.core.module;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * 模块状态信息
 * 记录模块的当前状态及相关元数据
 */
public record ModuleStatus(
        ModuleState state,
        Instant stateChangedAt,
        @Nullable String errorMessage,
        @Nullable Throwable errorCause,
        Map<String, Object> metadata) {

    /**
     * 创建模块状态信息
     */
    public ModuleStatus {
        if (metadata == null) {
            metadata = new ConcurrentHashMap<>();
        }
    }

    /**
     * 创建初始状态（REGISTERED）
     */
    public static ModuleStatus initial() {
        return new ModuleStatus(ModuleState.REGISTERED, Instant.now(), null, null, new ConcurrentHashMap<>());
    }

    /**
     * 创建失败状态
     *
     * @param errorMessage 错误消息
     * @param errorCause   错误原因
     * @return 失败状态信息
     */
    public static ModuleStatus failed(String errorMessage, @Nullable Throwable errorCause) {
        return new ModuleStatus(ModuleState.FAILED, Instant.now(), errorMessage, errorCause, new ConcurrentHashMap<>());
    }

    /**
     * 转换到新状态
     *
     * @param newState 新状态
     * @return 新的状态信息
     */
    public ModuleStatus transitionTo(ModuleState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition from " + state + " to " + newState);
        }
        // 保留原有 metadata
        Map<String, Object> newMetadata = new ConcurrentHashMap<>(this.metadata);
        return new ModuleStatus(newState, Instant.now(), null, null, newMetadata);
    }

    /**
     * 获取不可变的元数据视图
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * 判断模块是否可操作
     */
    public boolean isOperational() {
        return state.isOperational();
    }
}
