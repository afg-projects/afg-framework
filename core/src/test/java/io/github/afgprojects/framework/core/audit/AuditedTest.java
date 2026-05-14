package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;

/**
 * Audited 注解测试。
 * <p>
 * 测试 {@link Audited} 注解的默认值、自定义值以及注解类型。
 *
 * @see Audited
 */
class AuditedTest {

    /**
     * 测试注解的默认值是否正确。
     */
    @Test
    void should_haveDefaultValues() throws NoSuchMethodException {
        // Given
        TestService service = new TestService();
        Audited annotation = service.getClass()
                .getMethod("defaultMethod")
                .getAnnotation(Audited.class);

        // Then
        assertThat(annotation.operation()).isEmpty();
        assertThat(annotation.module()).isEmpty();
        assertThat(annotation.sensitiveFields()).isEmpty();
        assertThat(annotation.recordArgs()).isTrue();
        assertThat(annotation.recordResult()).isTrue();
        assertThat(annotation.target()).isEmpty();
    }

    /**
     * 测试注解是否能够使用自定义值。
     */
    @Test
    void should_useCustomValues() throws NoSuchMethodException {
        // Given
        TestService service = new TestService();
        Audited annotation = service.getClass()
                .getMethod("customMethod", String.class)
                .getAnnotation(Audited.class);

        // Then
        assertThat(annotation.operation()).isEqualTo("创建用户");
        assertThat(annotation.module()).isEqualTo("用户管理");
        assertThat(annotation.sensitiveFields()).containsExactly("password", "token");
        assertThat(annotation.recordArgs()).isFalse();
        assertThat(annotation.recordResult()).isFalse();
        assertThat(annotation.target()).isEqualTo("#userId");
    }

    /**
     * 测试注解的 annotationType 方法是否返回正确的类型。
     */
    @Test
    void should_returnCorrectAnnotationType() throws NoSuchMethodException {
        // Given
        TestService service = new TestService();
        Audited annotation = service.getClass()
                .getMethod("defaultMethod")
                .getAnnotation(Audited.class);

        // Then
        assertThat(annotation.annotationType()).isEqualTo(Audited.class);
    }

    /**
     * 测试服务类
     */
    static class TestService {

        @Audited
        public void defaultMethod() {}

        @Audited(
                operation = "创建用户",
                module = "用户管理",
                sensitiveFields = {"password", "token"},
                recordArgs = false,
                recordResult = false,
                target = "#userId"
        )
        public void customMethod(String userId) {}
    }
}
