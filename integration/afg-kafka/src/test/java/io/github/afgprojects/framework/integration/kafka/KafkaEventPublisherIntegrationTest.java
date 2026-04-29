package io.github.afgprojects.framework.integration.kafka;

import io.github.afgprojects.framework.core.api.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaEventPublisherIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private KafkaTemplate<String, Object> kafkaTemplate;
    private KafkaEventProperties properties;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(factory);

        properties = new KafkaEventProperties();
        properties.setDefaultTopic("test-topic");
    }

    @Test
    void shouldCreatePublisher() {
        KafkaEventPublisher<String> publisher = new KafkaEventPublisher<>(kafkaTemplate, properties);

        assertThat(publisher).isNotNull();
    }

    @Test
    void shouldPublishEventToKafka() throws Exception {
        // Given
        String topic = "test-topic";
        String payload = "test-payload";
        DomainEvent<String> event = new DomainEvent<>(
                "event-123",
                topic,
                payload,
                Instant.now(),
                "test-source"
        );

        KafkaEventPublisher<String> publisher = new KafkaEventPublisher<>(kafkaTemplate, properties);

        // When
        publisher.publish(event);

        // Then - verify the message was published by consuming it
        ConsumerRecord<String, String> record = consumeRecord(topic, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("event-123");
        assertThat(record.value()).contains("test-payload");
    }

    @Test
    void shouldPublishEventAsyncToKafka() throws Exception {
        // Given
        String topic = "test-topic";
        String payload = "async-payload";
        DomainEvent<String> event = new DomainEvent<>(
                "event-async-456",
                topic,
                payload,
                Instant.now(),
                "test-source"
        );

        KafkaEventPublisher<String> publisher = new KafkaEventPublisher<>(kafkaTemplate, properties);

        // When
        var future = publisher.publishAsync(event);
        future.get(5, TimeUnit.SECONDS);

        // Then - verify the message was published by consuming it
        ConsumerRecord<String, String> record = consumeRecord(topic, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("event-async-456");
        assertThat(record.value()).contains("async-payload");
    }

    @Test
    void shouldUseDefaultTopicWhenEventTopicIsEmpty() throws Exception {
        // Given
        String defaultTopic = "default-events";
        properties.setDefaultTopic(defaultTopic);

        String payload = "default-topic-payload";
        // 创建空 topic 的事件 - 将使用默认 topic
        DomainEvent<String> event = new DomainEvent<>(
                "event-default-789",
                "", // empty topic
                payload,
                Instant.now(),
                "test-source"
        );

        KafkaEventPublisher<String> publisher = new KafkaEventPublisher<>(kafkaTemplate, properties);

        // When
        publisher.publish(event);

        // Then - verify the message was published to default topic
        ConsumerRecord<String, String> record = consumeRecord(defaultTopic, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("event-default-789");
    }

    @Test
    void shouldPublishComplexPayload() throws Exception {
        // Given
        String topic = "test-topic";
        TestPayload payload = new TestPayload("user-123", "test@example.com", 100);
        DomainEvent<TestPayload> event = new DomainEvent<>(
                "event-complex-001",
                topic,
                payload,
                Instant.now(),
                "test-source"
        );

        KafkaEventPublisher<TestPayload> publisher = new KafkaEventPublisher<>(kafkaTemplate, properties);

        // When
        publisher.publish(event);

        // Then
        ConsumerRecord<String, String> record = consumeRecord(topic, Duration.ofSeconds(10));

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("event-complex-001");
        assertThat(record.value()).contains("user-123", "test@example.com");
    }

    private ConsumerRecord<String, String> consumeRecord(String topic, Duration timeout) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(topic));

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }
        return null;
    }

    /**
     * 复杂对象序列化测试载荷
     */
    static class TestPayload {
        private String userId;
        private String email;
        private int score;

        // Jackson 反序列化需要的默认构造函数
        @SuppressWarnings("unused")
        TestPayload() {}

        TestPayload(String userId, String email, int score) {
            this.userId = userId;
            this.email = email;
            this.score = score;
        }

        @SuppressWarnings("unused")
        public String getUserId() { return userId; }
        @SuppressWarnings("unused")
        public String getEmail() { return email; }
        @SuppressWarnings("unused")
        public int getScore() { return score; }
    }
}
