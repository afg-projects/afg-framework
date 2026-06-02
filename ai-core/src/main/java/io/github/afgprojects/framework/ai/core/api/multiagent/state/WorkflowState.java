package io.github.afgprojects.framework.ai.core.api.multiagent.state;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流状态
 *
 * <p>工作流执行过程中的状态容器，支持不可变更新。
 */
public class WorkflowState {

    private final @NonNull Map<String, Object> data;
    private final @Nullable String currentNodeId;
    private final @NonNull Instant createdAt;
    private final @Nullable Instant updatedAt;

    public WorkflowState() {
        this(new HashMap<>(), null, Instant.now(), null);
    }

    public WorkflowState(@NonNull Map<String, Object> data) {
        this(data, null, Instant.now(), null);
    }

    private WorkflowState(
            @NonNull Map<String, Object> data,
            @Nullable String currentNodeId,
            @NonNull Instant createdAt,
            @Nullable Instant updatedAt) {
        this.data = new HashMap<>(data);
        this.currentNodeId = currentNodeId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 获取状态值
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(@NonNull String key) {
        return (T) data.get(key);
    }

    /**
     * 获取状态值（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> @NonNull T get(@NonNull String key, @NonNull T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取状态值（Optional）
     */
    @SuppressWarnings("unchecked")
    public <T> @NonNull Optional<T> getOptional(@NonNull String key) {
        return Optional.ofNullable((T) data.get(key));
    }

    /**
     * 更新状态值（返回新实例）
     */
    public @NonNull WorkflowState with(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newData = new HashMap<>(this.data);
        if (value != null) {
            newData.put(key, value);
        } else {
            newData.remove(key);
        }
        return new WorkflowState(newData, this.currentNodeId, this.createdAt, Instant.now());
    }

    /**
     * 批量更新状态值（返回新实例）
     */
    public @NonNull WorkflowState withAll(@NonNull Map<String, Object> values) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.putAll(values);
        return new WorkflowState(newData, this.currentNodeId, this.createdAt, Instant.now());
    }

    /**
     * 设置当前节点
     */
    public @NonNull WorkflowState withCurrentNode(@Nullable String nodeId) {
        return new WorkflowState(this.data, nodeId, this.createdAt, Instant.now());
    }

    /**
     * 获取当前节点ID
     */
    public @Nullable String getCurrentNodeId() {
        return currentNodeId;
    }

    /**
     * 获取创建时间
     */
    public @NonNull Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取更新时间
     */
    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取所有数据（只读）
     */
    public @NonNull Map<String, Object> getData() {
        return Map.copyOf(data);
    }

    /**
     * 检查是否包含键
     */
    public boolean containsKey(@NonNull String key) {
        return data.containsKey(key);
    }

    /**
     * 创建空状态
     */
    public static @NonNull WorkflowState empty() {
        return new WorkflowState();
    }
}
