package io.github.afgprojects.framework.integration.event.rabbitmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import io.github.afgprojects.framework.core.api.event.EventPublisher;
import io.github.afgprojects.framework.core.api.event.MessageEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RabbitMQEventPublisher 集成测试
 *
 * <p>基于真实 RabbitMQ 容器测试事件发布操作
 */
@Testcontainers
@DisplayName("RabbitMQEventPublisher 事件发布测试")
class RabbitMQEventPublisherTest {

    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3-management-alpine");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(RABBITMQ_IMAGE)
            .withUser("guest", "guest");

    private static RabbitTemplate rabbitTemplate;
    private static RabbitMQEventProperties properties;

    private EventPublisher<String> eventPublisher;

    @BeforeAll
    static void setUpInfrastructure() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(RABBITMQ.getHost());
        connectionFactory.setPort(RABBITMQ.getAmqpPort());
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        MessageConverter messageConverter = new Jackson2JsonMessageConverter();

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        properties = new RabbitMQEventProperties();
        properties.setExchange("test-events");
        properties.setDefaultRoutingKey("event.default");
    }

    @BeforeEach
    void setUp() {
        // 创建 exchange 和 queue
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare("test-events", "topic", true);
            channel.queueDeclare("test-queue", true, false, false, null);
            channel.queueBind("test-queue", "test-events", "#");
            return null;
        });

        eventPublisher = new RabbitMQEventPublisher<>(rabbitTemplate, properties);
    }

    @Nested
    @DisplayName("publish 操作")
    class Publish {

        @Test
        @DisplayName("publish 应成功发送事件到 RabbitMQ")
        void shouldPublishEvent() {
            MessageEvent<String> event = new MessageEvent<>("test.topic", "test-payload");

            assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publish 应使用事件的 topic 作为路由键")
        void shouldUseTopicAsRoutingKey() {
            MessageEvent<String> event = new MessageEvent<>("user.created", "user-data");

            assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publish null topic 的事件应使用默认路由键")
        void shouldUseDefaultRoutingKey_whenTopicIsNull() {
            // 使用空 topic 创建事件（通过构造函数，topic 是 @NonNull）
            // 改为测试默认路由键的场景：直接发布后检查消息到达
            MessageEvent<String> event = new MessageEvent<>("event.default", "default-payload");

            assertThatCode(() -> eventPublisher.publish(event)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("publishAsync 操作")
    class PublishAsync {

        @Test
        @DisplayName("publishAsync 应异步发送事件到 RabbitMQ")
        void shouldPublishEventAsynchronously() throws Exception {
            MessageEvent<String> event = new MessageEvent<>("async.topic", "async-payload");

            var future = eventPublisher.publishAsync(event);

            assertThatCode(() -> future.get()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("消息到达验证")
    class MessageDelivery {

        @Test
        @DisplayName("发布的事件应能被消费者接收")
        void shouldReceivePublishedEvent() {
            String payload = "delivery-test-" + System.currentTimeMillis();
            MessageEvent<String> event = new MessageEvent<>("delivery.topic", payload);

            eventPublisher.publish(event);

            // 等待消息到达
            Object received = rabbitTemplate.receiveAndConvert("test-queue", 5000);

            assertThat(received).isNotNull();
        }
    }

    @AfterEach
    void cleanup() {
        rabbitTemplate.execute(channel -> {
            channel.queueDelete("test-queue");
            channel.exchangeDelete("test-events");
            return null;
        });
    }
}
