package io.github.afgprojects.framework.core.model.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DeprecatedApi 注解测试
 */
@DisplayName("DeprecatedApi 注解测试")
class DeprecatedApiTest {

    @Nested
    @DisplayName("注解属性测试")
    class AnnotationAttributeTests {

        @Test
        @DisplayName("应该正确获取 since 属性")
        void shouldGetSince() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("1.5.0");
        }

        @Test
        @DisplayName("应该正确获取所有属性")
        void shouldGetAllAttributes() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("fullyDeprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("1.5.0");
            assertThat(deprecated.removedIn()).isEqualTo("2.0.0");
            assertThat(deprecated.replacement()).isEqualTo("use newMethod() instead");
            assertThat(deprecated.reason()).isEqualTo("性能问题");
        }

        @Test
        @DisplayName("removedIn 默认值应该是空字符串")
        void shouldHaveEmptyRemovedInDefault() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated.removedIn()).isEmpty();
        }

        @Test
        @DisplayName("replacement 默认值应该是空字符串")
        void shouldHaveEmptyReplacementDefault() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated.replacement()).isEmpty();
        }

        @Test
        @DisplayName("reason 默认值应该是空字符串")
        void shouldHaveEmptyReasonDefault() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated.reason()).isEmpty();
        }
    }

    @Nested
    @DisplayName("注解目标测试")
    class AnnotationTargetTests {

        @Test
        @DisplayName("应该可以标注在方法上")
        void shouldBeAllowedOnMethod() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
        }

        @Test
        @DisplayName("应该可以标注在类上")
        void shouldBeAllowedOnClass() {
            // when
            DeprecatedApi deprecated = DeprecatedTestClass.class.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("2.0.0");
            assertThat(deprecated.removedIn()).isEqualTo("3.0.0");
        }

        @Test
        @DisplayName("应该可以标注在字段上")
        void shouldBeAllowedOnField() throws NoSuchFieldException {
            // when
            DeprecatedApi deprecated =
                    TestClass.class.getField("DEPRECATED_FIELD").getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("1.6.0");
        }

        @Test
        @DisplayName("应该可以标注在构造函数上")
        void shouldBeAllowedOnConstructor() throws NoSuchMethodException {
            // when
            DeprecatedApi deprecated =
                    TestClass.class.getConstructor(String.class).getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("1.7.0");
        }
    }

    @Nested
    @DisplayName("注解保留策略测试")
    class RetentionPolicyTests {

        @Test
        @DisplayName("应该在运行时可用")
        void shouldBeAvailableAtRuntime() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("deprecatedMethod");

            // when
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(deprecated).isNotNull();
        }
    }

    @Nested
    @DisplayName("与 @Since 注解组合使用测试")
    class CombinedWithSinceTests {

        @Test
        @DisplayName("应该可以同时标注 @Since 和 @DeprecatedApi")
        void shouldCombineWithSince() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithBothAnnotations");

            // when
            Since since = method.getAnnotation(Since.class);
            DeprecatedApi deprecated = method.getAnnotation(DeprecatedApi.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("1.0.0");
            assertThat(deprecated).isNotNull();
            assertThat(deprecated.since()).isEqualTo("1.8.0");
        }
    }

    // 测试用的类和方法
    static class TestClass {

        @DeprecatedApi(since = "1.6.0", removedIn = "2.0.0", reason = "不再使用")
        public static final String DEPRECATED_FIELD = "old";

        @DeprecatedApi(since = "1.7.0")
        public TestClass(String name) {
            // 构造函数
        }

        @DeprecatedApi(since = "1.5.0")
        public void deprecatedMethod() {
            // 废弃方法
        }

        @DeprecatedApi(
                since = "1.5.0",
                removedIn = "2.0.0",
                replacement = "use newMethod() instead",
                reason = "性能问题")
        public void fullyDeprecatedMethod() {
            // 完整废弃信息的方法
        }

        @Since("1.0.0")
        @DeprecatedApi(since = "1.8.0", removedIn = "2.0.0", replacement = "use newMethod() instead")
        public void methodWithBothAnnotations() {
            // 同时标注两个注解的方法
        }
    }

    @DeprecatedApi(since = "2.0.0", removedIn = "3.0.0", replacement = "use NewClass instead")
    static class DeprecatedTestClass {
        // 废弃的类
    }
}
