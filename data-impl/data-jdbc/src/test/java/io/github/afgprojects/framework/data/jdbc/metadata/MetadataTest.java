package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.relation.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实体元数据测试
 */
@DisplayName("实体元数据测试")
class MetadataTest {

    // ==================== SimpleEntityMetadata 测试 ====================

    @Nested
    @DisplayName("SimpleEntityMetadata 测试")
    class SimpleEntityMetadataTest {

        @Test
        @DisplayName("应正确推断表名（camelCase 转 snake_case）")
        void shouldInferTableName() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.getTableName()).isEqualTo("test_user");
        }

        @Test
        @DisplayName("应正确获取实体类")
        void shouldGetEntityClass() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.getEntityClass()).isEqualTo(TestUser.class);
        }

        @Test
        @DisplayName("应正确识别主键字段")
        void shouldIdentifyIdField() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            var idField = metadata.getIdField();

            assertThat(idField).isNotNull();
            assertThat(idField.getPropertyName()).isEqualTo("id");
            assertThat(idField.isId()).isTrue();
        }

        @Test
        @DisplayName("应正确获取所有字段")
        void shouldGetAllFields() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            var fields = metadata.getFields();

            assertThat(fields).hasSize(5);
            assertThat(fields.stream().map(f -> f.getPropertyName()))
                    .containsExactlyInAnyOrder("id", "name", "email", "createdAt", "updatedAt");
        }

        @Test
        @DisplayName("应正确获取指定字段")
        void shouldGetFieldByName() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            var nameField = metadata.getField("name");

            assertThat(nameField).isNotNull();
            assertThat(nameField.getPropertyName()).isEqualTo("name");
        }

        @Test
        @DisplayName("不存在的字段应返回 null")
        void shouldReturnNullForNonExistentField() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            var field = metadata.getField("nonExistent");

            assertThat(field).isNull();
        }

        @Test
        @DisplayName("应正确识别软删除实体")
        void shouldIdentifySoftDeletableEntity() {
            SimpleEntityMetadata<SoftDeleteEntity> metadata = new SimpleEntityMetadata<>(SoftDeleteEntity.class);

            assertThat(metadata.isSoftDeletable()).isTrue();
        }

        @Test
        @DisplayName("应正确识别非软删除实体")
        void shouldIdentifyNonSoftDeletableEntity() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.isSoftDeletable()).isFalse();
        }

        @Test
        @DisplayName("应正确识别租户感知实体")
        void shouldIdentifyTenantAwareEntity() {
            SimpleEntityMetadata<TenantAwareEntity> metadata = new SimpleEntityMetadata<>(TenantAwareEntity.class);

            assertThat(metadata.isTenantAware()).isTrue();
        }

        @Test
        @DisplayName("应正确识别非租户感知实体")
        void shouldIdentifyNonTenantAwareEntity() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.isTenantAware()).isFalse();
        }

        @Test
        @DisplayName("应正确识别可审计实体")
        void shouldIdentifyAuditableEntity() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.isAuditable()).isTrue();
        }

        @Test
        @DisplayName("应正确识别非可审计实体")
        void shouldIdentifyNonAuditableEntity() {
            SimpleEntityMetadata<SimpleEntity> metadata = new SimpleEntityMetadata<>(SimpleEntity.class);

            assertThat(metadata.isAuditable()).isFalse();
        }

        @Test
        @DisplayName("应正确识别版本化实体")
        void shouldIdentifyVersionedEntity() {
            SimpleEntityMetadata<VersionedEntity> metadata = new SimpleEntityMetadata<>(VersionedEntity.class);

            assertThat(metadata.isVersioned()).isTrue();
        }

        @Test
        @DisplayName("应正确识别非版本化实体")
        void shouldIdentifyNonVersionedEntity() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            assertThat(metadata.isVersioned()).isFalse();
        }

        // ==================== 继承测试 ====================

        @Test
        @DisplayName("应正确处理继承的字段")
        void shouldHandleInheritedFields() {
            SimpleEntityMetadata<ChildEntity> metadata = new SimpleEntityMetadata<>(ChildEntity.class);

            var fields = metadata.getFields();

            // 应包含父类和子类的字段
            assertThat(fields).hasSize(3);
            assertThat(fields.stream().map(f -> f.getPropertyName()))
                    .containsExactlyInAnyOrder("id", "name", "childField");
        }

        // ==================== 关联关系测试 ====================

        @Test
        @DisplayName("应正确识别 ManyToOne 关联")
        void shouldIdentifyManyToOneRelation() {
            SimpleEntityMetadata<OrderEntity> metadata = new SimpleEntityMetadata<>(OrderEntity.class);

            List<RelationMetadata> relations = metadata.getRelations();

            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
            assertThat(relation.getFieldName()).isEqualTo("user");
            assertThat(relation.getTargetEntityClass()).isEqualTo(TestUser.class);
            assertThat(relation.getForeignKeyColumn()).isEqualTo("user_id");
        }

        @Test
        @DisplayName("应正确识别 OneToMany 关联")
        void shouldIdentifyOneToManyRelation() {
            SimpleEntityMetadata<UserWithOrders> metadata = new SimpleEntityMetadata<>(UserWithOrders.class);

            List<RelationMetadata> relations = metadata.getRelations();

            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_MANY);
            assertThat(relation.getFieldName()).isEqualTo("orders");
            assertThat(relation.getMappedBy()).isEqualTo("user");
        }

        @Test
        @DisplayName("应正确识别 OneToOne 关联")
        void shouldIdentifyOneToOneRelation() {
            SimpleEntityMetadata<UserWithProfile> metadata = new SimpleEntityMetadata<>(UserWithProfile.class);

            List<RelationMetadata> relations = metadata.getRelations();

            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.ONE_TO_ONE);
            assertThat(relation.getFieldName()).isEqualTo("profile");
        }

        @Test
        @DisplayName("应正确识别 ManyToMany 关联")
        void shouldIdentifyManyToManyRelation() {
            SimpleEntityMetadata<UserWithRoles> metadata = new SimpleEntityMetadata<>(UserWithRoles.class);

            List<RelationMetadata> relations = metadata.getRelations();

            assertThat(relations).hasSize(1);
            RelationMetadata relation = relations.get(0);
            assertThat(relation.getRelationType()).isEqualTo(RelationType.MANY_TO_MANY);
            assertThat(relation.getFieldName()).isEqualTo("roles");
            assertThat(relation.getJoinTable()).isEqualTo("user_with_roles_role");
        }

        @Test
        @DisplayName("应正确通过 getRelation 获取关联")
        void shouldGetRelationByName() {
            SimpleEntityMetadata<OrderEntity> metadata = new SimpleEntityMetadata<>(OrderEntity.class);

            Optional<RelationMetadata> relation = metadata.getRelation("user");

            assertThat(relation).isPresent();
            assertThat(relation.get().getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
        }

        @Test
        @DisplayName("不存在的关联应返回空 Optional")
        void shouldReturnEmptyForNonExistentRelation() {
            SimpleEntityMetadata<OrderEntity> metadata = new SimpleEntityMetadata<>(OrderEntity.class);

            Optional<RelationMetadata> relation = metadata.getRelation("nonExistent");

            assertThat(relation).isEmpty();
        }

        @Test
        @DisplayName("应正确检查关联是否存在")
        void shouldCheckRelationExists() {
            SimpleEntityMetadata<OrderEntity> metadata = new SimpleEntityMetadata<>(OrderEntity.class);

            assertThat(metadata.hasRelation("user")).isTrue();
            assertThat(metadata.hasRelation("nonExistent")).isFalse();
        }

        @Test
        @DisplayName("无关联的实体应返回空列表")
        void shouldReturnEmptyListForNoRelations() {
            SimpleEntityMetadata<TestUser> metadata = new SimpleEntityMetadata<>(TestUser.class);

            List<RelationMetadata> relations = metadata.getRelations();

            assertThat(relations).isEmpty();
        }
    }

    // ==================== SimpleFieldMetadata 测试 ====================

    @Nested
    @DisplayName("SimpleFieldMetadata 测试")
    class SimpleFieldMetadataTest {

        @Test
        @DisplayName("应正确获取字段属性")
        void shouldGetFieldProperties() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            assertThat(metadata.getPropertyName()).isEqualTo("name");
            assertThat(metadata.getFieldAccessor().getFieldType()).isEqualTo(String.class);
            assertThat(metadata.isId()).isFalse();
        }

        @Test
        @DisplayName("应正确识别主键字段")
        void shouldIdentifyIdField() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("id");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            assertThat(metadata.isId()).isTrue();
        }

        @Test
        @DisplayName("应正确获取和设置字段值")
        void shouldGetAndSetFieldValue() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);
            TestUser user = new TestUser();

            metadata.setValue(user, "test");

            assertThat(metadata.getValue(user)).isEqualTo("test");
        }

        @Test
        @DisplayName("应正确推断列名（camelCase 转 snake_case）")
        void shouldInferColumnName() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("createdAt");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            assertThat(metadata.getColumnName()).isEqualTo("created_at");
        }

        @Test
        @DisplayName("应正确使用简单构造方法")
        void shouldUseSimpleConstructor() {
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("testField", String.class);

            assertThat(metadata.getPropertyName()).isEqualTo("testField");
            assertThat(metadata.getFieldType()).isEqualTo(String.class);
            assertThat(metadata.isId()).isFalse();
            assertThat(metadata.getFieldAccessor()).isNull();
        }

        @Test
        @DisplayName("简单构造方法应识别名为 id 的字段为主键")
        void simpleConstructorShouldIdentifyIdField() {
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("id", Long.class);

            assertThat(metadata.isId()).isTrue();
        }

        @Test
        @DisplayName("isGenerated 应与 isId 一致")
        void isGeneratedShouldMatchIsId() throws NoSuchFieldException {
            java.lang.reflect.Field idField = TestUser.class.getDeclaredField("id");
            java.lang.reflect.Field nameField = TestUser.class.getDeclaredField("name");

            SimpleFieldMetadata idMetadata = new SimpleFieldMetadata(idField);
            SimpleFieldMetadata nameMetadata = new SimpleFieldMetadata(nameField);

            assertThat(idMetadata.isGenerated()).isTrue();
            assertThat(nameMetadata.isGenerated()).isFalse();
        }

        @Test
        @DisplayName("无 FieldAccessor 时 getValue 应抛出异常")
        void getValueShouldThrowWithoutFieldAccessor() {
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("testField", String.class);
            TestUser user = new TestUser();

            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                metadata.getValue(user);
            });
        }

        @Test
        @DisplayName("无 FieldAccessor 时 setValue 应抛出异常")
        void setValueShouldThrowWithoutFieldAccessor() {
            SimpleFieldMetadata metadata = new SimpleFieldMetadata("testField", String.class);
            TestUser user = new TestUser();

            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                metadata.setValue(user, "test");
            });
        }

        @Test
        @DisplayName("应正确获取字段类型")
        void shouldGetFieldType() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("id");
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(field);

            assertThat(metadata.getFieldType()).isEqualTo(Long.class);
        }
    }

    // ==================== CachedFieldAccessor 测试 ====================

    @Nested
    @DisplayName("CachedFieldAccessor 测试")
    class CachedFieldAccessorTest {

        @Test
        @DisplayName("应正确缓存字段访问器")
        void shouldCacheFieldAccessor() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            CachedFieldAccessor accessor1 = new CachedFieldAccessor(field);
            CachedFieldAccessor accessor2 = new CachedFieldAccessor(field);

            // 两个不同的实例
            assertThat(accessor1).isNotSameAs(accessor2);

            // 但都应该能正常工作
            TestUser user = new TestUser();
            accessor1.setValue(user, "test1");
            assertThat(accessor1.getValue(user)).isEqualTo("test1");

            accessor2.setValue(user, "test2");
            assertThat(accessor2.getValue(user)).isEqualTo("test2");
        }

        @Test
        @DisplayName("应正确处理 null 值")
        void shouldHandleNullValue() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            CachedFieldAccessor accessor = new CachedFieldAccessor(field);
            TestUser user = new TestUser();

            accessor.setValue(user, null);

            assertThat(accessor.getValue(user)).isNull();
        }

        @Test
        @DisplayName("应正确获取字段类型")
        void shouldGetFieldType() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            CachedFieldAccessor accessor = new CachedFieldAccessor(field);

            assertThat(accessor.getFieldType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("应正确获取字段名")
        void shouldGetFieldName() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            CachedFieldAccessor accessor = new CachedFieldAccessor(field);

            assertThat(accessor.getFieldName()).isEqualTo("name");
        }

        @Test
        @DisplayName("应正确获取底层 Field 对象")
        void shouldGetUnderlyingField() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("name");
            CachedFieldAccessor accessor = new CachedFieldAccessor(field);

            assertThat(accessor.getField()).isEqualTo(field);
        }

        @Test
        @DisplayName("应正确处理类型不匹配")
        void shouldHandleTypeMismatch() throws NoSuchFieldException {
            java.lang.reflect.Field field = TestUser.class.getDeclaredField("id");
            CachedFieldAccessor accessor = new CachedFieldAccessor(field);
            TestUser user = new TestUser();

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
                accessor.setValue(user, "not-a-long");
            });
        }
    }

    // ==================== 测试实体类 ====================

    static class SimpleEntity {
        private Long id;
    }

    static class TestUser {
        private Long id;
        private String name;
        private String email;
        private java.time.Instant createdAt;
        private java.time.Instant updatedAt;

        public TestUser() {}

        public TestUser(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    static class SoftDeleteEntity {
        private Long id;
        private String name;
        private Boolean deleted;
    }

    static class TenantAwareEntity {
        private Long id;
        private String name;
        private String tenantId;
    }

    static class VersionedEntity {
        private Long id;
        private String name;
        private Long version;
    }

    static class ParentEntity {
        private Long id;
        private String name;
    }

    static class ChildEntity extends ParentEntity {
        private String childField;
    }

    static class OrderEntity {
        private Long id;
        private String orderNo;

        @ManyToOne(targetEntity = TestUser.class)
        private TestUser user;
    }

    static class UserWithOrders {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "user", targetEntity = OrderEntity.class)
        private List<OrderEntity> orders;
    }

    static class UserProfile {
        private Long id;
        private String bio;
    }

    static class UserWithProfile {
        private Long id;
        private String name;

        @OneToOne(targetEntity = UserProfile.class)
        private UserProfile profile;
    }

    static class Role {
        private Long id;
        private String name;
    }

    static class UserWithRoles {
        private Long id;
        private String name;

        @ManyToMany(targetEntity = Role.class)
        private Set<Role> roles;
    }
}
