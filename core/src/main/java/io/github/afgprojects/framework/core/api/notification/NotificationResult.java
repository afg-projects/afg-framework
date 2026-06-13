package io.github.afgprojects.framework.core.api.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知发送结果
 * <p>
 * 表示单条通知的发送结果，包含成功/失败状态、消息 ID 和错误信息。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    /**
     * 是否发送成功
     */
    private boolean success;

    /**
     * 消息 ID（发送成功时由实现方生成）
     */
    private String messageId;

    /**
     * 错误消息（发送失败时填充）
     */
    private String errorMessage;

    /**
     * 通知渠道
     */
    private NotificationChannel channel;

    /**
     * 创建成功结果
     *
     * @param messageId 消息 ID
     * @param channel   通知渠道
     * @return 成功的 NotificationResult
     */
    public static NotificationResult success(String messageId, NotificationChannel channel) {
        return NotificationResult.builder()
                .success(true)
                .messageId(messageId)
                .channel(channel)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMessage 错误消息
     * @param channel      通知渠道
     * @return 失败的 NotificationResult
     */
    public static NotificationResult failure(String errorMessage, NotificationChannel channel) {
        return NotificationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .channel(channel)
                .build();
    }
}
