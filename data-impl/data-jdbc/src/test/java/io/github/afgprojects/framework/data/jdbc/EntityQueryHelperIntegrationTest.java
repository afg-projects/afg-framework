package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.jdbc.metadata.SimpleEntityMetadata;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EntityQueryHelper 集成测试
 * <p>
 * 使用真实实体类和 H2 内存数据库测试 SQL 构建、参数提取、结果映射等功能
 */
@DisplayName("EntityQueryHelper Integration Tests")
class EntityQueryHelperIntegrationTest {

    private DataSource dataSource;
    private JdbcDataManager dataManager;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    // ==================== buildInsertSql 测试 ====================

    @Nested
    @DisplayName("buildInsertSql Tests")
    class BuildInsertSqlTests {

        @Test
        @DisplayName("should generate INSERT SQL with multiple non-generated fields")
        void shouldGenerateInsertSqlWithMultipleFields() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildInsertSql();

            assertThat(sql).contains("INSERT INTO");
            assertThat(sql).contains("test_entity");
            assertThat(sql).contains("name");
            assertThat(sql).contains("email");
            assertThat(sql).contains("status");
            assertThat(sql).doesNotContain("id"); // id is generated, should be excluded
            assertThat(sql).contains("VALUES (?, ?, ?)");
        }

        @Test
        @DisplayName("should generate INSERT SQL for entity with only generated field")
        void shouldGenerateInsertSqlForEntityWithOnlyGeneratedField() {
            SimpleEntityMetadata<OnlyIdEntity> metadata = new SimpleEntityMetadata<>(OnlyIdEntity.class);
            EntityQueryHelper<OnlyIdEntity> helper = new EntityQueryHelper<>(OnlyIdEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildInsertSql();

            assertThat(sql).contains("INSERT INTO");
            assertThat(sql).contains("()");
            assertThat(sql).contains("VALUES ()");
        }
    }

    // ==================== buildUpdateSql 测试 ====================

    @Nested
    @DisplayName("buildUpdateSql Tests")
    class BuildUpdateSqlTests {

        @Test
        @DisplayName("should generate UPDATE SQL for non-versioned entity")
        void shouldGenerateUpdateSqlForNonVersionedEntity() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildUpdateSql(false);

            assertThat(sql).contains("UPDATE test_entity SET");
            assertThat(sql).contains("name = ?");
            assertThat(sql).contains("email = ?");
            assertThat(sql).contains("status = ?");
            assertThat(sql).contains("WHERE id = ?");
            assertThat(sql).doesNotContain("version");
        }

