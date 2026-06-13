package io.github.afgprojects.framework.core.api.webhook;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * NoOp Webhook 服务实现
 * <p>
 * 本地降级实现，所有 Webhook 操作均为空操作。
 * {@code dispatch} 返回空列表，
 * {@code register}/{@code unregister} 不执行任何操作。
 * <p>
 * 由 {@code WebhookAutoConfiguration} 在无其他 {@link WebhookService} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpWebhookService implements WebhookService {

    @Override
    public List<WebhookDeliveryResult> dispatch(@NonNull String event, @NonNull Object payload) {
        return Collections.emptyList();
    }

    @Override
    public void register(@NonNull WebhookRegistration registration) {
        // no-op
    }

    @Override
    public void unregister(@NonNull String id) {
        // no-op
    }
}
