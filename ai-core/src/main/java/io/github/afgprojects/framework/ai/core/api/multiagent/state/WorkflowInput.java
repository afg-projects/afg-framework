package io.github.afgprojects.framework.ai.core.api.multiagent.state;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流输入
 *
 * <p>工作流执行的输入参数容器。
 */
public class WorkflowInput {

    private final @NonNull Map<String, Object> params;
    private final @Nullable String threadId;
    private final @Nullable String checkpointId;

    public WorkflowInput() {
        this(new HashMap<>(), null, null);
    }

    public WorkflowInput(@NonNull Map<String, Object> params) {
        this(params, null, null);
    }

    private WorkflowInput(
            @NonNull Map<String, Object> params,
            @Nullable String threadId,
            @Nullable String checkpointId) {
        this.params = new HashMap<>(params);
        this.threadId = threadId;
        this.checkpointId = checkpointId;
    }

    /**
     * 获取参数值
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(@NonNull String key) {
        return (T) params.get(key);
    }

    /**
     * 获取参数值（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> @NonNull T get(@NonNull String key, @NonNull T defaultValue) {
        Object value = params.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 设置参数
     */
    public @NonNull WorkflowInput with(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newParams = new HashMap<>(this.params);
        if (value != null) {
            newParams.put(key, value);
        } else {
            newParams.remove(key);
        }
        return new WorkflowInput(newParams, this.threadId, this.checkpointId);
    }

    /**
     * 设置线程ID
     */
    public @NonNull WorkflowInput withThreadId(@Nullable String threadId) {
        return new WorkflowInput(this.params, threadId, this.checkpointId);
    }

    /**
     * 设置检查点ID（用于恢复）
     */
    public @NonNull WorkflowInput withCheckpointId(@Nullable String checkpointId) {
        return new WorkflowInput(this.params, this.threadId, checkpointId);
    }

    /**
     * 获取线程ID
     */
    public @Nullable String getThreadId() {
        return threadId;
    }

    /**
     * 获取检查点ID
     */
    public @Nullable String getCheckpointId() {
        return checkpointId;
    }

    /**
     * 获取所有参数（只读）
     */
    public @NonNull Map<String, Object> getParams() {
        return Map.copyOf(params);
    }

    /**
     * 转换为初始状态
     */
    public @NonNull WorkflowState toInitialState() {
        return new WorkflowState(params);
    }

    /**
     * 创建空输入
     */
    public static @NonNull WorkflowInput empty() {
        return new WorkflowInput();
    }
}
