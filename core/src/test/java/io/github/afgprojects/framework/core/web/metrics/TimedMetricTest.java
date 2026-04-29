package io.github.afgprojects.framework.core.web.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class TimedMetricTest {

    @Test
    void should_haveDefaultValues_when_annotationDeclared() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("annotatedMethod");
        TimedMetric annotation = method.getAnnotation(TimedMetric.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEmpty();
        assertThat(annotation.description()).isEmpty();
        assertThat(annotation.percentiles()).containsExactly(0.5, 0.95, 0.99);
    }

    @Test
    void should_haveCustomValues_when_annotationConfigured() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("customMethod");
        TimedMetric annotation = method.getAnnotation(TimedMetric.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("custom.timer");
        assertThat(annotation.description()).isEqualTo("Custom timer description");
        assertThat(annotation.percentiles()).containsExactly(0.75, 0.9);
    }

    @Test
    void should_targetMethod_when_annotationApplied() {
        assertThat(TimedMetric.class.isAnnotationPresent(java.lang.annotation.Target.class))
                .isTrue();
        assertThat(TimedMetric.class
                        .getAnnotation(java.lang.annotation.Target.class)
                        .value())
                .containsExactly(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void should_retainRuntime_when_annotationApplied() {
        assertThat(TimedMetric.class.isAnnotationPresent(java.lang.annotation.Retention.class))
                .isTrue();
        assertThat(TimedMetric.class
                        .getAnnotation(java.lang.annotation.Retention.class)
                        .value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    // 带注解方法的测试类
    static class TestClass {

        @TimedMetric
        public void annotatedMethod() {}

        @TimedMetric(
                name = "custom.timer",
                description = "Custom timer description",
                percentiles = {0.75, 0.9})
        public void customMethod() {}
    }
}
