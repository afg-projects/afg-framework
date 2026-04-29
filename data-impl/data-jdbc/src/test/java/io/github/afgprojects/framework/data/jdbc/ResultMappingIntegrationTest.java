package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 结果映射集成测试
 * <p>
 * 验证各种类型的结果能够正确映射到实体属性中。
 * 使用 H2 内存数据库进行测试。
 * </p>
 */
@DisplayName("结果映射集成测试")
class ResultMappingIntegrationTest {

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

    @Nested
    @DisplayName("列名到属性名映射")
    class ColumnNameMappingTests {

        @BeforeEach
        void setUp() {
            createCamelCaseTable();
            createSnakeCaseTable();
            createMultiLevelSnakeCaseTable();
        }

        @Test
        @DisplayName("应该正确映射驼峰命名属性")
        void shouldMapCamelCaseProperties() {
            // Given
            EntityProxy<CamelCaseEntity> proxy = dataManager.entity(CamelCaseEntity.class);
            CamelCaseEntity entity = new CamelCaseEntity();
            entity.setFirstProperty("testValue");
            entity.setSecondProperty("anotherField");

            // When
            CamelCaseEntity inserted = proxy.insert(entity);

            // Then
            CamelCaseEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getFirstProperty()).isEqualTo("testValue");
            assertThat(found.getSecondProperty()).isEqualTo("anotherField");
        }

