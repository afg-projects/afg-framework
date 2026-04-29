package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.afgprojects.framework.core.web.health.DataSourceHealthIndicator;
import io.github.afgprojects.framework.core.web.health.DataSourceHealthProperties;

/**
 * DataSourceHealthIndicator 集成测试
 * 使用真实的 HikariDataSource 和 H2 数据库验证健康检查功能
 */
@DisplayName("DataSourceHealthIndicator 集成测试")
class DataSourceHealthIntegrationTest {

    private HikariDataSource dataSource;
    private DataSourceHealthProperties properties;
    private DataSourceHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("testPool");
        dataSource = new HikariDataSource(config);

        properties = new DataSourceHealthProperties();
        healthIndicator = new DataSourceHealthIndicator(dataSource, properties);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Nested
    @DisplayName("HikariCP 连接池集成测试")
    class HikariIntegrationTest {

        @Test
        @DisplayName("健康检查应该返回 UP 状态")
        void healthCheckShouldReturnUp() {
            Health health = healthIndicator.health();
            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("应该包含数据库状态")
        void shouldContainDatabaseStatus() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("database")).isEqualTo("UP");
        }

        @Test
        @DisplayName("应该包含验证查询")
        void shouldContainValidationQuery() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("validationQuery")).isEqualTo("SELECT 1");
        }

        @Test
        @DisplayName("应该包含响应时间")
        void shouldContainResponseTime() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("responseTime")).isNotNull();
        }
    }

    @Nested
    @DisplayName("连接池详情测试")
    class PoolDetailsTest {

        @Test
        @DisplayName("应该包含 HikariCP 标识")
        void shouldContainHikariPoolType() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("poolType")).isEqualTo("HikariCP");
        }

        @Test
        @DisplayName("应该包含连接池运行状态")
        void shouldContainPoolStatus() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("poolStatus")).isEqualTo("RUNNING");
        }

        @Test
        @DisplayName("应该包含活跃连接数")
        void shouldContainActiveConnections() {
            Health health = healthIndicator.health();
            Integer activeConnections = (Integer) health.getDetails().get("activeConnections");
            assertThat(activeConnections).isNotNull();
            assertThat(activeConnections).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("应该包含空闲连接数")
        void shouldContainIdleConnections() {
            Health health = healthIndicator.health();
            Integer idleConnections = (Integer) health.getDetails().get("idleConnections");
            assertThat(idleConnections).isNotNull();
            assertThat(idleConnections).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("应该包含最大连接数")
        void shouldContainMaximumPoolSize() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("maximumPoolSize")).isEqualTo(10);
        }

        @Test
        @DisplayName("应该包含最小空闲连接数")
        void shouldContainMinimumIdle() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("minimumIdle")).isEqualTo(2);
        }

        @Test
        @DisplayName("应该包含连接池使用率")
        void shouldContainPoolUsagePercent() {
            Health health = healthIndicator.health();
            String usage = (String) health.getDetails().get("poolUsagePercent");
            assertThat(usage).isNotNull();
            assertThat(usage).endsWith("%");
        }

        @Test
        @DisplayName("应该包含等待线程数")
        void shouldContainThreadsAwaitingConnection() {
            Health health = healthIndicator.health();
            Integer threads = (Integer) health.getDetails().get("threadsAwaitingConnection");
            assertThat(threads).isNotNull();
            assertThat(threads).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("应该包含连接池健康状态")
        void shouldContainPoolHealth() {
            Health health = healthIndicator.health();
            assertThat(health.getDetails().get("poolHealth")).isEqualTo("NORMAL");
        }
    }

    @Nested
    @DisplayName("连接池使用测试")
    class ConnectionUsageTest {

        @Test
        @DisplayName("活跃连接时应该正确统计")
        void shouldCountActiveConnections() throws SQLException {
            // given - 获取一个连接
            Connection connection = dataSource.getConnection();

            try {
                // when
                Health health = healthIndicator.health();

                // then
                Integer activeConnections = (Integer) health.getDetails().get("activeConnections");
                assertThat(activeConnections).isGreaterThanOrEqualTo(1);
            } finally {
                connection.close();
            }
        }

        @Test
        @DisplayName("高使用率时应该报告警告")
        void shouldReportWarningWhenHighUsage() throws SQLException {
            // given - 修改警告阈值
            properties.setPoolUsageWarningThreshold(5);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(dataSource, properties);

            // 获取多个连接提高使用率
            Connection[] connections = new Connection[8];
            for (int i = 0; i < 8; i++) {
                connections[i] = dataSource.getConnection();
            }

            try {
                // when
                Health health = indicator.health();

                // then
                String poolHealth = (String) health.getDetails().get("poolHealth");
                assertThat(poolHealth).isIn("WARNING", "CRITICAL");
            } finally {
                for (Connection conn : connections) {
                    if (conn != null) {
                        conn.close();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("配置验证测试")
    class ConfigurationTest {

        @Test
        @DisplayName("自定义验证查询应该生效")
        void customValidationQueryShouldWork() {
            // given
            properties.setValidationQuery("SELECT 1 FROM DUAL");
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(dataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertThat(health.getDetails().get("validationQuery")).isEqualTo("SELECT 1 FROM DUAL");
        }

        @Test
        @DisplayName("自定义阈值应该生效")
        void customThresholdsShouldWork() {
            // given
            properties.setPoolUsageWarningThreshold(50);
            properties.setPoolUsageCriticalThreshold(80);
            properties.setThreadsAwaitingWarningThreshold(3);
            properties.setThreadsAwaitingCriticalThreshold(8);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(dataSource, properties);

            // when
            Health health = indicator.health();

            // then - 低使用率时应该正常
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails().get("poolHealth")).isEqualTo("NORMAL");
        }
    }
}