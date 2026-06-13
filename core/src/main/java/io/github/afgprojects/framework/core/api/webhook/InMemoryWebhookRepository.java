package io.github.afgprojects.framework.core.api.webhook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 内存 Webhook 仓库实现
 * <p>
 * 使用 {@link ConcurrentHashMap} 管理 Webhook 注册信息，适用于单机部署或降级场景。
 * 支持按事件类型查找订阅者。
 * </p>
 * <p>
 * 注意：此实现仅对单实例有效，应用重启后注册信息丢失。
 * 生产环境应使用持久化实现（如 JDBC）。
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class InMemoryWebhookRepository implements WebhookRepository {

    private final ConcurrentHashMap<String, WebhookRegistration> registrations = new ConcurrentHashMap<>();

    @Override
    public void register(@NonNull WebhookRegistration registration) {
        registrations.put(registration.getId(), registration);
        log.debug("Registered webhook: id={}, event={}, url={}",
                registration.getId(), registration.getEvent(), registration.getUrl());
    }

    @Override
    public void unregister(@NonNull String id) {
        WebhookRegistration removed = registrations.remove(id);
        if (removed != null) {
            log.debug("Unregistered webhook: id={}, event={}", id, removed.getEvent());
        }
    }

    @Override
    public List<WebhookRegistration> findByEvent(@NonNull String event) {
        return registrations.values().stream()
                .filter(r -> r.isActive() && event.equals(r.getEvent()))
                .toList();
    }

    @Override
    public List<WebhookRegistration> findAll() {
        return new ArrayList<>(registrations.values());
    }
}
