package io.github.afgprojects.framework.integration.event.rabbitmq.health;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RabbitMQHealthIndicator 单元测试
 */
@DisplayName("RabbitMQHealthIndicator 测试")
class RabbitMQHealthIndicatorTest {

    private RabbitMQHealthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RabbitMQHealthProperties();
        properties.setConnectionTimeout(3000);
        properties.setQueuesToCheck(Collections.emptySet());
        properties.setExchangesToCheck(Collections.emptySet());
        properties.setFailOnMissingQueues(false);
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertiesTest {

        @Test
        @DisplayName("默认配置应该正确")
        void defaultConfigShouldBeCorrect() {
            // given - 使用默认配置
            RabbitMQHealthProperties defaultProps = new RabbitMQHealthProperties();

            // then
            assertTrue(defaultProps.isEnabled());
            assertEquals(3000, defaultProps.getConnectionTimeout());
            assertTrue(defaultProps.getQueuesToCheck().isEmpty());
            assertTrue(defaultProps.getExchangesToCheck().isEmpty());
            assertFalse(defaultProps.isFailOnMissingQueues());
            assertEquals(1000, defaultProps.getResponseTimeWarningThreshold());
            assertEquals(3000, defaultProps.getResponseTimeCriticalThreshold());
        }

        @Test
        @DisplayName("应该可以设置需要检查的队列")
        void shouldSetQueuesToCheck() {
            // given
            Set<String> queues = Set.of("test-queue", "another-queue");
            properties.setQueuesToCheck(queues);

            // then
            assertEquals(2, properties.getQueuesToCheck().size());
            assertTrue(properties.getQueuesToCheck().contains("test-queue"));
            assertTrue(properties.getQueuesToCheck().contains("another-queue"));
        }

        @Test
        @DisplayName("应该可以设置需要检查的交换器")
        void shouldSetExchangesToCheck() {
            // given
            Set<String> exchanges = Set.of("test-exchange");
            properties.setExchangesToCheck(exchanges);

            // then
            assertEquals(1, properties.getExchangesToCheck().size());
            assertTrue(properties.getExchangesToCheck().contains("test-exchange"));
        }
    }

    @Nested
    @DisplayName("响应时间阈值测试")
    class ResponseTimeTest {

        @Test
        @DisplayName("响应时间阈值应该可以配置")
        void responseTimeThresholdsShouldBeConfigurable() {
            // given
            properties.setResponseTimeWarningThreshold(500);
            properties.setResponseTimeCriticalThreshold(1000);

            // then
            assertEquals(500, properties.getResponseTimeWarningThreshold());
            assertEquals(1000, properties.getResponseTimeCriticalThreshold());
        }
    }
}
