package io.github.afgprojects.framework.core.api.notification;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志通知服务实现
 * <p>
 * 将通知内容以 INFO 级别记录到日志，不实际发送。
 * 适用于开发和测试环境，生产环境应替换为真实渠道实现。
 * <p>
 * 由 {@code NotificationAutoConfiguration} 在无其他 {@link NotificationService} 实现且
 * {@code afg.core.notification.log-notifications=true} 时自动注册。
 *
 * @since 1.0.0
 */
@Slf4j
public class LogNotificationService implements NotificationService {

    @Override
    public NotificationResult send(@NonNull Notification notification) {
        log.info("[Notification] channel={}, to={}, subject={}, template={}, content={}",
                notification.getChannel(),
                notification.getTo(),
                notification.getSubject(),
                notification.getTemplate(),
                notification.getContent());
        return NotificationResult.success("log-" + UUID.randomUUID(), notification.getChannel());
    }

    @Override
    public List<NotificationResult> sendBatch(@NonNull List<Notification> notifications) {
        return notifications.stream()
                .map(this::send)
                .toList();
    }

    @Override
    public boolean supports(@NonNull NotificationChannel channel) {
        return true;
    }
}
