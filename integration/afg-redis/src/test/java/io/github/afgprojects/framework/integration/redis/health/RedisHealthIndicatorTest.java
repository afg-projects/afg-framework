package io.github.afgprojects.framework.integration.redis.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.NodesGroup;
import org.redisson.api.RedissonClient;
import org.redisson.api.Node;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

/**
 * RedisHealthIndicator 单元测试
 */
@DisplayName("RedisHealthIndicator 测试")
class RedisHealthIndicatorTest {

    private RedisHealthProperties properties;
    private RedissonClient mockRedissonClient;
    private NodesGroup<Node> mockNodesGroup;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        properties = new RedisHealthProperties();
        properties.setConnectionTimeout(3000);
        properties.setIncludeServerInfo(false);

        mockRedissonClient = mock(RedissonClient.class);
        mockNodesGroup = mock(NodesGroup.class);
        when(mockRedissonClient.getNodesGroup()).thenReturn(mockNodesGroup);
    }

    @Nested
    @DisplayName("基础连接检查测试")
    class BasicConnectionTest {

        @Test
        @DisplayName("连接有效时应该返回 UP 状态")
        void shouldReturnUpWhenConnectionValid() {
            // given
            when(mockNodesGroup.pingAll(anyLong(), any())).thenReturn(true);
            RedisHealthIndicator indicator = new RedisHealthIndicator(mockRedissonClient, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.UP, health.getStatus());
            assertEquals("UP", health.getDetails().get("redis"));
            assertNotNull(health.getDetails().get("responseTime"));
        }

        @Test
        @DisplayName("Ping 失败时应该返回 DOWN 状态")
        void shouldReturnDownWhenPingFailed() {
            // given
            when(mockNodesGroup.pingAll(anyLong(), any())).thenReturn(false);
            RedisHealthIndicator indicator = new RedisHealthIndicator(mockRedissonClient, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("redis"));
            assertEquals("Ping failed", health.getDetails().get("error"));
        }

        @Test
        @DisplayName("抛出异常时应该返回 DOWN 状态")
        void shouldReturnDownWhenExceptionThrown() {
            // given
            when(mockNodesGroup.pingAll(anyLong(), any())).thenThrow(new RuntimeException("Connection refused"));
            RedisHealthIndicator indicator = new RedisHealthIndicator(mockRedissonClient, properties);

            // when
            Health health = indicator.health();

            // then
            assertEquals(Status.DOWN, health.getStatus());
            assertEquals("DOWN", health.getDetails().get("redis"));
            assertEquals("Connection refused", health.getDetails().get("error"));
        }
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertiesTest {

        @Test
        @DisplayName("默认配置应该正确")
        void defaultConfigShouldBeCorrect() {
            // given - 使用默认配置
            RedisHealthProperties defaultProps = new RedisHealthProperties();

            // then
            assertTrue(defaultProps.isEnabled());
            assertEquals(3000, defaultProps.getConnectionTimeout());
            assertTrue(defaultProps.isIncludeServerInfo());
            assertEquals(1000, defaultProps.getResponseTimeWarningThreshold());
            assertEquals(3000, defaultProps.getResponseTimeCriticalThreshold());
        }
    }
}
