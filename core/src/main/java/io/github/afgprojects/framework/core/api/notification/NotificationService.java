package io.github.afgprojects.framework.core.api.notification;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * 通知服务 SPI 接口
 * <p>
 * 定义统一的通知发送接口，支持多种渠道和批量发送。
 * 真实渠道实现（邮件、短信、钉钉等）由集成模块提供，
 * Core 模块提供 {@link LogNotificationService} 和 {@link NoOpNotificationService} 降级实现。
 *
 * <pre>{@code
 * @Autowired
 * private NotificationService notificationService;
 *
 * // 发送单条通知
 * NotificationResult result = notificationService.send(Notification.builder()
 *     .to("user-123")
 *     .channel(NotificationChannel.EMAIL)
 *     .template("welcome")
 *     .variable("username", "张三")
 *     .build());
 *
 * // 批量发送
 * List<NotificationResult> results = notificationService.sendBatch(notifications);
 *
 * // 检查渠道支持
 * if (notificationService.supports(NotificationChannel.SMS)) {
 *     notificationService.send(smsNotification);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface NotificationService {

    /**
     * 发送单条通知
     *
     * @param notification 通知内容
     * @return 发送结果
     */
    NotificationResult send(@NonNull Notification notification);

    /**
     * 批量发送通知
     * <p>
     * 逐条发送，返回每条通知的发送结果。某条失败不影响其他通知的发送。
     *
     * @param notifications 通知列表
     * @return 发送结果列表，与输入列表一一对应
     */
    List<NotificationResult> sendBatch(@NonNull List<Notification> notifications);

    /**
     * 检查是否支持指定的通知渠道
     *
     * @param channel 通知渠道
     * @return 是否支持该渠道
     */
    boolean supports(@NonNull NotificationChannel channel);
}
