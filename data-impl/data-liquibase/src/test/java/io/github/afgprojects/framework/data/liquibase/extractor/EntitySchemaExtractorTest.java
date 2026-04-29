package io.github.afgprojects.framework.data.liquibase.extractor;

import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.CascadeType;
import io.github.afgprojects.framework.data.core.relation.FetchType;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.core.schema.ColumnMetadata;
import io.github.afgprojects.framework.data.core.schema.SchemaMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EntitySchemaExtractor 测试
 */
@DisplayName("EntitySchemaExtractor 测试")
class EntitySchemaExtractorTest {

    private EntitySchemaExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new EntitySchemaExtractor(new H2Dialect());
    }

    @Nested
    @DisplayName("convert 测试")
    class ConvertTests {

        @Test
        @DisplayName("应正确转换简单实体")
        void shouldConvertSimpleEntity() {
            EntityMetadata<TestEntity> metadata = new TestEntityMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema).isNotNull();
            assertThat(schema.getTableName()).isEqualTo("test_entity");
            assertThat(schema.getColumns()).hasSize(3);
            assertThat(schema.getPrimaryKey()).isNotNull();
        }

        @Test
        @DisplayName("应正确提取主键")
        void shouldExtractPrimaryKey() {
            EntityMetadata<TestEntity> metadata = new TestEntityMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getPrimaryKey()).isNotNull();
            assertThat(schema.getPrimaryKey().getColumnNames()).containsExactly("id");
        }

        @Test
        @DisplayName("应正确提取列信息")
        void shouldExtractColumns() {
            EntityMetadata<TestEntity> metadata = new TestEntityMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            List<ColumnMetadata> columns = schema.getColumns();
            assertThat(columns).hasSize(3);

            ColumnMetadata idColumn = schema.getColumn("id");
            assertThat(idColumn).isNotNull();
            assertThat(idColumn.isPrimaryKey()).isTrue();
            assertThat(idColumn.isNullable()).isFalse();
        }

        @Test
        @DisplayName("应正确处理带关联的实体")
        void shouldHandleEntityWithRelations() {
            EntityMetadata<TestEntityWithRelation> metadata = new TestEntityWithRelationMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getForeignKeys()).isNotEmpty();
        }

        @Test
        @DisplayName("ONE_TO_ONE 关联 - 拥有方应创建外键")
        void shouldCreateForeignKeyForOneToOneOwningSide() {
            EntityMetadata<TestEntityWithRelation> metadata = new TestEntityWithOneToOneOwningMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getForeignKeys()).hasSize(1);
            assertThat(schema.getForeignKeys().get(0).getConstraintName())
                    .isEqualTo("fk_test_entity_one_to_one_owning_profile");
            assertThat(schema.getForeignKeys().get(0).getColumnNames()).containsExactly("profile_id");
            assertThat(schema.getForeignKeys().get(0).getReferencedTableName()).isEqualTo("profile_entity");
            assertThat(schema.getForeignKeys().get(0).getReferencedColumnNames()).containsExactly("id");
        }

        @Test
        @DisplayName("ONE_TO_ONE 关联 - 非拥有方不应创建外键")
        void shouldNotCreateForeignKeyForOneToOneInverseSide() {
            EntityMetadata<TestEntityWithRelation> metadata = new TestEntityWithOneToOneInverseMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getForeignKeys()).isEmpty();
        }

        @Test
        @DisplayName("应正确处理 null idField 的情况")
        void shouldHandleNullIdField() {
            EntityMetadata<TestEntity> metadata = new TestEntityWithNullIdFieldMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema).isNotNull();
            assertThat(schema.getTableName()).isEqualTo("test_entity_null_id");
            assertThat(schema.getPrimaryKey()).isNull();
            assertThat(schema.getColumns()).hasSize(1);
        }

        @Test
        @DisplayName("应正确处理非主键但标记为 id 的字段 - nullable 应为 false")
        void shouldHandleFieldMarkedAsId() {
            EntityMetadata<TestEntity> metadata = new TestFieldMarkedAsIdMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema).isNotNull();
            // idField 是主键，非空
            ColumnMetadata idColumn = schema.getColumn("id");
            assertThat(idColumn.isPrimaryKey()).isTrue();
            assertThat(idColumn.isNullable()).isFalse();

            // anotherId 标记为 isId=true 但不是主键，应非空
            ColumnMetadata anotherIdColumn = schema.getColumn("another_id");
            assertThat(anotherIdColumn.isPrimaryKey()).isFalse();
            assertThat(anotherIdColumn.isNullable()).isFalse();
        }

        @Test
        @DisplayName("inferTableName 应正确转换各种类名")
        void shouldCorrectlyInferTableName() {
            // 通过 ONE_TO_ONE 关联测试 inferTableName 方法
            // ProfileEntity -> profile_entity
            EntityMetadata<TestEntityWithRelation> metadata = new TestEntityWithOneToOneOwningMetadata();
            SchemaMetadata schema = extractor.convert(metadata);

            assertThat(schema.getForeignKeys().get(0).getReferencedTableName()).isEqualTo("profile_entity");
        }

        @Test
        @DisplayName("应正确设置 autoIncrement 为 true 当字段是 generated")
        void shouldSetAutoIncrementForGeneratedField() {
            EntityMetadata<TestEntity> metadata = new TestEntityMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            ColumnMetadata idColumn = schema.getColumn("id");
            assertThat(idColumn.isAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应正确设置 autoIncrement 为 false 当字段不是 generated")
        void shouldNotSetAutoIncrementForNonGeneratedField() {
            EntityMetadata<TestEntity> metadata = new TestEntityMetadata();

            SchemaMetadata schema = extractor.convert(metadata);

            ColumnMetadata nameColumn = schema.getColumn("name");
            assertThat(nameColumn.isAutoIncrement()).isFalse();
        }
    }

    // 测试实体
    static class TestEntity {
        private Long id;
        private String name;
        private String email;
    }

    static class TestEntityWithRelation {
        private Long id;
        private Long relatedId;
    }

    static class RelatedEntity {
        private Long id;
    }

    // 测试用的 EntityMetadata 实现
    static class TestEntityMetadata implements EntityMetadata<TestEntity> {

        private final TestFieldMetadata idField = new TestFieldMetadata("id", "id", Long.class, true, true);

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
            return idField;
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(
                    idField,
                    new TestFieldMetadata("name", "name", String.class, false, false),
                    new TestFieldMetadata("email", "email", String.class, false, false)
            );
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }

    static class TestEntityWithRelationMetadata implements EntityMetadata<TestEntityWithRelation> {

        @Override
        public Class<TestEntityWithRelation> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public String getTableName() {
            return "test_entity_with_relation";
        }

        @Override
        public FieldMetadata getIdField() {
            return new TestFieldMetadata("id", "id", Long.class, true, true);
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(
                    getIdField(),
                    new TestFieldMetadata("relatedId", "related_id", Long.class, false, false)
            );
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of(new TestRelationMetadata());
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return getRelations().stream()
                    .filter(r -> r.getFieldName().equals(fieldName))
                    .findFirst();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return getRelations().stream()
                    .anyMatch(r -> r.getFieldName().equals(fieldName));
        }
    }

    static class TestFieldMetadata implements FieldMetadata {

        private final String propertyName;
        private final String columnName;
        private final Class<?> fieldType;
        private final boolean isId;
        private final boolean isGenerated;

        TestFieldMetadata(String propertyName, String columnName, Class<?> fieldType, boolean isId, boolean isGenerated) {
            this.propertyName = propertyName;
            this.columnName = columnName;
            this.fieldType = fieldType;
            this.isId = isId;
            this.isGenerated = isGenerated;
        }

        @Override
        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public String getColumnName() {
            return columnName;
        }

        @Override
        public Class<?> getFieldType() {
            return fieldType;
        }

        @Override
        public boolean isId() {
            return isId;
        }

        @Override
        public boolean isGenerated() {
            return isGenerated;
        }
    }

    static class TestRelationMetadata implements RelationMetadata {

        @Override
        public RelationType getRelationType() {
            return RelationType.MANY_TO_ONE;
        }

        @Override
        public Class<?> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public Class<?> getTargetEntityClass() {
            return RelatedEntity.class;
        }

        @Override
        public String getFieldName() {
            return "related";
        }

        @Override
        public String getMappedBy() {
            return null;
        }

        @Override
        public String getForeignKeyColumn() {
            return "related_id";
        }

        @Override
        public String getJoinTable() {
            return null;
        }

        @Override
        public String getJoinColumn() {
            return null;
        }

        @Override
        public String getInverseJoinColumn() {
            return null;
        }

        @Override
        public Set<CascadeType> getCascadeTypes() {
            return Set.of();
        }

        @Override
        public FetchType getFetchType() {
            return FetchType.LAZY;
        }

        @Override
        public boolean isOwningSide() {
            return true;
        }

        @Override
        public boolean isOrphanRemoval() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return true;
        }
    }

    // ONE_TO_ONE 关联测试 - 拥有方（isOwningSide = true）
    static class TestEntityWithOneToOneOwningMetadata implements EntityMetadata<TestEntityWithRelation> {

        @Override
        public Class<TestEntityWithRelation> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public String getTableName() {
            return "test_entity_one_to_one_owning";
        }

        @Override
        public FieldMetadata getIdField() {
            return new TestFieldMetadata("id", "id", Long.class, true, true);
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(
                    getIdField(),
                    new TestFieldMetadata("profileId", "profile_id", Long.class, false, false)
            );
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of(new TestOneToOneOwningRelationMetadata());
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return getRelations().stream()
                    .filter(r -> r.getFieldName().equals(fieldName))
                    .findFirst();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return getRelations().stream()
                    .anyMatch(r -> r.getFieldName().equals(fieldName));
        }
    }

    static class TestOneToOneOwningRelationMetadata implements RelationMetadata {

        @Override
        public RelationType getRelationType() {
            return RelationType.ONE_TO_ONE;
        }

        @Override
        public Class<?> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public Class<?> getTargetEntityClass() {
            return ProfileEntity.class;
        }

        @Override
        public String getFieldName() {
            return "profile";
        }

        @Override
        public String getMappedBy() {
            return null;
        }

        @Override
        public String getForeignKeyColumn() {
            return "profile_id";
        }

        @Override
        public String getJoinTable() {
            return null;
        }

        @Override
        public String getJoinColumn() {
            return null;
        }

        @Override
        public String getInverseJoinColumn() {
            return null;
        }

        @Override
        public Set<CascadeType> getCascadeTypes() {
            return Set.of();
        }

        @Override
        public FetchType getFetchType() {
            return FetchType.LAZY;
        }

        @Override
        public boolean isOwningSide() {
            return true;
        }

        @Override
        public boolean isOrphanRemoval() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return true;
        }
    }

    // ONE_TO_ONE 关联测试 - 非拥有方（isOwningSide = false）
    static class TestEntityWithOneToOneInverseMetadata implements EntityMetadata<TestEntityWithRelation> {

        @Override
        public Class<TestEntityWithRelation> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public String getTableName() {
            return "test_entity_one_to_one_inverse";
        }

        @Override
        public FieldMetadata getIdField() {
            return new TestFieldMetadata("id", "id", Long.class, true, true);
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(getIdField());
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of(new TestOneToOneInverseRelationMetadata());
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return getRelations().stream()
                    .filter(r -> r.getFieldName().equals(fieldName))
                    .findFirst();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return getRelations().stream()
                    .anyMatch(r -> r.getFieldName().equals(fieldName));
        }
    }

    static class TestOneToOneInverseRelationMetadata implements RelationMetadata {

        @Override
        public RelationType getRelationType() {
            return RelationType.ONE_TO_ONE;
        }

        @Override
        public Class<?> getEntityClass() {
            return TestEntityWithRelation.class;
        }

        @Override
        public Class<?> getTargetEntityClass() {
            return UserEntity.class;
        }

        @Override
        public String getFieldName() {
            return "user";
        }

        @Override
        public String getMappedBy() {
            return "profile";
        }

        @Override
        public String getForeignKeyColumn() {
            return "user_id";
        }

        @Override
        public String getJoinTable() {
            return null;
        }

        @Override
        public String getJoinColumn() {
            return null;
        }

        @Override
        public String getInverseJoinColumn() {
            return null;
        }

        @Override
        public Set<CascadeType> getCascadeTypes() {
            return Set.of();
        }

        @Override
        public FetchType getFetchType() {
            return FetchType.LAZY;
        }

        @Override
        public boolean isOwningSide() {
            return false;
        }

        @Override
        public boolean isOrphanRemoval() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return true;
        }
    }

    // 用于测试 inferTableName 的各种类名
    static class ProfileEntity {
        private Long id;
    }

    static class UserEntity {
        private Long id;
    }

    static class SimpleClassNameConversionTestEntity {
        private Long id;
    }

    // 测试 null idField 的实体
    static class TestEntityWithNullIdFieldMetadata implements EntityMetadata<TestEntity> {

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public String getTableName() {
            return "test_entity_null_id";
        }

        @Override
        public FieldMetadata getIdField() {
            return null;
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(
                    new TestFieldMetadata("name", "name", String.class, false, false)
            );
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }

    // 测试非主键但标记为 id 的字段
    static class TestFieldMarkedAsIdMetadata implements EntityMetadata<TestEntity> {

        private final TestFieldMetadata idField = new TestFieldMetadata("id", "id", Long.class, true, true);

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public String getTableName() {
            return "test_entity_field_as_id";
        }

        @Override
        public FieldMetadata getIdField() {
            return idField;
        }

        @Override
        public List<FieldMetadata> getFields() {
            return List.of(
                    idField,
                    new TestFieldMetadata("anotherId", "another_id", Long.class, false, false) {
                        @Override
                        public boolean isId() {
                            return true; // 非主键但标记为 id
                        }
                    }
            );
        }

        @Override
        public FieldMetadata getField(String propertyName) {
            return getFields().stream()
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
        public List<RelationMetadata> getRelations() {
            return List.of();
        }

        @Override
        public Optional<RelationMetadata> getRelation(String fieldName) {
            return Optional.empty();
        }

        @Override
        public boolean hasRelation(String fieldName) {
            return false;
        }
    }
}
