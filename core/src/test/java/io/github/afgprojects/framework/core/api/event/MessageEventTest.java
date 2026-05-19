package io.github.afgprojects.framework.core.api.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MessageEvent} 消息事件测试
 *
 * <p>测试消息事件的创建和属性：
 * <ul>
 *   <li>简化构造函数（自动生成 eventId 和 occurredAt）</li>
 *   <li>完整构造函数（指定所有属性）</li>
 *   <li>自定义 source</li>
 *   <li>eventId 唯一性</li>
 * </ul>
 *
 * @see MessageEvent
 */
@DisplayName("MessageEvent 测试")
class MessageEventTest {

    /**
     * 测试使用简化构造函数创建事件
     */
    @Test
    @DisplayName("应该使用简化构造函数创建 MessageEvent")
    void shouldCreateWithSimpleConstructor() {
        TestPayload payload = new TestPayload("test", 123);
        MessageEvent<TestPayload> event = new MessageEvent<>("user.created", payload);

        assertNotNull(event.eventId());
        assertEquals("user.created", event.topic());
        assertEquals(payload, event.payload());
        assertNotNull(event.occurredAt());
        assertEquals("afg-core", event.source());
    }

    /**
     * 测试使用完整构造函数创建事件
     */
    @Test
    @DisplayName("应该使用完整构造函数创建 MessageEvent")
    void shouldCreateWithFullConstructor() {
        TestPayload payload = new TestPayload("test", 456);
        Instant now = Instant.now();
        MessageEvent<TestPayload> event = new MessageEvent<>(
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

    /**
     * 测试使用自定义 source 创建事件
     */
    @Test
    @DisplayName("应该使用自定义 source 创建 MessageEvent")
    void shouldCreateWithCustomSource() {
        TestPayload payload = new TestPayload("data", 789);
        MessageEvent<TestPayload> event = new MessageEvent<>("payment.processed", payload, "payment-service");

        assertNotNull(event.eventId());
        assertEquals("payment.processed", event.topic());
        assertEquals(payload, event.payload());
        assertNotNull(event.occurredAt());
        assertEquals("payment-service", event.source());
    }

    /**
     * 测试 eventId 的唯一性
     */
    @Test
    @DisplayName("eventId 应该是唯一的")
    void eventIdShouldBeUnique() {
        TestPayload payload = new TestPayload("test", 1);
        MessageEvent<TestPayload> event1 = new MessageEvent<>("topic", payload);
        MessageEvent<TestPayload> event2 = new MessageEvent<>("topic", payload);

        assertNotEquals(event1.eventId(), event2.eventId());
    }

    /**
     * 测试 occurredAt 时间接近当前时间
     */
    @Test
    @DisplayName("occurredAt 应该接近当前时间")
    void occurredAtShouldBeCloseToNow() {
        Instant before = Instant.now().minusSeconds(1);
        TestPayload payload = new TestPayload("test", 1);
        MessageEvent<TestPayload> event = new MessageEvent<>("topic", payload);
        Instant after = Instant.now().plusSeconds(1);

        assertTrue(event.occurredAt().isAfter(before) || event.occurredAt().equals(before));
        assertTrue(event.occurredAt().isBefore(after) || event.occurredAt().equals(after));
    }

    /**
     * 测试用 payload
     */
    record TestPayload(String name, int value) {}
}
