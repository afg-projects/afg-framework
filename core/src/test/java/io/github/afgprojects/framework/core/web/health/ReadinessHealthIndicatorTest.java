package io.github.afgprojects.framework.core.web.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.NodesGroup;
import org.redisson.api.RedissonClient;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.support.TestDataFactory;

/**
 * ReadinessHealthIndicator 单元测试
 */
@DisplayName("ReadinessHealthIndicator 测试")
class ReadinessHealthIndicatorTest extends BaseUnitTest {

    private HealthCheckProperties properties;
    private DataSource dataSource;
    private RedissonClient redissonClient;
    private ModuleRegistry moduleRegistry;
    private ReadinessHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new HealthCheckProperties();
        dataSource = mock(DataSource.class);
        redissonClient = mock(RedissonClient.class);
        moduleRegistry = new ModuleRegistry();
    }

    @Nested
    @DisplayName("无依赖服务测试")
    class NoDependenciesTest {

        @Test
        @DisplayName("没有数据源时应该返回 UP")
        void shouldReturnUpWhenNoDataSource() {
            // given
            healthIndicator = new ReadinessHealthIndicator(properties, null, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
        }

        @Test
        @DisplayName("没有配置数据源时详情应该显示 NOT_CONFIGURED")
        void shouldShowNotConfiguredWhenNoDataSource() {
            // given
            healthIndicator = new ReadinessHealthIndicator(properties, null, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals("NOT_CONFIGURED", health.getDetails().get("database"));
            assertEquals("NOT_CONFIGURED", health.getDetails().get("redis"));
            assertEquals("NOT_CONFIGURED", health.getDetails().get("modules"));
        }
    }

    @Nested
    @DisplayName("数据库检查测试")
    class DatabaseCheckTest {

        @Test
        @DisplayName("数据库连接正常时应该返回 UP")
        void shouldReturnUpWhenDatabaseHealthy() throws SQLException {
            // given
            Connection connection = mock(Connection.class);
            when(connection.isValid(anyInt())).thenReturn(true);
            when(dataSource.getConnection()).thenReturn(connection);
            healthIndicator = new ReadinessHealthIndicator(properties, dataSource, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("UP", health.getDetails().get("database"));
        }

        @Test
        @DisplayName("数据库连接失败时应该返回 DOWN")
        void shouldReturnDownWhenDatabaseUnhealthy() throws SQLException {
            // given
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            healthIndicator = new ReadinessHealthIndicator(properties, dataSource, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("database"));
            assertTrue(health.getDetails().get("databaseError").toString().contains("Connection failed"));
        }

        @Test
        @DisplayName("数据库连接验证失败时应该返回 DOWN")
        void shouldReturnDownWhenDatabaseValidationFails() throws SQLException {
            // given
            Connection connection = mock(Connection.class);
            when(connection.isValid(anyInt())).thenReturn(false);
            when(dataSource.getConnection()).thenReturn(connection);
            healthIndicator = new ReadinessHealthIndicator(properties, dataSource, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("database"));
        }

        @Test
        @DisplayName("禁用数据库检查时应该不检查")
        void shouldNotCheckWhenDisabled() {
            // given
            properties.getReadiness().setDatabaseCheckEnabled(false);
            healthIndicator = new ReadinessHealthIndicator(properties, dataSource, null, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertFalse(health.getDetails().containsKey("database"));
        }
    }

    @Nested
    @DisplayName("Redis 检查测试")
    class RedisCheckTest {

        @Test
        @DisplayName("Redis 连接正常时应该返回 UP")
        @SuppressWarnings("unchecked")
        void shouldReturnUpWhenRedisHealthy() {
            // given
            NodesGroup<?> nodesGroup = mock(NodesGroup.class);
            when(nodesGroup.pingAll(anyLong(), any(TimeUnit.class))).thenReturn(true);
            doReturn(nodesGroup).when(redissonClient).getNodesGroup();
            healthIndicator = new ReadinessHealthIndicator(properties, null, redissonClient, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("UP", health.getDetails().get("redis"));
        }

        @Test
        @DisplayName("Redis ping 失败时应该返回 DOWN")
        @SuppressWarnings("unchecked")
        void shouldReturnDownWhenRedisPingFails() {
            // given
            NodesGroup<?> nodesGroup = mock(NodesGroup.class);
            when(nodesGroup.pingAll(anyLong(), any(TimeUnit.class))).thenReturn(false);
            doReturn(nodesGroup).when(redissonClient).getNodesGroup();
            healthIndicator = new ReadinessHealthIndicator(properties, null, redissonClient, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("redis"));
        }

        @Test
        @DisplayName("Redis 异常时应该返回 DOWN")
        @SuppressWarnings("unchecked")
        void shouldReturnDownWhenRedisException() {
            // given
            when(redissonClient.getNodesGroup()).thenThrow(new RuntimeException("Redis error"));
            healthIndicator = new ReadinessHealthIndicator(properties, null, redissonClient, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("redis"));
        }

        @Test
        @DisplayName("禁用 Redis 检查时应该不检查")
        void shouldNotCheckWhenDisabled() {
            // given
            properties.getReadiness().setRedisCheckEnabled(false);
            healthIndicator = new ReadinessHealthIndicator(properties, null, redissonClient, null);

            // when
            Health health = healthIndicator.health();

            // then
            assertFalse(health.getDetails().containsKey("redis"));
        }
    }

    @Nested
    @DisplayName("模块检查测试")
    class ModuleCheckTest {

        @Test
        @DisplayName("所有模块健康时应该返回 UP")
        void shouldReturnUpWhenAllModulesHealthy() {
            // given
            ModuleDefinition module1 = TestDataFactory.createModuleDefinition("module-1", "Module 1");
            ModuleDefinition module2 = TestDataFactory.createModuleDefinition("module-2", "Module 2");
            moduleRegistry.register(module1);
            moduleRegistry.register(module2);
            healthIndicator = new ReadinessHealthIndicator(properties, null, null, moduleRegistry);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals(2, health.getDetails().get("totalModules"));
            assertEquals(2, health.getDetails().get("upModules"));
            assertEquals(0, health.getDetails().get("downModules"));
        }

        @Test
        @DisplayName("没有模块时应该返回 UP")
        void shouldReturnUpWhenNoModules() {
            // given
            healthIndicator = new ReadinessHealthIndicator(properties, null, null, moduleRegistry);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals(0, health.getDetails().get("totalModules"));
        }

        @Test
        @DisplayName("禁用模块检查时应该不检查")
        void shouldNotCheckWhenDisabled() {
            // given
            properties.getReadiness().setModuleCheckEnabled(false);
            healthIndicator = new ReadinessHealthIndicator(properties, null, null, moduleRegistry);

            // when
            Health health = healthIndicator.health();

            // then
            assertFalse(health.getDetails().containsKey("modules"));
        }
    }

    @Nested
    @DisplayName("综合测试")
    class CombinedTest {

        @Test
        @DisplayName("所有检查正常时应该返回 UP")
        @SuppressWarnings("unchecked")
        void shouldReturnUpWhenAllChecksPass() throws SQLException {
            // given
            Connection connection = mock(Connection.class);
            when(connection.isValid(anyInt())).thenReturn(true);
            when(dataSource.getConnection()).thenReturn(connection);

            NodesGroup<?> nodesGroup = mock(NodesGroup.class);
            when(nodesGroup.pingAll(anyLong(), any(TimeUnit.class))).thenReturn(true);
            doReturn(nodesGroup).when(redissonClient).getNodesGroup();

            ModuleDefinition module = TestDataFactory.createModuleDefinition("test-module", "Test Module");
            moduleRegistry.register(module);

            healthIndicator = new ReadinessHealthIndicator(properties, dataSource, redissonClient, moduleRegistry);

            // when
            Health health = healthIndicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("UP", health.getDetails().get("database"));
            assertEquals("UP", health.getDetails().get("redis"));
            assertEquals(1, health.getDetails().get("totalModules"));
        }
    }
}
