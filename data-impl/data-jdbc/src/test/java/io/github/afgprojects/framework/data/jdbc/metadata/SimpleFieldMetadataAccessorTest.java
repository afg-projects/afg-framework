package io.github.afgprojects.framework.data.jdbc.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleFieldMetadata 字段访问器测试
 */
class SimpleFieldMetadataAccessorTest {

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

    private SimpleEntityMetadata<TestEntity> metadata;

    @BeforeEach
    void setUp() {
        metadata = new SimpleEntityMetadata<>(TestEntity.class);
    }

    @Test
    @DisplayName("应该正确创建带有字段访问器的元数据")
    void shouldCreateMetadataWithFieldAccessors() {
        // When
        var fields = metadata.getFields();

        // Then
        assertThat(fields).hasSize(3);
        for (var field : fields) {
            assertThat(field).isInstanceOf(SimpleFieldMetadata.class);
            SimpleFieldMetadata sf = (SimpleFieldMetadata) field;
            assertThat(sf.getFieldAccessor()).isNotNull();
        }
    }

    @Test
    @DisplayName("应该通过 SimpleFieldMetadata 获取字段值")
    void shouldGetFieldValueViaSimpleFieldMetadata() {
        // Given
        TestEntity entity = new TestEntity(1L, "test", 25);

        // When
        SimpleFieldMetadata idField = findField("id");
        SimpleFieldMetadata nameField = findField("name");
        SimpleFieldMetadata ageField = findField("age");

        // Then
        assertThat(idField.getValue(entity)).isEqualTo(1L);
        assertThat(nameField.getValue(entity)).isEqualTo("test");
        assertThat(ageField.getValue(entity)).isEqualTo(25);
    }

    @Test
    @DisplayName("应该通过 SimpleFieldMetadata 设置字段值")
    void shouldSetFieldValueViaSimpleFieldMetadata() {
        // Given
        TestEntity entity = new TestEntity();

        // When
        SimpleFieldMetadata idField = findField("id");
        SimpleFieldMetadata nameField = findField("name");
        SimpleFieldMetadata ageField = findField("age");

        idField.setValue(entity, 2L);
        nameField.setValue(entity, "updated");
        ageField.setValue(entity, 30);

        // Then
        assertThat(idField.getValue(entity)).isEqualTo(2L);
        assertThat(nameField.getValue(entity)).isEqualTo("updated");
        assertThat(ageField.getValue(entity)).isEqualTo(30);
    }

    @Test
    @DisplayName("应该正确识别主键字段")
    void shouldIdentifyIdField() {
        // When
        var idField = metadata.getIdField();

        // Then
        assertThat(idField).isNotNull();
        assertThat(idField.getPropertyName()).isEqualTo("id");
        assertThat(idField.isId()).isTrue();
    }

    private SimpleFieldMetadata findField(String name) {
        for (var field : metadata.getFields()) {
            if (field.getPropertyName().equals(name)) {
                return (SimpleFieldMetadata) field;
            }
        }
        throw new AssertionError("Field not found: " + name);
    }
}