        @Test
        @DisplayName("应该正确映射下划线列名到驼峰属性")
        void shouldMapSnakeCaseToCamelCase() {
            // Given
            EntityProxy<SnakeCaseEntity> proxy = dataManager.entity(SnakeCaseEntity.class);
            SnakeCaseEntity entity = new SnakeCaseEntity();
            entity.setUserName("userValue");
            entity.setUserDesc("descValue");

            // When
            SnakeCaseEntity inserted = proxy.insert(entity);

            // Then
            SnakeCaseEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getUserName()).isEqualTo("userValue");
            assertThat(found.getUserDesc()).isEqualTo("descValue");
        }

        @Test
        @DisplayName("应该正确映射多级下划线列名")
        void shouldMapMultiLevelSnakeCase() {
            // Given
            EntityProxy<MultiLevelSnakeCaseEntity> proxy = dataManager.entity(MultiLevelSnakeCaseEntity.class);
            MultiLevelSnakeCaseEntity entity = new MultiLevelSnakeCaseEntity();
            entity.setFirstLevelProperty("value1");
            entity.setSecondLevelProperty("value2");

            // When
            MultiLevelSnakeCaseEntity inserted = proxy.insert(entity);

            // Then
            MultiLevelSnakeCaseEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getFirstLevelProperty()).isEqualTo("value1");
            assertThat(found.getSecondLevelProperty()).isEqualTo("value2");
        }
    }

    @Nested
    @DisplayName("类型转换映射")
    class TypeConversionTests {

        @BeforeEach
        void setUp() {
            createTypeConversionTable();
        }

        @Test
        @DisplayName("应该正确映射数值类型")
        void shouldMapNumericTypes() {
            // Given
            EntityProxy<NumericEntity> proxy = dataManager.entity(NumericEntity.class);
            NumericEntity entity = new NumericEntity();
            entity.setIntField(100);
            entity.setLongField(200L);
            entity.setDoubleField(3.14);
            entity.setBigDecimalField(new BigDecimal("12345.67"));

            // When
            NumericEntity inserted = proxy.insert(entity);

            // Then
            NumericEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getIntField()).isEqualTo(100);
            assertThat(found.getLongField()).isEqualTo(200L);
            assertThat(found.getDoubleField()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.01));
            assertThat(found.getBigDecimalField()).isEqualByComparingTo("12345.67");
        }

        @Test
        @DisplayName("应该正确映射布尔类型")
        void shouldMapBooleanType() {
            // Given
            EntityProxy<BooleanEntity> proxy = dataManager.entity(BooleanEntity.class);
            BooleanEntity entity = new BooleanEntity();
            entity.setActiveFlag(true);
            entity.setDeletedFlag(false);

            // When
            BooleanEntity inserted = proxy.insert(entity);

            // Then
            BooleanEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getActiveFlag()).isTrue();
            assertThat(found.getDeletedFlag()).isFalse();
        }

        @Test
        @DisplayName("应该正确映射日期类型")
        void shouldMapDateTypes() {
            // Given
            EntityProxy<DateEntity> proxy = dataManager.entity(DateEntity.class);
            LocalDate date = LocalDate.of(2024, 6, 15);
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            DateEntity entity = new DateEntity();
            entity.setDateField(date);
            entity.setDateTimeField(dateTime);

            // When
            DateEntity inserted = proxy.insert(entity);

            // Then
            DateEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getDateField()).isEqualTo(date);
            assertThat(found.getDateTimeField()).isEqualTo(dateTime);
        }
    }

    @Nested
    @DisplayName("空值和 NULL 映射")
    class NullMappingTests {

        @BeforeEach
        void setUp() {
            createNullableTable();
        }

        @Test
        @DisplayName("应该正确映射 NULL 字符串值")
        void shouldMapNullString() {
            // Given
            EntityProxy<NullableEntity> proxy = dataManager.entity(NullableEntity.class);
            NullableEntity entity = new NullableEntity();
            entity.setRequiredField("non-null");

            // When
            NullableEntity inserted = proxy.insert(entity);

            // Then
            NullableEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getNullableField()).isNull();
            assertThat(found.getRequiredField()).isEqualTo("non-null");
        }

        @Test
        @DisplayName("应该正确映射 NULL 数值")
        void shouldMapNullNumeric() {
            // Given
            EntityProxy<NullableNumericEntity> proxy = dataManager.entity(NullableNumericEntity.class);
            NullableNumericEntity entity = new NullableNumericEntity();
            entity.setRequiredInt(100);

            // When
            NullableNumericEntity inserted = proxy.insert(entity);

            // Then
            NullableNumericEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getNullableInt()).isNull();
            assertThat(found.getRequiredInt()).isEqualTo(100);
        }

        @Test
        @DisplayName("应该正确查询 NULL 值")
        void shouldQueryNullValues() {
            // Given
            EntityProxy<NullableEntity> proxy = dataManager.entity(NullableEntity.class);
            NullableEntity e1 = new NullableEntity();
            e1.setRequiredField("test1");
            proxy.insert(e1);

            NullableEntity e2 = new NullableEntity();
            e2.setNullableField("value");
            e2.setRequiredField("test2");
            proxy.insert(e2);

            // When
            Condition isNullCondition = Conditions.builder().isNull("nullable_field").build();
            Condition isNotNullCondition = Conditions.builder().isNotNull("nullable_field").build();

            // Then
            List<NullableEntity> nullResults = proxy.findAll(isNullCondition);
            List<NullableEntity> notNullResults = proxy.findAll(isNotNullCondition);

            assertThat(nullResults).hasSize(1);
            assertThat(nullResults.get(0).getNullableField()).isNull();

            assertThat(notNullResults).hasSize(1);
            assertThat(notNullResults.get(0).getNullableField()).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("批量结果映射")
    class BatchResultMappingTests {

        @BeforeEach
        void setUp() {
            createSimpleTable();
        }

        @Test
        @DisplayName("应该正确映射多条记录")
        void shouldMapMultipleRecords() {
            // Given
            EntityProxy<SimpleEntity> proxy = dataManager.entity(SimpleEntity.class);
            SimpleEntity e1 = new SimpleEntity(); e1.setName("item1");
            SimpleEntity e2 = new SimpleEntity(); e2.setName("item2");
            SimpleEntity e3 = new SimpleEntity(); e3.setName("item3");
            proxy.insertAll(List.of(e1, e2, e3));

            // When
            List<SimpleEntity> all = proxy.findAll();

            // Then
            assertThat(all).hasSize(3);
            assertThat(all.stream().map(SimpleEntity::getName).toList())
                .containsExactlyInAnyOrder("item1", "item2", "item3");
        }

        @Test
        @DisplayName("应该正确映射条件查询结果")
        void shouldMapConditionQueryResults() {
            // Given
            EntityProxy<SimpleEntity> proxy = dataManager.entity(SimpleEntity.class);
            SimpleEntity e1 = new SimpleEntity(); e1.setName("apple");
            SimpleEntity e2 = new SimpleEntity(); e2.setName("banana");
            SimpleEntity e3 = new SimpleEntity(); e3.setName("apricot");
            proxy.insertAll(List.of(e1, e2, e3));

            // When
            Condition likeCondition = Conditions.like("name", "ap");
            List<SimpleEntity> results = proxy.findAll(likeCondition);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.stream().map(SimpleEntity::getName).toList())
                .containsExactlyInAnyOrder("apple", "apricot");
        }

        @Test
        @DisplayName("应该正确映射 IN 查询结果")
        void shouldMapInQueryResults() {
            // Given
            EntityProxy<SimpleEntity> proxy = dataManager.entity(SimpleEntity.class);
            SimpleEntity e1 = new SimpleEntity(); e1.setName("first");
            SimpleEntity e2 = new SimpleEntity(); e2.setName("second");
            SimpleEntity e3 = new SimpleEntity(); e3.setName("third");
            proxy.insertAll(List.of(e1, e2, e3));

            // When
            Condition inCondition = Conditions.in("name", List.of("first", "third"));
            List<SimpleEntity> results = proxy.findAll(inCondition);

            // Then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("字段缺失映射")
    class MissingFieldTests {

        @BeforeEach
        void setUp() {
            createExtraFieldsTable();
        }

        @Test
        @DisplayName("应该忽略数据库中多余列")
        void shouldIgnoreExtraColumns() {
            // Given - 表有 extra_column，但实体没有对应字段
            EntityProxy<MinimalEntity> proxy = dataManager.entity(MinimalEntity.class);
            MinimalEntity entity = new MinimalEntity();
            entity.setName("test-name");

            // When
            MinimalEntity inserted = proxy.insert(entity);

            // Then - 映射应该成功，忽略 extra_column
            MinimalEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getName()).isEqualTo("test-name");
        }

        @Test
        @DisplayName("应该正确映射部分字段")
        void shouldMapPartialFields() {
            // Given
            EntityProxy<MinimalEntity> proxy = dataManager.entity(MinimalEntity.class);
            MinimalEntity entity = new MinimalEntity();
            entity.setName("partial-test");
            proxy.insert(entity);

            // When
            MinimalEntity found = proxy.findById(1L).orElseThrow();

            // Then
            assertThat(found.getId()).isEqualTo(1L);
            assertThat(found.getName()).isEqualTo("partial-test");
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:resultdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createCamelCaseTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE camel_case_entity (
                    id SERIAL PRIMARY KEY,
                    first_property VARCHAR(100),
                    second_property VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create camel_case table", e);
        }
    }

    private void createSnakeCaseTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE snake_case_entity (
                    id SERIAL PRIMARY KEY,
                    user_name VARCHAR(100),
                    user_desc VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create snake_case table", e);
        }
    }

    private void createMultiLevelSnakeCaseTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE multi_level_snake_case_entity (
                    id SERIAL PRIMARY KEY,
                    first_level_property VARCHAR(100),
                    second_level_property VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create multi_level table", e);
        }
    }

    private void createTypeConversionTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE numeric_entity (
                    id SERIAL PRIMARY KEY,
                    int_field INTEGER,
                    long_field BIGINT,
                    double_field DOUBLE PRECISION,
                    big_decimal_field DECIMAL(15,2)
                )
                """);
            stmt.execute("""
                CREATE TABLE boolean_entity (
                    id SERIAL PRIMARY KEY,
                    active_flag BOOLEAN,
                    deleted_flag BOOLEAN
                )
                """);
            stmt.execute("""
                CREATE TABLE date_entity (
                    id SERIAL PRIMARY KEY,
                    date_field DATE,
                    date_time_field TIMESTAMP
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create type_conversion tables", e);
        }
    }

    private void createNullableTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE nullable_entity (
                    id SERIAL PRIMARY KEY,
                    nullable_field VARCHAR(100),
                    required_field VARCHAR(100)
                )
                """);
            stmt.execute("""
                CREATE TABLE nullable_numeric_entity (
                    id SERIAL PRIMARY KEY,
                    nullable_int INTEGER,
                    required_int INTEGER
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create nullable tables", e);
        }
    }

    private void createSimpleTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE simple_entity (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create simple table", e);
        }
    }

    private void createExtraFieldsTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE minimal_entity (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    extra_column VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create extra_fields table", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS camel_case_entity");
            stmt.execute("DROP TABLE IF EXISTS snake_case_entity");
            stmt.execute("DROP TABLE IF EXISTS multi_level_snake_case_entity");
            stmt.execute("DROP TABLE IF EXISTS numeric_entity");
            stmt.execute("DROP TABLE IF EXISTS boolean_entity");
            stmt.execute("DROP TABLE IF EXISTS date_entity");
            stmt.execute("DROP TABLE IF EXISTS nullable_entity");
            stmt.execute("DROP TABLE IF EXISTS nullable_numeric_entity");
            stmt.execute("DROP TABLE IF EXISTS simple_entity");
            stmt.execute("DROP TABLE IF EXISTS minimal_entity");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CamelCaseEntity {
        private Long id;
        private String firstProperty;
        private String secondProperty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SnakeCaseEntity {
        private Long id;
        private String userName;
        private String userDesc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class MultiLevelSnakeCaseEntity {
        private Long id;
        private String firstLevelProperty;
        private String secondLevelProperty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NumericEntity {
        private Long id;
        private Integer intField;
        private Long longField;
        private Double doubleField;
        private BigDecimal bigDecimalField;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class BooleanEntity {
        private Long id;
        private Boolean activeFlag;
        private Boolean deletedFlag;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DateEntity {
        private Long id;
        private LocalDate dateField;
        private LocalDateTime dateTimeField;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NullableEntity {
        private Long id;
        private String nullableField;
        private String requiredField;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NullableNumericEntity {
        private Long id;
        private Integer nullableInt;
        private Integer requiredInt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SimpleEntity {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class MinimalEntity {
        private Long id;
        private String name;
    }
}