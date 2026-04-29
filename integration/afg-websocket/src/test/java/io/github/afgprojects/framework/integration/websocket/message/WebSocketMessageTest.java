package io.github.afgprojects.framework.integration.websocket.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 消息测试
 */
@DisplayName("WebSocket 消息测试")
class WebSocketMessageTest {

    @Test
    @DisplayName("创建广播消息测试")
    void testBroadcastMessage() {
        String content = "Hello everyone!";
        String fromUserId = "user123";
        String fromUsername = "Alice";

        WebSocketMessage message = WebSocketMessage.broadcast(content, fromUserId, fromUsername);

        assertThat(message.getType()).isEqualTo(WebSocketMessage.MessageType.BROADCAST);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getFromUserId()).isEqualTo(fromUserId);
        assertThat(message.getFromUsername()).isEqualTo(fromUsername);
        assertThat(message.getToUserId()).isNull();
        assertThat(message.getToUsername()).isNull();
        assertThat(message.getTimestamp()).isNotNull();
        assertThat(message.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建点对点消息测试")
    void testDirectMessage() {
        String content = "Hello Bob!";
        String fromUserId = "user123";
        String fromUsername = "Alice";
        String toUserId = "user456";
        String toUsername = "Bob";

        WebSocketMessage message = WebSocketMessage.direct(
                content, fromUserId, fromUsername, toUserId, toUsername);

        assertThat(message.getType()).isEqualTo(WebSocketMessage.MessageType.DIRECT);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getFromUserId()).isEqualTo(fromUserId);
        assertThat(message.getFromUsername()).isEqualTo(fromUsername);
        assertThat(message.getToUserId()).isEqualTo(toUserId);
        assertThat(message.getToUsername()).isEqualTo(toUsername);
        assertThat(message.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("创建系统消息测试")
    void testSystemMessage() {
        String content = "System maintenance in 10 minutes";

        WebSocketMessage message = WebSocketMessage.system(content);

        assertThat(message.getType()).isEqualTo(WebSocketMessage.MessageType.SYSTEM);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getFromUserId()).isNull();
        assertThat(message.getFromUsername()).isNull();
        assertThat(message.getToUserId()).isNull();
        assertThat(message.getToUsername()).isNull();
        assertThat(message.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Builder 测试")
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.BROADCAST)
                .fromUserId("user1")
                .fromUsername("User One")
                .content("Test content")
                .traceId("trace-123")
                .tenantId("tenant-1")
                .timestamp(now)
                .build();

        assertThat(message.getType()).isEqualTo(WebSocketMessage.MessageType.BROADCAST);
        assertThat(message.getFromUserId()).isEqualTo("user1");
        assertThat(message.getFromUsername()).isEqualTo("User One");
        assertThat(message.getContent()).isEqualTo("Test content");
        assertThat(message.getTraceId()).isEqualTo("trace-123");
        assertThat(message.getTenantId()).isEqualTo("tenant-1");
        assertThat(message.getTimestamp()).isEqualTo(now);
    }
}
