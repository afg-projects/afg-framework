package io.github.afgprojects.framework.data.jdbc.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * CachedFieldAccessor 单元测试
 */
class CachedFieldAccessorTest {

    // 测试实体
    static class TestEntity {
        private Long id;
        private String name;
        private Integer age;

        public TestEntity() {
        }

        public TestEntity(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    private CachedFieldAccessor idAccessor;
    private CachedFieldAccessor nameAccessor;
    private CachedFieldAccessor ageAccessor;

    @BeforeEach
    void setUp() throws Exception {
        idAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("id"));
        nameAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("name"));
        ageAccessor = new CachedFieldAccessor(TestEntity.class.getDeclaredField("age"));
    }

    @Test
    @DisplayName("应该正确获取字段值")
    void shouldGetFieldValue() {
        // Given
        TestEntity entity = new TestEntity(1L, "test", 25);

        // When & Then
        assertThat(idAccessor.getValue(entity)).isEqualTo(1L);
        assertThat(nameAccessor.getValue(entity)).isEqualTo("test");
        assertThat(ageAccessor.getValue(entity)).isEqualTo(25);
    }

    @Test
    @DisplayName("应该正确设置字段值")
    void shouldSetFieldValue() {
        // Given
        TestEntity entity = new TestEntity();

        // When
        idAccessor.setValue(entity, 2L);
        nameAccessor.setValue(entity, "updated");
        ageAccessor.setValue(entity, 30);

        // Then
        assertThat(idAccessor.getValue(entity)).isEqualTo(2L);
        assertThat(nameAccessor.getValue(entity)).isEqualTo("updated");
        assertThat(ageAccessor.getValue(entity)).isEqualTo(30);
    }

    @Test
    @DisplayName("应该正确处理 null 值")
    void shouldHandleNullValue() {
        // Given
        TestEntity entity = new TestEntity(1L, "test", 25);

        // When
        nameAccessor.setValue(entity, null);

        // Then
        assertThat(nameAccessor.getValue(entity)).isNull();
    }

    @Test
    @DisplayName("应该返回正确的字段类型")
    void shouldReturnCorrectFieldType() {
        assertThat(idAccessor.getFieldType()).isEqualTo(Long.class);
        assertThat(nameAccessor.getFieldType()).isEqualTo(String.class);
        assertThat(ageAccessor.getFieldType()).isEqualTo(Integer.class);
    }

    @Test
    @DisplayName("应该返回正确的字段名")
    void shouldReturnCorrectFieldName() {
        assertThat(idAccessor.getFieldName()).isEqualTo("id");
        assertThat(nameAccessor.getFieldName()).isEqualTo("name");
        assertThat(ageAccessor.getFieldName()).isEqualTo("age");
    }

    @Test
    @DisplayName("类型不匹配时应该抛出异常")
    void shouldThrowOnTypeMismatch() {
        // Given
        TestEntity entity = new TestEntity();

        // When & Then
        assertThatCode(() -> idAccessor.setValue(entity, "not a long"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set field 'id'");
    }
}
