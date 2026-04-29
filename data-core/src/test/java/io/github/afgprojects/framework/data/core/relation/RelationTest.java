package io.github.afgprojects.framework.data.core.relation;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RelationTest {

    // ==================== 测试实体 ====================

    static class User {
        @OneToOne
        UserProfile profile;

        @OneToMany(mappedBy = "user")
        java.util.List<Order> orders;

        @ManyToOne(fetch = FetchType.EAGER)
        Department department;

        @ManyToMany
        java.util.Set<Role> roles;
    }

    static class UserProfile {
        @OneToOne(mappedBy = "profile")
        User user;
    }

    static class Order {
        @ManyToOne
        User user;
    }

    static class Department {
        @OneToMany(mappedBy = "department")
        java.util.List<User> users;
    }

    static class Role {
        @ManyToMany(mappedBy = "roles")
        java.util.Set<User> users;
    }

    // ==================== 注解测试 ====================

    @Test
    void shouldDefineOneToOneAnnotation() {
        assertThat(OneToOne.class).isAnnotation();
        assertThat(OneToOne.class.isAnnotationPresent(java.lang.annotation.Retention.class)).isTrue();
    }

    @Test
    void shouldDefineOneToManyAnnotation() {
        assertThat(OneToMany.class).isAnnotation();
        assertThat(OneToMany.class.isAnnotationPresent(java.lang.annotation.Retention.class)).isTrue();
    }

    @Test
    void shouldDefineManyToOneAnnotation() {
        assertThat(ManyToOne.class).isAnnotation();
        assertThat(ManyToOne.class.isAnnotationPresent(java.lang.annotation.Retention.class)).isTrue();
    }

    @Test
    void shouldDefineManyToManyAnnotation() {
        assertThat(ManyToMany.class).isAnnotation();
        assertThat(ManyToMany.class.isAnnotationPresent(java.lang.annotation.Retention.class)).isTrue();
    }

    @Test
    void shouldReadOneToOneAnnotation() throws NoSuchFieldException {
        OneToOne annotation = User.class.getDeclaredField("profile").getAnnotation(OneToOne.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.mappedBy()).isEmpty();
        assertThat(annotation.fetch()).isEqualTo(FetchType.LAZY);
    }

    @Test
    void shouldReadOneToManyAnnotation() throws NoSuchFieldException {
        OneToMany annotation = User.class.getDeclaredField("orders").getAnnotation(OneToMany.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.mappedBy()).isEqualTo("user");
        assertThat(annotation.orphanRemoval()).isFalse();
    }

    @Test
    void shouldReadManyToOneAnnotation() throws NoSuchFieldException {
        ManyToOne annotation = User.class.getDeclaredField("department").getAnnotation(ManyToOne.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.fetch()).isEqualTo(FetchType.EAGER);
        assertThat(annotation.optional()).isTrue();
    }

    @Test
    void shouldReadManyToManyAnnotation() throws NoSuchFieldException {
        ManyToMany annotation = User.class.getDeclaredField("roles").getAnnotation(ManyToMany.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.mappedBy()).isEmpty();
    }

    // ==================== CascadeType 测试 ====================

    @Test
    void shouldDefineCascadeTypes() {
        assertThat(CascadeType.PERSIST).isNotNull();
        assertThat(CascadeType.MERGE).isNotNull();
        assertThat(CascadeType.REMOVE).isNotNull();
        assertThat(CascadeType.REFRESH).isNotNull();
        assertThat(CascadeType.DETACH).isNotNull();
        assertThat(CascadeType.ALL).isNotNull();
    }

    // ==================== FetchType 测试 ====================

    @Test
    void shouldDefineFetchTypes() {
        assertThat(FetchType.LAZY).isNotNull();
        assertThat(FetchType.EAGER).isNotNull();
    }

    // ==================== RelationType 测试 ====================

    @Test
    void shouldDefineRelationTypes() {
        assertThat(RelationType.ONE_TO_ONE).isNotNull();
        assertThat(RelationType.ONE_TO_MANY).isNotNull();
        assertThat(RelationType.MANY_TO_ONE).isNotNull();
        assertThat(RelationType.MANY_TO_MANY).isNotNull();
    }

    // ==================== RelationMetadata 测试 ====================

    @Test
    void shouldBuildRelationMetadata() {
        Set<CascadeType> cascadeTypes = EnumSet.of(CascadeType.PERSIST, CascadeType.MERGE);

        RelationMetadata metadata = RelationMetadataImpl.builder()
                .relationType(RelationType.ONE_TO_MANY)
                .entityClass(User.class)
                .targetEntityClass(Order.class)
                .fieldName("orders")
                .mappedBy("user")
                .foreignKeyColumn("user_id")
                .fetchType(FetchType.LAZY)
                .cascadeTypes(cascadeTypes)
                .orphanRemoval(true)
                .build();

        assertThat(metadata.getRelationType()).isEqualTo(RelationType.ONE_TO_MANY);
        assertThat(metadata.getEntityClass()).isEqualTo(User.class);
        assertThat(metadata.getTargetEntityClass()).isEqualTo(Order.class);
        assertThat(metadata.getFieldName()).isEqualTo("orders");
        assertThat(metadata.getMappedBy()).isEqualTo("user");
        assertThat(metadata.getForeignKeyColumn()).isEqualTo("user_id");
        assertThat(metadata.getFetchType()).isEqualTo(FetchType.LAZY);
        assertThat(metadata.getCascadeTypes()).containsExactlyInAnyOrder(CascadeType.PERSIST, CascadeType.MERGE);
        assertThat(metadata.isOwningSide()).isFalse();
        assertThat(metadata.isOrphanRemoval()).isTrue();
    }

    @Test
    void shouldIdentifyOwningSide() {
        // mappedBy 为空，是维护方
        RelationMetadata owning = RelationMetadataImpl.builder()
                .relationType(RelationType.MANY_TO_ONE)
                .entityClass(Order.class)
                .targetEntityClass(User.class)
                .fieldName("user")
                .foreignKeyColumn("user_id")
                .build();
        assertThat(owning.isOwningSide()).isTrue();

        // mappedBy 不为空，不是维护方
        RelationMetadata inverse = RelationMetadataImpl.builder()
                .relationType(RelationType.ONE_TO_MANY)
                .entityClass(User.class)
                .targetEntityClass(Order.class)
                .fieldName("orders")
                .mappedBy("user")
                .foreignKeyColumn("user_id")
                .build();
        assertThat(inverse.isOwningSide()).isFalse();
    }

    @Test
    void shouldBuildManyToManyMetadata() {
        RelationMetadata metadata = RelationMetadataImpl.builder()
                .relationType(RelationType.MANY_TO_MANY)
                .entityClass(User.class)
                .targetEntityClass(Role.class)
                .fieldName("roles")
                .foreignKeyColumn("user_id")
                .joinTable("user_role")
                .joinColumn("user_id")
                .inverseJoinColumn("role_id")
                .build();

        assertThat(metadata.getRelationType()).isEqualTo(RelationType.MANY_TO_MANY);
        assertThat(metadata.getJoinTable()).isEqualTo("user_role");
        assertThat(metadata.getJoinColumn()).isEqualTo("user_id");
        assertThat(metadata.getInverseJoinColumn()).isEqualTo("role_id");
    }

    @Test
    void shouldReturnImmutableCascadeTypes() {
        Set<CascadeType> cascadeTypes = EnumSet.of(CascadeType.ALL);
        RelationMetadata metadata = RelationMetadataImpl.builder()
                .relationType(RelationType.ONE_TO_ONE)
                .entityClass(User.class)
                .targetEntityClass(UserProfile.class)
                .fieldName("profile")
                .foreignKeyColumn("profile_id")
                .cascadeTypes(cascadeTypes)
                .build();

        assertThat(metadata.getCascadeTypes()).isUnmodifiable();
    }
}
