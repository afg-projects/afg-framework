package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;

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
            EventProperties properties = new EventProperties();

            // then
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("type 默认为 LOCAL")
        void typeDefaultIsLocal() {
            // given
            EventProperties properties = new EventProperties();

            // then
            assertThat(properties.getType()).isEqualTo(EventProperties.EventType.LOCAL);
        }

        @Test
        @DisplayName("defaultTopic 默认为 afg.events")
        void defaultTopicDefaultIsAfgEvents() {
            // given
            EventProperties properties = new EventProperties();

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
            EventProperties.LocalConfig config = new EventProperties.LocalConfig();

            // then
            assertThat(config.isAsync()).isFalse();
        }

        @Test
        @DisplayName("threadPoolSize 默认为 4")
        void threadPoolSizeDefaultIs4() {
            // given
            EventProperties.LocalConfig config = new EventProperties.LocalConfig();

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
            EventProperties.KafkaConfig config = new EventProperties.KafkaConfig();

            // then
            assertThat(config.getBootstrapServers()).isNull();
        }

        @Test
        @DisplayName("autoCreateTopics 默认为 true")
        void autoCreateTopicsDefaultIsTrue() {
            // given
            EventProperties.KafkaConfig config = new EventProperties.KafkaConfig();

            // then
            assertThat(config.isAutoCreateTopics()).isTrue();
        }

        @Test
        @DisplayName("partitions 默认为 3")
        void partitionsDefaultIs3() {
            // given
            EventProperties.KafkaConfig config = new EventProperties.KafkaConfig();

            // then
            assertThat(config.getPartitions()).isEqualTo(3);
        }

        @Test
        @DisplayName("replicationFactor 默认为 1")
        void replicationFactorDefaultIs1() {
            // given
            EventProperties.KafkaConfig config = new EventProperties.KafkaConfig();

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
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getHost()).isEqualTo("localhost");
        }

        @Test
        @DisplayName("port 默认为 5672")
        void portDefaultIs5672() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getPort()).isEqualTo(5672);
        }

        @Test
        @DisplayName("username 默认为 guest")
        void usernameDefaultIsGuest() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getUsername()).isEqualTo("guest");
        }

        @Test
        @DisplayName("password 默认为 guest")
        void passwordDefaultIsGuest() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getPassword()).isEqualTo("guest");
        }

        @Test
        @DisplayName("exchange 默认为 afg.events")
        void exchangeDefaultIsAfgEvents() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getExchange()).isEqualTo("afg.events");
        }

        @Test
        @DisplayName("ackMode 默认为 AUTO")
        void ackModeDefaultIsAuto() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

            // then
            assertThat(config.getAckMode()).isEqualTo(EventProperties.AckMode.AUTO);
        }

        @Test
        @DisplayName("prefetchCount 默认为 10")
        void prefetchCountDefaultIs10() {
            // given
            EventProperties.RabbitMQConfig config = new EventProperties.RabbitMQConfig();

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
            EventProperties.RetryConfig config = new EventProperties.RetryConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("maxAttempts 默认为 3")
        void maxAttemptsDefaultIs3() {
            // given
            EventProperties.RetryConfig config = new EventProperties.RetryConfig();

            // then
            assertThat(config.getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("initialInterval 默认为 1000")
        void initialIntervalDefaultIs1000() {
            // given
            EventProperties.RetryConfig config = new EventProperties.RetryConfig();

            // then
            assertThat(config.getInitialInterval()).isEqualTo(1000);
        }

        @Test
        @DisplayName("multiplier 默认为 2.0")
        void multiplierDefaultIs2() {
            // given
            EventProperties.RetryConfig config = new EventProperties.RetryConfig();

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
            EventProperties.DeadLetterConfig config = new EventProperties.DeadLetterConfig();

            // then
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("topicPrefix 默认为 dlq.")
        void topicPrefixDefaultIsDlq() {
            // given
            EventProperties.DeadLetterConfig config = new EventProperties.DeadLetterConfig();

            // then
            assertThat(config.getTopicPrefix()).isEqualTo("dlq.");
        }

        @Test
        @DisplayName("retentionMs 默认为 0")
        void retentionMsDefaultIs0() {
            // given
            EventProperties.DeadLetterConfig config = new EventProperties.DeadLetterConfig();

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
            assertThat(EventProperties.AckMode.values())
                    .containsExactlyInAnyOrder(
                            EventProperties.AckMode.AUTO,
                            EventProperties.AckMode.MANUAL,
                            EventProperties.AckMode.BATCH);
        }
    }

    @Nested
    @DisplayName("EventType 枚举测试")
    class EventTypeEnumTests {

        @Test
        @DisplayName("应该包含 LOCAL、KAFKA、RABBITMQ")
        void shouldContainAllValues() {
            // then
            assertThat(EventProperties.EventType.values())
                    .containsExactlyInAnyOrder(
                            EventProperties.EventType.LOCAL,
                            EventProperties.EventType.KAFKA,
                            EventProperties.EventType.RABBITMQ);
        }
    }
}
