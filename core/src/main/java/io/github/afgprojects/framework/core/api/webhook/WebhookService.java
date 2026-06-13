package io.github.afgprojects.framework.core.api.webhook;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * Webhook 服务 SPI 接口
 * <p>
 * 定义统一的 Webhook 分发和管理接口。
 * Core 模块提供 {@link LocalWebhookService} 本地实现（HTTP 分发），
 * 分布式实现由集成模块（如 Redis + 消息队列）提供。
 *
 * <pre>{@code
 * @Autowired
 * private WebhookService webhookService;
 *
 * // 注册 Webhook
 * webhookService.register(WebhookRegistration.builder()
 *     .id("webhook-1")
 *     .event("order.created")
 *     .url("https://example.com/webhook")
 *     .secret("my-signing-secret")
 *     .build());
 *
 * // 分发事件
 * List<WebhookDeliveryResult> results = webhookService.dispatch("order.created", orderData);
 *
 * // 取消注册
 * webhookService.unregister("webhook-1");
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WebhookService {

    /**
     * 分发事件到所有订阅者
     * <p>
     * 查找订阅了指定事件的所有 Webhook，逐个进行 HTTP POST 调用，
     * 返回每个订阅者的投递结果。
     *
     * @param event   事件类型
     * @param payload 业务数据
     * @return 投递结果列表
     */
    List<WebhookDeliveryResult> dispatch(@NonNull String event, @NonNull Object payload);

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
}
