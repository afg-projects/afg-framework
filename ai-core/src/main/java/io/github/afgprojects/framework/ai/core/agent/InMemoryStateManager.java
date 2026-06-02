package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.state.*;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存状态管理器
 *
 * <p>适用于单机环境，状态存储在内存中。
 * 基于 ConcurrentHashMap 实现线程安全的状态存储。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class InMemoryStateManager implements StateManager {

    private final ConcurrentHashMap<String, WorkflowState> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Checkpoint>> checkpoints = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public WorkflowState createState(@NonNull String workflowId, @NonNull String graphName, @NonNull WorkflowInput input) {
        WorkflowState state = input.toInitialState()
                .with("_workflowId", workflowId)
                .with("_graphName", graphName)
                .with("_status", WorkflowStatus.RUNNING.name())
                .with("_createdAt", Instant.now());

        states.put(workflowId, state);
        return state;
    }

    @Override
    @NonNull
    public Optional<WorkflowState> getState(@NonNull String workflowId) {
        return Optional.ofNullable(states.get(workflowId));
    }

    @Override
    @NonNull
    public WorkflowState updateState(@NonNull String workflowId, @NonNull WorkflowState state) {
        WorkflowState updated = state.with("_updatedAt", Instant.now());
        states.put(workflowId, updated);
        return updated;
    }

    @Override
    public void deleteState(@NonNull String workflowId) {
        states.remove(workflowId);
        checkpoints.remove(workflowId);
    }

    @Override
    public void saveCheckpoint(@NonNull String workflowId, @NonNull Checkpoint checkpoint) {
        checkpoints.compute(workflowId, (k, v) -> {
            List<Checkpoint> list = v != null ? v : new ArrayList<>();
            list.add(checkpoint);
            return list;
        });
    }

    @Override
    @NonNull
    public Optional<Checkpoint> getLatestCheckpoint(@NonNull String workflowId) {
        List<Checkpoint> list = checkpoints.get(workflowId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(list.size() - 1));
    }

    @Override
    @NonNull
    public Optional<Checkpoint> getCheckpoint(@NonNull String workflowId, @NonNull String checkpointId) {
        List<Checkpoint> list = checkpoints.get(workflowId);
        if (list == null) {
            return Optional.empty();
        }
        return list.stream()
                .filter(c -> c.checkpointId().equals(checkpointId))
                .findFirst();
    }

    @Override
    @NonNull
    public WorkflowState restoreFromCheckpoint(@NonNull String workflowId, @NonNull String checkpointId) {
        WorkflowState current = getState(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        Checkpoint checkpoint = getCheckpoint(workflowId, checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + checkpointId));

        WorkflowState restored = new WorkflowState(checkpoint.state())
                .with("_workflowId", current.get("_workflowId"))
                .with("_graphName", current.get("_graphName"))
                .with("_status", WorkflowStatus.RUNNING.name())
                .with("_currentNodeId", checkpoint.nodeId())
                .with("_restoredFrom", checkpointId)
                .with("_restoredAt", Instant.now());

        states.put(workflowId, restored);
        return restored;
    }

    @Override
    public void cleanup(@NonNull Duration ttl) {
        Instant threshold = Instant.now().minus(ttl);

        states.entrySet().removeIf(entry -> {
            WorkflowState state = entry.getValue();
            Instant createdAt = state.get("_createdAt");
            Instant updatedAt = state.get("_updatedAt");
            Instant lastUpdate = updatedAt != null ? updatedAt : createdAt;

            if (lastUpdate != null && lastUpdate.isBefore(threshold)) {
                checkpoints.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}