package io.github.afgprojects.framework.integration.kafka.health;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * KafkaHealthIndicator 单元测试
 */
@DisplayName("KafkaHealthIndicator 测试")
class KafkaHealthIndicatorTest {

    private KafkaHealthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KafkaHealthProperties();
        properties.setTimeout(5000);
        properties.setTopicsToCheck(Collections.emptySet());
        properties.setFailOnMissingTopics(false);
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertiesTest {

        @Test
        @DisplayName("默认配置应该正确")
        void defaultConfigShouldBeCorrect() {
            // given - 使用默认配置
            KafkaHealthProperties defaultProps = new KafkaHealthProperties();

            // then
            assertTrue(defaultProps.isEnabled());
            assertEquals(5000, defaultProps.getTimeout());
            assertTrue(defaultProps.getTopicsToCheck().isEmpty());
            assertFalse(defaultProps.isFailOnMissingTopics());
            assertEquals(1000, defaultProps.getResponseTimeWarningThreshold());
            assertEquals(3000, defaultProps.getResponseTimeCriticalThreshold());
        }

        @Test
        @DisplayName("应该可以设置需要检查的主题")
        void shouldSetTopicsToCheck() {
            // given
            Set<String> topics = Set.of("test-topic", "another-topic");
            properties.setTopicsToCheck(topics);

            // then
            assertEquals(2, properties.getTopicsToCheck().size());
            assertTrue(properties.getTopicsToCheck().contains("test-topic"));
            assertTrue(properties.getTopicsToCheck().contains("another-topic"));
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
