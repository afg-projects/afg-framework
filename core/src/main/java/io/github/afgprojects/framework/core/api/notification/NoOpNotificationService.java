package io.github.afgprojects.framework.core.api.notification;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 通知服务实现
 * <p>
 * 本地降级实现，所有通知操作均为空操作。
 * {@code send} 返回成功结果（mock messageId），
 * {@code sendBatch} 逐条返回成功结果，
 * {@code supports} 总是返回 {@code true}。
 * <p>
 * 由 {@code NotificationAutoConfiguration} 在无其他 {@link NotificationService} 实现时自动注册。
 * 适用于不需要任何通知功能的场景。
 *
 * @since 1.0.0
 */
public class NoOpNotificationService implements NotificationService {

    @Override
    public NotificationResult send(@NonNull Notification notification) {
        return NotificationResult.success("noop-" + UUID.randomUUID(), notification.getChannel());
    }

    @Override
    public List<NotificationResult> sendBatch(@NonNull List<Notification> notifications) {
        return notifications.stream()
                .map(n -> NotificationResult.success("noop-" + UUID.randomUUID(), n.getChannel()))
                .toList();
    }

    @Override
    public boolean supports(@NonNull NotificationChannel channel) {
        return true;
    }
}
