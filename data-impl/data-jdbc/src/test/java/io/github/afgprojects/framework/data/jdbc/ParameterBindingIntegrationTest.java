package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数绑定集成测试
 * <p>
 * 验证各种类型的参数能够正确绑定到 SQL 语句中。
 * 使用 H2 内存数据库进行测试。
 * </p>
 */
@DisplayName("参数绑定集成测试")
class ParameterBindingIntegrationTest {

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
    @DisplayName("基本类型参数绑定")
    class BasicTypeTests {

        @BeforeEach
        void setUp() {
            createBasicTypesTable();
        }

        @Test
        @DisplayName("应该正确绑定字符串参数")
        void shouldBindStringParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal("测试字符串");

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            assertThat(inserted.getId()).isNotNull();
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo("测试字符串");
        }

        @Test
        @DisplayName("应该正确绑定整数参数")
        void shouldBindIntegerParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setIntVal(42);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getIntVal()).isEqualTo(42);
        }

        @Test
        @DisplayName("应该正确绑定长整数参数")
        void shouldBindLongParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            long longValue = 9876543210L;
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setLongVal(longValue);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getLongVal()).isEqualTo(longValue);
        }

        @Test
        @DisplayName("应该正确绑定双精度浮点数参数")
        void shouldBindDoubleParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            double doubleValue = 3.14159265358979;
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setDoubleVal(doubleValue);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getDoubleVal()).isCloseTo(doubleValue, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("应该正确绑定布尔参数")
        void shouldBindBooleanParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setBooleanVal(true);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getBooleanVal()).isTrue();
        }

        @Test
        @DisplayName("应该正确绑定 BigDecimal 参数")
        void shouldBindBigDecimalParameter() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BigDecimal decimal = new BigDecimal("123456.78");
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setDecimalVal(decimal);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getDecimalVal()).isEqualByComparingTo(decimal);
        }
    }

    @Nested
    @DisplayName("日期时间类型参数绑定")
    class DateTimeTypeTests {

        @BeforeEach
        void setUp() {
            createDateTimeTable();
        }

        @Test
        @DisplayName("应该正确绑定 LocalDate 参数")
        void shouldBindLocalDateParameter() {
            // Given
            EntityProxy<DateTimeEntity> proxy = dataManager.entity(DateTimeEntity.class);
            LocalDate date = LocalDate.of(2024, 12, 25);
            DateTimeEntity entity = new DateTimeEntity();
            entity.setDateVal(date);

            // When
            DateTimeEntity inserted = proxy.insert(entity);

            // Then
            DateTimeEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getDateVal()).isEqualTo(date);
        }

        @Test
        @DisplayName("应该正确绑定 LocalDateTime 参数")
        void shouldBindLocalDateTimeParameter() {
            // Given
            EntityProxy<DateTimeEntity> proxy = dataManager.entity(DateTimeEntity.class);
            LocalDateTime dateTime = LocalDateTime.of(2024, 12, 25, 10, 30, 45);
            DateTimeEntity entity = new DateTimeEntity();
            entity.setDateTimeVal(dateTime);

            // When
            DateTimeEntity inserted = proxy.insert(entity);

            // Then
            DateTimeEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getDateTimeVal()).isEqualTo(dateTime);
        }
    }

    @Nested
    @DisplayName("NULL 值参数绑定")
    class NullValueTests {

        @BeforeEach
        void setUp() {
            createBasicTypesTable();
        }

        @Test
        @DisplayName("应该正确处理 null 字符串")
        void shouldHandleNullString() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setIntVal(1);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isNull();
            assertThat(found.getIntVal()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该正确处理全 null 实体")
        void shouldHandleAllNullEntity() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            BasicTypesEntity entity = new BasicTypesEntity();

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            assertThat(inserted.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("特殊字符参数绑定")
    class SpecialCharacterTests {

        @BeforeEach
        void setUp() {
            createBasicTypesTable();
        }

        @Test
        @DisplayName("应该正确处理包含单引号的字符串")
        void shouldHandleSingleQuotes() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            String valueWithQuote = "O'Brien's test";
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal(valueWithQuote);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo(valueWithQuote);
        }

        @Test
        @DisplayName("应该正确处理包含双引号的字符串")
        void shouldHandleDoubleQuotes() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            String valueWithQuotes = "He said \"Hello\"";
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal(valueWithQuotes);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo(valueWithQuotes);
        }

        @Test
        @DisplayName("应该正确处理包含反斜杠的字符串")
        void shouldHandleBackslashes() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            String valueWithBackslash = "path\\to\\file";
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal(valueWithBackslash);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo(valueWithBackslash);
        }

        @Test
        @DisplayName("应该正确处理包含换行符的字符串")
        void shouldHandleNewlines() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            String valueWithNewline = "line1\nline2\r\nline3";
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal(valueWithNewline);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo(valueWithNewline);
        }

        @Test
        @DisplayName("应该正确处理 Unicode 字符")
        void shouldHandleUnicodeCharacters() {
            // Given
            EntityProxy<BasicTypesEntity> proxy = dataManager.entity(BasicTypesEntity.class);
            String unicodeValue = "中文日本語한국어🎉🚀";
            BasicTypesEntity entity = new BasicTypesEntity();
            entity.setStringVal(unicodeValue);

            // When
            BasicTypesEntity inserted = proxy.insert(entity);

            // Then
            BasicTypesEntity found = proxy.findById(inserted.getId()).orElseThrow();
            assertThat(found.getStringVal()).isEqualTo(unicodeValue);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:paramdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createBasicTypesTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE basic_types_entity (
                    id SERIAL PRIMARY KEY,
                    string_val VARCHAR(500),
                    int_val INTEGER,
                    long_val BIGINT,
                    double_val DOUBLE PRECISION,
                    boolean_val BOOLEAN,
                    decimal_val DECIMAL(15,2),
                    extra_val VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create basic_types table", e);
        }
    }

    private void createDateTimeTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE date_time_entity (
                    id SERIAL PRIMARY KEY,
                    date_val DATE,
                    date_time_val TIMESTAMP
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create date_time table", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS basic_types_entity");
            stmt.execute("DROP TABLE IF EXISTS date_time_entity");
        } catch (Exception ignored) {
        }
    }

    /**
     * 基本类型测试实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class BasicTypesEntity {
        private Long id;
        private String stringVal;
        private Integer intVal;
        private Long longVal;
        private Double doubleVal;
        private Boolean booleanVal;
        private BigDecimal decimalVal;
        private String extraVal;
    }

    /**
     * 日期时间测试实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DateTimeEntity {
        private Long id;
        private LocalDate dateVal;
        private LocalDateTime dateTimeVal;
    }
}