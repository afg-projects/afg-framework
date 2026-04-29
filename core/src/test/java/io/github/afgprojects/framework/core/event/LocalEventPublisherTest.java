package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * LocalEventPublisher 测试
 */
@DisplayName("LocalEventPublisher 测试")
class LocalEventPublisherTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private Executor asyncExecutor;
    private LocalEventPublisher publisher;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        asyncExecutor = Executors.newSingleThreadExecutor();
        publisher = new LocalEventPublisher(applicationEventPublisher, asyncExecutor);
    }

    /**
     * 测试用领域事件
     */
    record TestEvent(
            String eventId,
            String eventType,
            Instant timestamp,
            String aggregateId,
            String payload) implements DomainEvent<String> {

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }

        @Override
        public String getPayload() {
            return payload;
        }
    }

    @Nested
    @DisplayName("publish 测试")
    class PublishTests {

        @Test
        @DisplayName("应该成功发布事件")
        void shouldPublishEvent() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // when
            publisher.publish(event);

            // then
            verify(applicationEventPublisher).publishEvent(any(LocalEventPublisher.DomainEventWrapper.class));
        }

        @Test
        @DisplayName("发布失败时应该抛出异常")
        void shouldThrowExceptionWhenPublishFails() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            doThrow(new RuntimeException("Publish failed")).when(applicationEventPublisher).publishEvent(any());

            // then
            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(EventPublishException.class)
                    .hasMessageContaining("Failed to publish local event");
        }
    }

    @Nested
    @DisplayName("publishAsync 测试")
    class PublishAsyncTests {

        @Test
        @DisplayName("应该异步发布事件")
        void shouldPublishEventAsync() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // when
            CompletableFuture<Void> future = publisher.publishAsync(event);

            // then
            assertThat(future).isCompleted();
            verify(applicationEventPublisher).publishEvent(any(LocalEventPublisher.DomainEventWrapper.class));
        }
    }

    @Nested
    @DisplayName("DomainEventWrapper 测试")
    class DomainEventWrapperTests {

        @Test
        @DisplayName("应该正确包装领域事件")
        void shouldWrapDomainEvent() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // when
            LocalEventPublisher.DomainEventWrapper<String> wrapper =
                    new LocalEventPublisher.DomainEventWrapper<>(event);

            // then
            assertThat(wrapper.getDomainEvent()).isEqualTo(event);
            assertThat(wrapper.getSource()).isEqualTo(event);
        }

        @Test
        @DisplayName("toString 应该包含事件信息")
        void toStringShouldContainEventInfo() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            LocalEventPublisher.DomainEventWrapper<String> wrapper =
                    new LocalEventPublisher.DomainEventWrapper<>(event);

            // when
            String result = wrapper.toString();

            // then
            assertThat(result).contains("DomainEventWrapper");
        }
    }
}
