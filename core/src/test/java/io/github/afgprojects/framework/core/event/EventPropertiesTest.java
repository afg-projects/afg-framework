package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * EventProperties 测试
 */
@DisplayName("EventProperties 测试")
class EventPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("enabled 默认为 true")
        void enabledDefaultIsTrue() {
            // given
            AfgCoreProperties.EventConfig properties = new AfgCoreProperties.EventConfig();

            // then
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("type 默认为 LOCAL")
        void typeDefaultIsLocal() {
            // given
            AfgCoreProperties.EventConfig properties = new AfgCoreProperties.EventConfig();

            // then
            assertThat(properties.getType()).isEqualTo(AfgCoreProperties.EventConfig.EventType.LOCAL);
        }

        @Test
        @DisplayName("defaultTopic 默认为 afg.events")
        void defaultTopicDefaultIsAfgEvents() {
            // given
            AfgCoreProperties.EventConfig properties = new AfgCoreProperties.EventConfig();

            // then
            assertThat(properties.getDefaultTopic()).isEqualTo("afg.events");
        }
    }

    @Nested
    @DisplayName("本地配置测试")
    class LocalConfigTests {

        @Test
        @DisplayName("async 默认为 false")
        void asyncDefaultIsFalse() {
            // given
            AfgCoreProperties.EventConfig.LocalEventConfig config = new AfgCoreProperties.EventConfig.LocalEventConfig();

            // then
            assertThat(config.isAsync()).isFalse();
        }

        @Test
        @DisplayName("threadPoolSize 默认为 4")
        void threadPoolSizeDefaultIs4() {
            // given
            AfgCoreProperties.EventConfig.LocalEventConfig config = new AfgCoreProperties.EventConfig.LocalEventConfig();

            // then
            assertThat(config.getThreadPoolSize()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Kafka 配置测试")
    class KafkaConfigTests {

        @Test
        @DisplayName("bootstrapServers 默认为 null")
        void bootstrapServersDefaultIsNull() {
            // given
            AfgCoreProperties.EventConfig.KafkaEventConfig config = new AfgCoreProperties.EventConfig.KafkaEventConfig();

            // then
            assertThat(config.getBootstrapServers()).isNull();
        }

        @Test
        @DisplayName("autoCreateTopics 默认为 true")
        void autoCreateTopicsDefaultIsTrue() {
            // given
            AfgCoreProperties.EventConfig.KafkaEventConfig config = new AfgCoreProperties.EventConfig.KafkaEventConfig();

            // then
            assertThat(config.isAutoCreateTopics()).isTrue();
        }

        @Test
        @DisplayName("partitions 默认为 3")
        void partitionsDefaultIs3() {
            // given
            AfgCoreProperties.EventConfig.KafkaEventConfig config = new AfgCoreProperties.EventConfig.KafkaEventConfig();

            // then
            assertThat(config.getPartitions()).isEqualTo(3);
        }

        @Test
        @DisplayName("replicationFactor 默认为 1")
        void replicationFactorDefaultIs1() {
            // given
            AfgCoreProperties.EventConfig.KafkaEventConfig config = new AfgCoreProperties.EventConfig.KafkaEventConfig();

            // then
            assertThat(config.getReplicationFactor()).isEqualTo((short) 1);
        }
    }

    @Nested
    @DisplayName("RabbitMQ 配置测试")
    class RabbitMQConfigTests {

        @Test
        @DisplayName("host 默认为 localhost")
        void hostDefaultIsLocalhost() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getHost()).isEqualTo("localhost");
        }

        @Test
        @DisplayName("port 默认为 5672")
        void portDefaultIs5672() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getPort()).isEqualTo(5672);
        }

        @Test
        @DisplayName("username 默认为 guest")
        void usernameDefaultIsGuest() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getUsername()).isEqualTo("guest");
        }

        @Test
        @DisplayName("password 默认为 guest")
        void passwordDefaultIsGuest() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getPassword()).isEqualTo("guest");
        }

        @Test
        @DisplayName("exchange 默认为 afg.events")
        void exchangeDefaultIsAfgEvents() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getExchange()).isEqualTo("afg.events");
        }

        @Test
        @DisplayName("ackMode 默认为 AUTO")
        void ackModeDefaultIsAuto() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getAckMode()).isEqualTo(AfgCoreProperties.EventConfig.AckMode.AUTO);
        }

        @Test
        @DisplayName("prefetchCount 默认为 10")
        void prefetchCountDefaultIs10() {
            // given
            AfgCoreProperties.EventConfig.RabbitMqEventConfig config = new AfgCoreProperties.EventConfig.RabbitMqEventConfig();

            // then
            assertThat(config.getPrefetchCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("重试配置测试")
    class RetryConfigTests {

        @Test
        @DisplayName("enabled 默认为 true")
        void enabledDefaultIsTrue() {
            // given
            AfgCoreProperties.EventConfig.EventRetryConfig config = new AfgCoreProperties.EventConfig.EventRetryConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("maxAttempts 默认为 3")
        void maxAttemptsDefaultIs3() {
            // given
            AfgCoreProperties.EventConfig.EventRetryConfig config = new AfgCoreProperties.EventConfig.EventRetryConfig();

            // then
            assertThat(config.getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("initialInterval 默认为 1000")
        void initialIntervalDefaultIs1000() {
            // given
            AfgCoreProperties.EventConfig.EventRetryConfig config = new AfgCoreProperties.EventConfig.EventRetryConfig();

            // then
            assertThat(config.getInitialInterval()).isEqualTo(1000);
        }

        @Test
        @DisplayName("multiplier 默认为 2.0")
        void multiplierDefaultIs2() {
            // given
            AfgCoreProperties.EventConfig.EventRetryConfig config = new AfgCoreProperties.EventConfig.EventRetryConfig();

            // then
            assertThat(config.getMultiplier()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("死信队列配置测试")
    class DeadLetterConfigTests {

        @Test
        @DisplayName("enabled 默认为 true")
        void enabledDefaultIsTrue() {
            // given
            AfgCoreProperties.EventConfig.DeadLetterConfig config = new AfgCoreProperties.EventConfig.DeadLetterConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("topicPrefix 默认为 dlq.")
        void topicPrefixDefaultIsDlq() {
            // given
            AfgCoreProperties.EventConfig.DeadLetterConfig config = new AfgCoreProperties.EventConfig.DeadLetterConfig();

            // then
            assertThat(config.getTopicPrefix()).isEqualTo("dlq.");
        }

        @Test
        @DisplayName("retentionMs 默认为 0")
        void retentionMsDefaultIs0() {
            // given
            AfgCoreProperties.EventConfig.DeadLetterConfig config = new AfgCoreProperties.EventConfig.DeadLetterConfig();

            // then
            assertThat(config.getRetentionMs()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("AckMode 枚举测试")
    class AckModeEnumTests {

        @Test
        @DisplayName("应该包含 AUTO、MANUAL、BATCH")
        void shouldContainAllValues() {
            // then
            assertThat(AfgCoreProperties.EventConfig.AckMode.values())
                    .containsExactlyInAnyOrder(
                            AfgCoreProperties.EventConfig.AckMode.AUTO,
                            AfgCoreProperties.EventConfig.AckMode.MANUAL,
                            AfgCoreProperties.EventConfig.AckMode.BATCH);
        }
    }

    @Nested
    @DisplayName("EventType 枚举测试")
    class EventTypeEnumTests {

        @Test
        @DisplayName("应该包含 LOCAL、KAFKA、RABBITMQ")
        void shouldContainAllValues() {
            // then
            assertThat(AfgCoreProperties.EventConfig.EventType.values())
                    .containsExactlyInAnyOrder(
                            AfgCoreProperties.EventConfig.EventType.LOCAL,
                            AfgCoreProperties.EventConfig.EventType.KAFKA,
                            AfgCoreProperties.EventConfig.EventType.RABBITMQ);
        }
    }
}