package io.github.afgprojects.framework.data.core.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntityMetadata Tests
 */
@DisplayName("EntityMetadata Tests")
class EntityMetadataTest {

    @Test
    @DisplayName("should get entity class")
    void shouldGetEntityClass() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        assertThat(metadata.getEntityClass()).isEqualTo(TestEntity.class);
    }

    @Test
    @DisplayName("should get all fields")
    void shouldGetFields() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        assertThat(metadata.getFields()).hasSize(2);
    }

    @Test
    @DisplayName("should get field by property name")
    void shouldGetFieldByName() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        FieldMetadata field = metadata.getField("id");
        assertThat(field).isNotNull();
        assertThat(field.getPropertyName()).isEqualTo("id");
    }

    @Test
    @DisplayName("should return null for non-existent field")
    void shouldReturnNullForNonExistentField() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        FieldMetadata field = metadata.getField("nonExistent");
        assertThat(field).isNull();
    }

    @Test
    @DisplayName("should get id field")
    void shouldGetIdField() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        FieldMetadata idField = metadata.getIdField();
        assertThat(idField).isNotNull();
        assertThat(idField.isId()).isTrue();
    }

    @Test
    @DisplayName("should get table name")
    void shouldGetTableName() {
        EntityMetadata<TestEntity> metadata = new TestEntityMetadata();
        assertThat(metadata.getTableName()).isEqualTo("test_entity");
    }

    // Test entity class
    static class TestEntity {
        private Long id;
        private String name;
    }

    // Test metadata implementation
    static class TestEntityMetadata implements EntityMetadata<TestEntity> {

        private static final List<FieldMetadata> FIELDS = List.of(
            new TestFieldMetadata("id", Long.class, true, true),
            new TestFieldMetadata("name", String.class, false, false)
        );

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public String getTableName() {
            return "test_entity";
        }

        @Override
        public FieldMetadata getIdField() {
            return FIELDS.stream()
                .filter(FieldMetadata::isId)
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<FieldMetadata> getFields() {
            return FIELDS;
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return FIELDS.stream()
                .filter(f -> f.getPropertyName().equals(propertyName))
                .findFirst()
                .orElse(null);
        }

        @Override
        public boolean isSoftDeletable() {
            return false;
        }

        @Override
        public boolean isTenantAware() {
            return false;
        }

        @Override
        public boolean isAuditable() {
            return false;
        }

        @Override
        public boolean isVersioned() {
            return false;
        }

        @Override
        public List<io.github.afgprojects.framework.data.core.relation.RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public java.util.Optional<io.github.afgprojects.framework.data.core.relation.RelationMetadata> getRelation(String fieldName) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }

    // Test field metadata implementation
    record TestFieldMetadata(
        String propertyName,
        Class<?> fieldType,
        boolean id,
        boolean generated
    ) implements FieldMetadata {

        @Override
        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public String getColumnName() {
            return toSnakeCase(propertyName);
        }

        @Override
        public Class<?> getFieldType() {
            return fieldType;
        }

        @Override
        public boolean isId() {
            return id;
        }

        @Override
        public boolean isGenerated() {
            return generated;
        }

        private String toSnakeCase(String name) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (i > 0 && Character.isUpperCase(c)) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            }
            return result.toString();
        }
    }
}
