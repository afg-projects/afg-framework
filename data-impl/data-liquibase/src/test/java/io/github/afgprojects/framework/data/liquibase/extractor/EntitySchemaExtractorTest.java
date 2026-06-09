package io.github.afgprojects.framework.data.liquibase.extractor;

import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.schema.ColumnMetadata;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntitySchemaExtractor 单元测试
 * <p>
 * 验证 EntityMetadata 到 SchemaMetadata 的转换逻辑。
 */
class EntitySchemaExtractorTest {

    // ========== 基本转换 ==========

    @Nested
    @DisplayName("基本转换")
    class BasicConversion {

        @Test
        @DisplayName("should convert table name when entity metadata provided")
        void shouldConvertTableName_whenEntityMetadataProvided() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(), null, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getTableName()).isEqualTo("sys_user");
        }

        @Test
        @DisplayName("should convert fields to columns when entity has fields")
        void shouldConvertFieldsToColumns_whenEntityHasFields() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            FieldMetadata idField = new StubFieldMetadata("id", "id", Long.class, true, true);
            FieldMetadata nameField = new StubFieldMetadata("name", "name", String.class, false, false);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(idField, nameField), idField, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getColumns()).hasSize(2);
            assertThat(schema.getColumns().get(0).getColumnName()).isEqualTo("id");
            assertThat(schema.getColumns().get(1).getColumnName()).isEqualTo("name");
        }

        @Test
        @DisplayName("should set primary key when id field exists")
        void shouldSetPrimaryKey_whenIdFieldExists() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            FieldMetadata idField = new StubFieldMetadata("id", "id", Long.class, true, true);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(idField), idField, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getPrimaryKey().getConstraintName()).isEqualTo("pk_sys_user");
            assertThat(schema.getPrimaryKey().getColumnNames()).containsExactly("id");
        }

        @Test
        @DisplayName("should set id column as non-nullable when id field is primary key")
        void shouldSetIdColumnAsNonNullable_whenIdFieldIsPrimaryKey() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            FieldMetadata idField = new StubFieldMetadata("id", "id", Long.class, true, true);
            FieldMetadata nameField = new StubFieldMetadata("name", "name", String.class, false, false);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(idField, nameField), idField, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            ColumnMetadata idColumn = schema.getColumns().stream()
                    .filter(c -> c.getColumnName().equals("id"))
                    .findFirst().orElseThrow();
            assertThat(idColumn.isNullable()).isFalse();
            assertThat(idColumn.isPrimaryKey()).isTrue();
        }
    }

    // ========== 方言影响 ==========

    @Nested
    @DisplayName("方言影响")
    class DialectEffect {

        @Test
        @DisplayName("should use H2 dialect SQL types when H2 dialect provided")
        void shouldUseH2DialectSqlTypes_whenH2DialectProvided() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            FieldMetadata nameField = new StubFieldMetadata("name", "name", String.class, false, false);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(nameField), null, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getColumns()).isNotEmpty();
            assertThat(schema.getColumns().get(0).getDataType()).isNotBlank();
        }

        @Test
        @DisplayName("should use MySQL dialect SQL types when MySQL dialect provided")
        void shouldUseMySQLDialectSqlTypes_whenMySQLDialectProvided() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new MySQLDialect());

            FieldMetadata nameField = new StubFieldMetadata("name", "name", String.class, false, false);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(nameField), null, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getColumns()).isNotEmpty();
            assertThat(schema.getColumns().get(0).getDataType()).isNotBlank();
        }
    }

    // ========== 无主键 ==========

    @Nested
    @DisplayName("无主键")
    class NoPrimaryKey {

        @Test
        @DisplayName("should not set primary key when id field is null")
        void shouldNotSetPrimaryKey_whenIdFieldIsNull() {
            EntitySchemaExtractor extractor = new EntitySchemaExtractor(new H2Dialect());

            FieldMetadata nameField = new StubFieldMetadata("name", "name", String.class, false, false);
            EntityMetadata<TestEntity> metadata = new StubEntityMetadata<>(
                    TestEntity.class, "sys_user", List.of(nameField), null, null, null);

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getPrimaryKey()).isNull();
        }
    }

    // ========== Helper classes ==========

    private static class TestEntity {}

    private record StubEntityMetadata<T>(
            Class<T> entityClass, String tableName,
            List<? extends FieldMetadata> fields,
            FieldMetadata idField, FieldMetadata softDeleteField,
            FieldMetadata tenantField
    ) implements EntityMetadata<T> {
        @Override public Class<T> getEntityClass() { return entityClass; }
        @Override public String getTableName() { return tableName; }
        @Override public List<? extends FieldMetadata> getFields() { return fields; }
        @Override public FieldMetadata getField(String fieldName) {
            return fields.stream().filter(f -> f.getPropertyName().equals(fieldName)).findFirst().orElse(null);
        }
        @Override public FieldMetadata getIdField() { return idField; }
        @Override public String getIdFieldName() { return "id"; }
        @Override public FieldMetadata getSoftDeleteField() { return softDeleteField; }
        @Override public FieldMetadata getTenantField() { return tenantField; }
        @Override public Map<String, String> getColumnToFieldMap() { return Map.of(); }
        @Override public Map<String, String> getFieldToColumnMap() { return Map.of(); }
        @Override public boolean hasTrait(io.github.afgprojects.framework.data.core.metadata.EntityTrait trait) { return false; }
        @Override public Set<io.github.afgprojects.framework.data.core.metadata.EntityTrait> getTraits() { return Set.of(); }
        @Override public Condition getDefaultCondition() { return Condition.empty(); }
        @Override public boolean hasRelation(String fieldName) { return false; }
        @Override public List<RelationMetadata> getRelations() { return List.of(); }
    }

    private record StubFieldMetadata(
            String propertyName, String columnName, Class<?> fieldType,
            boolean id, boolean generated
    ) implements FieldMetadata {
        @Override public String getPropertyName() { return propertyName; }
        @Override public String getColumnName() { return columnName; }
        @Override public Class<?> getFieldType() { return fieldType; }
        @Override public boolean isId() { return id; }
        @Override public boolean isGenerated() { return generated; }
    }
}