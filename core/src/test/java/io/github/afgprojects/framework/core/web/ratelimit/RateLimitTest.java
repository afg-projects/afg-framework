package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class RateLimitTest {

    @Test
    void should_haveDefaultValues_when_annotationDeclared() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("defaultMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.key()).isEqualTo("test.default");
        assertThat(annotation.rate()).isEqualTo(10);
        assertThat(annotation.burst()).isEqualTo(0);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.IP);
        assertThat(annotation.algorithm()).isEqualTo(RateLimitAlgorithm.TOKEN_BUCKET);
        assertThat(annotation.windowSize()).isEqualTo(1);
        assertThat(annotation.fallbackMethod()).isEmpty();
        assertThat(annotation.message()).isEqualTo("请求过于频繁，请稍后再试");
        assertThat(annotation.responseHeaders()).isTrue();
    }

    @Test
    void should_haveCustomValues_when_annotationConfigured() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("customMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.key()).isEqualTo("api.user");
        assertThat(annotation.rate()).isEqualTo(100);
        assertThat(annotation.burst()).isEqualTo(200);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.USER);
        assertThat(annotation.algorithm()).isEqualTo(RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(annotation.windowSize()).isEqualTo(60);
        assertThat(annotation.fallbackMethod()).isEqualTo("userFallback");
        assertThat(annotation.message()).isEqualTo("用户请求过于频繁");
    }

    @Test
    void should_targetMethod_when_annotationApplied() {
        assertThat(RateLimit.class.isAnnotationPresent(java.lang.annotation.Target.class))
                .isTrue();
        assertThat(RateLimit.class.getAnnotation(java.lang.annotation.Target.class).value())
                .containsExactly(java.lang.annotation.ElementType.METHOD);
    }

    @Test
    void should_retainRuntime_when_annotationApplied() {
        assertThat(RateLimit.class.isAnnotationPresent(java.lang.annotation.Retention.class))
                .isTrue();
        assertThat(RateLimit.class.getAnnotation(java.lang.annotation.Retention.class).value())
                .isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    @Test
    void should_beRepeatable_when_multipleAnnotations() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("multiLimitMethod");
        RateLimit[] annotations = method.getAnnotationsByType(RateLimit.class);

        assertThat(annotations).hasSize(2);
        assertThat(annotations[0].dimension()).isEqualTo(RateLimitDimension.IP);
        assertThat(annotations[1].dimension()).isEqualTo(RateLimitDimension.USER);
    }

    @Test
    void should_supportAllDimensions() throws NoSuchMethodException {
        assertThat(RateLimitDimension.values())
                .containsExactly(
                        RateLimitDimension.IP,
                        RateLimitDimension.USER,
                        RateLimitDimension.TENANT,
                        RateLimitDimension.API);
    }

    @Test
    void should_supportAllAlgorithms() {
        assertThat(RateLimitAlgorithm.values())
                .containsExactly(
                        RateLimitAlgorithm.TOKEN_BUCKET,
                        RateLimitAlgorithm.SLIDING_WINDOW);
    }

    // 带注解方法的测试类
    static class TestClass {

        @RateLimit(key = "test.default")
        public void defaultMethod() {}

        @RateLimit(
                key = "api.user",
                rate = 100,
                burst = 200,
                dimension = RateLimitDimension.USER,
                algorithm = RateLimitAlgorithm.SLIDING_WINDOW,
                windowSize = 60,
                fallbackMethod = "userFallback",
                message = "用户请求过于频繁")
        public void customMethod() {}

        @RateLimit(key = "api.search", rate = 100, dimension = RateLimitDimension.IP)
        @RateLimit(key = "api.search", rate = 50, dimension = RateLimitDimension.USER)
        public void multiLimitMethod() {}
    }
}
