package io.github.afgprojects.framework.ai.agent.state;

import io.github.afgprojects.framework.ai.core.api.multiagent.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryStateManagerTest {

    private InMemoryStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new InMemoryStateManager();
    }

    @Test
    @DisplayName("创建工作流状态")
    void createState_shouldCreateState() {
        WorkflowInput input = WorkflowInput.empty().with("input", "test input");

        WorkflowState state = stateManager.createState("wf-1", "test-graph", input);

        assertThat((String) state.get("_workflowId")).isEqualTo("wf-1");
        assertThat((String) state.get("_graphName")).isEqualTo("test-graph");
        assertThat((String) state.get("_status")).isEqualTo(WorkflowStatus.RUNNING.name());
        assertThat((String) state.get("input")).isEqualTo("test input");
        assertThat(state.<Instant>get("_createdAt")).isNotNull();
    }

    @Test
    @DisplayName("创建状态时合并输入上下文")
    void createState_shouldMergeInputContext() {
        WorkflowInput input = WorkflowInput.empty()
                .with("key1", "value1")
                .with("key2", "value2");

        WorkflowState state = stateManager.createState("wf-1", "test-graph", input);

        assertThat((String) state.get("key1")).isEqualTo("value1");
        assertThat((String) state.get("key2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("获取存在的状态")
    void getState_existingState_returnsState() {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        Optional<WorkflowState> state = stateManager.getState("wf-1");

        assertThat(state).isPresent();
        assertThat((String) state.get().get("_workflowId")).isEqualTo("wf-1");
    }

    @Test
    @DisplayName("获取不存在的状态返回空")
    void getState_nonExisting_returnsEmpty() {
        Optional<WorkflowState> state = stateManager.getState("non-existing");

        assertThat(state).isEmpty();
    }

    @Test
    @DisplayName("更新状态")
    void updateState_shouldUpdate() {
        WorkflowState original = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());
        WorkflowState updated = original.with("key", "value");

        stateManager.updateState("wf-1", updated);

        Optional<WorkflowState> retrieved = stateManager.getState("wf-1");
        assertThat(retrieved).isPresent();
        assertThat((String) retrieved.get().get("key")).isEqualTo("value");
        assertThat(retrieved.get().<Instant>get("_updatedAt")).isNotNull();
    }

    @Test
    @DisplayName("删除状态")
    void deleteState_shouldRemove() {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        stateManager.deleteState("wf-1");

        assertThat(stateManager.getState("wf-1")).isEmpty();
    }

    @Test
    @DisplayName("删除状态时同时删除检查点")
    void deleteState_shouldAlsoRemoveCheckpoints() {
        WorkflowState state = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());
        Checkpoint checkpoint = Checkpoint.of("wf-1", "node-1", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint);

        stateManager.deleteState("wf-1");

        assertThat(stateManager.getLatestCheckpoint("wf-1")).isEmpty();
    }

    @Test
    @DisplayName("保存检查点")
    void saveCheckpoint_shouldSave() {
        WorkflowState state = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());
        Checkpoint checkpoint = Checkpoint.of("wf-1", "node-1", state.getData());

        stateManager.saveCheckpoint("wf-1", checkpoint);

        Optional<Checkpoint> retrieved = stateManager.getCheckpoint("wf-1", checkpoint.checkpointId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().nodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("获取最新检查点")
    void getLatestCheckpoint_shouldReturnLatest() {
        WorkflowState state = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        Checkpoint checkpoint1 = Checkpoint.of("wf-1", "node-1", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint1);

        Checkpoint checkpoint2 = Checkpoint.of("wf-1", "node-2", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint2);

        Optional<Checkpoint> latest = stateManager.getLatestCheckpoint("wf-1");
        assertThat(latest).isPresent();
        assertThat(latest.get().nodeId()).isEqualTo("node-2");
    }

    @Test
    @DisplayName("无检查点时返回空")
    void getLatestCheckpoint_noCheckpoints_returnsEmpty() {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        Optional<Checkpoint> latest = stateManager.getLatestCheckpoint("wf-1");

        assertThat(latest).isEmpty();
    }

    @Test
    @DisplayName("获取指定检查点")
    void getCheckpoint_shouldReturnCorrectCheckpoint() {
        WorkflowState state = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        Checkpoint checkpoint1 = Checkpoint.of("wf-1", "node-1", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint1);

        Checkpoint checkpoint2 = Checkpoint.of("wf-1", "node-2", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint2);

        Optional<Checkpoint> retrieved = stateManager.getCheckpoint("wf-1", checkpoint1.checkpointId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().nodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("从检查点恢复状态")
    void restoreFromCheckpoint_shouldRestore() {
        WorkflowState original = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty().with("original", "value"));
        Checkpoint checkpoint = Checkpoint.of("wf-1", "node-1", original.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint);

        // 修改状态
        WorkflowState modified = original.with("modified", "new value");
        stateManager.updateState("wf-1", modified);

        // 从检查点恢复
        WorkflowState restored = stateManager.restoreFromCheckpoint("wf-1", checkpoint.checkpointId());

        assertThat((String) restored.get("original")).isEqualTo("value");
        assertThat(restored.containsKey("modified")).isFalse();
        assertThat((String) restored.get("_currentNodeId")).isEqualTo("node-1");
        assertThat((String) restored.get("_restoredFrom")).isEqualTo(checkpoint.checkpointId());
        assertThat((String) restored.get("_status")).isEqualTo(WorkflowStatus.RUNNING.name());
    }

    @Test
    @DisplayName("从不存在的检查点恢复抛出异常")
    void restoreFromCheckpoint_nonExisting_throwsException() {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        assertThatThrownBy(() -> stateManager.restoreFromCheckpoint("wf-1", "non-existing-checkpoint"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Checkpoint not found");
    }

    @Test
    @DisplayName("从不存在的workflow恢复抛出异常")
    void restoreFromCheckpoint_nonExistingWorkflow_throwsException() {
        assertThatThrownBy(() -> stateManager.restoreFromCheckpoint("non-existing-wf", "checkpoint-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow not found");
    }

    @Test
    @DisplayName("清理过期状态")
    void cleanup_shouldRemoveExpired() throws InterruptedException {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        // 等待 100ms 让状态过期
        Thread.sleep(100);
        stateManager.cleanup(Duration.ofMillis(50));

        assertThat(stateManager.getState("wf-1")).isEmpty();
    }

    @Test
    @DisplayName("清理时保留未过期状态")
    void cleanup_shouldKeepNonExpired() throws InterruptedException {
        stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());

        // 等待 50ms，但 TTL 是 1 秒
        Thread.sleep(50);
        stateManager.cleanup(Duration.ofSeconds(1));

        assertThat(stateManager.getState("wf-1")).isPresent();
    }

    @Test
    @DisplayName("清理状态时同时清理检查点")
    void cleanup_shouldAlsoRemoveCheckpoints() throws InterruptedException {
        WorkflowState state = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());
        Checkpoint checkpoint = Checkpoint.of("wf-1", "node-1", state.getData());
        stateManager.saveCheckpoint("wf-1", checkpoint);

        // 等待让状态过期
        Thread.sleep(100);
        stateManager.cleanup(Duration.ofMillis(50));

        assertThat(stateManager.getLatestCheckpoint("wf-1")).isEmpty();
    }

    @Test
    @DisplayName("多工作流状态隔离")
    void multipleWorkflows_shouldBeIsolated() {
        WorkflowState state1 = stateManager.createState("wf-1", "graph-1", WorkflowInput.empty().with("key", "value1"));
        WorkflowState state2 = stateManager.createState("wf-2", "graph-2", WorkflowInput.empty().with("key", "value2"));

        assertThat((String) state1.get("key")).isEqualTo("value1");
        assertThat((String) state2.get("key")).isEqualTo("value2");

        stateManager.deleteState("wf-1");
        assertThat(stateManager.getState("wf-1")).isEmpty();
        assertThat(stateManager.getState("wf-2")).isPresent();
    }

    @Test
    @DisplayName("状态更新时间戳自动更新")
    void updateState_shouldUpdateTimestamp() throws InterruptedException {
        WorkflowState original = stateManager.createState("wf-1", "test-graph", WorkflowInput.empty());
        Instant originalCreatedAt = original.get("_createdAt");

        Thread.sleep(10);

        WorkflowState updated = original.with("key", "value");
        stateManager.updateState("wf-1", updated);

        WorkflowState retrieved = stateManager.getState("wf-1").orElseThrow();
        Instant updatedAt = retrieved.get("_updatedAt");

        assertThat(updatedAt).isAfter(originalCreatedAt);
    }
}
