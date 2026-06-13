package io.github.afgprojects.framework.core.api.webhook;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Webhook 载荷
 * <p>
 * 发送到 Webhook 回调 URL 的数据结构，包含事件类型、业务数据和元信息。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    /**
     * 事件类型
     */
    private String event;

    /**
     * 业务数据
     */
    private Object data;

    /**
     * 事件时间戳
     */
    private Instant timestamp;

    /**
     * 唯一标识，用于幂等处理
     */
    private String id;

    /**
     * 创建 Webhook 载荷
     *
     * @param event 事件类型
     * @param data  业务数据
     * @return WebhookPayload 实例
     */
    public static WebhookPayload of(String event, Object data) {
        return WebhookPayload.builder()
                .event(event)
                .data(data)
                .timestamp(Instant.now())
                .id(UUID.randomUUID().toString())
                .build();
    }
}
