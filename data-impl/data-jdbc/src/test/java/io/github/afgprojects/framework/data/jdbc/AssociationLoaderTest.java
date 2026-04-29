package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.relation.*;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AssociationLoader 单元测试
 * <p>
 * 直接测试 AssociationLoader 的各个方法，确保功能正确性。
 */
@DisplayName("AssociationLoader 单元测试")
class AssociationLoaderTest {

    private JdbcDataManager dataManager;
    private AssociationLoader associationLoader;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
        associationLoader = new AssociationLoader(new H2Dialect(), dataManager);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    // ==================== fetchAssociation 测试 ====================

    @Nested
    @DisplayName("fetchAssociation 测试")
    class FetchAssociationTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应正确加载 ManyToOne 关联")
        void shouldFetchManyToOne() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("研发部");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp = new Employee();
            emp.setName("张三");
            emp.setDepartmentId(dept.getId());
            emp = empProxy.insert(emp);

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            Object result = associationLoader.fetchAssociation(
                    emp, emp.getId(), relation, Employee.class, metadata);

            // Then
            assertThat(result).isInstanceOf(Department.class);
            Department loaded = (Department) result;
            assertThat(loaded.getId()).isEqualTo(dept.getId());
            assertThat(loaded.getName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("外键为 null 时应返回 null")
        void shouldReturnNullWhenForeignKeyIsNull() {
            // Given
            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp = new Employee();
            emp.setName("李四");
            emp.setDepartmentId(null);
            emp = empProxy.insert(emp);

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            Object result = associationLoader.fetchAssociation(
                    emp, emp.getId(), relation, Employee.class, metadata);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("应正确加载 OneToMany 关联")
        void shouldFetchOneToMany() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("研发部");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 3; i++) {
                Employee emp = new Employee();
                emp.setName("员工" + i);
                emp.setDepartmentId(dept.getId());
                empProxy.insert(emp);
            }

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            Object result = associationLoader.fetchAssociation(
                    dept, dept.getId(), relation, Department.class, metadata);

            // Then
            assertThat(result).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<Employee> employees = (List<Employee>) result;
            assertThat(employees).hasSize(3);
        }

