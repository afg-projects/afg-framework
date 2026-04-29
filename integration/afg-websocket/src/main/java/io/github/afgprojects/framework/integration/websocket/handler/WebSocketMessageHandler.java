package io.github.afgprojects.framework.integration.websocket.handler;

import java.security.Principal;

import io.github.afgprojects.framework.integration.websocket.message.WebSocketMessage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息处理器基类
 * <p>
 * 提供通用的消息处理方法：
 * </p>
 * <ul>
 *   <li>广播消息到所有订阅者</li>
 *   <li>发送点对点消息</li>
 *   <li>发送到特定目的地</li>
 *   <li>异常处理</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;Controller
 * public class ChatController extends WebSocketMessageHandler {
 *
 *     public ChatController(SimpMessagingTemplate messagingTemplate) {
 *         super(messagingTemplate);
 *     }
 *
 *     // 广播消息
 *     &#064;MessageMapping("/chat.broadcast")
 *     public void broadcast(Principal principal, ChatMessage message) {
 *         sendToTopic("/topic/chat", message, principal);
 *     }
 *
 *     // 点对点消息
 *     &#064;MessageMapping("/chat.private")
 *     public void privateMessage(Principal principal, ChatMessage message) {
 *         sendToUser(message.getToUser(), "/queue/chat", message, principal);
 *     }
 * }
 * </pre>
 */
@Controller
public abstract class WebSocketMessageHandler {

    protected final SimpMessagingTemplate messagingTemplate;

    protected WebSocketMessageHandler(@NonNull SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ==================== Broadcast Methods ====================

    /**
     * 发送广播消息
     *
     * @param destination 目的地（如 /topic/chat）
     * @param message     消息内容
     */
    protected void broadcast(@NonNull String destination, @NonNull Object message) {
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * 发送广播消息（带发送者信息）
     *
     * @param destination 目的地
     * @param content     消息内容
     * @param principal   发送者
     */
    protected void sendToTopic(
            @NonNull String destination,
            @NonNull Object content,
            @Nullable Principal principal) {

        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.BROADCAST)
                .content(content)
                .fromUserId(principal != null ? principal.getName() : null)
                .fromUsername(principal != null ? principal.getName() : null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(destination, message);
    }

    // ==================== Direct Message Methods ====================

    /**
     * 发送点对点消息
     *
     * @param username    目标用户名
     * @param destination 目的地（如 /queue/chat）
     * @param message     消息内容
     */
    protected void sendToUser(
            @NonNull String username,
            @NonNull String destination,
            @NonNull Object message) {

        messagingTemplate.convertAndSendToUser(username, destination, message);
    }

    /**
     * 发送点对点消息（带发送者信息）
     *
     * @param toUsername  目标用户名
     * @param destination 目的地
     * @param content     消息内容
     * @param principal   发送者
     */
    protected void sendDirectMessage(
            @NonNull String toUsername,
            @NonNull String destination,
            @NonNull Object content,
            @Nullable Principal principal) {

        WebSocketMessage message = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.DIRECT)
                .content(content)
                .fromUserId(principal != null ? principal.getName() : null)
                .fromUsername(principal != null ? principal.getName() : null)
                .toUserId(toUsername)
                .toUsername(toUsername)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(toUsername, destination, message);
    }

    /**
     * 发送系统通知给指定用户
     *
     * @param username 目标用户名
     * @param content  通知内容
     */
    protected void sendSystemNotification(@NonNull String username, @NonNull Object content) {
        WebSocketMessage message = WebSocketMessage.system(content);
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

    // ==================== Exception Handling ====================

    /**
     * 处理消息处理异常
     * <p>
     * 将异常信息发送给发送者。
     * </p>
     *
     * @param exception 异常
     * @param accessor  STOMP 头访问器
     * @return 错误消息
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    protected String handleException(Throwable exception, StompHeaderAccessor accessor) {
        return "Error: " + exception.getMessage();
    }
}
