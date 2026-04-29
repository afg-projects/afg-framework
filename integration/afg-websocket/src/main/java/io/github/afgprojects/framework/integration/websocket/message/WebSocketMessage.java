package io.github.afgprojects.framework.integration.websocket.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息基类
 * <p>
 * 提供消息的基本结构，包含：
 * </p>
 * <ul>
 *   <li>消息类型</li>
 *   <li>发送者信息</li>
 *   <li>时间戳</li>
 *   <li>消息内容</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 发送者ID
     */
    private String fromUserId;

    /**
     * 发送者名称
     */
    private String fromUsername;

    /**
     * 目标用户ID（点对点消息时使用）
     */
    private String toUserId;

    /**
     * 目标用户名称（点对点消息时使用）
     */
    private String toUsername;

    /**
     * 消息内容
     */
    private Object content;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 创建广播消息
     *
     * @param content    消息内容
     * @param fromUserId 发送者ID
     * @param fromUsername 发送者名称
     * @return WebSocket 消息
     */
    public static WebSocketMessage broadcast(Object content, String fromUserId, String fromUsername) {
        return WebSocketMessage.builder()
                .type(MessageType.BROADCAST)
                .fromUserId(fromUserId)
                .fromUsername(fromUsername)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建点对点消息
     *
     * @param content      消息内容
     * @param fromUserId   发送者ID
     * @param fromUsername 发送者名称
     * @param toUserId     目标用户ID
     * @param toUsername   目标用户名称
     * @return WebSocket 消息
     */
    public static WebSocketMessage direct(
            Object content,
            String fromUserId,
            String fromUsername,
            String toUserId,
            String toUsername) {
        return WebSocketMessage.builder()
                .type(MessageType.DIRECT)
                .fromUserId(fromUserId)
                .fromUsername(fromUsername)
                .toUserId(toUserId)
                .toUsername(toUsername)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建系统消息
     *
     * @param content 消息内容
     * @return WebSocket 消息
     */
    public static WebSocketMessage system(Object content) {
        return WebSocketMessage.builder()
                .type(MessageType.SYSTEM)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 广播消息
         */
        BROADCAST,

        /**
         * 点对点消息
         */
        DIRECT,

        /**
         * 系统消息
         */
        SYSTEM
    }
}
