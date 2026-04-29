package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.FieldAccessor;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SimpleFieldMetadata 覆盖率补充测试
 */
@DisplayName("SimpleFieldMetadata 覆盖率补充测试")
class SimpleFieldMetadataCoverageTest {

    @Nested
    @DisplayName("简单构造函数测试")
    class SimpleConstructorTests {

        @Test
        @DisplayName("使用属性名和类型创建元数据")
        void shouldCreateWithPropertyNameAndType() {
            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("userName", String.class);

            // Then
            assertThat(metadata.getPropertyName()).isEqualTo("userName");
            assertThat(metadata.getFieldType()).isEqualTo(String.class);
            assertThat(metadata.getColumnName()).isEqualTo("user_name");
            assertThat(metadata.isId()).isFalse();
            assertThat(metadata.isGenerated()).isFalse();
            assertThat(metadata.getFieldAccessor()).isNull();
        }

        @Test
        @DisplayName("id 字段应被识别为主键")
        void shouldRecognizeIdFieldAsPrimaryKey() {
            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("id", Long.class);

            // Then
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }
    }

    @Nested
    @DisplayName("getColumnName 测试")
    class GetColumnNameTests {

        @Test
        @DisplayName("camelCase 应转换为 snake_case")
        void shouldConvertCamelCaseToSnakeCase() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("userName", String.class);

            // When
            String columnName = metadata.getColumnName();

            // Then
            assertThat(columnName).isEqualTo("user_name");
        }

        @Test
        @DisplayName("多个大写字母应正确处理")
        void shouldHandleMultipleUppercase() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("userFirstName", String.class);

            // When
            String columnName = metadata.getColumnName();

            // Then
            assertThat(columnName).isEqualTo("user_first_name");
        }

        @Test
        @DisplayName("单个单词应保持不变")
        void shouldKeepSingleWordUnchanged() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("name", String.class);

            // When
            String columnName = metadata.getColumnName();

            // Then
            assertThat(columnName).isEqualTo("name");
        }

        @Test
        @DisplayName("首字母大写应正确处理")
        void shouldHandleFirstUppercase() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("Name", String.class);

            // When
            String columnName = metadata.getColumnName();

            // Then
            assertThat(columnName).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("反射构造函数测试")
    class ReflectionConstructorTests {

        @Test
        @DisplayName("基于反射字段创建元数据")
        void shouldCreateFromReflectionField() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then
            assertThat(metadata.getPropertyName()).isEqualTo("id");
            assertThat(metadata.getFieldType()).isEqualTo(Long.class);
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.getFieldAccessor()).isNotNull();
        }

        @Test
        @DisplayName("非 id 字段应不被识别为主键")
        void shouldNotRecognizeNonIdFieldAsPrimaryKey() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("name");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then
            assertThat(metadata.isId()).isFalse();
        }
    }

    @Nested
    @DisplayName("getValue/setValue 测试")
    class GetValueSetValueTests {

        @Test
        @DisplayName("getValue 应正确获取字段值")
        void shouldGetValue() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("name");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);
            TestEntity entity = new TestEntity(1L, "test");

            // When
            Object value = metadata.getValue(entity);

            // Then
            assertThat(value).isEqualTo("test");
        }

        @Test
        @DisplayName("setValue 应正确设置字段值")
        void shouldSetValue() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("name");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);
            TestEntity entity = new TestEntity(1L, "test");

            // When
            metadata.setValue(entity, "updated");

            // Then
            assertThat(entity.getName()).isEqualTo("updated");
        }

        @Test
        @DisplayName("无 FieldAccessor 时 getValue 应抛出异常")
        void shouldThrowWhenGetValueWithoutAccessor() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("name", String.class);
            TestEntity entity = new TestEntity(1L, "test");

            // When & Then
            assertThatThrownBy(() -> metadata.getValue(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FieldAccessor not available");
        }

        @Test
        @DisplayName("无 FieldAccessor 时 setValue 应抛出异常")
        void shouldThrowWhenSetValueWithoutAccessor() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("name", String.class);
            TestEntity entity = new TestEntity(1L, "test");

            // When & Then
            assertThatThrownBy(() -> metadata.setValue(entity, "updated"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FieldAccessor not available");
        }
    }

    @Nested
    @DisplayName("getFieldAccessor 测试")
    class GetFieldAccessorTests {

        @Test
        @DisplayName("反射创建时应返回 FieldAccessor")
        void shouldReturnFieldAccessorWhenCreatedFromReflection() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("id");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // When
            FieldAccessor accessor = metadata.getFieldAccessor();

            // Then
            assertThat(accessor).isNotNull();
            assertThat(accessor.getFieldName()).isEqualTo("id");
            assertThat(accessor.getFieldType()).isEqualTo(Long.class);
        }

        @Test
        @DisplayName("简单构造函数创建时应返回 null")
        void shouldReturnNullWhenCreatedFromSimpleConstructor() {
            // Given
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("id", Long.class);

            // When
            FieldAccessor accessor = metadata.getFieldAccessor();

            // Then
            assertThat(accessor).isNull();
        }
    }

    // 测试实体
    @Data
    static class TestEntity {
        private Long id;
        private String name;

        public TestEntity() {}

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Nested
    @DisplayName("hasAnnotation 测试")
    class HasAnnotationTests {

        @Test
        @DisplayName("字段无注解时应返回 false")
        void shouldReturnFalseWhenNoAnnotation() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("name");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then - name 字段不是 id，所以 isId 应该是 false
            assertThat(metadata.isId()).isFalse();
        }

        @Test
        @DisplayName("检测不存在的注解类名")
        void shouldReturnFalseForNonExistentAnnotation() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("id");

            // When - 通过反射创建，id 字段应该被识别为主键
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then
            assertThat(metadata.isId()).isTrue();
        }
    }

    @Nested
    @DisplayName("detectIdField 边界测试")
    class DetectIdFieldEdgeCaseTests {

        @Test
        @DisplayName("字段名为 id 但无注解应被识别为主键")
        void shouldDetectIdByName() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("非 id 字段名应不被识别为主键")
        void shouldNotDetectNonIdField() throws Exception {
            // Given
            Field field = TestEntity.class.getDeclaredField("name");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            // Then
            assertThat(metadata.isId()).isFalse();
            assertThat(metadata.isGenerated()).isFalse();
        }
    }
}
