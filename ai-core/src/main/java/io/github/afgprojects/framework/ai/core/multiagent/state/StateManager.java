package io.github.afgprojects.framework.ai.core.multiagent.state;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * 状态管理器接口
 */
public interface StateManager {

    /**
     * 创建工作流状态
     */
    @NonNull
    WorkflowState createState(@NonNull String workflowId, @NonNull String graphName, @NonNull WorkflowInput input);

    /**
     * 获取状态
     */
    @NonNull
    Optional<WorkflowState> getState(@NonNull String workflowId);

    /**
     * 更新状态
     */
    @NonNull
    WorkflowState updateState(@NonNull String workflowId, @NonNull WorkflowState state);

    /**
     * 删除状态
     */
    void deleteState(@NonNull String workflowId);

    /**
     * 保存检查点
     */
    void saveCheckpoint(@NonNull String workflowId, @NonNull Checkpoint checkpoint);

    /**
     * 获取最近检查点
     */
    @NonNull
    Optional<Checkpoint> getLatestCheckpoint(@NonNull String workflowId);

    /**
     * 获取指定检查点
     */
    @NonNull
    Optional<Checkpoint> getCheckpoint(@NonNull String workflowId, @NonNull String checkpointId);

    /**
     * 从检查点恢复
     */
    @NonNull
    WorkflowState restoreFromCheckpoint(@NonNull String workflowId, @NonNull String checkpointId);

    /**
     * 清理过期状态
     */
    void cleanup(@NonNull Duration ttl);
}
