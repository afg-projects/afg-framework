package io.github.afgprojects.framework.integration.event.rabbitmq;

import io.github.afgprojects.framework.core.api.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RabbitMQ 事件发布器集成测试
 * <p>
 * 使用 Testcontainers 启动 RabbitMQ 容器进行真实集成测试。
 * </p>
 */
@Testcontainers
@DisplayName("RabbitMQ EventPublisher 集成测试")
class RabbitMQEventPublisherIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.12-management-alpine"));

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;
    private AmqpAdmin amqpAdmin;
    private RabbitMQEventProperties properties;

    @BeforeEach
    void setUp() {
        connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHost());
        connectionFactory.setPort(rabbitMQ.getAmqpPort());
        connectionFactory.setUsername(rabbitMQ.getAdminUsername());
        connectionFactory.setPassword(rabbitMQ.getAdminPassword());

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        amqpAdmin = new RabbitAdmin(connectionFactory);

        properties = new RabbitMQEventProperties();
        properties.setExchange("test-exchange");
        properties.setDefaultRoutingKey("event.default");
    }

    @Nested
    @DisplayName("同步发布测试")
    class SyncPublishTests {

        @Test
        @DisplayName("应该成功创建发布器")
        void shouldCreatePublisher() {
            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            assertThat(publisher).isNotNull();
        }

        @Test
        @DisplayName("应该成功发送消息到指定交换机")
        void shouldPublishMessageToExchange() {
            // Given
            String exchangeName = "test-sync-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            String routingKey = "user.created";
            String payload = "test-payload";
            DomainEvent<String> event = new DomainEvent<>(
                    "event-001",
                    routingKey,
                    payload,
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then - should not throw exception
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该使用默认路由键当事件 topic 为空时")
        void shouldUseDefaultRoutingKeyWhenTopicIsEmpty() {
            // Given
            String exchangeName = "default-routing-exchange";
            properties.setExchange(exchangeName);
            properties.setDefaultRoutingKey("default.key");
            createExchange(exchangeName);

            String payload = "default-routing-payload";
            DomainEvent<String> event = new DomainEvent<>(
                    "event-002",
                    "", // empty topic
                    payload,
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then - should not throw exception
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该成功序列化复杂对象")
        void shouldSerializeComplexPayload() {
            // Given - create RabbitTemplate with JSON converter for complex objects
            RabbitTemplate jsonRabbitTemplate = new RabbitTemplate(connectionFactory);
            MessageConverter jsonConverter = new Jackson2JsonMessageConverter();
            jsonRabbitTemplate.setMessageConverter(jsonConverter);

            String exchangeName = "complex-payload-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            TestPayload payload = new TestPayload("user-123", "test@example.com", 100);
            DomainEvent<TestPayload> event = new DomainEvent<>(
                    "event-003",
                    "user.complex",
                    payload,
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<TestPayload> publisher = new RabbitMQEventPublisher<>(jsonRabbitTemplate, properties);

            // When & Then - should not throw exception
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该成功发送多条消息")
        void shouldPublishMultipleMessages() {
            // Given
            String exchangeName = "multi-message-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then
            for (int i = 0; i < 10; i++) {
                DomainEvent<String> event = new DomainEvent<>(
                        "event-" + i,
                        "message." + i,
                        "payload-" + i,
                        Instant.now(),
                        "test-source"
                );
                assertThatCode(() -> publisher.publish(event))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("异步发布测试")
    class AsyncPublishTests {

        @Test
        @DisplayName("应该成功异步发送消息")
        void shouldPublishAsyncSuccessfully() throws Exception {
            // Given
            String exchangeName = "async-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            String payload = "async-payload";
            DomainEvent<String> event = new DomainEvent<>(
                    "event-async-001",
                    "async.topic",
                    payload,
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When
            CompletableFuture<Void> future = publisher.publishAsync(event);

            // Then
            assertThatCode(() -> future.get(5, TimeUnit.SECONDS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该返回完成的 Future 当异步发送成功")
        void shouldReturnCompletedFuture() throws Exception {
            // Given
            String exchangeName = "async-future-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            DomainEvent<String> event = new DomainEvent<>(
                    "event-async-002",
                    "async.future",
                    "payload",
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When
            CompletableFuture<Void> future = publisher.publishAsync(event);

            // Then
            assertThat(future.get(5, TimeUnit.SECONDS)).isNull();
            assertThat(future.isDone()).isTrue();
        }

        @Test
        @DisplayName("应该支持并发异步发送")
        void shouldSupportConcurrentAsyncPublish() throws Exception {
            // Given
            String exchangeName = "concurrent-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When
            CompletableFuture<?>[] futures = new CompletableFuture[5];
            for (int i = 0; i < 5; i++) {
                DomainEvent<String> event = new DomainEvent<>(
                        "event-concurrent-" + i,
                        "concurrent." + i,
                        "payload-" + i,
                        Instant.now(),
                        "test-source"
                );
                futures[i] = publisher.publishAsync(event);
            }

            // Then
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            for (CompletableFuture<?> future : futures) {
                assertThat(future.isDone()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("配置属性测试")
    class PropertiesTests {

        @Test
        @DisplayName("应该使用配置的交换机名称")
        void shouldUseConfiguredExchange() {
            // Given
            String customExchange = "custom-event-exchange";
            properties.setExchange(customExchange);
            createExchange(customExchange);

            DomainEvent<String> event = new DomainEvent<>(
                    "event-custom-exchange",
                    "routing.key",
                    "payload",
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该支持动态修改配置")
        void shouldSupportDynamicConfigModification() {
            // Given
            String exchange1 = "exchange-1";
            String exchange2 = "exchange-2";
            createExchange(exchange1);
            createExchange(exchange2);

            DomainEvent<String> event = new DomainEvent<>(
                    "event-dynamic",
                    "routing.key",
                    "payload",
                    Instant.now(),
                    "test-source"
            );

            // When - use first exchange
            properties.setExchange(exchange1);
            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);
            assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();

            // Then - switch to second exchange
            properties.setExchange(exchange2);
            RabbitMQEventPublisher<String> publisher2 = new RabbitMQEventPublisher<>(rabbitTemplate, properties);
            assertThatCode(() -> publisher2.publish(event)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("路由键解析测试")
    class RoutingKeyTests {

        @Test
        @DisplayName("应该使用事件 topic 作为路由键")
        void shouldUseEventTopicAsRoutingKey() {
            // Given
            String exchangeName = "routing-key-exchange";
            properties.setExchange(exchangeName);
            createExchange(exchangeName);

            String customRoutingKey = "order.created.premium";
            DomainEvent<String> event = new DomainEvent<>(
                    "event-routing-001",
                    customRoutingKey,
                    "payload",
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应该使用默认路由键当 topic 为 null 时")
        void shouldUseDefaultRoutingKeyWhenTopicIsNull() {
            // Given
            String exchangeName = "null-topic-exchange";
            properties.setExchange(exchangeName);
            properties.setDefaultRoutingKey("default.routing");
            createExchange(exchangeName);

            DomainEvent<String> event = new DomainEvent<>(
                    "event-null-topic",
                    null,
                    "payload",
                    Instant.now(),
                    "test-source"
            );

            RabbitMQEventPublisher<String> publisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);

            // When & Then
            assertThatCode(() -> publisher.publish(event))
                    .doesNotThrowAnyException();
        }
    }

    // 辅助方法

    private void createExchange(String exchangeName) {
        Exchange exchange = new TopicExchange(exchangeName);
        amqpAdmin.declareExchange(exchange);
    }

    /**
     * Test payload for complex object serialization
     */
    static class TestPayload {
        private String userId;
        private String email;
        private int score;

        // Default constructor for Jackson deserialization
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
        public void setUserId(String userId) { this.userId = userId; }
        @SuppressWarnings("unused")
        public String getEmail() { return email; }
        @SuppressWarnings("unused")
        public void setEmail(String email) { this.email = email; }
        @SuppressWarnings("unused")
        public int getScore() { return score; }
        @SuppressWarnings("unused")
        public void setScore(int score) { this.score = score; }
    }
}
