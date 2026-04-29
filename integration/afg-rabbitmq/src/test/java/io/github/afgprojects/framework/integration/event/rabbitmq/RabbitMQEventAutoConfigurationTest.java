package io.github.afgprojects.framework.integration.event.rabbitmq;

import io.github.afgprojects.framework.core.api.event.DomainEvent;
import io.github.afgprojects.framework.core.api.event.EventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RabbitMQEventAutoConfiguration 自动配置测试
 * <p>
 * 验证条件装配逻辑。
 * </p>
 */
@Testcontainers
@DisplayName("RabbitMQEventAutoConfiguration 测试")
class RabbitMQEventAutoConfigurationTest {

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.12-management-alpine"));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitMQEventAutoConfiguration.class));

    @Nested
    @DisplayName("条件装配测试")
    class ConditionalConfigurationTests {

        @Test
        @DisplayName("应该在启用时自动配置 EventPublisher")
        void shouldAutoConfigureWhenEnabled() {
            contextRunner
                    .withPropertyValues(
                            "afg.rabbitmq.event.enabled=true",
                            "afg.rabbitmq.event.exchange=test-exchange"
                    )
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .run(context -> {
                        assertThat(context).hasSingleBean(EventPublisher.class);
                        assertThat(context).hasSingleBean(RabbitMQEventPublisher.class);
                        assertThat(context).hasSingleBean(RabbitMQEventProperties.class);
                    });
        }

        @Test
        @DisplayName("应该在禁用时不配置 EventPublisher")
        void shouldNotAutoConfigureWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.rabbitmq.event.enabled=false")
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EventPublisher.class);
                        assertThat(context).doesNotHaveBean(RabbitMQEventPublisher.class);
                    });
        }

        @Test
        @DisplayName("应该在缺少 RabbitTemplate 时不配置 EventPublisher")
        void shouldNotAutoConfigureWithoutRabbitTemplate() {
            contextRunner
                    .withPropertyValues("afg.rabbitmq.event.enabled=true")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(EventPublisher.class);
                        assertThat(context).doesNotHaveBean(RabbitMQEventPublisher.class);
                    });
        }

        @Test
        @DisplayName("应该在默认配置下自动装配（matchIfMissing=true）")
        void shouldAutoConfigureByDefault() {
            contextRunner
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .run(context -> {
                        assertThat(context).hasSingleBean(EventPublisher.class);
                        assertThat(context).hasSingleBean(RabbitMQEventPublisher.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定测试")
    class PropertiesBindingTests {

        @Test
        @DisplayName("应该正确绑定配置属性")
        void shouldBindConfigurationProperties() {
            contextRunner
                    .withPropertyValues(
                            "afg.rabbitmq.event.enabled=true",
                            "afg.rabbitmq.event.exchange=custom-exchange",
                            "afg.rabbitmq.event.default-routing-key=custom.routing",
                            "afg.rabbitmq.event.include-metadata=false",
                            "afg.rabbitmq.event.message-ttl=60000"
                    )
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .run(context -> {
                        RabbitMQEventProperties properties = context.getBean(RabbitMQEventProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                        assertThat(properties.getExchange()).isEqualTo("custom-exchange");
                        assertThat(properties.getDefaultRoutingKey()).isEqualTo("custom.routing");
                        assertThat(properties.isIncludeMetadata()).isFalse();
                        assertThat(properties.getMessageTtl()).isEqualTo(60000L);
                    });
        }

        @Test
        @DisplayName("应该使用默认属性值")
        void shouldUseDefaultPropertyValues() {
            contextRunner
                    .withPropertyValues("afg.rabbitmq.event.enabled=true")
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .run(context -> {
                        RabbitMQEventProperties properties = context.getBean(RabbitMQEventProperties.class);
                        assertThat(properties.getExchange()).isEqualTo("afg-events");
                        assertThat(properties.getDefaultRoutingKey()).isEqualTo("event.default");
                        assertThat(properties.isIncludeMetadata()).isTrue();
                        assertThat(properties.getMessageTtl()).isEqualTo(0L);
                    });
        }
    }

    @Nested
    @DisplayName("Bean 条件测试")
    class BeanConditionTests {

        @Test
        @DisplayName("应该尊重 ConditionalOnMissingBean 条件")
        void shouldRespectConditionalOnMissingBean() {
            // Given - a custom EventPublisher already exists
            @SuppressWarnings("unchecked")
            EventPublisher<String> customPublisher = new EventPublisher<String>() {
                @Override
                public void publish(DomainEvent<String> event) {
                    // custom implementation
                }

                @Override
                public java.util.concurrent.CompletableFuture<Void> publishAsync(
                        DomainEvent<String> event) {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
            };

            contextRunner
                    .withPropertyValues("afg.rabbitmq.event.enabled=true")
                    .withBean(RabbitTemplate.class, () -> createRabbitTemplate())
                    .withBean(EventPublisher.class, () -> customPublisher)
                    .run(context -> {
                        // 由于 EventPublisher 已存在，不应创建 RabbitMQEventPublisher
                        assertThat(context).hasSingleBean(EventPublisher.class);
                        assertThat(context.getBean(EventPublisher.class))
                                .isNotInstanceOf(RabbitMQEventPublisher.class);
                    });
        }
    }

    // 辅助方法

    private RabbitTemplate createRabbitTemplate() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHost());
        connectionFactory.setPort(rabbitMQ.getAmqpPort());
        connectionFactory.setUsername(rabbitMQ.getAdminUsername());
        connectionFactory.setPassword(rabbitMQ.getAdminPassword());
        return new RabbitTemplate(connectionFactory);
    }
}
