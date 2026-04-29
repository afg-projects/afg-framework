package io.github.afgprojects.framework.core.model.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Since 注解测试
 */
@DisplayName("Since 注解测试")
class SinceTest {

    @Nested
    @DisplayName("注解属性测试")
    class AnnotationAttributeTests {

        @Test
        @DisplayName("应该正确获取 value 属性")
        void shouldGetValue() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithSince");

            // when
            Since since = method.getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("应该正确获取 note 属性")
        void shouldGetNote() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithSinceAndNote");

            // when
            Since since = method.getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("1.2.0");
            assertThat(since.note()).isEqualTo("支持批量操作");
        }

        @Test
        @DisplayName("note 默认值应该是空字符串")
        void shouldHaveEmptyNoteDefault() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithSince");

            // when
            Since since = method.getAnnotation(Since.class);

            // then
            assertThat(since.note()).isEmpty();
        }
    }

    @Nested
    @DisplayName("注解目标测试")
    class AnnotationTargetTests {

        @Test
        @DisplayName("应该可以标注在方法上")
        void shouldBeAllowedOnMethod() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithSince");

            // when
            Since since = method.getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
        }

        @Test
        @DisplayName("应该可以标注在类上")
        void shouldBeAllowedOnClass() {
            // when
            Since since = TestClassWithSince.class.getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("2.0.0");
        }

        @Test
        @DisplayName("应该可以标注在字段上")
        void shouldBeAllowedOnField() throws NoSuchFieldException {
            // when
            Since since = TestClass.class.getField("FIELD_WITH_SINCE").getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("1.5.0");
        }

        @Test
        @DisplayName("应该可以标注在构造函数上")
        void shouldBeAllowedOnConstructor() throws NoSuchMethodException {
            // when
            Since since = TestClass.class.getConstructor(String.class).getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
            assertThat(since.value()).isEqualTo("1.3.0");
        }
    }

    @Nested
    @DisplayName("注解保留策略测试")
    class RetentionPolicyTests {

        @Test
        @DisplayName("应该在运行时可用")
        void shouldBeAvailableAtRuntime() throws NoSuchMethodException {
            // given
            Method method = TestClass.class.getMethod("methodWithSince");

            // when
            Since since = method.getAnnotation(Since.class);

            // then
            assertThat(since).isNotNull();
        }
    }

    // 测试用的类和方法
    static class TestClass {

        @Since("1.5.0")
        public static final String FIELD_WITH_SINCE = "constant";

        @Since("1.3.0")
        public TestClass(String name) {
            // 构造函数
        }

        @Since("1.0.0")
        public void methodWithSince() {
            // 测试方法
        }

        @Since(value = "1.2.0", note = "支持批量操作")
        public void methodWithSinceAndNote() {
            // 测试方法
        }
    }

    @Since("2.0.0")
    static class TestClassWithSince {
        // 测试类
    }
}
