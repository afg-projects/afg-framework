package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * DataSourceHealthIndicator 单元测试
 */
@DisplayName("DataSourceHealthIndicator 测试")
class DataSourceHealthIndicatorTest extends BaseUnitTest {

    private DataSourceHealthProperties properties;
    private DataSource mockDataSource;
    private Connection mockConnection;

    @BeforeEach
    void setUp() throws SQLException {
        properties = new DataSourceHealthProperties();
        properties.setValidationQuery("SELECT 1");
        properties.setPoolUsageWarningThreshold(70);
        properties.setPoolUsageCriticalThreshold(90);
        properties.setThreadsAwaitingWarningThreshold(5);
        properties.setThreadsAwaitingCriticalThreshold(10);
        properties.setConnectionTimeout(3000);

        mockDataSource = mock(DataSource.class);
        mockConnection = mock(Connection.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
    }

    @Nested
    @DisplayName("基础连接检查测试")
    class BasicConnectionTest {

        @Test
        @DisplayName("连接有效时应该返回 UP 状态")
        void shouldReturnUpWhenConnectionValid() throws SQLException {
            // given
            when(mockConnection.isValid(anyInt())).thenReturn(true);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(mockDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("UP", health.getDetails().get("database"));
            assertNotNull(health.getDetails().get("responseTime"));
            assertEquals("SELECT 1", health.getDetails().get("validationQuery"));
        }

        @Test
        @DisplayName("连接无效时应该返回 DOWN 状态")
        void shouldReturnDownWhenConnectionInvalid() throws SQLException {
            // given
            when(mockConnection.isValid(anyInt())).thenReturn(false);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(mockDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("database"));
            assertEquals("Connection validation failed", health.getDetails().get("error"));
        }

        @Test
        @DisplayName("获取连接失败时应该返回 DOWN 状态")
        void shouldReturnDownWhenConnectionFailed() throws SQLException {
            // given
            when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection refused"));
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(mockDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("database"));
            assertEquals("Connection refused", health.getDetails().get("error"));
        }

        @Test
        @DisplayName("非 HikariCP 数据源应该显示 poolType")
        void shouldShowPoolTypeForNonHikariDataSource() throws SQLException {
            // given
            when(mockConnection.isValid(anyInt())).thenReturn(true);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(mockDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertNotNull(health.getDetails().get("poolType"));
            assertEquals(false, health.getDetails().get("hikariSupported"));
        }
    }

    @Nested
    @DisplayName("HikariCP 连接池检查测试")
    class HikariPoolTest {

        @Test
        @DisplayName("HikariCP 数据源应该返回连接池详情")
        void shouldReturnHikariPoolDetails() throws SQLException {
            // given - 使用真实的 HikariDataSource（内存数据库）
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            config.setDriverClassName("org.h2.Driver");
            config.setUsername("sa");
            config.setPassword("");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            HikariDataSource hikariDataSource = new HikariDataSource(config);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(hikariDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("HikariCP", health.getDetails().get("poolType"));
            assertEquals("RUNNING", health.getDetails().get("poolStatus"));
            assertNotNull(health.getDetails().get("activeConnections"));
            assertNotNull(health.getDetails().get("idleConnections"));
            assertNotNull(health.getDetails().get("totalConnections"));
            assertEquals(10, health.getDetails().get("maximumPoolSize"));
            assertEquals(2, health.getDetails().get("minimumIdle"));
            assertNotNull(health.getDetails().get("poolUsagePercent"));
            assertNotNull(health.getDetails().get("threadsAwaitingConnection"));

            // cleanup
            hikariDataSource.close();
        }

        @Test
        @DisplayName("连接池使用率低于警告阈值时应该返回 NORMAL")
        void shouldReturnNormalWhenPoolUsageLow() throws SQLException {
            // given
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1");
            config.setDriverClassName("org.h2.Driver");
            config.setUsername("sa");
            config.setPassword("");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            HikariDataSource hikariDataSource = new HikariDataSource(config);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(hikariDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("NORMAL", health.getDetails().get("poolHealth"));

            // cleanup
            hikariDataSource.close();
        }
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertiesTest {

        @Test
        @DisplayName("应该使用配置的验证查询")
        void shouldUseConfiguredValidationQuery() throws SQLException {
            // given
            properties.setValidationQuery("SELECT 1 FROM DUAL");
            when(mockConnection.isValid(anyInt())).thenReturn(true);
            DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(mockDataSource, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals("SELECT 1 FROM DUAL", health.getDetails().get("validationQuery"));
        }

        @Test
        @DisplayName("默认阈值配置应该正确")
        void defaultThresholdsShouldBeCorrect() {
            // given - 使用默认配置
            DataSourceHealthProperties defaultProps = new DataSourceHealthProperties();

            // then
            assertTrue(defaultProps.isEnabled());
            assertEquals("SELECT 1", defaultProps.getValidationQuery());
            assertEquals(70, defaultProps.getPoolUsageWarningThreshold());
            assertEquals(90, defaultProps.getPoolUsageCriticalThreshold());
            assertEquals(5, defaultProps.getThreadsAwaitingWarningThreshold());
            assertEquals(10, defaultProps.getThreadsAwaitingCriticalThreshold());
            assertEquals(3000, defaultProps.getConnectionTimeout());
        }
    }
}