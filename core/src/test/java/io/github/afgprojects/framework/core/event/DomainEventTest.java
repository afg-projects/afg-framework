package io.github.afgprojects.framework.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DomainEvent 测试
 */
@DisplayName("DomainEvent 测试")
class DomainEventTest {

    /**
     * 测试用领域事件实现
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
    @DisplayName("基本属性测试")
    class BasicPropertiesTests {

        @Test
        @DisplayName("应该正确获取事件 ID")
        void shouldGetEventId() {
            // given
            String eventId = "event-001";
            TestEvent event = new TestEvent(eventId, "user.created", Instant.now(), "user-123", "payload");

            // then
            assertThat(event.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("应该正确获取事件类型")
        void shouldGetEventType() {
            // given
            String eventType = "user.created";
            TestEvent event = new TestEvent("event-001", eventType, Instant.now(), "user-123", "payload");

            // then
            assertThat(event.getEventType()).isEqualTo(eventType);
        }

        @Test
        @DisplayName("应该正确获取时间戳")
        void shouldGetTimestamp() {
            // given
            Instant timestamp = Instant.now();
            TestEvent event = new TestEvent("event-001", "user.created", timestamp, "user-123", "payload");

            // then
            assertThat(event.getTimestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("应该正确获取聚合 ID")
        void shouldGetAggregateId() {
            // given
            String aggregateId = "user-123";
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), aggregateId, "payload");

            // then
            assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        }

        @Test
        @DisplayName("应该正确获取载荷")
        void shouldGetPayload() {
            // given
            String payload = "test-payload";
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", payload);

            // then
            assertThat(event.getPayload()).isEqualTo(payload);
        }
    }

    @Nested
    @DisplayName("默认方法测试")
    class DefaultMethodsTests {

        @Test
        @DisplayName("getVersion 应该返回默认值 1")
        void shouldReturnDefaultVersion() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // then
            assertThat(event.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("getSource 应该返回 null")
        void shouldReturnNullSource() {
            // given
            TestEvent event = new TestEvent("event-001", "user.created", Instant.now(), "user-123", "payload");

            // then
            assertThat(event.getSource()).isNull();
        }
    }

    @Nested
    @DisplayName("可空属性测试")
    class NullablePropertiesTests {

        @Test
        @DisplayName("聚合 ID 可以为 null")
        void aggregateIdCanBeNull() {
            // given
            TestEvent event = new TestEvent("event-001", "system.startup", Instant.now(), null, "payload");

            // then
            assertThat(event.getAggregateId()).isNull();
        }

        @Test
        @DisplayName("载荷可以为 null")
        void payloadCanBeNull() {
            // given
            TestEvent event = new TestEvent("event-001", "user.deleted", Instant.now(), "user-123", null);

            // then
            assertThat(event.getPayload()).isNull();
        }
    }

    @Nested
    @DisplayName("自定义事件实现测试")
    class CustomEventImplementationTests {

        /**
         * 带版本和来源的事件
         */
        record VersionedEvent(
                String eventId,
                String eventType,
                Instant timestamp,
                String aggregateId,
                String payload,
                int version,
                String source) implements DomainEvent<String> {

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

            @Override
            public int getVersion() {
                return version;
            }

            @Override
            public String getSource() {
                return source;
            }
        }

        @Test
        @DisplayName("可以自定义版本号")
        void canCustomizeVersion() {
            // given
            VersionedEvent event = new VersionedEvent(
                    "event-001", "order.created", Instant.now(), "order-123", "payload", 2, "order-service");

            // then
            assertThat(event.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("可以自定义来源")
        void canCustomizeSource() {
            // given
            VersionedEvent event = new VersionedEvent(
                    "event-001", "order.created", Instant.now(), "order-123", "payload", 1, "order-service");

            // then
            assertThat(event.getSource()).isEqualTo("order-service");
        }
    }
}
