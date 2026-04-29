package io.github.afgprojects.framework.integration.event.rabbitmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitMQEventProperties 单元测试
 */
@DisplayName("RabbitMQEventProperties 测试")
class RabbitMQEventPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            // When
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // Then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getExchange()).isEqualTo("afg-events");
            assertThat(properties.getDefaultRoutingKey()).isEqualTo("event.default");
            assertThat(properties.isIncludeMetadata()).isTrue();
            assertThat(properties.getMessageTtl()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTests {

        @Test
        @DisplayName("应该能设置 enabled 属性")
        void shouldSetEnabled() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // When
            properties.setEnabled(false);

            // Then
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该能设置 exchange 属性")
        void shouldSetExchange() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();
            String customExchange = "custom-events";

            // When
            properties.setExchange(customExchange);

            // Then
            assertThat(properties.getExchange()).isEqualTo(customExchange);
        }

        @Test
        @DisplayName("应该能设置 defaultRoutingKey 属性")
        void shouldSetDefaultRoutingKey() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();
            String customRoutingKey = "custom.routing.key";

            // When
            properties.setDefaultRoutingKey(customRoutingKey);

            // Then
            assertThat(properties.getDefaultRoutingKey()).isEqualTo(customRoutingKey);
        }

        @Test
        @DisplayName("应该能设置 includeMetadata 属性")
        void shouldSetIncludeMetadata() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // When
            properties.setIncludeMetadata(false);

            // Then
            assertThat(properties.isIncludeMetadata()).isFalse();
        }

        @Test
        @DisplayName("应该能设置 messageTtl 属性")
        void shouldSetMessageTtl() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();
            long ttl = 60000L;

            // When
            properties.setMessageTtl(ttl);

            // Then
            assertThat(properties.getMessageTtl()).isEqualTo(ttl);
        }
    }

    @Nested
    @DisplayName("配置场景测试")
    class ConfigurationScenarioTests {

        @Test
        @DisplayName("应该支持生产环境配置")
        void shouldSupportProductionConfiguration() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // When - production config
            properties.setEnabled(true);
            properties.setExchange("prod-events");
            properties.setDefaultRoutingKey("prod.default");
            properties.setIncludeMetadata(true);
            properties.setMessageTtl(86400000L); // 24 hours

            // Then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getExchange()).isEqualTo("prod-events");
            assertThat(properties.getDefaultRoutingKey()).isEqualTo("prod.default");
            assertThat(properties.isIncludeMetadata()).isTrue();
            assertThat(properties.getMessageTtl()).isEqualTo(86400000L);
        }

        @Test
        @DisplayName("应该支持开发环境配置")
        void shouldSupportDevelopmentConfiguration() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // When - development config
            properties.setEnabled(true);
            properties.setExchange("dev-events");
            properties.setDefaultRoutingKey("dev.default");
            properties.setIncludeMetadata(false);
            properties.setMessageTtl(0); // no TTL

            // Then
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getExchange()).isEqualTo("dev-events");
            assertThat(properties.isIncludeMetadata()).isFalse();
            assertThat(properties.getMessageTtl()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该支持禁用配置")
        void shouldSupportDisabledConfiguration() {
            // Given
            RabbitMQEventProperties properties = new RabbitMQEventProperties();

            // When
            properties.setEnabled(false);

            // Then
            assertThat(properties.isEnabled()).isFalse();
        }
    }
}
