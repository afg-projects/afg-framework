package io.github.afgprojects.framework.data.jdbc.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 执行指标集成测试
 * <p>
 * 使用 H2 内存数据库和 SimpleMeterRegistry 进行测试
 */
@DisplayName("SQL 执行指标测试")
class SqlMetricsTest {

    @Nested
    @DisplayName("SqlMetrics 单元测试")
    class SqlMetricsUnitTest {

        private MeterRegistry meterRegistry;
        private SqlMetricsProperties properties;
        private SqlMetrics sqlMetrics;

        @BeforeEach
        void setUp() {
            meterRegistry = new SimpleMeterRegistry();
            properties = new SqlMetricsProperties();
            properties.setEnabled(true);
            properties.setSlowQueryThreshold(Duration.ofMillis(100));
            sqlMetrics = new SqlMetrics(meterRegistry, properties);
        }

        @Test
        @DisplayName("应该记录 SQL 执行成功指标")
        void shouldRecordSuccessMetrics() {
            // When
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(50), 10L);

            // Then
            assertThat(meterRegistry.find("afg.jdbc.sql.calls").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.jdbc.sql.calls").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("afg.jdbc.sql.rows").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.jdbc.sql.rows").counter().count()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("应该记录 SQL 执行耗时")
        void shouldRecordDuration() {
            // When
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(50), 0L);

            // Then
            Timer timer = meterRegistry.find("afg.jdbc.sql.duration").timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该记录 SQL 执行错误指标")
        void shouldRecordErrorMetrics() {
            // When
            sqlMetrics.recordError("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(10), "Connection failed");

            // Then
            assertThat(meterRegistry.find("afg.jdbc.sql.errors").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.jdbc.sql.errors").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("应该记录慢查询指标")
        void shouldRecordSlowQuery() {
            // Given - 慢查询阈值为 100ms
            properties.setSlowQueryThreshold(Duration.ofMillis(100));

            // When - 执行时间超过阈值
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(150), 0L);

            // Then
            assertThat(meterRegistry.find("afg.jdbc.sql.slow").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.jdbc.sql.slow").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("禁用指标时不记录")
        void shouldNotRecordWhenDisabled() {
            // Given
            properties.setEnabled(false);

            // When
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(50), 10L);

            // Then
            assertThat(meterRegistry.find("afg.jdbc.sql.calls").counter()).isNull();
        }

        @Test
        @DisplayName("应该按操作类型分类记录指标")
        void shouldRecordByOperationType() {
            // When
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.SELECT, Duration.ofMillis(10), 0L);
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.INSERT, Duration.ofMillis(20), 1L);
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.UPDATE, Duration.ofMillis(30), 1L);
            sqlMetrics.recordSuccess("TestEntity", SqlOperationType.DELETE, Duration.ofMillis(40), 1L);

            // Then
            var counters = meterRegistry.find("afg.jdbc.sql.calls").counters();
            assertThat(counters).hasSize(4);
        }

        @Test
        @DisplayName("应该获取配置属性")
        void shouldGetProperties() {
            assertThat(sqlMetrics.getProperties()).isEqualTo(properties);
        }

        @Test
        @DisplayName("应该获取 MeterRegistry")
        void shouldGetMeterRegistry() {
            assertThat(sqlMetrics.getMeterRegistry()).isEqualTo(meterRegistry);
        }

        @Test
        @DisplayName("应该设置慢查询监听器")
        void shouldSetSlowQueryListener() {
            StringBuilder logBuilder = new StringBuilder();
            sqlMetrics.setSlowQueryListener(log -> logBuilder.append(log.sql()));

            SlowQueryLog log = SlowQueryLog.of("SELECT * FROM test", SqlOperationType.SELECT, Duration.ofMillis(200), null, "Test");
            sqlMetrics.logSlowQuery(log);

            assertThat(logBuilder.toString()).isEqualTo("SELECT * FROM test");
        }

        @Test
        @DisplayName("禁用慢查询日志时不记录")
        void shouldNotLogWhenLogSlowQueriesDisabled() {
            properties.setLogSlowQueries(false);
            sqlMetrics = new SqlMetrics(meterRegistry, properties);

            StringBuilder logBuilder = new StringBuilder();
            sqlMetrics.setSlowQueryListener(log -> logBuilder.append(log.sql()));

            SlowQueryLog log = SlowQueryLog.of("SELECT * FROM test", SqlOperationType.SELECT, Duration.ofMillis(200), null, "Test");
            sqlMetrics.logSlowQuery(log);

            assertThat(logBuilder.toString()).isEmpty();
        }

        @Test
        @DisplayName("无监听器时 logSlowQuery 不抛异常")
        void shouldNotThrowWhenNoListener() {
            sqlMetrics.setSlowQueryListener(null);

            SlowQueryLog log = SlowQueryLog.of("SELECT * FROM test", SqlOperationType.SELECT, Duration.ofMillis(200), null, "Test");
            sqlMetrics.logSlowQuery(log);
            // 不应抛出异常
        }
    }

