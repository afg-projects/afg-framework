package io.github.afgprojects.framework.core.api.webhook;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * NoOp Webhook 仓库实现
 * <p>
 * 本地降级实现，所有注册和查询操作均为空操作。
 * {@code register}/{@code unregister} 不执行任何操作，
 * {@code findByEvent}/{@code findAll} 返回空列表。
 * <p>
 * 由 {@code WebhookAutoConfiguration} 在无其他 {@link WebhookRepository} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpWebhookRepository implements WebhookRepository {

    @Override
    public void register(@NonNull WebhookRegistration registration) {
        // no-op
    }

    @Override
    public void unregister(@NonNull String id) {
        // no-op
    }

    @Override
    public List<WebhookRegistration> findByEvent(@NonNull String event) {
        return Collections.emptyList();
    }

    @Override
    public List<WebhookRegistration> findAll() {
        return Collections.emptyList();
    }
}
