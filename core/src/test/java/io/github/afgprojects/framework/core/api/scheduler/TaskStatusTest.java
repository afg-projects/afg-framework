package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link TaskStatus} 任务状态枚举测试
 *
 * <p>验证任务状态枚举包含所有定义的状态：
 * <ul>
 *   <li>SCHEDULED - 已调度</li>
 *   <li>RUNNING - 运行中</li>
 *   <li>PAUSED - 已暂停</li>
 *   <li>COMPLETED - 已完成</li>
 *   <li>CANCELLED - 已取消</li>
 *   <li>FAILED - 已失败</li>
 * </ul>
 *
 * @see TaskStatus
 */
@DisplayName("TaskStatus 测试")
class TaskStatusTest {

    /**
     * 验证枚举包含所有 6 种任务状态
     */
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

    /**
     * 验证枚举名称正确
     */
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
