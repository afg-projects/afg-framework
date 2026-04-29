package io.github.afgprojects.framework.data.jdbc.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * CachedFieldAccessor 覆盖率补充测试
 */
@DisplayName("CachedFieldAccessor 覆盖率补充测试")
class CachedFieldAccessorCoverageTest {

    static class TestEntity {
        private Long id;
        private String name;
        private Integer age;

        public TestEntity() {}

        public TestEntity(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    private CachedFieldAccessor idAccessor;
    private CachedFieldAccessor nameAccessor;

    @BeforeEach
    void setUp() throws Exception {
        idAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("id"));
        nameAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("name"));
    }

    @Nested
    @DisplayName("getField 测试")
    class GetFieldTests {

        @Test
        @DisplayName("应该返回底层 Field 对象")
        void shouldReturnUnderlyingField() throws Exception {
            // When
            Field field = idAccessor.getField();

            // Then
            assertThat(field).isNotNull();
            assertThat(field.getName()).isEqualTo("id");
            assertThat(field.getType()).isEqualTo(Long.class);
        }

        @Test
        @DisplayName("Field 对象应可访问")
        void shouldBeAccessible() throws Exception {
            // When
            Field field = idAccessor.getField();

            // Then
            assertThat(field.canAccess(new TestEntity())).isTrue();
        }
    }

    @Nested
    @DisplayName("setValue 类型不匹配测试")
    class SetValueTypeMismatchTests {

        @Test
        @DisplayName("设置 Long 字段为 String 应抛出 IllegalArgumentException")
        void shouldThrowOnTypeMismatch() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThatThrownBy(() -> idAccessor.setValue(entity, "not a long"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set field 'id'")
                .hasMessageContaining("Long")
                .hasMessageContaining("String");
        }

        @Test
        @DisplayName("设置 Integer 字段为 String 应抛出 IllegalArgumentException")
        void shouldThrowOnAgeTypeMismatch() throws Exception {
            // Given
            CachedFieldAccessor ageAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("age"));
            TestEntity entity = new TestEntity();

            // When & Then
            assertThatThrownBy(() -> ageAccessor.setValue(entity, "not an integer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set field 'age'");
        }
    }

    @Nested
    @DisplayName("null 值处理测试")
    class NullValueTests {

        @Test
        @DisplayName("获取未初始化字段应返回 null")
        void shouldReturnNullForUninitializedField() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            Object value = nameAccessor.getValue(entity);

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("设置 null 值应成功")
        void shouldSetNullValue() {
            // Given
            TestEntity entity = new TestEntity(1L, "test", 25);

            // When
            nameAccessor.setValue(entity, null);

            // Then
            assertThat(nameAccessor.getValue(entity)).isNull();
        }

        @Test
        @DisplayName("设置 Long 字段为 null 应成功")
        void shouldSetNullOnLongField() {
            // Given
            TestEntity entity = new TestEntity(1L, "test", 25);

            // When
            idAccessor.setValue(entity, null);

            // Then
            assertThat(idAccessor.getValue(entity)).isNull();
        }
    }

    @Nested
    @DisplayName("getValue 异常测试")
    class GetValueExceptionTests {

        @Test
        @DisplayName("getValue 对 null 实体应抛出 NullPointerException")
        void shouldThrowOnNullEntity() {
            // When & Then
            assertThatThrownBy(() -> idAccessor.getValue(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("setValue 异常测试")
    class SetValueExceptionTests {

        @Test
        @DisplayName("setValue 对 null 实体应抛出 NullPointerException")
        void shouldThrowOnNullEntity() {
            // When & Then
            assertThatThrownBy(() -> idAccessor.setValue(null, 1L))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("IllegalAccessException 分支测试")
    class IllegalAccessTests {

        @Test
        @DisplayName("getValue 抛出 IllegalAccessException 应包装为 RuntimeException")
        void shouldWrapIllegalAccessExceptionInGetValue() throws Exception {
            // Given - 创建一个 mock Field，即使调用了 setAccessible 后仍抛出 IllegalAccessException
            Field mockField = Mockito.mock(Field.class);
            Mockito.lenient().when(mockField.getType()).thenAnswer(invocation -> Long.class);
            Mockito.lenient().when(mockField.getName()).thenReturn("mockField");
            when(mockField.get(Mockito.any())).thenThrow(new IllegalAccessException("Access denied"));

            CachedFieldAccessor accessor = new CachedFieldAccessor(mockField);
            TestEntity entity = new TestEntity();

            // When & Then
            assertThatThrownBy(() -> accessor.getValue(entity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get field value")
                .hasMessageContaining("mockField")
                .hasCauseInstanceOf(IllegalAccessException.class);
        }

        @Test
        @DisplayName("setValue 抛出 IllegalAccessException 应包装为 RuntimeException")
        void shouldWrapIllegalAccessExceptionInSetValue() throws Exception {
            // Given - 创建一个 mock Field，即使调用了 setAccessible 后仍抛出 IllegalAccessException
            Field mockField = Mockito.mock(Field.class);
            Mockito.lenient().when(mockField.getType()).thenAnswer(invocation -> Long.class);
            Mockito.lenient().when(mockField.getName()).thenReturn("mockField");
            doThrow(new IllegalAccessException("Access denied")).when(mockField).set(Mockito.any(), Mockito.any());

            CachedFieldAccessor accessor = new CachedFieldAccessor(mockField);
            TestEntity entity = new TestEntity();

            // When & Then
            assertThatThrownBy(() -> accessor.setValue(entity, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to set field value")
                .hasMessageContaining("mockField")
                .hasCauseInstanceOf(IllegalAccessException.class);
        }
    }

    @Nested
    @DisplayName("setValue IllegalArgumentException null 分支测试")
    class SetValueIllegalArgumentExceptionNullBranchTests {

        @Test
        @DisplayName("设置 primitive 字段为 null 应抛出 IllegalArgumentException 并处理 null 值")
        void shouldHandleNullValueInIllegalArgumentException() throws Exception {
            // Given - 创建一个有原始类型字段的测试类
            class PrimitiveEntity {
                @SuppressWarnings("unused")
                private int primitiveInt;
            }

            Field primitiveField = PrimitiveEntity.class.getDeclaredField("primitiveInt");
            CachedFieldAccessor accessor = new CachedFieldAccessor(primitiveField);
            PrimitiveEntity entity = new PrimitiveEntity();

            // When & Then - 设置 null 到原始类型字段会抛出 IllegalArgumentException
            assertThatThrownBy(() -> accessor.setValue(entity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set field 'primitiveInt'")
                .hasMessageContaining("int")
                .hasMessageContaining("null");  // 这覆盖了 value == null 分支
        }
    }
}
