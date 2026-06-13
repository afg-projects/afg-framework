package io.github.afgprojects.framework.core.api.webhook;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * Webhook 注册仓库 SPI 接口
 * <p>
 * 定义 Webhook 订阅者的注册、查询和管理接口。
 * Core 模块提供 {@link InMemoryWebhookRepository} 内存实现，
 * 持久化实现由集成模块（如 JDBC）提供。
 *
 * @since 1.0.0
 */
public interface WebhookRepository {

    /**
     * 注册 Webhook 订阅
     *
     * @param registration 注册信息
     */
    void register(@NonNull WebhookRegistration registration);

    /**
     * 取消注册 Webhook 订阅
     *
     * @param id 注册 ID
     */
    void unregister(@NonNull String id);

    /**
     * 按事件类型查找所有订阅者
     *
     * @param event 事件类型
     * @return 匹配的 Webhook 注册列表
     */
    List<WebhookRegistration> findByEvent(@NonNull String event);

    /**
     * 查找所有 Webhook 注册
     *
     * @return 所有 Webhook 注册列表
     */
    List<WebhookRegistration> findAll();
}
