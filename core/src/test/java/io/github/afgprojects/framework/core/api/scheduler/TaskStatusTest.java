package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TaskStatus 测试
 */
@DisplayName("TaskStatus 测试")
class TaskStatusTest {

    @Test
    @DisplayName("应该包含所有任务状态")
    void shouldContainAllStatuses() {
        TaskStatus[] statuses = TaskStatus.values();

        assertThat(statuses).hasSize(6);
        assertThat(statuses).contains(
                TaskStatus.SCHEDULED,
                TaskStatus.RUNNING,
                TaskStatus.PAUSED,
                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED,
                TaskStatus.FAILED
        );
    }

    @Test
    @DisplayName("应该正确获取枚举名称")
    void shouldGetName() {
        assertThat(TaskStatus.SCHEDULED.name()).isEqualTo("SCHEDULED");
        assertThat(TaskStatus.RUNNING.name()).isEqualTo("RUNNING");
        assertThat(TaskStatus.PAUSED.name()).isEqualTo("PAUSED");
        assertThat(TaskStatus.COMPLETED.name()).isEqualTo("COMPLETED");
        assertThat(TaskStatus.CANCELLED.name()).isEqualTo("CANCELLED");
        assertThat(TaskStatus.FAILED.name()).isEqualTo("FAILED");
    }
}
