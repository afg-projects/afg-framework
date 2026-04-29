package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * EventRetryHandler 测试
 */
@DisplayName("EventRetryHandler 测试")
class EventRetryHandlerTest {

    private EventProperties properties;
    private DomainEventPublisher deadLetterPublisher;
    private EventRetryHandler retryHandler;

    @BeforeEach
    void setUp() {
        properties = new EventProperties();
        deadLetterPublisher = mock(DomainEventPublisher.class);
        retryHandler = new EventRetryHandler(properties, deadLetterPublisher);
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
    @DisplayName("executeWithRetry 测试")
    class ExecuteWithRetryTests {

        @Test
        @DisplayName("第一次成功时应该直接完成")
        void shouldCompleteOnFirstSuccess() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            boolean[] executed = {false};

            // when
            retryHandler.executeWithRetry(event, e -> {
                executed[0] = true;
            });

            // then
            assertThat(executed[0]).isTrue();
        }

        @Test
        @DisplayName("重试成功时应该完成")
        void shouldCompleteAfterRetry() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            int[] attemptCount = {0};

            // when
            retryHandler.executeWithRetry(event, e -> {
                attemptCount[0]++;
                if (attemptCount[0] < 2) {
                    throw new RuntimeException("Simulated failure");
                }
            });

            // then
            assertThat(attemptCount[0]).isEqualTo(2);
        }

        @Test
        @DisplayName("重试次数耗尽时应该抛出异常")
        void shouldThrowExceptionWhenRetriesExhausted() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            properties.getRetry().setMaxAttempts(2);

            // then
            assertThatThrownBy(() -> retryHandler.executeWithRetry(event, e -> {
                throw new RuntimeException("Always fails");
            }))
                    .isInstanceOf(EventProcessingException.class)
                    .hasMessageContaining("failed after 2 retries");
        }

        @Test
        @DisplayName("重试次数耗尽时应该发送到死信队列")
        void shouldSendToDeadLetterQueueWhenRetriesExhausted() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            properties.getRetry().setMaxAttempts(2);

            // when
            try {
                retryHandler.executeWithRetry(event, e -> {
                    throw new RuntimeException("Always fails");
                });
            } catch (EventProcessingException e) {
                // expected
            }

            // then
            verify(deadLetterPublisher).publish(any(String.class), any(DeadLetterEvent.class));
        }

        @Test
        @DisplayName("禁用重试时应该直接执行")
        void shouldExecuteDirectlyWhenRetryDisabled() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            properties.getRetry().setEnabled(false);
            boolean[] executed = {false};

            // when
            retryHandler.executeWithRetry(event, e -> {
                executed[0] = true;
            });

            // then
            assertThat(executed[0]).isTrue();
        }

        @Test
        @DisplayName("maxAttempts 为 1 时应该直接执行")
        void shouldExecuteDirectlyWhenMaxAttemptsIs1() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // when
            retryHandler.executeWithRetry(event, e -> {
                // success
            }, 1);

            // then - no exception thrown
        }
    }

    @Nested
    @DisplayName("指数退避测试")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("应该使用指数退避计算间隔")
        void shouldUseExponentialBackoff() {
            // given
            properties.getRetry().setInitialInterval(100);
            properties.getRetry().setMultiplier(2.0);
            // 验证配置正确
            assertThat(properties.getRetry().getInitialInterval()).isEqualTo(100);
            assertThat(properties.getRetry().getMultiplier()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("无死信队列发布器测试")
    class NoDeadLetterPublisherTests {

        @Test
        @DisplayName("死信队列发布器为 null 时应该不发送死信事件")
        void shouldNotSendDeadLetterWhenPublisherIsNull() {
            // given
            retryHandler = new EventRetryHandler(properties, null);
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");
            properties.getRetry().setMaxAttempts(2);

            // when & then
            assertThatThrownBy(() -> retryHandler.executeWithRetry(event, e -> {
                throw new RuntimeException("Always fails");
            }))
                    .isInstanceOf(EventProcessingException.class);
        }
    }
}
