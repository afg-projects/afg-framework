package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * EventAutoConfiguration 集成测试
 */
@DisplayName("EventAutoConfiguration 集成测试")
class EventAutoConfigurationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventAutoConfiguration.class));

    @Nested
    @DisplayName("LOCAL 类型配置测试")
    class LocalEventConfigurationTests {

        @Test
        @DisplayName("应该配置 LocalEventPublisher")
        void shouldConfigureLocalEventPublisher() {
            contextRunner
                    .withPropertyValues("afg.event.type=LOCAL")
                    .run(context -> {
                        assertThat(context).hasSingleBean(DomainEventPublisher.class);
                        assertThat(context).hasSingleBean(EventRetryHandler.class);
                        assertThat(context).hasSingleBean(Executor.class);

                        DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
                        assertThat(publisher).isInstanceOf(LocalEventPublisher.class);
                    });
        }

        @Test
        @DisplayName("默认类型应该是 LOCAL")
        void defaultTypeShouldBeLocal() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(DomainEventPublisher.class);

                DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
                assertThat(publisher).isInstanceOf(LocalEventPublisher.class);
            });
        }
    }

    @Nested
    @DisplayName("EventRetryHandler 配置测试")
    class EventRetryHandlerTests {

        @Test
        @DisplayName("应该配置 EventRetryHandler")
        void shouldConfigureEventRetryHandler() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(EventRetryHandler.class);
            });
        }

        @Test
        @DisplayName("应该使用自定义配置")
        void shouldUseCustomConfiguration() {
            contextRunner
                    .withPropertyValues(
                            "afg.event.retry.max-attempts=5",
                            "afg.event.retry.initial-interval=2000")
                    .run(context -> {
                        EventProperties properties = context.getBean(EventProperties.class);
                        assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);
                        assertThat(properties.getRetry().getInitialInterval()).isEqualTo(2000);
                    });
        }
    }

    @Nested
    @DisplayName("禁用事件测试")
    class DisabledEventTests {

        @Test
        @DisplayName("禁用事件时不应该配置任何 Bean")
        void shouldNotConfigureWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.event.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(DomainEventPublisher.class);
                        assertThat(context).doesNotHaveBean(EventRetryHandler.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性绑定测试")
    class PropertiesBindingTests {

        @Test
        @DisplayName("应该绑定事件属性")
        void shouldBindEventProperties() {
            contextRunner
                    .withPropertyValues(
                            "afg.event.enabled=true",
                            "afg.event.type=LOCAL",
                            "afg.event.default-topic=test.events")
                    .run(context -> {
                        EventProperties properties = context.getBean(EventProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                        assertThat(properties.getType()).isEqualTo(EventProperties.EventType.LOCAL);
                        assertThat(properties.getDefaultTopic()).isEqualTo("test.events");
                    });
        }

        @Test
        @DisplayName("应该绑定本地事件配置")
        void shouldBindLocalConfig() {
            contextRunner
                    .withPropertyValues(
                            "afg.event.local.async=true",
                            "afg.event.local.thread-pool-size=8")
                    .run(context -> {
                        EventProperties properties = context.getBean(EventProperties.class);
                        assertThat(properties.getLocal().isAsync()).isTrue();
                        assertThat(properties.getLocal().getThreadPoolSize()).isEqualTo(8);
                    });
        }

        @Test
        @DisplayName("应该绑定死信队列配置")
        void shouldBindDeadLetterConfig() {
            contextRunner
                    .withPropertyValues(
                            "afg.event.dead-letter.enabled=false",
                            "afg.event.dead-letter.topic-prefix=dead.")
                    .run(context -> {
                        EventProperties properties = context.getBean(EventProperties.class);
                        assertThat(properties.getDeadLetter().isEnabled()).isFalse();
                        assertThat(properties.getDeadLetter().getTopicPrefix()).isEqualTo("dead.");
                    });
        }
    }
}
