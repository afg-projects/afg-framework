package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.relation.ManyToMany;
import io.github.afgprojects.framework.data.core.relation.ManyToOne;
import io.github.afgprojects.framework.data.core.relation.OneToMany;
import io.github.afgprojects.framework.data.core.relation.OneToOne;
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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 全面集成测试
 * <p>
 * 覆盖更多复杂场景：并发、批量操作边界、复杂查询、关联加载等
 */
@DisplayName("全面集成测试")
class ComprehensiveIntegrationTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    // ==================== 并发测试 ====================

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @BeforeEach
        void setUp() {
            createCounterTable();
        }

        @Test
        @DisplayName("应该正确处理并发插入")
        void shouldHandleConcurrentInserts() throws InterruptedException {
            // Given
            EntityProxy<Counter> counterProxy = dataManager.entity(Counter.class);
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        Counter counter = new Counter();
                        counter.setName("COUNTER_" + index);
                        counter.setCounterValue(1);
                        counterProxy.insert(counter);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executor.shutdown();

            // Then
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(counterProxy.count()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("应该正确处理并发查询")
        void shouldHandleConcurrentQueries() throws InterruptedException {
            // Given
            EntityProxy<Counter> counterProxy = dataManager.entity(Counter.class);
            for (int i = 0; i < 100; i++) {
                Counter counter = new Counter();
                counter.setName("QUERY_" + i);
                counter.setCounterValue(i);
                counterProxy.insert(counter);
            }

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount * 10);
            AtomicInteger queryCount = new AtomicInteger(0);

            // When
            for (int t = 0; t < threadCount; t++) {
                for (int q = 0; q < 10; q++) {
                    executor.submit(() -> {
                        try {
                            counterProxy.findAll();
                            queryCount.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }
            latch.await();
            executor.shutdown();

            // Then
            assertThat(queryCount.get()).isEqualTo(threadCount * 10);
        }

        private void createCounterTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE counter (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        counter_value INT NOT NULL
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create counter table", e);
            }
        }
    }

    // ==================== 批量操作边界测试 ====================

    @Nested
    @DisplayName("批量操作边界测试")
    class BatchOperationBoundaryTests {

        @BeforeEach
        void setUp() {
            createProductTable();
        }

        @Test
        @DisplayName("应该正确处理空列表批量插入")
        void shouldHandleEmptyBatchInsert() {
            // Given
            EntityProxy<Product> productProxy = dataManager.entity(Product.class);

            // When
            List<Product> result = productProxy.insertAll(List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该正确处理单条记录批量插入")
        void shouldHandleSingleRecordBatchInsert() {
            // Given
            EntityProxy<Product> productProxy = dataManager.entity(Product.class);
            Product product = new Product();
            product.setCode("SINGLE");
            product.setName("Single Product");
            product.setPrice(99.99);

            // When
            List<Product> result = productProxy.insertAll(List.of(product));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isNotNull();
        }

        @Test
        @DisplayName("应该正确处理大批量插入")
        void shouldHandleLargeBatchInsert() {
            // Given
            EntityProxy<Product> productProxy = dataManager.entity(Product.class);
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Product p = new Product();
                p.setCode("BATCH_" + i);
                p.setName("Product " + i);
                p.setPrice(10.0 + i);
                products.add(p);
            }

            // When
            List<Product> result = productProxy.insertAll(products);

            // Then
            assertThat(result).hasSize(1000);
            assertThat(result).allMatch(p -> p.getId() != null);
        }

        @Test
        @DisplayName("应该正确处理批量更新")
        void shouldHandleBatchUpdate() {
            // Given
            EntityProxy<Product> productProxy = dataManager.entity(Product.class);
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Product p = new Product();
                p.setCode("UPD_" + i);
                p.setName("Original " + i);
                p.setPrice(100.0);
                products.add(productProxy.insert(p));
            }

            // When
            products.forEach(p -> p.setPrice(p.getPrice() + 50.0));
            List<Product> updated = productProxy.updateAll(products);

            // Then
            assertThat(updated).hasSize(10);
            assertThat(updated).allMatch(p -> p.getPrice() == 150.0);
        }

        @Test
        @DisplayName("应该正确处理批量删除")
        void shouldHandleBatchDelete() {
            // Given
            EntityProxy<Product> productProxy = dataManager.entity(Product.class);
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Product p = new Product();
                p.setCode("DEL_" + i);
                p.setName("Delete " + i);
                p.setPrice(50.0);
                products.add(productProxy.insert(p));
            }

            // When
            productProxy.deleteAll(products);

            // Then
            assertThat(productProxy.count()).isEqualTo(0);
        }

        private void createProductTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE product (
                        id SERIAL PRIMARY KEY,
                        code VARCHAR(50) NOT NULL,
                        name VARCHAR(200) NOT NULL,
                        price DECIMAL(19,2) NOT NULL
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create product table", e);
            }
        }
    }

    // ==================== 复杂查询测试 ====================

    @Nested
    @DisplayName("复杂查询测试")
    class ComplexQueryTests {

        @BeforeEach
        void setUp() {
            createOrderTable();
        }

        @Test
        @DisplayName("应该正确处理多条件组合查询")
        void shouldHandleMultipleConditions() {
            // Given
            EntityProxy<TestOrder> orderProxy = dataManager.entity(TestOrder.class);
            for (int i = 0; i < 20; i++) {
                String status = i < 5 ? "PENDING" : (i < 12 ? "COMPLETED" : "CANCELLED");
                TestOrder order = new TestOrder();
                order.setOrderNo("ORD_" + i);
                order.setStatus(status);
                order.setAmount(java.math.BigDecimal.valueOf(100.0 * (i + 1)));
                orderProxy.insert(order);
            }

            // When
            Condition condition = Conditions.builder()
                .eq("status", "COMPLETED")
                .ge("amount", java.math.BigDecimal.valueOf(700.0))
                .build();
            List<TestOrder> results = orderProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(6); // ORD_6 to ORD_11 with COMPLETED and amount >= 700
        }

        @Test
        @DisplayName("应该正确处理 BETWEEN 条件")
        void shouldHandleBetweenCondition() {
            // Given
            EntityProxy<TestOrder> orderProxy = dataManager.entity(TestOrder.class);
            for (int i = 0; i < 10; i++) {
                TestOrder order = new TestOrder();
                order.setOrderNo("ORD_" + i);
                order.setStatus("NEW");
                order.setAmount(java.math.BigDecimal.valueOf(100.0 * (i + 1)));
                orderProxy.insert(order);
            }

            // When
            Condition condition = Conditions.builder()
                .between("amount", java.math.BigDecimal.valueOf(300.0), java.math.BigDecimal.valueOf(700.0))
                .build();
            List<TestOrder> results = orderProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(5);
        }

        @Test
        @DisplayName("应该正确处理 IN 条件")
        void shouldHandleInCondition() {
            // Given
            EntityProxy<TestOrder> orderProxy = dataManager.entity(TestOrder.class);
            for (String status : List.of("A", "B", "C", "D")) {
                TestOrder order = new TestOrder();
                order.setOrderNo("ORD_" + status);
                order.setStatus(status);
                order.setAmount(java.math.BigDecimal.valueOf(100.0));
                orderProxy.insert(order);
            }

            // When
            Condition condition = Conditions.in("status", List.of("A", "B", "C"));
            List<TestOrder> results = orderProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("应该正确处理 IS NULL 条件")
        void shouldHandleIsNullCondition() {
            // Given
            EntityProxy<TestOrder> orderProxy = dataManager.entity(TestOrder.class);
            TestOrder o1 = new TestOrder();
            o1.setOrderNo("ORD_1");
            o1.setStatus("NEW");
            o1.setAmount(java.math.BigDecimal.valueOf(100.0));
            orderProxy.insert(o1);

            TestOrder o2 = new TestOrder();
            o2.setOrderNo("ORD_2");
            o2.setStatus(null);
            o2.setAmount(java.math.BigDecimal.valueOf(200.0));
            orderProxy.insert(o2);

            // When
            Condition nullCondition = Conditions.builder().isNull("status").build();
            Condition notNullCondition = Conditions.builder().isNotNull("status").build();
            List<TestOrder> nullStatus = orderProxy.findAll(nullCondition);
            List<TestOrder> notNullStatus = orderProxy.findAll(notNullCondition);

            // Then
            assertThat(nullStatus).hasSize(1);
            assertThat(notNullStatus).hasSize(1);
        }

        private void createOrderTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE test_order (
                        id SERIAL PRIMARY KEY,
                        order_no VARCHAR(50) NOT NULL,
                        status VARCHAR(20),
                        amount DECIMAL(19,2) NOT NULL
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create orders table", e);
            }
        }
    }

    // ==================== 关联加载全面测试 ====================

    @Nested
    @DisplayName("关联加载全面测试")
    class AssociationLoadingComprehensiveTests {

        @BeforeEach
        void setUp() {
            createAssociationTables();
        }

        @Test
        @DisplayName("应该正确加载 ManyToOne 关联")
        void shouldLoadManyToOneAssociation() {
            // Given
            EntityProxy<Author> authorProxy = dataManager.entity(Author.class);
            EntityProxy<Book> bookProxy = dataManager.entity(Book.class);

            Author author = new Author();
            author.setName("张三");
            author = authorProxy.insert(author);

            Book book = new Book();
            book.setTitle("Java编程");
            book.setAuthorId(author.getId());
            book = bookProxy.insert(book);

            // When
            Author loadedAuthor = bookProxy.fetch(book, "author");

            // Then
            assertThat(loadedAuthor).isNotNull();
            assertThat(loadedAuthor.getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("应该正确加载 OneToMany 关联")
        void shouldLoadOneToManyAssociation() {
            // Given
            EntityProxy<Category> categoryProxy = dataManager.entity(Category.class);
            EntityProxy<Item> itemProxy = dataManager.entity(Item.class);

            Category category = new Category();
            category.setName("技术书籍");
            category = categoryProxy.insert(category);

            for (int i = 0; i < 3; i++) {
                Item item = new Item();
                item.setName("书籍" + i);
                item.setCategoryId(category.getId());
                itemProxy.insert(item);
            }

            // When
            List<Item> items = categoryProxy.fetch(category, "items");

            // Then
            assertThat(items).hasSize(3);
        }

        @Test
        @DisplayName("应该正确加载 OneToOne 关联")
        void shouldLoadOneToOneAssociation() {
            // Given
            EntityProxy<UserProfile> profileProxy = dataManager.entity(UserProfile.class);
            EntityProxy<UserAccount> accountProxy = dataManager.entity(UserAccount.class);

            UserProfile profile = new UserProfile();
            profile.setBio("这是个人简介");
            profile = profileProxy.insert(profile);

            UserAccount account = new UserAccount();
            account.setUsername("user1");
            account.setProfileId(profile.getId());
            account = accountProxy.insert(account);

            // When
            UserProfile loadedProfile = accountProxy.fetch(account, "profile");

            // Then
            assertThat(loadedProfile).isNotNull();
            assertThat(loadedProfile.getBio()).isEqualTo("这是个人简介");
        }

        @Test
        @DisplayName("应该正确加载 ManyToMany 关联")
        void shouldLoadManyToManyAssociation() {
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

            // When
            Set<Course> courses = studentProxy.fetch(student, "courses");

            // Then
            assertThat(courses).hasSize(2);
        }

        @Test
        @DisplayName("外键为 null 时应返回 null")
        void shouldReturnNullWhenForeignKeyIsNull() {
            // Given
            EntityProxy<Book> bookProxy = dataManager.entity(Book.class);
            Book book = new Book();
            book.setTitle("无作者书籍");
            book.setAuthorId(null);
            book = bookProxy.insert(book);

            // When
            Author author = bookProxy.fetch(book, "author");

            // Then
            assertThat(author).isNull();
        }

        private void createAssociationTables() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // Author 表
                stmt.execute("CREATE TABLE author (id SERIAL PRIMARY KEY, name VARCHAR(100))");

                // Book 表
                stmt.execute("CREATE TABLE book (id SERIAL PRIMARY KEY, title VARCHAR(200), author_id BIGINT)");

                // Category 表
                stmt.execute("CREATE TABLE category (id SERIAL PRIMARY KEY, name VARCHAR(100))");

                // Item 表
                stmt.execute("CREATE TABLE item (id SERIAL PRIMARY KEY, name VARCHAR(100), category_id BIGINT)");

                // UserProfile 表
                stmt.execute("CREATE TABLE user_profile (id SERIAL PRIMARY KEY, bio VARCHAR(500))");

                // UserAccount 表
                stmt.execute("CREATE TABLE user_account (id SERIAL PRIMARY KEY, username VARCHAR(50), profile_id BIGINT)");

                // Student 表
                stmt.execute("CREATE TABLE student (id SERIAL PRIMARY KEY, name VARCHAR(100))");

                // Course 表
                stmt.execute("CREATE TABLE course (id SERIAL PRIMARY KEY, name VARCHAR(100))");

                // Student-Course 中间表
                stmt.execute("CREATE TABLE student_course (student_id BIGINT, course_id BIGINT)");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create association tables", e);
            }
        }
    }

    // ==================== 分页边界测试 ====================

    @Nested
    @DisplayName("分页边界测试")
    class PaginationBoundaryTests {

        @BeforeEach
        void setUp() {
            createLogTable();
        }

        @Test
        @DisplayName("应该正确处理第一页")
        void shouldHandleFirstPage() {
            // Given
            EntityProxy<LogEntry> logProxy = dataManager.entity(LogEntry.class);
            for (int i = 0; i < 25; i++) {
                LogEntry entry = new LogEntry();
                entry.setMessage("LOG_" + i);
                entry.setLevel("INFO");
                logProxy.insert(entry);
            }

            // When
            Page<LogEntry> page = logProxy.findAll(Conditions.like("message", "LOG"), PageRequest.of(1, 10));

            // Then
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.hasNext()).isTrue();
            assertThat(page.getTotal()).isEqualTo(25);
        }

        @Test
        @DisplayName("应该正确处理最后一页")
        void shouldHandleLastPage() {
            // Given
            EntityProxy<LogEntry> logProxy = dataManager.entity(LogEntry.class);
            for (int i = 0; i < 25; i++) {
                LogEntry entry = new LogEntry();
                entry.setMessage("LOG_" + i);
                entry.setLevel("INFO");
                logProxy.insert(entry);
            }

            // When
            Page<LogEntry> page = logProxy.findAll(Conditions.like("message", "LOG"), PageRequest.of(3, 10));

            // Then
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.isLast()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("应该正确处理空结果分页")
        void shouldHandleEmptyPage() {
            // Given
            EntityProxy<LogEntry> logProxy = dataManager.entity(LogEntry.class);

            // When
            Page<LogEntry> page = logProxy.findAll(Conditions.eq("message", "NOTEXIST"), PageRequest.of(1, 10));

            // Then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotal()).isEqualTo(0);
            assertThat(page.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该正确处理单页结果")
        void shouldHandleSinglePage() {
            // Given
            EntityProxy<LogEntry> logProxy = dataManager.entity(LogEntry.class);
            for (int i = 0; i < 5; i++) {
                LogEntry entry = new LogEntry();
                entry.setMessage("LOG_" + i);
                entry.setLevel("INFO");
                logProxy.insert(entry);
            }

            // When
            Page<LogEntry> page = logProxy.findAll(Conditions.like("message", "LOG"), PageRequest.of(1, 10));

            // Then
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.isFirst()).isTrue();
            assertThat(page.isLast()).isTrue();
        }

        private void createLogTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE log_entry (
                        id SERIAL PRIMARY KEY,
                        message VARCHAR(200),
                        level VARCHAR(20)
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create log table", e);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:comptest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS counter");
            stmt.execute("DROP TABLE IF EXISTS product");
            stmt.execute("DROP TABLE IF EXISTS test_order");
            stmt.execute("DROP TABLE IF EXISTS author");
            stmt.execute("DROP TABLE IF EXISTS book");
            stmt.execute("DROP TABLE IF EXISTS category");
            stmt.execute("DROP TABLE IF EXISTS item");
            stmt.execute("DROP TABLE IF EXISTS user_profile");
            stmt.execute("DROP TABLE IF EXISTS user_account");
            stmt.execute("DROP TABLE IF EXISTS student");
            stmt.execute("DROP TABLE IF EXISTS course");
            stmt.execute("DROP TABLE IF EXISTS student_course");
            stmt.execute("DROP TABLE IF EXISTS log_entry");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体类 ====================

    @Data
    @NoArgsConstructor
    static class Counter {
        private Long id;
        private String name;
        private Integer counterValue;
    }

    @Data
    @NoArgsConstructor
    static class Product {
        private Long id;
        private String code;
        private String name;
        private Double price;
    }

    @Data
    @NoArgsConstructor
    static class TestOrder {
        private Long id;
        private String orderNo;
        private String status;
        private java.math.BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    static class Author {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    static class Book {
        private Long id;
        private String title;
        private Long authorId;

        @ManyToOne
        private Author author;
    }

    @Data
    @NoArgsConstructor
    static class Category {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "category")
        private List<Item> items;
    }

    @Data
    @NoArgsConstructor
    static class Item {
        private Long id;
        private String name;
        private Long categoryId;

        @ManyToOne
        private Category category;
    }

    @Data
    @NoArgsConstructor
    static class UserProfile {
        private Long id;
        private String bio;
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
    }

    @Data
    @NoArgsConstructor
    static class LogEntry {
        private Long id;
        private String message;
        private String level;
    }
}