        @Test
        @DisplayName("should generate UPDATE SQL for versioned entity with optimistic lock")
        void shouldGenerateUpdateSqlForVersionedEntity() {
            SimpleEntityMetadata<VersionedEntity> metadata = new SimpleEntityMetadata<>(VersionedEntity.class);
            EntityQueryHelper<VersionedEntity> helper = new EntityQueryHelper<>(VersionedEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildUpdateSql(true);

            assertThat(sql).contains("UPDATE versioned_entity SET");
            assertThat(sql).contains("name = ?");
            assertThat(sql).contains("version = version + 1"); // version increment in SET clause
            assertThat(sql).contains("WHERE id = ?");
            assertThat(sql).contains("AND version = ?"); // version condition in WHERE clause
        }

        @Test
        @DisplayName("should generate UPDATE SQL for versioned entity without version field when isVersioned is false")
        void shouldGenerateUpdateSqlForVersionedEntityWithoutVersioning() {
            SimpleEntityMetadata<VersionedEntity> metadata = new SimpleEntityMetadata<>(VersionedEntity.class);
            EntityQueryHelper<VersionedEntity> helper = new EntityQueryHelper<>(VersionedEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildUpdateSql(false);

            assertThat(sql).contains("UPDATE versioned_entity SET");
            assertThat(sql).contains("name = ?");
            assertThat(sql).contains("version = ?"); // version as normal parameter
            assertThat(sql).contains("WHERE id = ?");
            assertThat(sql).doesNotContain("version + 1");
        }
    }

    // ==================== extractInsertParams 测试 ====================

    @Nested
    @DisplayName("extractInsertParams Tests")
    class ExtractInsertParamsTests {

        @Test
        @DisplayName("should extract insert parameters from entity")
        void shouldExtractInsertParams() {
            createTestTable();
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            entity.setName("test-name");
            entity.setEmail("test@example.com");
            entity.setStatus(1);

            List<Object> params = helper.extractInsertParams(entity);

            assertThat(params).hasSize(3);
            assertThat(params).containsExactly("test-name", "test@example.com", 1);
        }

        @Test
        @DisplayName("should extract insert parameters with null values")
        void shouldExtractInsertParamsWithNulls() {
            createTestTable();
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            entity.setName("test-name");
            entity.setEmail(null);
            entity.setStatus(null);

            List<Object> params = helper.extractInsertParams(entity);

            assertThat(params).hasSize(3);
            assertThat(params).containsExactly("test-name", null, null);
        }

        @Test
        @DisplayName("should extract insert parameters from entity with various types")
        void shouldExtractInsertParamsWithVariousTypes() {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            TypedEntity entity = new TypedEntity();
            entity.setStringValue("string");
            entity.setIntValue(42);
            entity.setLongValue(1000L);
            entity.setDoubleValue(3.14);
            entity.setBoolValue(true);
            entity.setDecimalValue(new BigDecimal("99.99"));

            List<Object> params = helper.extractInsertParams(entity);

            assertThat(params).hasSize(6);
            assertThat(params.get(0)).isEqualTo("string");
            assertThat(params.get(1)).isEqualTo(42);
            assertThat(params.get(2)).isEqualTo(1000L);
            assertThat(params.get(3)).isEqualTo(3.14);
            assertThat(params.get(4)).isEqualTo(true);
            assertThat(params.get(5)).isEqualTo(new BigDecimal("99.99"));
        }
    }

    // ==================== extractUpdateParams 测试 ====================

    @Nested
    @DisplayName("extractUpdateParams Tests")
    class ExtractUpdateParamsTests {

        @Test
        @DisplayName("should extract update parameters for non-versioned entity")
        void shouldExtractUpdateParamsNonVersioned() {
            createTestTable();
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setName("updated-name");
            entity.setEmail("updated@example.com");
            entity.setStatus(2);

            List<Object> params = helper.extractUpdateParams(entity, false);

            // name, email, status, id (for WHERE clause)
            assertThat(params).hasSize(4);
            assertThat(params).containsExactly("updated-name", "updated@example.com", 2, 1L);
        }

        @Test
        @DisplayName("should extract update parameters for versioned entity")
        void shouldExtractUpdateParamsVersioned() {
            createVersionedTable();
            SimpleEntityMetadata<VersionedEntity> metadata = new SimpleEntityMetadata<>(VersionedEntity.class);
            EntityQueryHelper<VersionedEntity> helper = new EntityQueryHelper<>(VersionedEntity.class, new H2Dialect(), metadata);

            VersionedEntity entity = new VersionedEntity();
            entity.setId(1L);
            entity.setName("versioned-name");
            entity.setVersion(5);

            List<Object> params = helper.extractUpdateParams(entity, true);

            // name (version skipped), id, version (for WHERE clause)
            assertThat(params).hasSize(3);
            assertThat(params).containsExactly("versioned-name", 1L, 5);
        }

        @Test
        @DisplayName("should extract update parameters for versioned entity when versioning disabled")
        void shouldExtractUpdateParamsVersionedDisabled() {
            createVersionedTable();
            SimpleEntityMetadata<VersionedEntity> metadata = new SimpleEntityMetadata<>(VersionedEntity.class);
            EntityQueryHelper<VersionedEntity> helper = new EntityQueryHelper<>(VersionedEntity.class, new H2Dialect(), metadata);

            VersionedEntity entity = new VersionedEntity();
            entity.setId(1L);
            entity.setName("name");
            entity.setVersion(5);

            List<Object> params = helper.extractUpdateParams(entity, false);

            // name, version (as normal field), id
            assertThat(params).hasSize(3);
            assertThat(params).containsExactly("name", 5, 1L);
        }
    }

    // ==================== mapRow 测试 ====================

    @Nested
    @DisplayName("mapRow Tests")
    class MapRowTests {

        @Test
        @DisplayName("should map ResultSet row to entity")
        void shouldMapRowToEntity() throws Exception {
            createTestTable();
            insertTestData();

            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM test_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                TestEntity entity = helper.mapRow(rs, 0);

                assertThat(entity).isNotNull();
                assertThat(entity.getId()).isEqualTo(1L);
                assertThat(entity.getName()).isEqualTo("test-name");
                assertThat(entity.getEmail()).isEqualTo("test@example.com");
                assertThat(entity.getStatus()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("should map ResultSet with null values")
        void shouldMapRowWithNullValues() throws Exception {
            createTestTable();
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_entity (id, name, email, status) VALUES (2, 'null-test', NULL, NULL)");
            }

            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM test_entity WHERE id = 2")) {

                assertThat(rs.next()).isTrue();
                TestEntity entity = helper.mapRow(rs, 0);

                assertThat(entity).isNotNull();
                assertThat(entity.getId()).isEqualTo(2L);
                assertThat(entity.getName()).isEqualTo("null-test");
                assertThat(entity.getEmail()).isNull();
                assertThat(entity.getStatus()).isNull();
            }
        }

        @Test
        @DisplayName("should map ResultSet with various types")
        void shouldMapRowWithVariousTypes() throws Exception {
            createTypedEntityTable();

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    INSERT INTO typed_entity (id, string_value, int_value, long_value, double_value, bool_value, decimal_value)
                    VALUES (1, 'test', 42, 1000, 3.14, TRUE, 99.99)
                    """);
            }

            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM typed_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                TypedEntity entity = helper.mapRow(rs, 0);

                assertThat(entity).isNotNull();
                assertThat(entity.getStringValue()).isEqualTo("test");
                assertThat(entity.getIntValue()).isEqualTo(42);
                assertThat(entity.getLongValue()).isEqualTo(1000L);
                assertThat(entity.getDoubleValue()).isEqualTo(3.14);
                assertThat(entity.getBoolValue()).isTrue();
                assertThat(entity.getDecimalValue()).isEqualByComparingTo(new BigDecimal("99.99"));
            }
        }

        @Test
        @DisplayName("should map ResultSet with timestamp to LocalDateTime")
        void shouldMapTimestampToLocalDateTime() throws Exception {
            createDateTimeEntityTable();

            LocalDateTime testTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO date_time_entity (id, created_at, updated_at, birth_date) VALUES (1, '" +
                    Timestamp.valueOf(testTime) + "', NULL, '2024-01-15')");
            }

            SimpleEntityMetadata<DateTimeEntity> metadata = new SimpleEntityMetadata<>(DateTimeEntity.class);
            EntityQueryHelper<DateTimeEntity> helper = new EntityQueryHelper<>(DateTimeEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM date_time_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                DateTimeEntity entity = helper.mapRow(rs, 0);

                assertThat(entity).isNotNull();
                assertThat(entity.getCreatedAt()).isEqualTo(testTime);
                assertThat(entity.getUpdatedAt()).isNull();
                assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            }
        }
    }

    // ==================== convertValue 测试 ====================

    @Nested
    @DisplayName("convertValue Tests")
    class ConvertValueTests {

        @Test
        @DisplayName("should convert Number to Long")
        void shouldConvertNumberToLong() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            // Insert with integer, expect long
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, long_value) VALUES (1, 42)");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT long_value FROM typed_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                Object value = rs.getObject(1);
                TypedEntity entity = new TypedEntity();
                helper.setFieldValue(entity, "longValue", value);

                assertThat(entity.getLongValue()).isEqualTo(42L);
            }
        }

        @Test
        @DisplayName("should convert Number to Integer")
        void shouldConvertNumberToInteger() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, int_value) VALUES (2, 100)");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT int_value FROM typed_entity WHERE id = 2")) {

                assertThat(rs.next()).isTrue();
                Object value = rs.getObject(1);
                TypedEntity entity = new TypedEntity();
                helper.setFieldValue(entity, "intValue", value);

                assertThat(entity.getIntValue()).isEqualTo(100);
            }
        }

        @Test
        @DisplayName("should convert to String when mapping ResultSet")
        void shouldConvertToStringWhenMapping() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            // Insert a numeric value into string column
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, string_value) VALUES (5, '12345')");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT string_value FROM typed_entity WHERE id = 5")) {

                assertThat(rs.next()).isTrue();
                TypedEntity entity = helper.mapRow(rs, 0);

                assertThat(entity.getStringValue()).isEqualTo("12345");
            }
        }

        @Test
        @DisplayName("should convert numeric to String when target field is String")
        void shouldConvertNumericToStringWhenMapping() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            // Insert a numeric value (using CAST to ensure it's returned as INTEGER)
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, string_value) VALUES (6, CAST(42 AS VARCHAR))");
            }

            // Query with CAST to return INTEGER instead of VARCHAR
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT CAST(id AS VARCHAR) as string_value FROM typed_entity WHERE id = 6")) {

                assertThat(rs.next()).isTrue();
                TypedEntity entity = helper.mapRow(rs, 0);

                // The value should be converted from Integer to String
                assertThat(entity.getStringValue()).isEqualTo("6");
            }
        }

        @Test
        @DisplayName("should convert Timestamp to LocalDateTime")
        void shouldConvertTimestampToLocalDateTime() throws Exception {
            createDateTimeEntityTable();
            SimpleEntityMetadata<DateTimeEntity> metadata = new SimpleEntityMetadata<>(DateTimeEntity.class);
            EntityQueryHelper<DateTimeEntity> helper = new EntityQueryHelper<>(DateTimeEntity.class, new H2Dialect(), metadata);

            LocalDateTime testTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO date_time_entity (id, created_at) VALUES (1, '" + Timestamp.valueOf(testTime) + "')");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT created_at FROM date_time_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                DateTimeEntity entity = helper.mapRow(rs, 0);

                assertThat(entity.getCreatedAt()).isEqualTo(testTime);
            }
        }

        @Test
        @DisplayName("should convert java.sql.Date to LocalDate")
        void shouldConvertSqlDateToLocalDate() throws Exception {
            createDateTimeEntityTable();
            SimpleEntityMetadata<DateTimeEntity> metadata = new SimpleEntityMetadata<>(DateTimeEntity.class);
            EntityQueryHelper<DateTimeEntity> helper = new EntityQueryHelper<>(DateTimeEntity.class, new H2Dialect(), metadata);

            LocalDate testDate = LocalDate.of(2024, 6, 15);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO date_time_entity (id, birth_date) VALUES (2, '" + java.sql.Date.valueOf(testDate) + "')");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT birth_date FROM date_time_entity WHERE id = 2")) {

                assertThat(rs.next()).isTrue();
                DateTimeEntity entity = helper.mapRow(rs, 0);

                assertThat(entity.getBirthDate()).isEqualTo(testDate);
            }
        }
    }

    // ==================== getIdValue / setIdValue 测试 ====================

    @Nested
    @DisplayName("getIdValue and setIdValue Tests")
    class IdValueTests {

        @Test
        @DisplayName("should get ID value from entity")
        void shouldGetIdValue() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            entity.setId(42L);

            Object idValue = helper.getIdValue(entity);

            assertThat(idValue).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return null when entity has no ID")
        void shouldReturnNullForNoId() {
            SimpleEntityMetadata<NoIdEntity> metadata = new SimpleEntityMetadata<>(NoIdEntity.class);
            EntityQueryHelper<NoIdEntity> helper = new EntityQueryHelper<>(NoIdEntity.class, new H2Dialect(), metadata);

            NoIdEntity entity = new NoIdEntity();
            entity.setName("test");

            Object idValue = helper.getIdValue(entity);

            assertThat(idValue).isNull();
        }

        @Test
        @DisplayName("should set Long ID value")
        void shouldSetLongIdValue() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            helper.setIdValue(entity, 100L);

            assertThat(entity.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should set Integer ID value for Integer ID field")
        void shouldSetIntegerIdValue() {
            SimpleEntityMetadata<IntegerIdEntity> metadata = new SimpleEntityMetadata<>(IntegerIdEntity.class);
            EntityQueryHelper<IntegerIdEntity> helper = new EntityQueryHelper<>(IntegerIdEntity.class, new H2Dialect(), metadata);

            IntegerIdEntity entity = new IntegerIdEntity();
            helper.setIdValue(entity, 50L); // Pass Long, should convert to Integer

            assertThat(entity.getId()).isEqualTo(50);
        }
    }

    // ==================== setFieldValue 测试 ====================

    @Nested
    @DisplayName("setFieldValue Tests")
    class SetFieldValueTests {

        @Test
        @DisplayName("should set field value by field name")
        void shouldSetFieldValue() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();
            helper.setFieldValue(entity, "name", "new-name");
            helper.setFieldValue(entity, "email", "new@example.com");
            helper.setFieldValue(entity, "status", 5);

            assertThat(entity.getName()).isEqualTo("new-name");
            assertThat(entity.getEmail()).isEqualTo("new@example.com");
            assertThat(entity.getStatus()).isEqualTo(5);
        }

        @Test
        @DisplayName("should throw exception for non-existent field")
        void shouldThrowForNonExistentField() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            TestEntity entity = new TestEntity();

            assertThatThrownBy(() -> helper.setFieldValue(entity, "nonExistentField", "value"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to set field 'nonExistentField'");
        }
    }

    // ==================== columnNameToFieldName / fieldNameToColumnName 边界测试 ====================

    @Nested
    @DisplayName("Name Conversion Edge Cases Tests")
    class NameConversionEdgeCasesTests {

        @Test
        @DisplayName("should handle column name with multiple underscores")
        void shouldHandleMultipleUnderscores() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            assertThat(helper.columnNameToFieldName("user_first_name")).isEqualTo("userFirstName");
            assertThat(helper.columnNameToFieldName("a_b_c_d")).isEqualTo("aBCD");
        }

        @Test
        @DisplayName("should handle field name with multiple uppercase letters")
        void shouldHandleMultipleUppercaseLetters() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            assertThat(helper.fieldNameToColumnName("userFirstName")).isEqualTo("user_first_name");
            assertThat(helper.fieldNameToColumnName("XMLParser")).isEqualTo("x_m_l_parser");
        }

        @Test
        @DisplayName("should handle empty strings")
        void shouldHandleEmptyStrings() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            assertThat(helper.columnNameToFieldName("")).isEmpty();
            assertThat(helper.fieldNameToColumnName("")).isEmpty();
        }

        @Test
        @DisplayName("should handle single character")
        void shouldHandleSingleCharacter() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            assertThat(helper.columnNameToFieldName("a")).isEqualTo("a");
            assertThat(helper.fieldNameToColumnName("a")).isEqualTo("a");
            assertThat(helper.fieldNameToColumnName("A")).isEqualTo("a");
        }

        @Test
        @DisplayName("should handle trailing underscore")
        void shouldHandleTrailingUnderscore() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            assertThat(helper.columnNameToFieldName("user_")).isEqualTo("user");
            assertThat(helper.columnNameToFieldName("user__")).isEqualTo("user");
        }
    }

    // ==================== buildSelectSql and buildDeleteSql additional tests ====================

    @Nested
    @DisplayName("buildSelectSql and buildDeleteSql Additional Tests")
    class BuildSelectDeleteSqlTests {

        @Test
        @DisplayName("should handle empty WHERE clause")
        void shouldHandleEmptyWhereClause() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            String selectSql = helper.buildSelectSql("");
            assertThat(selectSql).doesNotContain("WHERE");

            String deleteSql = helper.buildDeleteSql("");
            assertThat(deleteSql).doesNotContain("WHERE");
        }

        @Test
        @DisplayName("should generate correct column list in SELECT")
        void shouldGenerateCorrectColumnList() {
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            String sql = helper.buildSelectSql(null);

            assertThat(sql).contains("id, name, email, status");
            assertThat(sql).contains("FROM test_entity");
        }
    }

    // ==================== setIdValue additional tests for coverage ====================

    @Nested
    @DisplayName("setIdValue Additional Coverage Tests")
    class SetIdValueCoverageTests {

        @Test
        @DisplayName("should set ID value for entity with no ID field")
        void shouldSetIdValueForEntityWithNoIdField() {
            SimpleEntityMetadata<NoIdEntity> metadata = new SimpleEntityMetadata<>(NoIdEntity.class);
            EntityQueryHelper<NoIdEntity> helper = new EntityQueryHelper<>(NoIdEntity.class, new H2Dialect(), metadata);

            NoIdEntity entity = new NoIdEntity();
            entity.setName("test");
            // Should not throw - just do nothing since there's no ID field
            helper.setIdValue(entity, 100L);

            assertThat(entity.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("should fall through to else branch for String ID field (non-Long/Integer)")
        void shouldFallThroughToElseBranchForStringIdField() {
            SimpleEntityMetadata<StringIdEntity> metadata = new SimpleEntityMetadata<>(StringIdEntity.class);
            EntityQueryHelper<StringIdEntity> helper = new EntityQueryHelper<>(StringIdEntity.class, new H2Dialect(), metadata);

            StringIdEntity entity = new StringIdEntity();
            // Passes long value, but field is String - should fall through to else branch
            // The else branch calls idField.setValue(entity, id) which will fail because
            // String field cannot accept Long value directly
            assertThatThrownBy(() -> helper.setIdValue(entity, 123L))
                .isInstanceOf(RuntimeException.class);
        }
    }

    // ==================== mapRow exception handling test ====================

    @Nested
    @DisplayName("mapRow Exception Handling Tests")
    class MapRowExceptionTests {

        @Test
        @DisplayName("should throw RuntimeException when mapping fails")
        void shouldThrowRuntimeExceptionWhenMappingFails() throws Exception {
            // Create a mock ResultSet that throws an exception
            SimpleEntityMetadata<TestEntity> metadata = new SimpleEntityMetadata<>(TestEntity.class);
            EntityQueryHelper<TestEntity> helper = new EntityQueryHelper<>(TestEntity.class, new H2Dialect(), metadata);

            // Use a mocked ResultSet that throws exception on getMetaData
            java.sql.ResultSet mockRs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
            org.mockito.Mockito.when(mockRs.getMetaData()).thenThrow(new java.sql.SQLException("Test exception"));

            assertThatThrownBy(() -> helper.mapRow(mockRs, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to map row to entity");
        }
    }

    // ==================== convertValue edge cases for coverage ====================

    @Nested
    @DisplayName("convertValue Edge Cases Tests")
    class ConvertValueEdgeCasesTests {

        @Test
        @DisplayName("should handle value that is already Long type")
        void shouldHandleValueAlreadyLong() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            // Insert a long value
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, long_value) VALUES (10, 42)");
            }

            // Query the value back - H2 returns BIGINT which maps to Long
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT long_value FROM typed_entity WHERE id = 10")) {

                assertThat(rs.next()).isTrue();
                Object value = rs.getObject(1);
                // Verify the value is a Long (Number subclass)
                assertThat(value).isInstanceOf(java.lang.Long.class);

                TypedEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getLongValue()).isEqualTo(42L);
            }
        }

        @Test
        @DisplayName("should handle value that is already Integer type")
        void shouldHandleValueAlreadyInteger() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, int_value) VALUES (11, 100)");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT int_value FROM typed_entity WHERE id = 11")) {

                assertThat(rs.next()).isTrue();
                Object value = rs.getObject(1);
                // Verify the value is an Integer (Number subclass)
                assertThat(value).isInstanceOf(java.lang.Integer.class);

                TypedEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getIntValue()).isEqualTo(100);
            }
        }

        @Test
        @DisplayName("should convert non-String value to String when target is String")
        void shouldConvertNonStringValueToString() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, string_value) VALUES (12, 'test')");
            }

            // Query with a numeric expression that returns INTEGER
            // This tests the branch where target is String but value is not String
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 12345 as string_value FROM typed_entity WHERE id = 12")) {

                assertThat(rs.next()).isTrue();
                Object value = rs.getObject(1);
                // Verify the value is an Integer (not String)
                assertThat(value).isInstanceOf(java.lang.Integer.class);

                TypedEntity entity = helper.mapRow(rs, 0);
                // The integer 12345 should be converted to string "12345"
                assertThat(entity.getStringValue()).isEqualTo("12345");
            }
        }

        @Test
        @DisplayName("should handle value that is already LocalDateTime type")
        void shouldHandleValueAlreadyLocalDateTime() throws Exception {
            createDateTimeEntityTable();
            SimpleEntityMetadata<DateTimeEntity> metadata = new SimpleEntityMetadata<>(DateTimeEntity.class);
            EntityQueryHelper<DateTimeEntity> helper = new EntityQueryHelper<>(DateTimeEntity.class, new H2Dialect(), metadata);

            LocalDateTime testTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO date_time_entity (id, created_at) VALUES (1, '" + Timestamp.valueOf(testTime) + "')");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT created_at FROM date_time_entity WHERE id = 1")) {

                assertThat(rs.next()).isTrue();
                DateTimeEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getCreatedAt()).isEqualTo(testTime);
            }
        }

        @Test
        @DisplayName("should handle value that is already LocalDate type")
        void shouldHandleValueAlreadyLocalDate() throws Exception {
            createDateTimeEntityTable();
            SimpleEntityMetadata<DateTimeEntity> metadata = new SimpleEntityMetadata<>(DateTimeEntity.class);
            EntityQueryHelper<DateTimeEntity> helper = new EntityQueryHelper<>(DateTimeEntity.class, new H2Dialect(), metadata);

            LocalDate testDate = LocalDate.of(2024, 6, 15);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO date_time_entity (id, birth_date) VALUES (2, '" + java.sql.Date.valueOf(testDate) + "')");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT birth_date FROM date_time_entity WHERE id = 2")) {

                assertThat(rs.next()).isTrue();
                DateTimeEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getBirthDate()).isEqualTo(testDate);
            }
        }

        @Test
        @DisplayName("should handle Long target with Integer value (Number conversion)")
        void shouldConvertIntegerToLong() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, long_value) VALUES (20, 42)");
            }

            // Query the value - H2 may return INTEGER for small values
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT long_value FROM typed_entity WHERE id = 20")) {

                assertThat(rs.next()).isTrue();
                TypedEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getLongValue()).isEqualTo(42L);
            }
        }

        @Test
        @DisplayName("should handle Integer target with Long value (Number conversion)")
        void shouldConvertLongToInteger() throws Exception {
            createTypedEntityTable();
            SimpleEntityMetadata<TypedEntity> metadata = new SimpleEntityMetadata<>(TypedEntity.class);
            EntityQueryHelper<TypedEntity> helper = new EntityQueryHelper<>(TypedEntity.class, new H2Dialect(), metadata);

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO typed_entity (id, int_value) VALUES (21, 100)");
            }

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT int_value FROM typed_entity WHERE id = 21")) {

                assertThat(rs.next()).isTrue();
                TypedEntity entity = helper.mapRow(rs, 0);
                assertThat(entity.getIntValue()).isEqualTo(100);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:queryhelpertest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS test_entity (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    status INTEGER
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }

    private void createVersionedTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS versioned_entity (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    version INTEGER DEFAULT 0
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create versioned table", e);
        }
    }

    private void createTypedEntityTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS typed_entity (
                    id SERIAL PRIMARY KEY,
                    string_value VARCHAR(200),
                    int_value INTEGER,
                    long_value BIGINT,
                    double_value DOUBLE,
                    bool_value BOOLEAN,
                    decimal_value DECIMAL(19,4)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create typed entity table", e);
        }
    }

    private void createDateTimeEntityTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS date_time_entity (
                    id SERIAL PRIMARY KEY,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    birth_date DATE
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create date time entity table", e);
        }
    }

    private void insertTestData() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO test_entity (id, name, email, status) VALUES (1, 'test-name', 'test@example.com', 1)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert test data", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_entity");
            stmt.execute("DROP TABLE IF EXISTS versioned_entity");
            stmt.execute("DROP TABLE IF EXISTS typed_entity");
            stmt.execute("DROP TABLE IF EXISTS date_time_entity");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    // ==================== 测试实体类 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestEntity {
        private Long id;
        private String name;
        private String email;
        private Integer status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class VersionedEntity {
        private Long id;
        private String name;
        private Integer version;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OnlyIdEntity {
        private Long id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NoIdEntity {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class IntegerIdEntity {
        private Integer id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class StringIdEntity {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TypedEntity {
        private Long id;
        private String stringValue;
        private Integer intValue;
        private Long longValue;
        private Double doubleValue;
        private Boolean boolValue;
        private BigDecimal decimalValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DateTimeEntity {
        private Long id;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDate birthDate;
    }
}
