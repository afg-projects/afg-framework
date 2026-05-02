package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import io.github.afgprojects.framework.core.annotation.ScheduledTask;
import io.github.afgprojects.framework.core.annotation.ScheduledTask.ErrorHandling;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * ScheduledTaskAspect 单元测试
 */
@DisplayName("ScheduledTaskAspect 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduledTaskAspectTest extends BaseUnitTest {

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

    private ScheduledTaskAspect aspect;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getLogStorage()).thenReturn(logStorageConfig);
        lenient().when(logStorageConfig.isLogSuccess()).thenReturn(true);
        lenient().when(logStorageConfig.isLogErrorStack()).thenReturn(true);
        lenient().when(properties.getDefaultRetryAttempts()).thenReturn(0);
        lenient().when(properties.getDefaultRetryDelay()).thenReturn(Duration.ofMillis(100));
        lenient().when(properties.getRetryMultiplier()).thenReturn(1.0);

        aspect = new ScheduledTaskAspect(metrics, logStorage, properties);
    }

    @Nested
    @DisplayName("任务执行测试")
    class ExecutionTests {

        @Test
        @DisplayName("任务禁用时应该跳过执行")
        void shouldSkipWhenDisabled() throws Throwable {
            // given
            ScheduledTask annotation = createAnnotation("test-id", false, ErrorHandling.LOG_AND_CONTINUE);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");

            // when
            Object result = aspect.aroundScheduledTask(joinPoint, annotation);

            // then
            assertThat(result).isNull();
            verify(joinPoint, never()).proceed();
            verify(metrics).recordSkipped(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("任务成功执行时应该记录指标")
        void shouldRecordMetricsOnSuccess() throws Throwable {
            // given
            ScheduledTask annotation = createAnnotation("test-id", true, ErrorHandling.LOG_AND_CONTINUE);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = aspect.aroundScheduledTask(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(metrics).recordStart(anyString(), anyString());
            verify(metrics).recordSuccess(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("任务失败时使用 LOG_AND_CONTINUE 策略应该继续")
        void shouldContinueWithLogAndContinue() throws Throwable {
            // given
            ScheduledTask annotation = createAnnotation("test-id", true, ErrorHandling.LOG_AND_CONTINUE);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            // when
            Object result = aspect.aroundScheduledTask(joinPoint, annotation);

            // then
            assertThat(result).isNull();
            verify(metrics).recordFailure(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("任务失败时使用 STOP_ON_ERROR 策略应该抛出异常")
        void shouldThrowWithStopOnError() throws Throwable {
            // given
            ScheduledTask annotation = createAnnotation("test-id", true, ErrorHandling.STOP_ON_ERROR);
            when(metrics.recordStart(anyString(), anyString())).thenReturn("exec-id");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            // when/then
            assertThatThrownBy(() -> aspect.aroundScheduledTask(joinPoint, annotation))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test error");
        }
    }

    private ScheduledTask createAnnotation(String id, boolean enabled, ErrorHandling errorHandling) {
        return new ScheduledTask() {
            @Override public String id() { return id; }
            @Override public String cron() { return ""; }
            @Override public long fixedRate() { return -1; }
            @Override public long fixedDelay() { return -1; }
            @Override public long initialDelay() { return 0; }
            @Override public String description() { return ""; }
            @Override public boolean enabled() { return enabled; }
            @Override public long timeout() { return -1; }
            @Override public ErrorHandling errorHandling() { return errorHandling; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ScheduledTask.class; }
        };
    }
}