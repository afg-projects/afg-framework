package io.github.afgprojects.framework.core.web.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class MetricsAspectTest {

    private MeterRegistry meterRegistry;
    private MetricsProperties properties;
    private MetricsAspect metricsAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new MetricsProperties();
        metricsAspect = new MetricsAspect(meterRegistry, properties);
    }

    @Test
    void should_recordTimer_when_timedMetricAnnotation() throws Throwable {
        // Given
        TimedMetric annotation = createTimedMetric("test.timer", "Test description", new double[] {0.5, 0.95});
        setupJoinPoint("testMethod");

        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = metricsAspect.timeAround(joinPoint, annotation);

        // Then
        assertThat(result).isEqualTo("result");
        Timer timer = meterRegistry.find("test.timer").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getDescription()).isEqualTo("Test description");
        assertThat(timer.getId().getTag("exception")).isEqualTo("none");
    }

    @Test
    void should_recordTimerWithException_when_exceptionThrown() throws Throwable {
        // Given
        TimedMetric annotation = createTimedMetric("test.timer.error", "Error timer", new double[] {0.5});
        setupJoinPoint("errorMethod");

        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

        // When & Then
        assertThatThrownBy(() -> metricsAspect.timeAround(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test error");

        Timer timer = meterRegistry.find("test.timer.error").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getTag("exception")).isEqualTo("RuntimeException");
    }

    @Test
    void should_useClassNameMethodName_when_nameIsEmpty() throws Throwable {
        // Given
        TimedMetric annotation = createTimedMetric("", "", new double[] {0.5});
        setupJoinPoint("myMethod");
        when(methodSignature.getDeclaringType()).thenReturn(TestService.class);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        metricsAspect.timeAround(joinPoint, annotation);

        // Then
        Timer timer = meterRegistry.find("TestService.myMethod").timer();
        assertThat(timer).isNotNull();
    }

    @Test
    void should_recordCounter_when_countedMetricAnnotation() throws Throwable {
        // Given
        CountedMetric annotation = createCountedMetric("test.counter", "Test counter");
        setupJoinPoint("countMethod");

        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = metricsAspect.countAround(joinPoint, annotation);

        // Then
        assertThat(result).isEqualTo("result");
        Counter counter = meterRegistry.find("test.counter").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.getId().getDescription()).isEqualTo("Test counter");
        assertThat(counter.getId().getTag("exception")).isEqualTo("none");
        assertThat(counter.getId().getTag("result")).isEqualTo("success");
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void should_recordCounterWithException_when_exceptionThrown() throws Throwable {
        // Given
        CountedMetric annotation = createCountedMetric("test.counter.error", "Error counter");
        setupJoinPoint("errorCountMethod");

        when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Invalid argument"));

        // When & Then
        assertThatThrownBy(() -> metricsAspect.countAround(joinPoint, annotation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid argument");

        Counter counter = meterRegistry.find("test.counter.error").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.getId().getTag("exception")).isEqualTo("IllegalArgumentException");
        assertThat(counter.getId().getTag("result")).isEqualTo("failure");
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void should_addGlobalTags_when_configured() throws Throwable {
        // Given
        properties.getTags().put("env", "test");
        properties.getTags().put("service", "afg-core");

        TimedMetric annotation = createTimedMetric("tagged.timer", "Tagged timer", new double[] {0.5});
        setupJoinPoint("taggedMethod");

        when(joinPoint.proceed()).thenReturn("result");

        // When
        metricsAspect.timeAround(joinPoint, annotation);

        // Then
        Timer timer = meterRegistry.find("tagged.timer").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.getId().getTag("env")).isEqualTo("test");
        assertThat(timer.getId().getTag("service")).isEqualTo("afg-core");
    }

    @Test
    void should_recordMultipleCalls_when_counterCalledMultipleTimes() throws Throwable {
        // Given
        CountedMetric annotation = createCountedMetric("multi.counter", "Multi counter");
        setupJoinPoint("multiMethod");

        when(joinPoint.proceed()).thenReturn("result");

        // When
        metricsAspect.countAround(joinPoint, annotation);
        metricsAspect.countAround(joinPoint, annotation);
        metricsAspect.countAround(joinPoint, annotation);

        // Then
        Counter counter = meterRegistry.find("multi.counter").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    private void setupJoinPoint(String methodName) {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getName()).thenReturn(methodName);
        lenient().when(methodSignature.getDeclaringType()).thenReturn(Object.class);
    }

    private TimedMetric createTimedMetric(String name, String description, double[] percentiles) {
        return new TimedMetric() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public double[] percentiles() {
                return percentiles;
            }

            @Override
            public Class<TimedMetric> annotationType() {
                return TimedMetric.class;
            }
        };
    }

    private CountedMetric createCountedMetric(String name, String description) {
        return new CountedMetric() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public Class<CountedMetric> annotationType() {
                return CountedMetric.class;
            }
        };
    }

    // 用于方法名解析的测试服务类
    static class TestService {}
}
