package io.github.afgprojects.framework.integration.websocket.example;

import java.security.Principal;

import io.github.afgprojects.framework.integration.websocket.handler.WebSocketMessageHandler;
import io.github.afgprojects.framework.integration.websocket.message.WebSocketMessage;

import org.jspecify.annotations.NonNull;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 示例消息处理器
 * <p>
 * 演示如何使用 WebSocket 进行广播和点对点消息发送。
 * </p>
 *
 * <h3>使用示例</h3>
 * <h4>客户端连接</h4>
 * <pre>
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 *
 * stompClient.connect({}, function(frame) {
 *     // 订阅广播消息
 *     stompClient.subscribe('/topic/chat', function(message) {
 *         console.log('Broadcast:', JSON.parse(message.body));
 *     });
 *
 *     // 订阅点对点消息
 *     stompClient.subscribe('/user/queue/chat', function(message) {
 *         console.log('Private:', JSON.parse(message.body));
 *     });
 *
 *     // 订阅系统通知
 *     stompClient.subscribe('/user/queue/notifications', function(message) {
 *         console.log('Notification:', JSON.parse(message.body));
 *     });
 * });
 *
 * // 发送广播消息
 * stompClient.send('/app/chat.broadcast', {}, JSON.stringify({
 *     content: 'Hello everyone!'
 * }));
 *
 * // 发送点对点消息
 * stompClient.send('/app/chat.private', {}, JSON.stringify({
 *     toUser: 'bob',
 *     content: 'Hi Bob!'
 * }));
 * </pre>
 */
@Controller
public class ExampleChatController extends WebSocketMessageHandler {

    public ExampleChatController(@NonNull SimpMessagingTemplate messagingTemplate) {
        super(messagingTemplate);
    }

    /**
     * 广播消息到所有订阅者
     * <p>
     * 客户端发送到 /app/chat.broadcast，消息会广播到 /topic/chat。
     * </p>
     *
     * @param principal 发送者
     * @param message   聊天消息
     */
    @MessageMapping("/chat.broadcast")
    public void broadcast(Principal principal, @Payload @NonNull ChatMessage message) {
        sendToTopic("/topic/chat", message, principal);
    }

    /**
     * 发送点对点消息
     * <p>
     * 客户端发送到 /app/chat.private，消息会发送到目标用户的 /user/queue/chat。
     * </p>
     *
     * @param principal 发送者
     * @param message   聊天消息（包含目标用户）
     */
    @MessageMapping("/chat.private")
    public void privateMessage(Principal principal, @Payload @NonNull ChatMessage message) {
        if (message.getToUser() != null) {
            sendDirectMessage(message.getToUser(), "/queue/chat", message, principal);
        }
    }

    /**
     * 发送系统通知
     * <p>
     * 演示如何发送系统通知给指定用户。
     * </p>
     *
     * @param principal 发送者
     * @param message   通知消息
     */
    @MessageMapping("/chat.notify")
    public void sendNotification(Principal principal, @Payload @NonNull NotificationMessage message) {
        if (message.getToUser() != null) {
            sendSystemNotification(message.getToUser(), message.getContent());
        }
    }

    /**
     * 聊天消息
     */
    public static class ChatMessage {
        private String content;
        private String toUser;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getToUser() {
            return toUser;
        }

        public void setToUser(String toUser) {
            this.toUser = toUser;
        }
    }

    /**
     * 通知消息
     */
    public static class NotificationMessage {
        private String content;
        private String toUser;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getToUser() {
            return toUser;
        }

        public void setToUser(String toUser) {
            this.toUser = toUser;
        }
    }
}