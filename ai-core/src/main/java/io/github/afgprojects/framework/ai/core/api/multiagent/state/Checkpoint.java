package io.github.afgprojects.framework.ai.core.api.multiagent.state;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Map;

/**
 * 检查点
 *
 * @param checkpointId 检查点ID
 * @param workflowId   工作流ID
 * @param nodeId       节点ID
 * @param timestamp    时间戳
 * @param state        状态快照
 */
public record Checkpoint(
        @NonNull String checkpointId,
        @NonNull String workflowId,
        @NonNull String nodeId,
        @NonNull Instant timestamp,
        @NonNull Map<String, Object> state
) {
    /**
     * 创建检查点
     */
    public static Checkpoint of(String workflowId, String nodeId, Map<String, Object> state) {
        return new Checkpoint(
                java.util.UUID.randomUUID().toString(),
                workflowId,
                nodeId,
                Instant.now(),
                Map.copyOf(state)
        );
    }
}
