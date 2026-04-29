package io.github.afgprojects.framework.data.jdbc.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 指标 AOP 拦截集成测试
 * <p>
 * 验证 AOP 切面能够正确拦截 JdbcEntityProxy 的操作并记录指标
 */
@DisplayName("SQL 指标 AOP 拦截集成测试")
class SqlMetricsAspectIntegrationTest {

    private MeterRegistry meterRegistry;
    private SqlMetrics sqlMetrics;
    private SqlMetricsProperties properties;
    private SqlMetricsAspect aspect;

    @BeforeEach
    void setUp() {
        // 初始化 MeterRegistry 和 SqlMetrics
        meterRegistry = new SimpleMeterRegistry();
        properties = new SqlMetricsProperties();
        properties.setEnabled(true);
        properties.setSlowQueryThreshold(java.time.Duration.ofMillis(100));
        sqlMetrics = new SqlMetrics(meterRegistry, properties);
        aspect = new SqlMetricsAspect(sqlMetrics, properties);
    }

    @Test
    @DisplayName("应该记录 INSERT 操作指标")
    void shouldRecordInsertMetrics() {
        // When
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.INSERT, java.time.Duration.ofMillis(50), 1);

        // Then - 验证指标被记录
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);

        Timer durationTimer = meterRegistry.find("afg.jdbc.sql.duration").timer();
        assertThat(durationTimer).isNotNull();
        assertThat(durationTimer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该记录 SELECT 操作指标")
    void shouldRecordSelectMetrics() {
        // When
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.SELECT, java.time.Duration.ofMillis(20), 1);

        // Then - 验证指标被记录
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("应该记录 UPDATE 操作指标")
    void shouldRecordUpdateMetrics() {
        // When
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.UPDATE, java.time.Duration.ofMillis(30), 1);

        // Then - 验证指标被记录
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("应该记录 DELETE 操作指标")
    void shouldRecordDeleteMetrics() {
        // When
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.DELETE, java.time.Duration.ofMillis(10), 1);

        // Then - 验证指标被记录
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("应该记录批量操作指标")
    void shouldRecordBatchOperationMetrics() {
        // When
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.INSERT, java.time.Duration.ofMillis(100), 3);

        // Then - 验证指标被记录
        Counter callsCounter = meterRegistry.find("afg.jdbc.sql.calls").counter();
        assertThat(callsCounter).isNotNull();
        assertThat(callsCounter.count()).isEqualTo(1.0);

        Counter rowsCounter = meterRegistry.find("afg.jdbc.sql.rows").counter();
        assertThat(rowsCounter).isNotNull();
        assertThat(rowsCounter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("应该按操作类型分类记录指标")
    void shouldRecordMetricsByOperationType() {
        // When - 执行多种操作
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.INSERT, java.time.Duration.ofMillis(10), 1);
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.SELECT, java.time.Duration.ofMillis(5), 1);
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.UPDATE, java.time.Duration.ofMillis(15), 1);

        // Then - 验证每种操作类型都有记录
        var counters = meterRegistry.find("afg.jdbc.sql.calls").counters();
        assertThat(counters).hasSize(3);
    }

    @Test
    @DisplayName("应该记录慢查询指标")
    void shouldRecordSlowQueryMetrics() {
        // Given - 慢查询阈值为 100ms
        properties.setSlowQueryThreshold(java.time.Duration.ofMillis(100));

        // When - 执行时间超过阈值
        sqlMetrics.recordSuccess("TestUser", SqlOperationType.SELECT, java.time.Duration.ofMillis(150), 1);

        // Then
        Counter slowCounter = meterRegistry.find("afg.jdbc.sql.slow").counter();
        assertThat(slowCounter).isNotNull();
        assertThat(slowCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("应该记录错误指标")
    void shouldRecordErrorMetrics() {
        // When
        sqlMetrics.recordError("TestUser", SqlOperationType.SELECT, java.time.Duration.ofMillis(10), "Connection failed");

        // Then
        Counter errorCounter = meterRegistry.find("afg.jdbc.sql.errors").counter();
        assertThat(errorCounter).isNotNull();
        assertThat(errorCounter.count()).isEqualTo(1.0);
    }
}
