package io.github.afgprojects.framework.data.core.relation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RelationMetadataImpl 单元测试
 * <p>
 * 验证关联元数据记录的构造、属性获取、isOwningSide 判断和构建器。
 */
class RelationMetadataImplTest {

    // ========== 记录构造 ==========

    @Nested
    @DisplayName("记录构造")
    class RecordConstruction {

        @Test
        @DisplayName("should create relation metadata with all fields")
        void shouldCreateRelationMetadata_withAllFields() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .cascadeTypes(Set.of(CascadeType.MERGE, CascadeType.PERSIST))
                    .fetchType(FetchType.EAGER)
                    .optional(true)
                    .orphanRemoval(false)
                    .build();

            assertThat(relation.relationType()).isEqualTo(RelationType.MANY_TO_ONE);
            assertThat(relation.entityClass()).isEqualTo(User.class);
            assertThat(relation.targetEntityClass()).isEqualTo(Department.class);
            assertThat(relation.fieldName()).isEqualTo("department");
            assertThat(relation.foreignKeyColumn()).isEqualTo("department_id");
            assertThat(relation.cascadeTypes()).containsExactlyInAnyOrder(CascadeType.MERGE, CascadeType.PERSIST);
            assertThat(relation.fetchType()).isEqualTo(FetchType.EAGER);
            assertThat(relation.optional()).isTrue();
            assertThat(relation.orphanRemoval()).isFalse();
        }

        @Test
        @DisplayName("should create immutable cascade types set")
        void shouldCreateImmutableCascadeTypesSet() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .cascadeTypes(EnumSet.of(CascadeType.ALL))
                    .build();

            assertThatThrownBy(() -> relation.cascadeTypes().add(CascadeType.MERGE))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should use empty cascade types set when none provided")
        void shouldUseEmptyCascadeTypesSet_whenNoneProvided() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.cascadeTypes()).isEmpty();
        }
    }

    // ========== isOwningSide ==========

    @Nested
    @DisplayName("isOwningSide")
    class IsOwningSide {

        @Test
        @DisplayName("should return true when mappedBy is null")
        void shouldReturnTrue_whenMappedByIsNull() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.isOwningSide()).isTrue();
        }

        @Test
        @DisplayName("should return true when mappedBy is empty string")
        void shouldReturnTrue_whenMappedByIsEmpty() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .mappedBy("")
                    .build();

            assertThat(relation.isOwningSide()).isTrue();
        }

        @Test
        @DisplayName("should return false when mappedBy is set")
        void shouldReturnFalse_whenMappedByIsSet() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .mappedBy("department")
                    .build();

            assertThat(relation.isOwningSide()).isFalse();
        }
    }

    // ========== 接口方法 ==========

    @Nested
    @DisplayName("接口方法")
    class InterfaceMethods {

        @Test
        @DisplayName("should return correct relation type")
        void shouldReturnCorrectRelationType() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Profile.class)
                    .fieldName("profile")
                    .foreignKeyColumn("profile_id")
                    .build();

            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_ONE);
        }

        @Test
        @DisplayName("should return correct entity class")
        void shouldReturnCorrectEntityClass() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.getEntityClass()).isEqualTo(User.class);
        }

        @Test
        @DisplayName("should return correct target entity class")
        void shouldReturnCorrectTargetEntityClass() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.getTargetEntityClass()).isEqualTo(Department.class);
        }

        @Test
        @DisplayName("should return correct field name")
        void shouldReturnCorrectFieldName() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.getFieldName()).isEqualTo("department");
        }

        @Test
        @DisplayName("should return correct foreign key column")
        void shouldReturnCorrectForeignKeyColumn() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.getForeignKeyColumn()).isEqualTo("department_id");
        }

        @Test
        @DisplayName("should return correct fetch type")
        void shouldReturnCorrectFetchType() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .fetchType(FetchType.LAZY)
                    .build();

            assertThat(relation.getFetchType()).isEqualTo(FetchType.LAZY);
        }

        @Test
        @DisplayName("should return correct mappedBy")
        void shouldReturnCorrectMappedBy() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .mappedBy("department")
                    .build();

            assertThat(relation.getMappedBy()).isEqualTo("department");
        }

        @Test
        @DisplayName("should return join table when set")
        void shouldReturnJoinTable_whenSet() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_MANY)
                    .entityClass(User.class)
                    .targetEntityClass(Role.class)
                    .fieldName("roles")
                    .foreignKeyColumn("user_id")
                    .joinTable("sys_user_role")
                    .joinColumn("user_id")
                    .inverseJoinColumn("role_id")
                    .build();

            assertThat(relation.getJoinTable()).isEqualTo("sys_user_role");
            assertThat(relation.getJoinColumn()).isEqualTo("user_id");
            assertThat(relation.getInverseJoinColumn()).isEqualTo("role_id");
        }

        @Test
        @DisplayName("should return orphan removal flag")
        void shouldReturnOrphanRemovalFlag() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .orphanRemoval(true)
                    .build();

            assertThat(relation.isOrphanRemoval()).isTrue();
        }

        @Test
        @DisplayName("should return optional flag")
        void shouldReturnOptionalFlag() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .optional(false)
                    .build();

            assertThat(relation.isOptional()).isFalse();
        }
    }

    // ========== 默认值 ==========

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("should default to LAZY fetch type when not specified")
        void shouldDefaultToLazyFetchType_whenNotSpecified() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.fetchType()).isEqualTo(FetchType.LAZY);
        }

        @Test
        @DisplayName("should default to false orphanRemoval when not specified")
        void shouldDefaultToFalseOrphanRemoval_whenNotSpecified() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.ONE_TO_MANY)
                    .entityClass(Department.class)
                    .targetEntityClass(User.class)
                    .fieldName("users")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.orphanRemoval()).isFalse();
        }

        @Test
        @DisplayName("should default to true optional when not specified")
        void shouldDefaultToTrueOptional_whenNotSpecified() {
            RelationMetadataImpl relation = RelationMetadataImpl.builder()
                    .relationType(RelationType.MANY_TO_ONE)
                    .entityClass(User.class)
                    .targetEntityClass(Department.class)
                    .fieldName("department")
                    .foreignKeyColumn("department_id")
                    .build();

            assertThat(relation.optional()).isTrue();
        }
    }

    // ========== Helper ==========

    private static class User {}
    private static class Department {}
    private static class Profile {}
    private static class Role {}
}