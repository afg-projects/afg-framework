package io.github.afgprojects.framework.core.web.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * CountedMetric 注解单元测试。
 * <p>
 * 测试计数指标注解的功能，验证注解属性和元注解配置。
 *
 * @see CountedMetric
 */
class CountedMetricTest {

    /**
     * 测试注解声明时具有默认值。
     */
    @Test
    void should_haveDefaultValues_when_annotationDeclared() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("annotatedMethod");
        CountedMetric annotation = method.getAnnotation(CountedMetric.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEmpty();
        assertThat(annotation.description()).isEmpty();
    }

    @Test
    void should_haveCustomValues_when_annotationConfigured() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("customMethod");
        CountedMetric annotation = method.getAnnotation(CountedMetric.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("custom.counter");
        assertThat(annotation.description()).isEqualTo("Custom counter description");
    }

    @Test
    void should_targetMethod_when_annotationApplied() {
        assertThat(CountedMetric.class.isAnnotationPresent(java.lang.annotation.Target.class))
                .isTrue();
        assertThat(CountedMetric.class
                        .getAnnotation(java.lang.annotation.Target.class)
                        .value())
                .containsExactly(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void should_retainRuntime_when_annotationApplied() {
        assertThat(CountedMetric.class.isAnnotationPresent(java.lang.annotation.Retention.class))
                .isTrue();
        assertThat(CountedMetric.class
                        .getAnnotation(java.lang.annotation.Retention.class)
                        .value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    // 带注解方法的测试类
    static class TestClass {

        @CountedMetric
        public void annotatedMethod() {}

        @CountedMetric(name = "custom.counter", description = "Custom counter description")
        public void customMethod() {}
    }
}
