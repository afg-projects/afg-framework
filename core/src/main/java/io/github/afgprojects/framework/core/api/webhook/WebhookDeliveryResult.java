package io.github.afgprojects.framework.core.api.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Webhook 投递结果
 * <p>
 * 表示单次 Webhook 回调的投递结果，包含 HTTP 响应状态和错误信息。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryResult {

    /**
     * 是否投递成功（HTTP 2xx 响应）
     */
    private boolean success;

    /**
     * HTTP 响应状态码
     */
    private int httpStatus;

    /**
     * HTTP 响应体
     */
    private String response;

    /**
     * 错误消息（投递失败时填充）
     */
    private String errorMessage;

    /**
     * 创建成功结果
     *
     * @param httpStatus HTTP 状态码
     * @param response   HTTP 响应体
     * @return 成功的 WebhookDeliveryResult
     */
    public static WebhookDeliveryResult success(int httpStatus, String response) {
        return WebhookDeliveryResult.builder()
                .success(true)
                .httpStatus(httpStatus)
                .response(response)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param httpStatus   HTTP 状态码（0 表示连接失败）
     * @param errorMessage 错误消息
     * @return 失败的 WebhookDeliveryResult
     */
    public static WebhookDeliveryResult failure(int httpStatus, String errorMessage) {
        return WebhookDeliveryResult.builder()
                .success(false)
                .httpStatus(httpStatus)
                .errorMessage(errorMessage)
                .build();
    }
}