        @Test
        @DisplayName("应正确加载 OneToMany 关联（拥有方）")
        void shouldFetchOneToManyOwningSide() {
            // Given - 创建表（使用 H2 默认的表名转换规则）
            // 当 OneToMany 没有指定 mappedBy 时，外键列名为 实体名_id
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE order_owning (id SERIAL PRIMARY KEY, order_no VARCHAR(100))");
                // 表需要包含实体字段和关联外键
                stmt.execute("CREATE TABLE order_item_owning (id SERIAL PRIMARY KEY, product_name VARCHAR(100), order_id BIGINT, order_owning_id BIGINT)");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            EntityProxy<OrderOwning> orderProxy = dataManager.entity(OrderOwning.class);
            OrderOwning order = new OrderOwning();
            order.setOrderNo("ORD-001");
            order = orderProxy.insert(order);

            EntityProxy<OrderItemOwning> itemProxy = dataManager.entity(OrderItemOwning.class);
            for (int i = 0; i < 2; i++) {
                OrderItemOwning item = new OrderItemOwning();
                item.setProductName("Product " + i);
                itemProxy.insert(item);
            }

            // 手动插入外键关联
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE order_item_owning SET order_owning_id = " + order.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            SimpleEntityMetadata<OrderOwning> metadata = new SimpleEntityMetadata<>(OrderOwning.class);
            RelationMetadata relation = metadata.getRelation("items").orElseThrow();

            // When - 拥有方查询
            Object result = associationLoader.fetchAssociation(
                    order, order.getId(), relation, OrderOwning.class, metadata);

            // Then
            assertThat(result).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<OrderItemOwning> items = (List<OrderItemOwning>) result;
            assertThat(items).hasSize(2);

            // Cleanup
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS order_item_owning");
                stmt.execute("DROP TABLE IF EXISTS order_owning");
            } catch (Exception ignored) {
            }
        }

        @Test
        @DisplayName("应正确加载 OneToOne 关联（拥有方）")
        void shouldFetchOneToOneOwningSide() {
            // Given
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            UserProfile profile = new UserProfile();
            profile.setBio("个人简介");
            profile = profileProxy.insert(profile);

            EntityProxy<UserAccount> accountProxy = dataManager.entity(UserAccount.class);
            UserAccount account = new UserAccount();
            account.setUsername("user1");
            account.setProfileId(profile.getId());
            account = accountProxy.insert(account);

            SimpleEntityMetadata<UserAccount> metadata = new SimpleEntityMetadata<>(UserAccount.class);
            RelationMetadata relation = metadata.getRelation("profile").orElseThrow();

            // When
            Object result = associationLoader.fetchAssociation(
                    account, account.getId(), relation, UserAccount.class, metadata);

            // Then
            assertThat(result).isInstanceOf(UserProfile.class);
            UserProfile loaded = (UserProfile) result;
            assertThat(loaded.getBio()).isEqualTo("个人简介");
        }

        @Test
        @DisplayName("应正确加载 ManyToMany 关联")
        void shouldFetchManyToMany() {
            // Given
            EntityProxy<Student> studentProxy = dataManager.entity(Student.class);
            EntityProxy<Course> courseProxy = dataManager.entity(Course.class);

            Student student = new Student();
            student.setName("学生A");
            student = studentProxy.insert(student);

            Course course1 = new Course();
            course1.setName("数学");
            Course course2 = new Course();
            course2.setName("英语");
            course1 = courseProxy.insert(course1);
            course2 = courseProxy.insert(course2);

            // 插入中间表
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student.getId() + ", " + course1.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student.getId() + ", " + course2.getId() + ")");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When
            Object result = associationLoader.fetchAssociation(
                    student, student.getId(), relation, Student.class, metadata);

            // Then
            assertThat(result).isInstanceOf(Set.class);
            @SuppressWarnings("unchecked")
            Set<Course> courses = (Set<Course>) result;
            assertThat(courses).hasSize(2);
        }
    }

    // ==================== fetchManyToOne 测试 ====================

    @Nested
    @DisplayName("fetchManyToOne 测试")
    class FetchManyToOneTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应正确加载关联实体")
        void shouldLoadRelatedEntity() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("技术部");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp = new Employee();
            emp.setName("王五");
            emp.setDepartmentId(dept.getId());
            emp = empProxy.insert(emp);

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            Object result = associationLoader.fetchManyToOne(emp, relation, Department.class, metadata);

            // Then
            assertThat(result).isNotNull();
            assertThat(((Department) result).getName()).isEqualTo("技术部");
        }

        @Test
        @DisplayName("关联实体不存在时应返回 null")
        void shouldReturnNullWhenNotFound() {
            // Given
            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp = new Employee();
            emp.setName("赵六");
            emp.setDepartmentId(99999L); // 不存在的 ID
            emp = empProxy.insert(emp);

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            Object result = associationLoader.fetchManyToOne(emp, relation, Department.class, metadata);

            // Then
            assertThat(result).isNull();
        }
    }

    // ==================== fetchOneToMany 测试 ====================

    @Nested
    @DisplayName("fetchOneToMany 测试")
    class FetchOneToManyTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应正确加载所有关联实体")
        void shouldLoadAllRelatedEntities() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("销售部");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 5; i++) {
                Employee emp = new Employee();
                emp.setName("销售员" + i);
                emp.setDepartmentId(dept.getId());
                empProxy.insert(emp);
            }

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            List<?> result = associationLoader.fetchOneToMany(dept.getId(), relation, Employee.class);

            // Then
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("无关联实体时应返回空列表")
        void shouldReturnEmptyListWhenNoRelations() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("空部门");
            dept = deptProxy.insert(dept);

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            List<?> result = associationLoader.fetchOneToMany(dept.getId(), relation, Employee.class);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== fetchManyToMany 测试 ====================

    @Nested
    @DisplayName("fetchManyToMany 测试")
    class FetchManyToManyTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应正确加载多对多关联")
        void shouldLoadManyToManyRelations() {
            // Given
            EntityProxy<Student> studentProxy = dataManager.entity(Student.class);
            EntityProxy<Course> courseProxy = dataManager.entity(Course.class);

            Student student = new Student();
            student.setName("学生B");
            student = studentProxy.insert(student);

            for (int i = 0; i < 3; i++) {
                Course course = new Course();
                course.setName("课程" + i);
                course = courseProxy.insert(course);

                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student.getId() + ", " + course.getId() + ")");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When
            Set<?> result = associationLoader.fetchManyToMany(student.getId(), relation, Course.class);

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("无关联时应返回空集合")
        void shouldReturnEmptySetWhenNoRelations() {
            // Given
            EntityProxy<Student> studentProxy = dataManager.entity(Student.class);
            Student student = new Student();
            student.setName("无课学生");
            student = studentProxy.insert(student);

            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When
            Set<?> result = associationLoader.fetchManyToMany(student.getId(), relation, Course.class);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== 批量加载测试 ====================

    @Nested
    @DisplayName("批量加载测试")
    class BatchFetchTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应正确批量加载 ManyToOne 关联")
        void shouldBatchFetchManyToOne() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept1 = new Department();
            dept1.setName("部门A");
            dept1 = deptProxy.insert(dept1);
            Department dept2 = new Department();
            dept2.setName("部门B");
            dept2 = deptProxy.insert(dept2);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp1 = new Employee();
            emp1.setName("员工1");
            emp1.setDepartmentId(dept1.getId());
            empProxy.insert(emp1);

            Employee emp2 = new Employee();
            emp2.setName("员工2");
            emp2.setDepartmentId(dept2.getId());
            empProxy.insert(emp2);

            Employee emp3 = new Employee();
            emp3.setName("员工3");
            emp3.setDepartmentId(null);
            empProxy.insert(emp3);

            List<Employee> employees = empProxy.findAll();

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            associationLoader.fetchAllManyToOne(employees, relation, Department.class, metadata);

            // Then
            assertThat(employees.get(0).getDepartment()).isNotNull();
            assertThat(employees.get(0).getDepartment().getName()).isEqualTo("部门A");
            assertThat(employees.get(1).getDepartment()).isNotNull();
            assertThat(employees.get(1).getDepartment().getName()).isEqualTo("部门B");
            assertThat(employees.get(2).getDepartment()).isNull();
        }

        @Test
        @DisplayName("应正确批量加载 OneToMany 关联")
        void shouldBatchFetchOneToMany() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept1 = new Department();
            dept1.setName("部门1");
            dept1 = deptProxy.insert(dept1);
            Department dept2 = new Department();
            dept2.setName("部门2");
            dept2 = deptProxy.insert(dept2);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 3; i++) {
                Employee emp = new Employee();
                emp.setName("员工" + i);
                emp.setDepartmentId(dept1.getId());
                empProxy.insert(emp);
            }
            for (int i = 0; i < 2; i++) {
                Employee emp = new Employee();
                emp.setName("员工B" + i);
                emp.setDepartmentId(dept2.getId());
                empProxy.insert(emp);
            }

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            Map<Object, List<Object>> result = associationLoader.fetchAllOneToMany(
                    List.of(dept1.getId(), dept2.getId()), relation, Employee.class);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(dept1.getId())).hasSize(3);
            assertThat(result.get(dept2.getId())).hasSize(2);
        }

        @Test
        @DisplayName("应正确批量加载 ManyToMany 关联")
        void shouldBatchFetchManyToMany() {
            // Given
            EntityProxy<Student> studentProxy = dataManager.entity(Student.class);
            EntityProxy<Course> courseProxy = dataManager.entity(Course.class);

            Student student1 = new Student();
            student1.setName("学生1");
            student1 = studentProxy.insert(student1);
            Student student2 = new Student();
            student2.setName("学生2");
            student2 = studentProxy.insert(student2);

            Course course1 = new Course();
            course1.setName("课程1");
            Course course2 = new Course();
            course2.setName("课程2");
            Course course3 = new Course();
            course3.setName("课程3");
            course1 = courseProxy.insert(course1);
            course2 = courseProxy.insert(course2);
            course3 = courseProxy.insert(course3);

            // 插入中间表
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student1.getId() + ", " + course1.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student1.getId() + ", " + course2.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student2.getId() + ", " + course3.getId() + ")");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When
            Map<Object, Set<Object>> result = associationLoader.fetchAllManyToMany(
                    List.of(student1.getId(), student2.getId()), relation, Course.class);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(student1.getId())).hasSize(2);
            assertThat(result.get(student2.getId())).hasSize(1);
        }

        @Test
        @DisplayName("应正确批量加载 OneToOne 关联（拥有方）")
        void shouldBatchFetchOneToOneOwningSide() {
            // Given
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            UserProfile profile1 = new UserProfile();
            profile1.setBio("简介1");
            profile1 = profileProxy.insert(profile1);
            UserProfile profile2 = new UserProfile();
            profile2.setBio("简介2");
            profile2 = profileProxy.insert(profile2);

            EntityProxy<UserAccount> accountProxy = dataManager.entity(UserAccount.class);
            UserAccount account1 = new UserAccount();
            account1.setUsername("user1");
            account1.setProfileId(profile1.getId());
            accountProxy.insert(account1);

            UserAccount account2 = new UserAccount();
            account2.setUsername("user2");
            account2.setProfileId(profile2.getId());
            accountProxy.insert(account2);

            List<UserAccount> accounts = accountProxy.findAll();

            SimpleEntityMetadata<UserAccount> metadata = new SimpleEntityMetadata<>(UserAccount.class);
            RelationMetadata relation = metadata.getRelation("profile").orElseThrow();

            // When
            associationLoader.fetchAllOneToOne(accounts, List.of(account1.getId(), account2.getId()),
                    relation, UserProfile.class, metadata);

            // Then
            assertThat(accounts.get(0).getProfile()).isNotNull();
            assertThat(accounts.get(0).getProfile().getBio()).isEqualTo("简介1");
            assertThat(accounts.get(1).getProfile()).isNotNull();
            assertThat(accounts.get(1).getProfile().getBio()).isEqualTo("简介2");
        }

        @Test
        @DisplayName("应正确批量加载 OneToOne 关联（非拥有方）")
        void shouldBatchFetchOneToOneNonOwningSide() {
            // Given
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            UserProfile profile1 = new UserProfile();
            profile1.setBio("简介A");
            profile1 = profileProxy.insert(profile1);
            UserProfile profile2 = new UserProfile();
            profile2.setBio("简介B");
            profile2 = profileProxy.insert(profile2);

            EntityProxy<UserAccount> accountProxy = dataManager.entity(UserAccount.class);
            UserAccount account1 = new UserAccount();
            account1.setUsername("userA");
            account1.setProfileId(profile1.getId());
            accountProxy.insert(account1);

            UserAccount account2 = new UserAccount();
            account2.setUsername("userB");
            account2.setProfileId(profile2.getId());
            accountProxy.insert(account2);

            List<UserProfile> profiles = profileProxy.findAll();

            SimpleEntityMetadata<UserProfile> metadata = new SimpleEntityMetadata<>(UserProfile.class);
            RelationMetadata relation = metadata.getRelation("account").orElseThrow();

            // When
            associationLoader.fetchAllOneToOne(profiles, List.of(profile1.getId(), profile2.getId()),
                    relation, UserAccount.class, metadata);

            // Then
            assertThat(profiles.get(0).getAccount()).isNotNull();
            assertThat(profiles.get(0).getAccount().getUsername()).isEqualTo("userA");
            assertThat(profiles.get(1).getAccount()).isNotNull();
            assertThat(profiles.get(1).getAccount().getUsername()).isEqualTo("userB");
        }
    }

    // ==================== 辅助方法测试 ====================

    @Nested
    @DisplayName("辅助方法测试")
    class HelperMethodTests {

        @Test
        @DisplayName("columnNameToFieldName 应正确转换")
        void shouldConvertColumnNameToFieldName() {
            assertThat(associationLoader.columnNameToFieldName("department_id")).isEqualTo("departmentId");
            assertThat(associationLoader.columnNameToFieldName("user_name")).isEqualTo("userName");
            assertThat(associationLoader.columnNameToFieldName("id")).isEqualTo("id");
        }

        @Test
        @DisplayName("inferTableName 应正确推断表名")
        void shouldInferTableName() {
            assertThat(associationLoader.inferTableName(Employee.class)).isEqualTo("employee");
            assertThat(associationLoader.inferTableName(UserProfile.class)).isEqualTo("user_profile");
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:assocloader;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createAssociationTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE department (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            stmt.execute("CREATE TABLE employee (id SERIAL PRIMARY KEY, name VARCHAR(100), department_id BIGINT)");
            stmt.execute("CREATE TABLE user_profile (id SERIAL PRIMARY KEY, bio VARCHAR(500))");
            stmt.execute("CREATE TABLE user_account (id SERIAL PRIMARY KEY, username VARCHAR(50), profile_id BIGINT)");
            stmt.execute("CREATE TABLE student (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            stmt.execute("CREATE TABLE course (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            stmt.execute("CREATE TABLE student_course (student_id BIGINT, course_id BIGINT)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS department");
            stmt.execute("DROP TABLE IF EXISTS employee");
            stmt.execute("DROP TABLE IF EXISTS user_profile");
            stmt.execute("DROP TABLE IF EXISTS user_account");
            stmt.execute("DROP TABLE IF EXISTS student");
            stmt.execute("DROP TABLE IF EXISTS course");
            stmt.execute("DROP TABLE IF EXISTS student_course");
        } catch (Exception ignored) {
        }
    }

    // ==================== 边界情况和异常分支测试 ====================

    @Nested
    @DisplayName("边界情况和异常分支测试")
    class EdgeCaseTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("fetchOneToOne 非拥有方应通过外键查询")
        void shouldFetchOneToOneNonOwningSide() {
            // Given - UserProfile 持有 mappedBy，为非拥有方
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            UserProfile profile = new UserProfile();
            profile.setBio("简介");
            profile = profileProxy.insert(profile);

            EntityProxy<UserAccount> accountProxy = dataManager.entity(UserAccount.class);
            UserAccount account = new UserAccount();
            account.setUsername("user");
            account.setProfileId(profile.getId());
            accountProxy.insert(account);

            SimpleEntityMetadata<UserProfile> metadata = new SimpleEntityMetadata<>(UserProfile.class);
            RelationMetadata relation = metadata.getRelation("account").orElseThrow();

            // When - 非拥有方查询
            Object result = associationLoader.fetchAssociation(
                    profile, profile.getId(), relation, UserProfile.class, metadata);

            // Then
            assertThat(result).isInstanceOf(UserAccount.class);
            UserAccount loaded = (UserAccount) result;
            assertThat(loaded.getUsername()).isEqualTo("user");
        }

        @Test
        @DisplayName("fetchOneToMany 非拥有方应通过 mappedBy 字段查询")
        void shouldFetchOneToManyNonOwningSide() {
            // Given - 创建带有 mappedBy 的关联
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("测试部门");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 2; i++) {
                Employee emp = new Employee();
                emp.setName("员工" + i);
                emp.setDepartmentId(dept.getId());
                empProxy.insert(emp);
            }

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When - 使用非拥有方查询（mappedBy 场景）
            List<?> result = associationLoader.fetchOneToMany(dept.getId(), relation, Employee.class);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("fetchAllManyToOne 空集合应正常处理")
        void shouldHandleEmptyCollectionForManyToOne() {
            // Given
            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When - 传入空列表
            associationLoader.fetchAllManyToOne(List.of(), relation, Department.class, metadata);

            // Then - 不应抛出异常
            // 测试通过即成功
        }

        @Test
        @DisplayName("fetchAllManyToOne 所有外键为 null 应正常处理")
        void shouldHandleAllNullForeignKeys() {
            // Given
            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp1 = new Employee();
            emp1.setName("无部门员工1");
            empProxy.insert(emp1);
            Employee emp2 = new Employee();
            emp2.setName("无部门员工2");
            empProxy.insert(emp2);

            List<Employee> employees = empProxy.findAll();

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            associationLoader.fetchAllManyToOne(employees, relation, Department.class, metadata);

            // Then - 所有员工的 department 应为 null
            assertThat(employees).allMatch(emp -> emp.getDepartment() == null);
        }

        @Test
        @DisplayName("setFieldValue 应能设置 null 值")
        void shouldSetNullValue() {
            // Given
            Employee emp = new Employee();
            emp.setName("测试");
            emp.setDepartment(new Department());

            // When
            associationLoader.setFieldValue(emp, "department", null);

            // Then
            assertThat(emp.getDepartment()).isNull();
        }

        @Test
        @DisplayName("fetchAllOneToOne 空集合应正常处理")
        void shouldHandleEmptyCollectionForOneToOne() {
            // Given
            SimpleEntityMetadata<UserAccount> metadata = new SimpleEntityMetadata<>(UserAccount.class);
            RelationMetadata relation = metadata.getRelation("profile").orElseThrow();

            // When
            associationLoader.fetchAllOneToOne(List.of(), List.of(), relation, UserProfile.class, metadata);

            // Then - 不应抛出异常
            // 测试通过即成功
        }

        @Test
        @DisplayName("fetchAllOneToMany 无匹配数据应返回空映射")
        void shouldReturnEmptyMapWhenNoMatches() {
            // Given - 使用不存在的 ID
            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            Map<Object, List<Object>> result = associationLoader.fetchAllOneToMany(
                    List.of(99999L, 88888L), relation, Employee.class);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("fetchAllManyToMany 无匹配数据应返回空映射")
        void shouldReturnEmptyMapForNoManyToManyMatches() {
            // Given
            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When - 使用不存在的 ID
            Map<Object, Set<Object>> result = associationLoader.fetchAllManyToMany(
                    List.of(99999L), relation, Course.class);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("fetchAllManyToOne 部分外键不存在目标实体应正常处理")
        void shouldHandleMissingTargetEntities() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("存在部门");
            dept = deptProxy.insert(dept);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            Employee emp1 = new Employee();
            emp1.setName("员工1");
            emp1.setDepartmentId(dept.getId());
            empProxy.insert(emp1);

            Employee emp2 = new Employee();
            emp2.setName("员工2");
            emp2.setDepartmentId(99999L); // 不存在的部门
            empProxy.insert(emp2);

            List<Employee> employees = empProxy.findAll();

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            associationLoader.fetchAllManyToOne(employees, relation, Department.class, metadata);

            // Then
            assertThat(employees.get(0).getDepartment()).isNotNull();
            assertThat(employees.get(1).getDepartment()).isNull();
        }

        @Test
        @DisplayName("fetchAllOneToOne 非拥有方无匹配数据应设置 null")
        void shouldHandleNonOwningSideNoMatch() {
            // Given
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            UserProfile profile = new UserProfile();
            profile.setBio("独立简介");
            profile = profileProxy.insert(profile);

            List<UserProfile> profiles = profileProxy.findAll();

            SimpleEntityMetadata<UserProfile> metadata = new SimpleEntityMetadata<>(UserProfile.class);
            RelationMetadata relation = metadata.getRelation("account").orElseThrow();

            // When - 没有 UserAccount 关联
            associationLoader.fetchAllOneToOne(profiles, List.of(profile.getId()),
                    relation, UserAccount.class, metadata);

            // Then
            assertThat(profiles.get(0).getAccount()).isNull();
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("findForeignKeyField 未找到外键字段应抛出异常")
        void shouldThrowWhenForeignKeyFieldNotFound() {
            // Given - 创建一个没有正确外键的实体
            SimpleEntityMetadata<InvalidEntity> metadata = new SimpleEntityMetadata<>(InvalidEntity.class);

            // 创建一个模拟的 RelationMetadata
            RelationMetadata invalidRelation = new RelationMetadata() {
                @Override
                public RelationType getRelationType() {
                    return RelationType.MANY_TO_ONE;
                }

                @Override
                public Class<?> getEntityClass() {
                    return InvalidEntity.class;
                }

                @Override
                public Class<?> getTargetEntityClass() {
                    return Department.class;
                }

                @Override
                public String getFieldName() {
                    return "department";
                }

                @Override
                public String getMappedBy() {
                    return null;
                }

                @Override
                public String getForeignKeyColumn() {
                    return "non_existent_fk"; // 不存在的列
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
                    return Collections.emptySet();
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
            };

            // When & Then
            assertThatThrownBy(() -> associationLoader.findForeignKeyField(invalidRelation, metadata))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Foreign key field not found");
        }

        @Test
        @DisplayName("setFieldValue 字段不存在应抛出异常")
        void shouldThrowWhenFieldNotFound() {
            // Given
            Employee emp = new Employee();
            emp.setName("测试");

            // When & Then
            assertThatThrownBy(() -> associationLoader.setFieldValue(emp, "nonExistentField", "value"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to set association field");
        }
    }

    // ==================== 复杂场景测试 ====================

    @Nested
    @DisplayName("复杂场景测试")
    class ComplexScenarioTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("fetchAllManyToOne 重复外键应正确处理")
        void shouldHandleDuplicateForeignKeys() {
            // Given - 多个员工关联同一个部门
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept = new Department();
            dept.setName("公共部门");
            Department insertedDept = deptProxy.insert(dept);
            final Long deptId = insertedDept.getId();

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 5; i++) {
                Employee emp = new Employee();
                emp.setName("员工" + i);
                emp.setDepartmentId(deptId); // 相同的外键
                empProxy.insert(emp);
            }

            List<Employee> employees = empProxy.findAll();

            SimpleEntityMetadata<Employee> metadata = new SimpleEntityMetadata<>(Employee.class);
            RelationMetadata relation = metadata.getRelation("department").orElseThrow();

            // When
            associationLoader.fetchAllManyToOne(employees, relation, Department.class, metadata);

            // Then - 所有员工应该关联到同一个部门
            assertThat(employees).allMatch(emp -> emp.getDepartment() != null);
            assertThat(employees).allMatch(emp -> emp.getDepartment().getId().equals(deptId));
        }

        @Test
        @DisplayName("fetchAllManyToMany 多对多复杂关联应正确加载")
        void shouldHandleComplexManyToManyRelations() {
            // Given - 创建复杂的多对多关联
            EntityProxy<Student> studentProxy = dataManager.entity(Student.class);
            EntityProxy<Course> courseProxy = dataManager.entity(Course.class);

            Student student1 = new Student();
            student1.setName("学生1");
            student1 = studentProxy.insert(student1);
            Student student2 = new Student();
            student2.setName("学生2");
            student2 = studentProxy.insert(student2);
            Student student3 = new Student();
            student3.setName("学生3");
            student3 = studentProxy.insert(student3);

            Course course1 = new Course();
            course1.setName("数学");
            Course course2 = new Course();
            course2.setName("英语");
            Course course3 = new Course();
            course3.setName("物理");
            course1 = courseProxy.insert(course1);
            course2 = courseProxy.insert(course2);
            course3 = courseProxy.insert(course3);

            // 创建复杂的关联关系
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // 学生1 选了数学和英语
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student1.getId() + ", " + course1.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student1.getId() + ", " + course2.getId() + ")");
                // 学生2 选了英语和物理
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student2.getId() + ", " + course2.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student2.getId() + ", " + course3.getId() + ")");
                // 学生3 选了数学、英语和物理
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student3.getId() + ", " + course1.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student3.getId() + ", " + course2.getId() + ")");
                stmt.execute("INSERT INTO student_course (student_id, course_id) VALUES (" + student3.getId() + ", " + course3.getId() + ")");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            SimpleEntityMetadata<Student> metadata = new SimpleEntityMetadata<>(Student.class);
            RelationMetadata relation = metadata.getRelation("courses").orElseThrow();

            // When
            Map<Object, Set<Object>> result = associationLoader.fetchAllManyToMany(
                    List.of(student1.getId(), student2.getId(), student3.getId()),
                    relation, Course.class);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(student1.getId())).hasSize(2);
            assertThat(result.get(student2.getId())).hasSize(2);
            assertThat(result.get(student3.getId())).hasSize(3);
        }

        @Test
        @DisplayName("fetchAllOneToMany 部分部门无员工应正确处理")
        void shouldHandlePartialEmptyOneToMany() {
            // Given
            EntityProxy<Department> deptProxy = dataManager.entity(Department.class);
            Department dept1 = new Department();
            dept1.setName("有员工部门");
            dept1 = deptProxy.insert(dept1);
            Department dept2 = new Department();
            dept2.setName("无员工部门");
            dept2 = deptProxy.insert(dept2);

            EntityProxy<Employee> empProxy = dataManager.entity(Employee.class);
            for (int i = 0; i < 3; i++) {
                Employee emp = new Employee();
                emp.setName("员工" + i);
                emp.setDepartmentId(dept1.getId());
                empProxy.insert(emp);
            }

            SimpleEntityMetadata<Department> metadata = new SimpleEntityMetadata<>(Department.class);
            RelationMetadata relation = metadata.getRelation("employees").orElseThrow();

            // When
            Map<Object, List<Object>> result = associationLoader.fetchAllOneToMany(
                    List.of(dept1.getId(), dept2.getId()), relation, Employee.class);

            // Then
            assertThat(result).hasSize(1); // 只有 dept1 有员工
            assertThat(result.get(dept1.getId())).hasSize(3);
            assertThat(result.get(dept2.getId())).isNull();
        }
    }

    // ==================== 测试实体 ====================

    @Data
    @NoArgsConstructor
    static class Department {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "department")
        private List<Employee> employees;
    }

    @Data
    @NoArgsConstructor
    static class Employee {
        private Long id;
        private String name;
        private Long departmentId;

        @ManyToOne
        private Department department;
    }

    @Data
    @NoArgsConstructor
    static class UserProfile {
        private Long id;
        private String bio;

        @OneToOne(mappedBy = "profile")
        private UserAccount account;
    }

    @Data
    @NoArgsConstructor
    static class UserAccount {
        private Long id;
        private String username;
        private Long profileId;

        @OneToOne
        private UserProfile profile;
    }

    @Data
    @NoArgsConstructor
    static class Student {
        private Long id;
        private String name;

        @ManyToMany
        private Set<Course> courses;
    }

    @Data
    @NoArgsConstructor
    static class Course {
        private Long id;
        private String name;

        @ManyToMany(mappedBy = "courses")
        private Set<Student> students;
    }

    // 用于测试异常场景的无效实体
    @Data
    @NoArgsConstructor
    static class InvalidEntity {
        private Long id;
        private String name;
        // 没有 department 或 departmentId 字段
    }

    // ==================== OneToMany 拥有方测试实体 ====================

    /**
     * 订单实体（OneToMany 拥有方 - 没有 mappedBy）
     */
    @Data
    @NoArgsConstructor
    static class OrderOwning {
        private Long id;
        private String orderNo;

        // 没有 mappedBy，表示这是拥有方
        @OneToMany
        private List<OrderItemOwning> items;
    }

    /**
     * 订单项实体
     */
    @Data
    @NoArgsConstructor
    static class OrderItemOwning {
        private Long id;
        private String productName;
        private Long orderId;  // 外键在子表中
    }
}
