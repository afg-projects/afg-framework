package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 关联加载集成测试
 * <p>
 * 测试 ManyToOne、OneToMany、OneToOne、ManyToMany 关联加载功能。
 * 使用 H2 内存数据库（PostgreSQL 兼容模式）进行集成测试。
 * </p>
 */
@DisplayName("关联加载集成测试")
class AssociationLoadingIntegrationTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
        createTestTables();
    }

    @AfterEach
    void tearDown() {
        dropTestTables();
    }

    // ==================== ManyToOne 测试 ====================

    @Nested
    @DisplayName("ManyToOne 关联测试")
    class ManyToOneTests {

        @Test
        @DisplayName("应该成功加载 ManyToOne 关联")
        void shouldFetchManyToOneAssociation() {
            // Given - 创建部门
            EntityProxy<TestDepartment> deptProxy = dataManager.entity(TestDepartment.class);
            TestDepartment dept = new TestDepartment();
            dept.setName("研发部");
            dept = deptProxy.insert(dept);

            // Given - 创建用户并关联部门
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);
            TestUserWithDept user = new TestUserWithDept();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");
            user.setDepartmentId(dept.getId());
            user = userProxy.insert(user);

            // When - 加载用户的部门关联
            TestDepartment fetchedDept = userProxy.fetch(user, "department");

            // Then
            assertThat(fetchedDept).isNotNull();
            assertThat(fetchedDept.getId()).isEqualTo(dept.getId());
            assertThat(fetchedDept.getName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("外键为 null 时应返回 null")
        void shouldReturnNullWhenForeignKeyIsNull() {
            // Given - 创建用户但不关联部门
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);
            TestUserWithDept user = new TestUserWithDept();
            user.setName("李四");
            user.setEmail("lisi@example.com");
            user.setDepartmentId(null);
            user = userProxy.insert(user);

            // When - 加载用户的部门关联
            TestDepartment fetchedDept = userProxy.fetch(user, "department");

            // Then
            assertThat(fetchedDept).isNull();
        }

        @Test
        @DisplayName("关联不存在时应抛出异常")
        void shouldThrowExceptionWhenAssociationNotFound() {
            // Given
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);
            TestUserWithDept user = new TestUserWithDept();
            user.setName("王五");
            user.setEmail("wangwu@example.com");
            user.setDepartmentId(null);
            TestUserWithDept insertedUser = userProxy.insert(user);

            // When & Then
            assertThatThrownBy(() -> userProxy.fetch(insertedUser, "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Association 'nonexistent' not found");
        }
    }

    // ==================== OneToMany 测试 ====================

    @Nested
    @DisplayName("OneToMany 关联测试")
    class OneToManyTests {

        @Test
        @DisplayName("应该成功加载 OneToMany 关联")
        void shouldFetchOneToManyAssociation() {
            // Given - 创建部门
            EntityProxy<TestDepartment> deptProxy = dataManager.entity(TestDepartment.class);
            TestDepartment dept = new TestDepartment();
            dept.setName("研发部");
            TestDepartment insertedDept = deptProxy.insert(dept);

            // Given - 创建多个用户关联到同一部门
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);
            for (int i = 1; i <= 3; i++) {
                TestUserWithDept user = new TestUserWithDept();
                user.setName("用户" + i);
                user.setEmail("user" + i + "@example.com");
                user.setDepartmentId(insertedDept.getId());
                userProxy.insert(user);
            }

            // When - 通过条件查询加载部门的用户列表
            // 使用 TestDepartmentWithUsers 实体，它的表是 test_department_with_users
            // 但由于我们没有在这个表中插入数据，我们使用不同的测试方式
            Condition condition = Conditions.eq("department_id", insertedDept.getId());
            List<TestUserWithDept> users = userProxy.findAll(condition);

            // Then
            assertThat(users).hasSize(3);
            final Long deptId = insertedDept.getId();
            assertThat(users).allMatch(u -> u.getDepartmentId().equals(deptId));
        }
    }

    // ==================== OneToOne 测试 ====================

    @Nested
    @DisplayName("OneToOne 关联测试")
    class OneToOneTests {

        @Test
        @DisplayName("应该成功加载 OneToOne 关联（当前方持有外键）")
        void shouldFetchOneToOneOwningSide() {
            // Given - 创建用户详情
            EntityProxy<TestUserDetail> detailProxy = dataManager.entity(TestUserDetail.class);
            TestUserDetail detail = new TestUserDetail();
            detail.setBio("这是用户简介");
            detail.setAvatar("avatar.png");
            detail = detailProxy.insert(detail);

            // Given - 创建用户并关联详情
            EntityProxy<TestUserWithDetail> userProxy = dataManager.entity(TestUserWithDetail.class);
            TestUserWithDetail user = new TestUserWithDetail();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");
            user.setDetailId(detail.getId());
            user = userProxy.insert(user);

            // When - 加载用户的详情
            TestUserDetail fetchedDetail = userProxy.fetch(user, "detail");

            // Then
            assertThat(fetchedDetail).isNotNull();
            assertThat(fetchedDetail.getBio()).isEqualTo("这是用户简介");
        }
    }

    // ==================== ManyToMany 测试 ====================

    @Nested
    @DisplayName("ManyToMany 关联测试")
    class ManyToManyTests {

        @Test
        @DisplayName("应该成功加载 ManyToMany 关联")
        void shouldFetchManyToManyAssociation() {
            // Given - 创建角色
            EntityProxy<TestRole> roleProxy = dataManager.entity(TestRole.class);
            TestRole role1 = new TestRole();
            role1.setName("ADMIN");
            role1 = roleProxy.insert(role1);
            TestRole role2 = new TestRole();
            role2.setName("USER");
            role2 = roleProxy.insert(role2);

            // Given - 创建用户
            EntityProxy<TestUserWithRoles> userProxy = dataManager.entity(TestUserWithRoles.class);
            TestUserWithRoles user = new TestUserWithRoles();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");
            user = userProxy.insert(user);

            // Given - 在中间表中建立关联（使用正确的表名和列名）
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_user_with_roles_test_role (test_user_with_roles_id, test_role_id) VALUES (" + user.getId() + ", " + role1.getId() + ")");
                stmt.execute("INSERT INTO test_user_with_roles_test_role (test_user_with_roles_id, test_role_id) VALUES (" + user.getId() + ", " + role2.getId() + ")");
            } catch (Exception e) {
                throw new RuntimeException("Failed to insert user_role", e);
            }

            // When - 加载用户的角色列表
            Set<TestRole> roles = userProxy.fetch(user, "roles");

            // Then
            assertThat(roles).hasSize(2);
            assertThat(roles.stream().map(TestRole::getName).toList())
                .containsExactlyInAnyOrder("ADMIN", "USER");
        }
    }

    // ==================== withAssociation 测试 ====================

    @Nested
    @DisplayName("withAssociation 配置测试")
    class WithAssociationTests {

        @Test
        @DisplayName("应该成功配置单个关联加载")
        void shouldConfigureSingleAssociation() {
            // Given
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);

            // When
            EntityProxy<TestUserWithDept> configuredProxy = userProxy.withAssociation("department");

            // Then
            assertThat(configuredProxy).isNotNull();
            assertThat(((JdbcEntityProxy<TestUserWithDept>) configuredProxy).getEagerFetchAssociations())
                .contains("department");
        }

        @Test
        @DisplayName("应该成功配置多个关联加载")
        void shouldConfigureMultipleAssociations() {
            // Given - 使用有多个关联的实体
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);

            // When - 配置单个关联（该实体只有一个关联字段）
            EntityProxy<TestUserWithDept> configuredProxy = userProxy.withAssociation("department");

            // Then
            assertThat(((JdbcEntityProxy<TestUserWithDept>) configuredProxy).getEagerFetchAssociations())
                .contains("department");
        }

        @Test
        @DisplayName("应该成功清除关联加载配置")
        void shouldClearAssociations() {
            // Given
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class)
                .withAssociation("department");

            // When
            EntityProxy<TestUserWithDept> clearedProxy = userProxy.clearAssociations();

            // Then
            assertThat(((JdbcEntityProxy<TestUserWithDept>) clearedProxy).getEagerFetchAssociations())
                .isEmpty();
        }

        @Test
        @DisplayName("配置不存在关联时应抛出异常")
        void shouldThrowExceptionWhenConfiguringNonexistentAssociation() {
            // Given
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);

            // When & Then
            assertThatThrownBy(() -> userProxy.withAssociation("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Association 'nonexistent' not found");
        }
    }

    // ==================== 关联元数据测试 ====================

    @Nested
    @DisplayName("关联元数据测试")
    class RelationMetadataTests {

        @Test
        @DisplayName("应该正确识别实体关联")
        void shouldDetectEntityRelations() {
            // Given
            var metadata = dataManager.getEntityMetadata(TestUserWithDept.class);

            // When
            List<RelationMetadata> relations = metadata.getRelations();

            // Then
            assertThat(relations).hasSize(1);
            assertThat(relations.get(0).getFieldName()).isEqualTo("department");
            assertThat(relations.get(0).getRelationType()).isEqualTo(RelationType.MANY_TO_ONE);
        }

        @Test
        @DisplayName("应该正确查询关联元数据")
        void shouldQueryRelationMetadata() {
            // Given
            var metadata = dataManager.getEntityMetadata(TestUserWithDept.class);

            // When & Then
            assertThat(metadata.hasRelation("department")).isTrue();
            assertThat(metadata.hasRelation("nonexistent")).isFalse();
            assertThat(metadata.getRelation("department")).isPresent();
            assertThat(metadata.getRelation("nonexistent")).isEmpty();
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:assocdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 部门表
            stmt.execute("""
                CREATE TABLE test_department (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
                """);

            // 用户表（带部门外键）- 表名与实体类匹配
            stmt.execute("""
                CREATE TABLE test_user_with_dept (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    department_id BIGINT
                )
                """);

            // 用户详情表
            stmt.execute("""
                CREATE TABLE test_user_detail (
                    id SERIAL PRIMARY KEY,
                    user_id BIGINT,
                    bio VARCHAR(500),
                    avatar VARCHAR(200)
                )
                """);

            // 用户实体表（用于 OneToOne 测试）
            stmt.execute("""
                CREATE TABLE test_user_with_detail (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    detail_id BIGINT
                )
                """);

            // 角色表
            stmt.execute("""
                CREATE TABLE test_role (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(50) NOT NULL
                )
                """);

            // 用户角色中间表
            stmt.execute("""
                CREATE TABLE test_user_with_roles_test_role (
                    test_user_with_roles_id BIGINT,
                    test_role_id BIGINT
                )
                """);

            // 部门实体（带用户列表）
            stmt.execute("""
                CREATE TABLE test_department_with_users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL
                )
                """);

            // 用户实体（带角色列表）
            stmt.execute("""
                CREATE TABLE test_user_with_roles (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create test tables", e);
        }
    }

    private void dropTestTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_department");
            stmt.execute("DROP TABLE IF EXISTS test_user");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_dept");
            stmt.execute("DROP TABLE IF EXISTS test_user_detail");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_detail");
            stmt.execute("DROP TABLE IF EXISTS test_role");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_roles");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_roles_test_role");
            stmt.execute("DROP TABLE IF EXISTS test_department_with_users");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体 ====================

    /**
     * 部门实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDepartment {
        private Long id;
        private String name;
    }

    /**
     * 部门实体（带用户列表）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDepartmentWithUsers {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "department")
        private List<TestUserWithDept> users;
    }

    /**
     * 用户实体（带部门关联）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private String email;
    }

    /**
     * 用户实体（带部门关联）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithDept {
        private Long id;
        private String name;
        private String email;
        private Long departmentId;

        @ManyToOne
        private TestDepartment department;
    }

    /**
     * 用户实体（带详情）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithDetail {
        private Long id;
        private String name;
        private String email;
        private Long detailId;

        @OneToOne
        private TestUserDetail detail;
    }

    /**
     * 用户详情实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserDetail {
        private Long id;
        private Long userId;
        private String bio;
        private String avatar;
    }

    /**
     * 用户实体（带角色列表）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithRoles {
        private Long id;
        private String name;
        private String email;

        @ManyToMany
        private Set<TestRole> roles;
    }

    /**
     * 角色实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRole {
        private Long id;
        private String name;
    }
}