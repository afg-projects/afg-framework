package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.tracing.Span;

/**
 * TracingLogRecorder 测试
 */
@DisplayName("TracingLogRecorder 测试")
@ExtendWith(MockitoExtension.class)
class TracingLogRecorderTest {

    private TracingLogRecorder recorder;

    @Mock
    private Span span;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        recorder = new TracingLogRecorder();
    }

    @Nested
    @DisplayName("logParameters 测试")
    class LogParametersTests {

        @Test
        @DisplayName("应该记录参数")
        void shouldLogParameters() throws NoSuchMethodException {
            when(joinPoint.getArgs()).thenReturn(new Object[]{"value1", 123});
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1", "param2"});

            recorder.logParameters(span, joinPoint);

            verify(span).tag("param.param1", "value1");
            verify(span).tag("param.param2", "123");
        }

        @Test
        @DisplayName("空参数应该跳过")
        void shouldSkipEmptyArgs() {
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            recorder.logParameters(span, joinPoint);

            verify(span, never()).tag(anyString(), anyString());
        }

        @Test
        @DisplayName("null 参数应该记录为 null")
        void shouldLogNullAsNull() throws NoSuchMethodException {
            when(joinPoint.getArgs()).thenReturn(new Object[]{null});
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"param1"});

            recorder.logParameters(span, joinPoint);

            verify(span).tag("param.param1", "null");
        }
    }

    @Nested
    @DisplayName("logResult 测试")
    class LogResultTests {

        @Test
        @DisplayName("应该记录返回值")
        void shouldLogResult() {
            recorder.logResult(span, "test-result");

            verify(span).tag("result", "test-result");
        }

        @Test
        @DisplayName("null 返回值应该记录为 null")
        void shouldLogNullResult() {
            recorder.logResult(span, null);

            verify(span).tag("result", "null");
        }

        @Test
        @DisplayName("长返回值应该截断")
        void shouldTruncateLongResult() {
            String longResult = "a".repeat(1500);

            recorder.logResult(span, longResult);

            verify(span).tag("result", "a".repeat(1000) + "...[truncated]");
        }
    }

    @Nested
    @DisplayName("logException 测试")
    class LogExceptionTests {

        @Test
        @DisplayName("应该记录异常消息")
        void shouldLogExceptionMessage() {
            Exception ex = new RuntimeException("Test error");

            recorder.logException(span, ex, ExceptionLogLevel.MESSAGE);

            verify(span).tag("exception.message", "Test error");
            verify(span, never()).tag(eq("exception.stacktrace"), anyString());
        }

        @Test
        @DisplayName("应该记录堆栈跟踪")
        void shouldLogStackTrace() {
            Exception ex = new RuntimeException("Test error");

            recorder.logException(span, ex, ExceptionLogLevel.STACK_TRACE);

            verify(span).tag("exception.message", "Test error");
            verify(span).tag(eq("exception.stacktrace"), anyString());
        }
    }

    @Nested
    @DisplayName("safeToString 测试")
    class SafeToStringTests {

        @Test
        @DisplayName("应该返回字符串表示")
        void shouldReturnStringRepresentation() {
            String result = recorder.safeToString("test");

            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("null 应该返回 null")
        void shouldReturnNullForNull() {
            String result = recorder.safeToString(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("对象 toString 异常应该返回错误消息")
        void shouldHandleToStringException() {
            Object obj = new Object() {
                @Override
                public String toString() {
                    throw new RuntimeException("toString error");
                }
            };

            String result = recorder.safeToString(obj);

            assertThat(result).contains("error calling toString");
        }
    }
}
