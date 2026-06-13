package io.github.afgprojects.framework.core.api.webhook;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Webhook 注册信息
 * <p>
 * 表示一个 Webhook 订阅者，包含回调 URL、签名密钥和自定义请求头。
 *
 * <pre>{@code
 * WebhookRegistration registration = WebhookRegistration.builder()
 *     .id("webhook-1")
 *     .event("order.created")
 *     .url("https://example.com/webhook")
 *     .secret("my-signing-secret")
 *     .build();
 * webhookService.register(registration);
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRegistration {

    /**
     * 注册 ID（全局唯一）
     */
    private String id;

    /**
     * 订阅的事件类型
     */
    private String event;

    /**
     * 回调 URL
     */
    private String url;

    /**
     * 签名密钥（用于 HMAC-SHA256 签名，可为空表示不签名）
     */
    private String secret;

    /**
     * 自定义请求头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean active = true;
}
