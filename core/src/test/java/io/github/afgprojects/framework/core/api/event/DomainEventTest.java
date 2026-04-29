package io.github.afgprojects.framework.core.api.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DomainEvent 测试
 */
@DisplayName("DomainEvent 测试")
class DomainEventTest {

    @Test
    @DisplayName("应该使用简化构造函数创建 DomainEvent")
    void shouldCreateWithSimpleConstructor() {
        TestPayload payload = new TestPayload("test", 123);
        DomainEvent<TestPayload> event = new DomainEvent<>("user.created", payload);

        assertNotNull(event.eventId());
        assertEquals("user.created", event.topic());
        assertEquals(payload, event.payload());
        assertNotNull(event.occurredAt());
        assertEquals("afg-core", event.source());
    }

    @Test
    @DisplayName("应该使用完整构造函数创建 DomainEvent")
    void shouldCreateWithFullConstructor() {
        TestPayload payload = new TestPayload("test", 456);
        Instant now = Instant.now();
        DomainEvent<TestPayload> event = new DomainEvent<>(
                "evt-001",
                "order.completed",
                payload,
                now,
                "order-service"
        );

        assertEquals("evt-001", event.eventId());
        assertEquals("order.completed", event.topic());
        assertEquals(payload, event.payload());
        assertEquals(now, event.occurredAt());
        assertEquals("order-service", event.source());
    }

    @Test
    @DisplayName("应该使用自定义 source 创建 DomainEvent")
    void shouldCreateWithCustomSource() {
        TestPayload payload = new TestPayload("data", 789);
        DomainEvent<TestPayload> event = new DomainEvent<>("payment.processed", payload, "payment-service");

        assertNotNull(event.eventId());
        assertEquals("payment.processed", event.topic());
        assertEquals(payload, event.payload());
        assertNotNull(event.occurredAt());
        assertEquals("payment-service", event.source());
    }

    @Test
    @DisplayName("eventId 应该是唯一的")
    void eventIdShouldBeUnique() {
        TestPayload payload = new TestPayload("test", 1);
        DomainEvent<TestPayload> event1 = new DomainEvent<>("topic", payload);
        DomainEvent<TestPayload> event2 = new DomainEvent<>("topic", payload);

        assertNotEquals(event1.eventId(), event2.eventId());
    }

    @Test
    @DisplayName("occurredAt 应该接近当前时间")
    void occurredAtShouldBeCloseToNow() {
        Instant before = Instant.now().minusSeconds(1);
        TestPayload payload = new TestPayload("test", 1);
        DomainEvent<TestPayload> event = new DomainEvent<>("topic", payload);
        Instant after = Instant.now().plusSeconds(1);

        assertTrue(event.occurredAt().isAfter(before) || event.occurredAt().equals(before));
        assertTrue(event.occurredAt().isBefore(after) || event.occurredAt().equals(after));
    }

    /**
     * 测试用 payload
     */
    record TestPayload(String name, int value) {}
}
