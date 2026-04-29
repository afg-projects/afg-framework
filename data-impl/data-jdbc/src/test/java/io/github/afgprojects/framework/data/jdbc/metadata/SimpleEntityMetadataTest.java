package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleEntityMetadata 测试
 * <p>
 * 针对覆盖率补充测试，重点覆盖：
 * <ul>
 *   <li>getIdField() 中后备查找 "id" 字段名的分支</li>
 *   <li>getGenericFieldType() 中非泛型字段返回 field.getType() 的分支</li>
 * </ul>
 */
@DisplayName("SimpleEntityMetadata 测试")
class SimpleEntityMetadataTest {

    // ==================== getIdField() 测试 ====================

    @Nested
    @DisplayName("getIdField 测试")
    class GetIdFieldTests {

        @Test
        @DisplayName("当没有 isId() 为 true 的字段时，应通过字段名 'id' 查找主键")
        void shouldFindIdFieldByNameWhenNoIdAnnotation() {
            // Given - 实体类有名为 id 的字段，但没有 @Id 注解
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When
            FieldMetadata idField = metadata.getIdField();

            // Then - 应该通过字段名 "id" 找到
            assertThat(idField).isNotNull();
            assertThat(idField.getPropertyName()).isEqualTo("id");
            assertThat(idField.getFieldType()).isEqualTo(Long.class);
        }

        @Test
        @DisplayName("当没有主键字段时应返回 null")
        void shouldReturnNullWhenNoIdField() {
            // Given - 实体类没有 id 字段
            SimpleEntityMetadata<EntityWithoutId> metadata = new SimpleEntityMetadata<>(EntityWithoutId.class);

            // When
            FieldMetadata idField = metadata.getIdField();

            // Then
            assertThat(idField).isNull();
        }
    }

    // ==================== getGenericFieldType() 测试 ====================

    @Nested
    @DisplayName("getGenericFieldType 测试")
    class GetGenericFieldTypeTests {

        @Test
        @DisplayName("OneToMany 使用原始类型（非泛型）时应返回字段类型")
        void shouldReturnFieldTypeWhenRawType() {
            // Given - 实体类有一个 OneToMany 关联，但字段类型是原始 List（没有泛型参数）
            SimpleEntityMetadata<EntityWithRawOneToMany> metadata = new SimpleEntityMetadata<>(EntityWithRawOneToMany.class);

            // When
            var relations = metadata.getRelations();

            // Then - 应该能正确推断出关系，targetEntityClass 应该是 List.class（因为无法获取泛型参数）
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_MANY);
            // 由于是原始类型，无法获取泛型参数，应该返回字段类型 List.class
            assertThat(relation.getTargetEntityClass()).isEqualTo(List.class);
        }

        @Test
        @DisplayName("ManyToMany 使用原始类型（非泛型）时应返回字段类型")
        void shouldReturnFieldTypeForRawManyToMany() {
            // Given - 实体类有一个 ManyToMany 关联，但字段类型是原始 Set（没有泛型参数）
            SimpleEntityMetadata<EntityWithRawManyToMany> metadata = new SimpleEntityMetadata<>(EntityWithRawManyToMany.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_MANY);
            // 由于是原始类型，无法获取泛型参数，应该返回字段类型 Set.class
            assertThat(relation.getTargetEntityClass()).isEqualTo(Set.class);
        }

