package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TaskDefinition 测试
 */
@DisplayName("TaskDefinition Tests")
class TaskDefinitionTest {

    @Test
    @DisplayName("Should create cron task definition")
    void shouldCreateCronTask() {
        TaskDefinition def = TaskDefinition.ofCron("my-task", "0 0/5 * * * ?");

        assertThat(def.taskId()).isEqualTo("my-task");
        assertThat(def.cron()).isEqualTo("0 0/5 * * * ?");
        assertThat(def.isCronTask()).isTrue();
        assertThat(def.isFixedRateTask()).isFalse();
        assertThat(def.isFixedDelayTask()).isFalse();
    }

    @Test
    @DisplayName("Should create fixed rate task definition")
    void shouldCreateFixedRateTask() {
        TaskDefinition def = TaskDefinition.ofFixedRate("my-task", 5000);

        assertThat(def.taskId()).isEqualTo("my-task");
        assertThat(def.fixedRate()).isEqualTo(5000);
        assertThat(def.isFixedRateTask()).isTrue();
        assertThat(def.isCronTask()).isFalse();
    }

    @Test
    @DisplayName("Should create fixed delay task definition")
    void shouldCreateFixedDelayTask() {
        TaskDefinition def = TaskDefinition.ofFixedDelay("my-task", 3000);

        assertThat(def.taskId()).isEqualTo("my-task");
        assertThat(def.fixedDelay()).isEqualTo(3000);
        assertThat(def.isFixedDelayTask()).isTrue();
        assertThat(def.isCronTask()).isFalse();
    }

    @Test
    @DisplayName("Should create one-time task definition")
    void shouldCreateOnceTask() {
        Instant executeTime = Instant.now().plusSeconds(3600);
        TaskDefinition def = TaskDefinition.ofOnce("my-task", executeTime);

        assertThat(def.taskId()).isEqualTo("my-task");
        assertThat(def.metadata()).containsEntry("executeTime", executeTime.toString());
    }

    @Test
    @DisplayName("Should allow fluent configuration")
    void shouldAllowFluentConfiguration() {
        TaskDefinition def = TaskDefinition.ofCron("my-task", "0 0/5 * * * ?")
            .withGroup("production")
            .withDescription("My scheduled task")
            .withEnabled(false)
            .withTimeout(30000)
            .withRetry(3, 1000);

        assertThat(def.taskGroup()).isEqualTo("production");
        assertThat(def.description()).isEqualTo("My scheduled task");
        assertThat(def.enabled()).isFalse();
        assertThat(def.timeout()).isEqualTo(30000);
        assertThat(def.maxRetries()).isEqualTo(3);
        assertThat(def.retryDelay()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should set metadata")
    void shouldSetMetadata() {
        TaskDefinition def = TaskDefinition.ofCron("my-task", "0 0/5 * * * ?")
            .withMetadata(Map.of("key1", "value1", "key2", "value2"));

        assertThat(def.metadata()).containsEntry("key1", "value1");
        assertThat(def.metadata()).containsEntry("key2", "value2");
    }
}
