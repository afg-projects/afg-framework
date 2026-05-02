package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.api.scheduler.InMemoryTaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockType;
import io.github.afgprojects.framework.core.scheduler.DistributedTaskAspect;
import io.github.afgprojects.framework.core.scheduler.DynamicTaskManager;
import io.github.afgprojects.framework.core.scheduler.LocalTaskScheduler;
import io.github.afgprojects.framework.core.scheduler.ScheduledTaskAspect;
import io.github.afgprojects.framework.core.scheduler.SchedulerHealthIndicator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * SchedulerAutoConfiguration 集成测试
 */
@DisplayName("SchedulerAutoConfiguration 集成测试")
class SchedulerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SchedulerAutoConfiguration.class));

    @Nested
    @DisplayName("基本 Bean 创建测试")
    class BasicBeanTests {

        @Test
        @DisplayName("应该创建 TaskExecutionLogStorage")
        void shouldCreateTaskExecutionLogStorage() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TaskExecutionLogStorage.class);
                        assertThat(context.getBean(TaskExecutionLogStorage.class))
                                .isInstanceOf(InMemoryTaskExecutionLogStorage.class);
                    });
        }

        @Test
        @DisplayName("应该创建 TaskExecutionMetrics")
        void shouldCreateTaskExecutionMetrics() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TaskExecutionMetrics.class);
                    });
        }

        @Test
        @DisplayName("应该创建 LocalTaskScheduler")
        void shouldCreateLocalTaskScheduler() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LocalTaskScheduler.class);
                    });
        }

        @Test
        @DisplayName("应该创建 ScheduledTaskAspect")
        void shouldCreateScheduledTaskAspect() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(ScheduledTaskAspect.class);
                    });
        }
    }

    @Nested
    @DisplayName("DistributedTaskAspect 测试")
    class DistributedTaskAspectTests {

        @Test
        @DisplayName("有 DistributedLock 时应该创建 DistributedTaskAspect")
        void shouldCreateDistributedTaskAspectWhenLockExists() {
            contextRunner
                    .withUserConfiguration(TestConfigurationWithLock.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(DistributedTaskAspect.class);
                    });
        }

        @Test
        @DisplayName("没有 DistributedLock 时不应该创建 DistributedTaskAspect")
        void shouldNotCreateDistributedTaskAspectWhenNoLock() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(DistributedTaskAspect.class);
                    });
        }
    }

    @Nested
    @DisplayName("DynamicTaskManager 测试")
    class DynamicTaskManagerTests {

        @Test
        @DisplayName("启用动态任务时应该创建 DynamicTaskManager")
        void shouldCreateDynamicTaskManagerWhenEnabled() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues("afg.scheduler.dynamic-task.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(DynamicTaskManager.class);
                    });
        }

        @Test
        @DisplayName("禁用动态任务时不应该创建 DynamicTaskManager")
        void shouldNotCreateDynamicTaskManagerWhenDisabled() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues("afg.scheduler.dynamic-task.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(DynamicTaskManager.class);
                    });
        }
    }

    @Nested
    @DisplayName("禁用测试")
    class DisabledTests {

        @Test
        @DisplayName("禁用调度器时不应该创建任何 Bean")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues("afg.scheduler.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LocalTaskScheduler.class);
                        assertThat(context).doesNotHaveBean(ScheduledTaskAspect.class);
                    });
        }
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    static class TestConfigurationWithLock extends TestConfiguration {
        @Bean
        DistributedLock distributedLock() {
            return new TestDistributedLock();
        }
    }

    static class TestDistributedLock implements DistributedLock {
        @Override
        public boolean tryLock(String key, long waitTime, long leaseTime) {
            return true;
        }

        @Override
        public boolean tryLock(String key, long waitTime, long leaseTime, LockType lockType) {
            return true;
        }

        @Override
        public void lock(String key) {
        }

        @Override
        public void lock(String key, LockType lockType) {
        }

        @Override
        public void unlock(String key) {
        }

        @Override
        public void unlock(String key, LockType lockType) {
        }

        @Override
        public boolean isLocked(String key) {
            return false;
        }

        @Override
        public boolean isHeldByCurrentThread(String key) {
            return false;
        }

        @Override
        public boolean tryReadLock(String key, long waitTime, long leaseTime) {
            return true;
        }

        @Override
        public boolean tryWriteLock(String key, long waitTime, long leaseTime) {
            return true;
        }

        @Override
        public void unlockReadLock(String key) {
        }

        @Override
        public void unlockWriteLock(String key) {
        }
    }
}