        @Test
        @DisplayName("OneToMany 带泛型参数时应正确推断目标实体类型")
        void shouldInferTargetEntityFromGenericType() {
            // Given
            SimpleEntityMetadata<EntityWithOneToMany> metadata = new SimpleEntityMetadata<>(EntityWithOneToMany.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getTargetEntityClass()).isEqualTo(Item.class);
        }
    }

    // ==================== 关联注解测试 ====================

    @Nested
    @DisplayName("关联注解测试")
    class RelationAnnotationTests {

        @Test
        @DisplayName("ManyToOne 注解应正确解析")
        void shouldParseManyToOneAnnotation() {
            // Given
            SimpleEntityMetadata<EntityWithManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithManyToOne.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
            assertThat(relation.getFieldName()).isEqualTo("department");
            assertThat(relation.getTargetEntityClass()).isEqualTo(Department.class);
            assertThat(relation.getForeignKeyColumn()).isEqualTo("department_id");
        }

        @Test
        @DisplayName("OneToOne 注解应正确解析")
        void shouldParseOneToOneAnnotation() {
            // Given
            SimpleEntityMetadata<EntityWithOneToOne> metadata = new SimpleEntityMetadata<>(EntityWithOneToOne.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_ONE);
            assertThat(relation.getFieldName()).isEqualTo("profile");
            assertThat(relation.getTargetEntityClass()).isEqualTo(Profile.class);
        }

        @Test
        @DisplayName("OneToMany 注解应正确解析")
        void shouldParseOneToManyAnnotation() {
            // Given
            SimpleEntityMetadata<EntityWithOneToMany> metadata = new SimpleEntityMetadata<>(EntityWithOneToMany.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_MANY);
            assertThat(relation.getFieldName()).isEqualTo("items");
            assertThat(relation.getTargetEntityClass()).isEqualTo(Item.class);
        }

        @Test
        @DisplayName("ManyToMany 注解应正确解析")
        void shouldParseManyToManyAnnotation() {
            // Given
            SimpleEntityMetadata<EntityWithManyToMany> metadata = new SimpleEntityMetadata<>(EntityWithManyToMany.class);

            // When
            var relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_MANY);
            assertThat(relation.getFieldName()).isEqualTo("roles");
            assertThat(relation.getTargetEntityClass()).isEqualTo(Role.class);
        }
    }

    // ==================== EntityMetadata 接口方法测试 ====================

    @Nested
    @DisplayName("EntityMetadata 接口方法测试")
    class EntityMetadataInterfaceTests {

        @Test
        @DisplayName("getEntityClass 应返回正确的实体类")
        void shouldReturnCorrectEntityClass() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When & Then
            assertThat(metadata.getEntityClass()).isEqualTo(EntityWithIdField.class);
        }

        @Test
        @DisplayName("getTableName 应返回正确的表名（snake_case）")
        void shouldReturnCorrectTableName() {
            // Given
            SimpleEntityMetadata<UserAccountInfo> metadata = new SimpleEntityMetadata<>(UserAccountInfo.class);

            // When & Then
            assertThat(metadata.getTableName()).isEqualTo("user_account_info");
        }

        @Test
        @DisplayName("getFields 应返回所有非关联字段")
        void shouldReturnAllNonRelationFields() {
            // Given
            SimpleEntityMetadata<EntityWithManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithManyToOne.class);

            // When
            var fields = metadata.getFields();

            // Then - 不应包含 department 关联字段
            assertThat(fields).extracting(FieldMetadata::getPropertyName)
                    .contains("id", "name", "departmentId")
                    .doesNotContain("department");
        }

        @Test
        @DisplayName("getField 应返回指定属性名的字段")
        void shouldReturnFieldByPropertyName() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When
            FieldMetadata field = metadata.getField("name");

            // Then
            assertThat(field).isNotNull();
            assertThat(field.getPropertyName()).isEqualTo("name");
        }

        @Test
        @DisplayName("getField 对于不存在的属性应返回 null")
        void shouldReturnNullForNonExistentField() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When
            FieldMetadata field = metadata.getField("nonExistent");

            // Then
            assertThat(field).isNull();
        }

        @Test
        @DisplayName("isSoftDeletable 当有 deleted 字段时应返回 true")
        void shouldReturnTrueWhenHasDeletedField() {
            // Given
            SimpleEntityMetadata<EntityWithDeleted> metadata = new SimpleEntityMetadata<>(EntityWithDeleted.class);

            // When & Then
            assertThat(metadata.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("isSoftDeletable 当有 deletedAt 字段时应返回 true")
        void shouldReturnTrueWhenHasDeletedAtField() {
            // Given
            SimpleEntityMetadata<EntityWithDeletedAt> metadata = new SimpleEntityMetadata<>(EntityWithDeletedAt.class);

            // When & Then
            assertThat(metadata.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("isSoftDeletable 当没有软删除字段时应返回 false")
        void shouldReturnFalseWhenNoSoftDeleteField() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When & Then
            assertThat(metadata.isSoftDeletable()).isFalse();
        }

        @Test
        @DisplayName("isTenantAware 当有 tenantId 字段时应返回 true")
        void shouldReturnTrueWhenHasTenantIdField() {
            // Given
            SimpleEntityMetadata<EntityWithTenantId> metadata = new SimpleEntityMetadata<>(EntityWithTenantId.class);

            // When & Then
            assertThat(metadata.isTenantAware()).isTrue();
        }

        @Test
        @DisplayName("isTenantAware 当没有 tenantId 字段时应返回 false")
        void shouldReturnFalseWhenNoTenantIdField() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When & Then
            assertThat(metadata.isTenantAware()).isFalse();
        }

        @Test
        @DisplayName("isAuditable 当有 createdAt 和 updatedAt 字段时应返回 true")
        void shouldReturnTrueWhenHasAuditFields() {
            // Given
            SimpleEntityMetadata<EntityWithAuditFields> metadata = new SimpleEntityMetadata<>(EntityWithAuditFields.class);

            // When & Then
            assertThat(metadata.isAuditable()).isTrue();
        }

        @Test
        @DisplayName("isAuditable 当缺少 updatedAt 字段时应返回 false")
        void shouldReturnFalseWhenMissingUpdatedAt() {
            // Given
            SimpleEntityMetadata<EntityWithOnlyCreatedAt> metadata = new SimpleEntityMetadata<>(EntityWithOnlyCreatedAt.class);

            // When & Then
            assertThat(metadata.isAuditable()).isFalse();
        }

        @Test
        @DisplayName("isVersioned 当有 version 字段时应返回 true")
        void shouldReturnTrueWhenHasVersionField() {
            // Given
            SimpleEntityMetadata<EntityWithVersion> metadata = new SimpleEntityMetadata<>(EntityWithVersion.class);

            // When & Then
            assertThat(metadata.isVersioned()).isTrue();
        }

        @Test
        @DisplayName("isVersioned 当没有 version 字段时应返回 false")
        void shouldReturnFalseWhenNoVersionField() {
            // Given
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When & Then
            assertThat(metadata.isVersioned()).isFalse();
        }

        @Test
        @DisplayName("getRelation 应返回 Optional 包含正确的关联元数据")
        void shouldReturnRelationByFieldName() {
            // Given
            SimpleEntityMetadata<EntityWithManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithManyToOne.class);

            // When
            var relation = metadata.getRelation("department");

            // Then
            assertThat(relation).isPresent();
            assertThat(relation.get().getFieldName()).isEqualTo("department");
        }

        @Test
        @DisplayName("getRelation 对于不存在的关联应返回空 Optional")
        void shouldReturnEmptyForNonExistentRelation() {
            // Given
            SimpleEntityMetadata<EntityWithManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithManyToOne.class);

            // When
            var relation = metadata.getRelation("nonExistent");

            // Then
            assertThat(relation).isEmpty();
        }

        @Test
        @DisplayName("hasRelation 应正确判断关联是否存在")
        void shouldCheckIfRelationExists() {
            // Given
            SimpleEntityMetadata<EntityWithManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithManyToOne.class);

            // When & Then
            assertThat(metadata.hasRelation("department")).isTrue();
            assertThat(metadata.hasRelation("nonExistent")).isFalse();
        }
    }

    // ==================== 注解属性覆盖测试 ====================

    @Nested
    @DisplayName("注解属性覆盖测试")
    class AnnotationAttributeOverrideTests {

        @Test
        @DisplayName("ManyToOne 显式指定 targetEntity 应生效")
        void shouldUseExplicitTargetEntityForManyToOne() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitTargetEntity> metadata = new SimpleEntityMetadata<>(EntityWithExplicitTargetEntity.class);

            // When
            var relation = metadata.getRelation("parent").orElseThrow();

            // Then - targetEntity 显式指定为 Department
            assertThat(relation.getTargetEntityClass()).isEqualTo(Department.class);
        }

        @Test
        @DisplayName("ManyToOne 显式指定 foreignKey 应生效")
        void shouldUseExplicitForeignKeyForManyToOne() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitForeignKey> metadata = new SimpleEntityMetadata<>(EntityWithExplicitForeignKey.class);

            // When
            var relation = metadata.getRelation("department").orElseThrow();

            // Then
            assertThat(relation.getForeignKeyColumn()).isEqualTo("custom_dept_id");
        }

        @Test
        @DisplayName("OneToMany 显式指定 targetEntity 应生效")
        void shouldUseExplicitTargetEntityForOneToMany() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitOneToManyTarget> metadata = new SimpleEntityMetadata<>(EntityWithExplicitOneToManyTarget.class);

            // When
            var relation = metadata.getRelation("items").orElseThrow();

            // Then
            assertThat(relation.getTargetEntityClass()).isEqualTo(CustomItem.class);
        }

        @Test
        @DisplayName("OneToMany 显式指定 foreignKey 应生效")
        void shouldUseExplicitForeignKeyForOneToMany() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitOneToManyFK> metadata = new SimpleEntityMetadata<>(EntityWithExplicitOneToManyFK.class);

            // When
            var relation = metadata.getRelation("items").orElseThrow();

            // Then
            assertThat(relation.getForeignKeyColumn()).isEqualTo("custom_parent_id");
        }

        @Test
        @DisplayName("OneToOne 显式指定 mappedBy 应生效")
        void shouldUseExplicitMappedByForOneToOne() {
            // Given
            SimpleEntityMetadata<EntityWithOneToOneMappedBy> metadata = new SimpleEntityMetadata<>(EntityWithOneToOneMappedBy.class);

            // When
            var relation = metadata.getRelation("profile").orElseThrow();

            // Then
            assertThat(relation.getMappedBy()).isEqualTo("user");
        }

        @Test
        @DisplayName("ManyToMany 显式指定 joinTable 应生效")
        void shouldUseExplicitJoinTableForManyToMany() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitJoinTable> metadata = new SimpleEntityMetadata<>(EntityWithExplicitJoinTable.class);

            // When
            var relation = metadata.getRelation("roles").orElseThrow();

            // Then
            assertThat(relation.getJoinTable()).isEqualTo("user_role_custom");
        }

        @Test
        @DisplayName("ManyToMany 显式指定 joinColumn 和 inverseJoinColumn 应生效")
        void shouldUseExplicitJoinColumnsForManyToMany() {
            // Given
            SimpleEntityMetadata<EntityWithExplicitJoinColumns> metadata = new SimpleEntityMetadata<>(EntityWithExplicitJoinColumns.class);

            // When
            var relation = metadata.getRelation("roles").orElseThrow();

            // Then
            assertThat(relation.getJoinColumn()).isEqualTo("user_fk");
            assertThat(relation.getInverseJoinColumn()).isEqualTo("role_fk");
        }
    }

    // ==================== 测试实体类 ====================

    /**
     * 有 id 字段但没有 @Id 注解的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithIdField {
        private Long id;
        private String name;
    }

    /**
     * 没有 id 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithoutId {
        private String name;
        private String description;
    }

    /**
     * 有 deleted 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithDeleted {
        private Long id;
        private Boolean deleted;
    }

    /**
     * 有 deletedAt 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithDeletedAt {
        private Long id;
        private java.time.LocalDateTime deletedAt;
    }

    /**
     * 有 tenantId 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithTenantId {
        private Long id;
        private String tenantId;
    }

    /**
     * 有审计字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithAuditFields {
        private Long id;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }

    /**
     * 只有 createdAt 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithOnlyCreatedAt {
        private Long id;
        private java.time.LocalDateTime createdAt;
    }

    /**
     * 有 version 字段的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithVersion {
        private Long id;
        private Integer version;
    }

    /**
     * 表名测试实体
     */
    @Data
    @NoArgsConstructor
    static class UserAccountInfo {
        private Long id;
        private String name;
    }

    /**
     * 带有原始类型 OneToMany 关联的实体（无泛型参数）
     * 注意：这会触发 getGenericFieldType() 返回 field.getType() 的分支
     * 关键：没有显式指定 targetEntity，所以会调用 getGenericFieldType
     */
    @Data
    @NoArgsConstructor
    @SuppressWarnings("rawtypes")
    static class EntityWithRawOneToMany {
        private Long id;
        private String name;

        // 原始类型 List，没有泛型参数，也没有显式 targetEntity
        @OneToMany
        private List items;
    }

    /**
     * 带有原始类型 ManyToMany 关联的实体（无泛型参数）
     * 关键：没有显式指定 targetEntity，所以会调用 getGenericFieldType
     */
    @Data
    @NoArgsConstructor
    @SuppressWarnings("rawtypes")
    static class EntityWithRawManyToMany {
        private Long id;
        private String name;

        // 原始类型 Set，没有泛型参数，也没有显式 targetEntity
        @ManyToMany
        private Set roles;
    }

    /**
     * 部门实体
     */
    @Data
    @NoArgsConstructor
    static class Department {
        private Long id;
        private String name;
    }

    /**
     * 带有 ManyToOne 关联的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithManyToOne {
        private Long id;
        private String name;
        private Long departmentId;

        @ManyToOne
        private Department department;
    }

    /**
     * 配置实体
     */
    @Data
    @NoArgsConstructor
    static class Profile {
        private Long id;
        private String bio;
    }

    /**
     * 带有 OneToOne 关联的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithOneToOne {
        private Long id;
        private String username;

        @OneToOne
        private Profile profile;
    }

    /**
     * 项目实体
     */
    @Data
    @NoArgsConstructor
    static class Item {
        private Long id;
        private String name;
    }

    /**
     * 带有 OneToMany 关联的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithOneToMany {
        private Long id;
        private String name;

        @OneToMany
        private List<Item> items;
    }

    /**
     * 角色实体
     */
    @Data
    @NoArgsConstructor
    static class Role {
        private Long id;
        private String name;
    }

    /**
     * 带有 ManyToMany 关联的实体
     */
    @Data
    @NoArgsConstructor
    static class EntityWithManyToMany {
        private Long id;
        private String username;

        @ManyToMany
        private Set<Role> roles;
    }

    /**
     * 显式指定 targetEntity 的 ManyToOne
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitTargetEntity {
        private Long id;

        @ManyToOne(targetEntity = Department.class)
        private Object parent;
    }

    /**
     * 显式指定 foreignKey 的 ManyToOne
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitForeignKey {
        private Long id;
        private Long departmentId;

        @ManyToOne(foreignKey = "custom_dept_id")
        private Department department;
    }

    /**
     * 自定义项目实体
     */
    @Data
    @NoArgsConstructor
    static class CustomItem {
        private Long id;
        private String name;
    }

    /**
     * 显式指定 targetEntity 的 OneToMany
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitOneToManyTarget {
        private Long id;

        @OneToMany(targetEntity = CustomItem.class)
        @SuppressWarnings("rawtypes")
        private List items;
    }

    /**
     * 显式指定 foreignKey 的 OneToMany
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitOneToManyFK {
        private Long id;

        @OneToMany(foreignKey = "custom_parent_id")
        private List<Item> items;
    }

    /**
     * 显式指定 mappedBy 的 OneToOne
     */
    @Data
    @NoArgsConstructor
    static class EntityWithOneToOneMappedBy {
        private Long id;

        @OneToOne(mappedBy = "user")
        private Profile profile;
    }

    /**
     * 显式指定 joinTable 的 ManyToMany
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitJoinTable {
        private Long id;

        @ManyToMany(joinTable = "user_role_custom")
        private Set<Role> roles;
    }

    /**
     * 显式指定 joinColumn 和 inverseJoinColumn 的 ManyToMany
     */
    @Data
    @NoArgsConstructor
    static class EntityWithExplicitJoinColumns {
        private Long id;

        @ManyToMany(joinColumn = "user_fk", inverseJoinColumn = "role_fk")
        private Set<Role> roles;
    }
}