    @Nested
    @DisplayName("SqlOperationType 测试")
    class SqlOperationTypeTest {

        @Test
        @DisplayName("应该从 SELECT SQL 推断操作类型")
        void shouldInferSelectType() {
            assertThat(SqlOperationType.fromSql("SELECT * FROM users")).isEqualTo(SqlOperationType.SELECT);
            assertThat(SqlOperationType.fromSql("  select id from table  ")).isEqualTo(SqlOperationType.SELECT);
        }

        @Test
        @DisplayName("应该从 INSERT SQL 推断操作类型")
        void shouldInferInsertType() {
            assertThat(SqlOperationType.fromSql("INSERT INTO users VALUES (1)")).isEqualTo(SqlOperationType.INSERT);
            assertThat(SqlOperationType.fromSql("insert into table (id) values (1)")).isEqualTo(SqlOperationType.INSERT);
        }

        @Test
        @DisplayName("应该从 UPDATE SQL 推断操作类型")
        void shouldInferUpdateType() {
            assertThat(SqlOperationType.fromSql("UPDATE users SET name = 'test'")).isEqualTo(SqlOperationType.UPDATE);
            assertThat(SqlOperationType.fromSql("update table set id = 1")).isEqualTo(SqlOperationType.UPDATE);
        }

        @Test
        @DisplayName("应该从 DELETE SQL 推断操作类型")
        void shouldInferDeleteType() {
            assertThat(SqlOperationType.fromSql("DELETE FROM users WHERE id = 1")).isEqualTo(SqlOperationType.DELETE);
            assertThat(SqlOperationType.fromSql("delete from table")).isEqualTo(SqlOperationType.DELETE);
        }

        @Test
        @DisplayName("未知 SQL 应该返回 OTHER 类型")
        void shouldReturnOtherForUnknown() {
            assertThat(SqlOperationType.fromSql("TRUNCATE TABLE users")).isEqualTo(SqlOperationType.OTHER);
            assertThat(SqlOperationType.fromSql("CALL procedure()")).isEqualTo(SqlOperationType.OTHER);
        }
    }

    @Nested
    @DisplayName("SlowQueryLog 测试")
    class SlowQueryLogTest {

        @Test
        @DisplayName("应该创建慢查询日志")
        void shouldCreateSlowQueryLog() {
            // When
            SlowQueryLog log = SlowQueryLog.of(
                    "SELECT * FROM users",
                    SqlOperationType.SELECT,
                    Duration.ofMillis(1500),
                    new Object[]{1L, "test"},
                    "User"
            );

            // Then
            assertThat(log.sql()).isEqualTo("SELECT * FROM users");
            assertThat(log.operationType()).isEqualTo(SqlOperationType.SELECT);
            assertThat(log.durationMillis()).isEqualTo(1500L);
            assertThat(log.params()).hasSize(2);
            assertThat(log.entityName()).isEqualTo("User");
            assertThat(log.isError()).isFalse();
        }

        @Test
        @DisplayName("应该格式化参数")
        void shouldFormatParams() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                    "SELECT * FROM users WHERE id = ? AND name = ?",
                    SqlOperationType.SELECT,
                    Duration.ofMillis(1500),
                    new Object[]{1L, "test"},
                    "User"
            );

            // When
            String formatted = log.formatParams();

            // Then
            assertThat(formatted).isEqualTo("[1, 'test']");
        }

        @Test
        @DisplayName("应该处理空参数")
        void shouldHandleEmptyParams() {
            // When
            SlowQueryLog log = SlowQueryLog.of(
                    "SELECT * FROM users",
                    SqlOperationType.SELECT,
                    Duration.ofMillis(1500),
                    null,
                    "User"
            );

            // Then
            assertThat(log.formatParams()).isEqualTo("[]");
        }

        @Test
        @DisplayName("应该创建错误日志")
        void shouldCreateErrorLog() {
            // When
            SlowQueryLog log = SlowQueryLog.ofError(
                    "SELECT * FROM users",
                    SqlOperationType.SELECT,
                    Duration.ofMillis(100),
                    null,
                    "User",
                    "Connection failed"
            );

            // Then
            assertThat(log.isError()).isTrue();
            assertThat(log.errorMessage()).isEqualTo("Connection failed");
        }
    }

    @Nested
    @DisplayName("SqlMetricsProperties 测试")
    class SqlMetricsPropertiesTest {

        @Test
        @DisplayName("应该使用默认配置")
        void shouldUseDefaultValues() {
            // When
            SqlMetricsProperties props = new SqlMetricsProperties();

            // Then
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(1000));
            assertThat(props.isLogSlowQueries()).isTrue();
            assertThat(props.getMaxSlowQueryLogs()).isEqualTo(100);
            assertThat(props.isLogSqlParams()).isFalse();
        }
    }

    /**
     * 测试实体类
     */
    @Data
    @NoArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private String email;
    }
}
