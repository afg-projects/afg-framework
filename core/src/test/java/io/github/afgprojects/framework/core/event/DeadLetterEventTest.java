package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DeadLetterEvent 测试
 */
@DisplayName("DeadLetterEvent 测试")
class DeadLetterEventTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该创建死信事件")
        void shouldCreateDeadLetterEvent() {
            // given
            String eventId = "dlq-event-001";
            String originalEventType = "user.created";
            String originalEventJson = "{\"eventId\":\"event-001\"}";
            String failureReason = "Database connection failed";
            String stackTrace = "java.sql.SQLException...";
            int retryCount = 3;
            int maxRetryCount = 3;
            String originalTopic = "user.created";
            Instant failedAt = Instant.now();

            // when
            DeadLetterEvent<String> event = new DeadLetterEvent<>(
                    eventId,
                    originalEventType,
                    originalEventJson,
                    failureReason,
                    stackTrace,
                    retryCount,
                    maxRetryCount,
                    originalTopic,
                    failedAt,
                    null,
                    null);

            // then
            assertThat(event.eventId()).isEqualTo(eventId);
            assertThat(event.originalEventType()).isEqualTo(originalEventType);
            assertThat(event.originalEventJson()).isEqualTo(originalEventJson);
            assertThat(event.failureReason()).isEqualTo(failureReason);
            assertThat(event.stackTrace()).isEqualTo(stackTrace);
            assertThat(event.retryCount()).isEqualTo(retryCount);
            assertThat(event.maxRetryCount()).isEqualTo(maxRetryCount);
            assertThat(event.originalTopic()).isEqualTo(originalTopic);
            assertThat(event.failedAt()).isEqualTo(failedAt);
            assertThat(event.metadata()).isNotNull();
        }

        @Test
        @DisplayName("null metadata 应该转换为空 Map")
        void nullMetadataShouldConvertToEmptyMap() {
            // when
            DeadLetterEvent<String> event = new DeadLetterEvent<>(
                    "dlq-001", "user.created", "{}", "Error", null, 3, 3, "topic", Instant.now(), null, null);

            // then
            assertThat(event.metadata()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("canRetry 测试")
    class CanRetryTests {

        @Test
        @DisplayName("重试次数小于最大次数时应该返回 true")
        void shouldReturnTrueWhenRetryCountLessThanMax() {
            // given
            DeadLetterEvent<String> event = createEvent(2, 3);

            // then
            assertThat(event.canRetry()).isTrue();
        }

        @Test
        @DisplayName("重试次数等于最大次数时应该返回 false")
        void shouldReturnFalseWhenRetryCountEqualsMax() {
            // given
            DeadLetterEvent<String> event = createEvent(3, 3);

            // then
            assertThat(event.canRetry()).isFalse();
        }

        @Test
        @DisplayName("重试次数大于最大次数时应该返回 false")
        void shouldReturnFalseWhenRetryCountGreaterThanMax() {
            // given
            DeadLetterEvent<String> event = createEvent(4, 3);

            // then
            assertThat(event.canRetry()).isFalse();
        }
    }

    @Nested
    @DisplayName("remainingRetries 测试")
    class RemainingRetriesTests {

        @Test
        @DisplayName("应该计算剩余重试次数")
        void shouldCalculateRemainingRetries() {
            // given
            DeadLetterEvent<String> event = createEvent(1, 3);

            // then
            assertThat(event.remainingRetries()).isEqualTo(2);
        }

        @Test
        @DisplayName("重试次数耗尽时应该返回 0")
        void shouldReturnZeroWhenRetriesExhausted() {
            // given
            DeadLetterEvent<String> event = createEvent(3, 3);

            // then
            assertThat(event.remainingRetries()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建事件")
        void shouldBuildEventWithBuilder() {
            // given
            String eventId = "dlq-001";
            String originalEventType = "user.created";
            String originalEventJson = "{\"eventId\":\"001\"}";
            String failureReason = "Error";
            String originalTopic = "user.created";

            // when
            DeadLetterEvent<String> event = DeadLetterEvent.<String>builder()
                    .eventId(eventId)
                    .originalEventType(originalEventType)
                    .originalEventJson(originalEventJson)
                    .failureReason(failureReason)
                    .retryCount(2)
                    .maxRetryCount(3)
                    .originalTopic(originalTopic)
                    .metadata("key", "value")
                    .build();

            // then
            assertThat(event.eventId()).isEqualTo(eventId);
            assertThat(event.originalEventType()).isEqualTo(originalEventType);
            assertThat(event.originalEventJson()).isEqualTo(originalEventJson);
            assertThat(event.failureReason()).isEqualTo(failureReason);
            assertThat(event.retryCount()).isEqualTo(2);
            assertThat(event.maxRetryCount()).isEqualTo(3);
            assertThat(event.originalTopic()).isEqualTo(originalTopic);
            assertThat(event.metadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("failedAt 未设置时应该自动设置为当前时间")
        void shouldSetCurrentTimeWhenFailedAtNotSet() {
            // given
            Instant before = Instant.now();

            // when
            DeadLetterEvent<String> event = DeadLetterEvent.<String>builder()
                    .eventId("dlq-001")
                    .originalEventType("user.created")
                    .originalEventJson("{}")
                    .failureReason("Error")
                    .originalTopic("topic")
                    .build();
            Instant after = Instant.now();

            // then
            assertThat(event.failedAt()).isBetween(before, after);
        }
    }

    /**
     * 创建测试用的死信事件
     */
    private DeadLetterEvent<String> createEvent(int retryCount, int maxRetryCount) {
        return new DeadLetterEvent<>(
                "dlq-001",
                "user.created",
                "{}",
                "Error",
                null,
                retryCount,
                maxRetryCount,
                "user.created",
                Instant.now(),
                null,
                null);
    }
}