package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.relation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleEntityMetadata 额外覆盖率测试
 * <p>
 * 覆盖以下场景：
 * - arrayToSet 空数组和 null 情况
 * - getGenericFieldType 非 Class 类型参数
 * - 关联关系的 targetEntity 和 foreignKey 分支
 */
@DisplayName("SimpleEntityMetadata 额外覆盖率测试")
class SimpleEntityMetadataAdditionalCoverageTest {

    @Nested
    @DisplayName("arrayToSet 测试")
    class ArrayToSetTests {

        @Test
        @DisplayName("ManyToMany 带 CascadeType 数组应正确转换")
        void shouldConvertCascadeTypesArray() {
            // Given
            SimpleEntityMetadata<EntityWithCascadeTypes> metadata = new SimpleEntityMetadata<>(EntityWithCascadeTypes.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            Set<CascadeType> cascadeTypes = relations.get(0).getCascadeTypes();
            assertThat(cascadeTypes).containsExactlyInAnyOrder(
                    CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE);
        }

        @Test
        @DisplayName("ManyToMany 空数组应返回空 Set")
        void shouldReturnEmptySetForEmptyCascadeArray() {
            // Given
            SimpleEntityMetadata<EntityWithEmptyCascade> metadata = new SimpleEntityMetadata<>(EntityWithEmptyCascade.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            Set<CascadeType> cascadeTypes = relations.get(0).getCascadeTypes();
            assertThat(cascadeTypes).isEmpty();
        }
    }

    @Nested
    @DisplayName("getGenericFieldType 测试")
    class GetGenericFieldTypeTests {

        @Test
        @DisplayName("OneToMany 无 targetEntity 时应从泛型推断类型")
        void shouldInferTypeFromGenerics() {
            // Given
            SimpleEntityMetadata<EntityWithGenericOneToMany> metadata = new SimpleEntityMetadata<>(EntityWithGenericOneToMany.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getTargetEntityClass()).isEqualTo(TargetEntity.class);
        }

        @Test
        @DisplayName("OneToMany 带 targetEntity 时应使用指定类型")
        void shouldUseSpecifiedTargetEntity() {
            // Given
            SimpleEntityMetadata<EntityWithTargetEntity> metadata = new SimpleEntityMetadata<>(EntityWithTargetEntity.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getTargetEntityClass()).isEqualTo(SpecifiedTargetEntity.class);
        }
    }

    @Nested
    @DisplayName("关联关系分支测试")
    class RelationBranchTests {

        @Test
        @DisplayName("ManyToOne 带 targetEntity 和自定义 foreignKey")
        void shouldHandleManyToOneWithCustomOptions() {
            // Given
            SimpleEntityMetadata<EntityWithCustomManyToOne> metadata = new SimpleEntityMetadata<>(EntityWithCustomManyToOne.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
            assertThat(relation.getTargetEntityClass()).isEqualTo(CustomTarget.class);
            assertThat(relation.getForeignKeyColumn()).isEqualTo("custom_fk");
        }

        @Test
        @DisplayName("OneToOne 带 targetEntity 和自定义 foreignKey")
        void shouldHandleOneToOneWithCustomOptions() {
            // Given
            SimpleEntityMetadata<EntityWithCustomOneToOne> metadata = new SimpleEntityMetadata<>(EntityWithCustomOneToOne.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_ONE);
            assertThat(relation.getTargetEntityClass()).isEqualTo(CustomTarget.class);
            assertThat(relation.getForeignKeyColumn()).isEqualTo("profile_fk");
        }

        @Test
        @DisplayName("ManyToMany 带 joinTable 配置")
        void shouldHandleManyToManyWithJoinTable() {
            // Given
            SimpleEntityMetadata<EntityWithJoinTableConfig> metadata = new SimpleEntityMetadata<>(EntityWithJoinTableConfig.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getJoinTable()).isEqualTo("custom_join_table");
            assertThat(relation.getJoinColumn()).isEqualTo("custom_join_col");
            assertThat(relation.getInverseJoinColumn()).isEqualTo("custom_inverse_col");
        }

        @Test
        @DisplayName("ManyToMany 非拥有方（mappedBy）")
        void shouldHandleManyToManyMappedBy() {
            // Given
            SimpleEntityMetadata<EntityWithMappedByManyToMany> metadata = new SimpleEntityMetadata<>(EntityWithMappedByManyToMany.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getMappedBy()).isEqualTo("entities");
        }
    }

    @Nested
    @DisplayName("isSoftDeletable 和 isAuditable 边界测试")
    class SoftDeleteAndAuditableTests {

        @Test
        @DisplayName("deletedAt 字段应被识别为软删除")
        void shouldRecognizeDeletedAtField() {
            // Given
            SimpleEntityMetadata<EntityWithDeletedAt> metadata = new SimpleEntityMetadata<>(EntityWithDeletedAt.class);

            // When & Then
            assertThat(metadata.isSoftDeletable()).isTrue();
        }
    }

    @Nested
    @DisplayName("getIdField 边界测试")
    class GetIdFieldTests {

        @Test
        @DisplayName("无 isId 为 true 的字段时，应查找名为 id 的字段")
        void shouldFallbackToIdName() {
            // Given - EntityWithIdField 没有 @Id 注解但有名为 id 的字段
            SimpleEntityMetadata<EntityWithIdField> metadata = new SimpleEntityMetadata<>(EntityWithIdField.class);

            // When
            var idField = metadata.getIdField();

            // Then
            assertThat(idField).isNotNull();
            assertThat(idField.getPropertyName()).isEqualTo("id");
        }
    }

    @Nested
    @DisplayName("getGenericFieldType 边界测试")
    class GetGenericFieldTypeEdgeCaseTests {

        @Test
        @DisplayName("非泛型字段应返回字段类型")
        void shouldReturnFieldTypeForNonGeneric() {
            // Given
            SimpleEntityMetadata<EntityWithNonGenericRelation> metadata = new SimpleEntityMetadata<>(EntityWithNonGenericRelation.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            // 非泛型字段，targetEntity 应该是字段类型本身
            assertThat(relations.get(0).getTargetEntityClass()).isEqualTo(RawTargetEntity.class);
        }
    }

    @Nested
    @DisplayName("ManyToMany 默认值测试")
    class ManyToManyDefaultValueTests {

        @Test
        @DisplayName("ManyToMany 无 joinTable 配置时应推断默认值")
        void shouldInferDefaultJoinTable() {
            // Given
            SimpleEntityMetadata<EntityWithDefaultManyToMany> metadata = new SimpleEntityMetadata<>(EntityWithDefaultManyToMany.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            // 默认 joinTable 名称为 entity_table_target_table
            assertThat(relation.getJoinTable()).isNotEmpty();
            assertThat(relation.getJoinColumn()).isNotEmpty();
            assertThat(relation.getInverseJoinColumn()).isNotEmpty();
        }
    }

    // ==================== 测试实体类 ====================

    static class TargetEntity {
        private Long id;
    }

    static class SpecifiedTargetEntity {
        private Long id;
    }

    static class CustomTarget {
        private Long id;
    }

    static class RawTargetEntity {
        private Long id;
    }

    static class EntityWithCascadeTypes {
        private Long id;

        @ManyToMany(targetEntity = TargetEntity.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
        private Set<TargetEntity> entities;
    }

    static class EntityWithEmptyCascade {
        private Long id;

        @ManyToMany(targetEntity = TargetEntity.class, cascade = {})
        private Set<TargetEntity> entities;
    }

    static class EntityWithGenericOneToMany {
        private Long id;

        @OneToMany(mappedBy = "parent")
        private List<TargetEntity> targets;
    }

    static class EntityWithTargetEntity {
        private Long id;

        @OneToMany(targetEntity = SpecifiedTargetEntity.class)
        private List<TargetEntity> targets;
    }

    static class EntityWithCustomManyToOne {
        private Long id;

        @ManyToOne(targetEntity = CustomTarget.class, foreignKey = "custom_fk")
        private CustomTarget target;
    }

    static class EntityWithCustomOneToOne {
        private Long id;

        @OneToOne(targetEntity = CustomTarget.class, foreignKey = "profile_fk")
        private CustomTarget profile;
    }

    static class EntityWithJoinTableConfig {
        private Long id;

        @ManyToMany(targetEntity = TargetEntity.class,
                joinTable = "custom_join_table",
                joinColumn = "custom_join_col",
                inverseJoinColumn = "custom_inverse_col")
        private Set<TargetEntity> entities;
    }

    static class EntityWithMappedByManyToMany {
        private Long id;

        @ManyToMany(mappedBy = "entities")
        private Set<TargetEntity> targets;
    }

    static class EntityWithDeletedAt {
        private Long id;
        private java.time.Instant deletedAt;
    }

    static class EntityWithIdField {
        private Long id;  // 无注解，但名为 id
        private String name;
    }

    static class EntityWithNonGenericRelation {
        private Long id;

        @ManyToOne(targetEntity = RawTargetEntity.class)
        private RawTargetEntity target;  // 非泛型字段
    }

    static class EntityWithDefaultManyToMany {
        private Long id;

        @ManyToMany(targetEntity = TargetEntity.class)
        private Set<TargetEntity> entities;
    }
}
