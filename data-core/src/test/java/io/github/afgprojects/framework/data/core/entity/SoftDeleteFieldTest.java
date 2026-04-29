package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeleteField 注解测试
 */
@DisplayName("SoftDeleteField 注解测试")
class SoftDeleteFieldTest {

    @Nested
    @DisplayName("注解属性默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认字段名应该是 deleted")
        void defaultFieldNameShouldBeDeleted() throws NoSuchFieldException {
            // Given
            Field field = AnnotatedEntity.class.getDeclaredField("status");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.value()).isEqualTo("deleted");
        }

        @Test
        @DisplayName("默认已删除值应该是 1")
        void defaultDeletedValueShouldBeOne() throws NoSuchFieldException {
            // Given
            Field field = AnnotatedEntity.class.getDeclaredField("status");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.deletedValue()).isEqualTo("1");
        }

        @Test
        @DisplayName("默认未删除值应该是 0")
        void defaultNotDeletedValueShouldBeZero() throws NoSuchFieldException {
            // Given
            Field field = AnnotatedEntity.class.getDeclaredField("status");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.notDeletedValue()).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("注解属性自定义值测试")
    class CustomValueTests {

        @Test
        @DisplayName("应该支持自定义字段名")
        void shouldSupportCustomFieldName() throws NoSuchFieldException {
            // Given
            Field field = CustomAnnotatedEntity.class.getDeclaredField("isRemoved");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.value()).isEqualTo("is_removed");
        }

        @Test
        @DisplayName("应该支持自定义已删除值")
        void shouldSupportCustomDeletedValue() throws NoSuchFieldException {
            // Given
            Field field = CustomAnnotatedEntity.class.getDeclaredField("isRemoved");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.deletedValue()).isEqualTo("Y");
        }

        @Test
        @DisplayName("应该支持自定义未删除值")
        void shouldSupportCustomNotDeletedValue() throws NoSuchFieldException {
            // Given
            Field field = CustomAnnotatedEntity.class.getDeclaredField("isRemoved");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.notDeletedValue()).isEqualTo("N");
        }
    }

    @Nested
    @DisplayName("注解元信息测试")
    class AnnotationMetaTests {

        @Test
        @DisplayName("注解应该标注在字段上")
        void annotationShouldTargetField() {
            // When
            java.lang.annotation.Target annotation = SoftDeleteField.class.getAnnotation(java.lang.annotation.Target.class);

            // Then
            assertThat(annotation).isNotNull();
            assertThat(java.lang.annotation.ElementType.FIELD).isIn(annotation.value());
        }

        @Test
        @DisplayName("注解应该在运行时保留")
        void annotationShouldBeRetainedAtRuntime() {
            // When
            java.lang.annotation.Retention annotation = SoftDeleteField.class.getAnnotation(java.lang.annotation.Retention.class);

            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
        }
    }

    @Nested
    @DisplayName("反射操作测试")
    class ReflectionTests {

        @Test
        @DisplayName("应该能通过反射获取注解")
        void shouldGetAnnotationViaReflection() throws NoSuchFieldException {
            // Given
            Field field = AnnotatedEntity.class.getDeclaredField("status");

            // When
            Annotation annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation).isNotNull();
            assertThat(annotation).isInstanceOf(SoftDeleteField.class);
        }

        @Test
        @DisplayName("没有注解的字段应该返回 null")
        void fieldWithoutAnnotationShouldReturnNull() throws NoSuchFieldException {
            // Given
            Field field = AnnotatedEntity.class.getDeclaredField("name");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation).isNull();
        }

        @Test
        @DisplayName("应该能获取所有带注解的字段")
        void shouldGetAllAnnotatedFields() {
            // When
            long annotatedCount = java.util.Arrays.stream(CustomAnnotatedEntity.class.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(SoftDeleteField.class))
                    .count();

            // Then
            assertThat(annotatedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("注解值可以是任意字符串")
        void annotationValueCanBeAnyString() throws NoSuchFieldException {
            // Given
            Field field = SpecialValueEntity.class.getDeclaredField("flag");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.deletedValue()).isEqualTo("DELETED");
            assertThat(annotation.notDeletedValue()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("注解值可以包含特殊字符")
        void annotationValueCanContainSpecialChars() throws NoSuchFieldException {
            // Given
            Field field = SpecialCharEntity.class.getDeclaredField("status");

            // When
            SoftDeleteField annotation = field.getAnnotation(SoftDeleteField.class);

            // Then
            assertThat(annotation.deletedValue()).isEqualTo("1");
            assertThat(annotation.notDeletedValue()).isEqualTo("0");
        }
    }

    /**
     * 默认注解测试实体
     */
    static class AnnotatedEntity {
        @SoftDeleteField
        private int status;

        private String name;
    }

    /**
     * 自定义注解值测试实体
     */
    static class CustomAnnotatedEntity {
        @SoftDeleteField(value = "is_removed", deletedValue = "Y", notDeletedValue = "N")
        private boolean isRemoved;

        private String data;
    }

    /**
     * 特殊值测试实体
     */
    static class SpecialValueEntity {
        @SoftDeleteField(deletedValue = "DELETED", notDeletedValue = "ACTIVE")
        private String flag;
    }

    /**
     * 特殊字符测试实体
     */
    static class SpecialCharEntity {
        @SoftDeleteField
        private int status;
    }
}
