package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.afgprojects.framework.core.annotation.DistributedTask;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * DistributedTaskAspect 单元测试
 */
@DisplayName("DistributedTaskAspect 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedTaskAspectTest extends BaseUnitTest {

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private TaskExecutionMetrics metrics;

    @Mock
    private TaskExecutionLogStorage logStorage;

    @Mock
    private SchedulerProperties properties;

    @Mock
    private SchedulerProperties.LogStorageConfig logStorageConfig;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private DistributedTaskAspect aspect;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getLogStorage()).thenReturn(logStorageConfig);
        lenient().when(logStorageConfig.isLogErrorStack()).thenReturn(true);
        lenient().when(properties.getDefaultRetryAttempts()).thenReturn(0);
        lenient().when(properties.getDefaultRetryDelay()).thenReturn(Duration.ofMillis(100));
        lenient().when(properties.getRetryMultiplier()).thenReturn(1.0);

        aspect = new DistributedTaskAspect(distributedLock, metrics, logStorage, properties);
    }

    @Nested
    @DisplayName("锁获取测试")
    class LockAcquisitionTests {

        @Test
        @DisplayName("成功获取锁时应该执行任务")
        void shouldExecuteWhenLockAcquired() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundDistributedTask(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(distributedLock).unlock(anyString());
        }

        @Test
        @DisplayName("获取锁失败时应该跳过执行")
        void shouldSkipWhenLockNotAcquired() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(false);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");

            // when
            Object result = aspect.aroundDistributedTask(joinPoint, annotation);

            // then
            assertThat(result).isNull();
            verify(joinPoint, never()).proceed();
            verify(metrics).recordSkipped(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("任务禁用时应该跳过执行")
        void shouldSkipWhenDisabled() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", false, 5000, 30000, 1);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");

            // when
            Object result = aspect.aroundDistributedTask(joinPoint, annotation);

            // then
            assertThat(result).isNull();
            verify(distributedLock, never()).tryLock(anyString(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("锁释放测试")
    class LockReleaseTests {

        @Test
        @DisplayName("任务执行成功后应该释放锁")
        void shouldReleaseLockAfterSuccess() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDistributedTask(joinPoint, annotation);

            // then
            verify(distributedLock).unlock(anyString());
        }

        @Test
        @DisplayName("任务执行失败后应该释放锁")
        void shouldReleaseLockAfterFailure() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            // when/then
            assertThatThrownBy(() -> aspect.aroundDistributedTask(joinPoint, annotation))
                    .isInstanceOf(RuntimeException.class);

            verify(distributedLock).unlock(anyString());
        }
    }

    @Nested
    @DisplayName("指标记录测试")
    class MetricsTests {

        @Test
        @DisplayName("成功执行时应该记录成功指标")
        void shouldRecordSuccessMetrics() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenReturn("result");

            // when
            aspect.aroundDistributedTask(joinPoint, annotation);

            // then
            verify(metrics).recordStart(anyString(), anyString());
            verify(metrics).recordSuccess(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("失败执行时应该记录失败指标")
        void shouldRecordFailureMetrics() throws Throwable {
            // given
            DistributedTask annotation = createAnnotation("test-id", "0 * * * * ?", true, 5000, 30000, 1);
            when(distributedLock.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            // when/then
            assertThatThrownBy(() -> aspect.aroundDistributedTask(joinPoint, annotation))
                    .isInstanceOf(RuntimeException.class);

            verify(metrics).recordFailure(anyString(), anyString(), anyString(), anyString());
        }
    }

    private DistributedTask createAnnotation(String id, String cron, boolean enabled, long lockWaitTime, long lockLeaseTime, int shardCount) {
        return new DistributedTask() {
            @Override public String id() { return id; }
            @Override public String cron() { return cron; }
            @Override public String description() { return ""; }
            @Override public long lockWaitTime() { return lockWaitTime; }
            @Override public long lockLeaseTime() { return lockLeaseTime; }
            @Override public int shardCount() { return shardCount; }
            @Override public boolean enabled() { return enabled; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return DistributedTask.class; }
        };
    }
}