package io.github.afgprojects.framework.core.api.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 本地 Webhook 服务实现
 * <p>
 * 组合 {@link WebhookRepository} 查找订阅者，通过 HTTP POST 逐个投递事件。
 * 支持 HMAC-SHA256 签名验证，确保回调的安全性。
 * </p>
 * <p>
 * 使用 JDK 内置的 {@link java.net.http.HttpClient} 进行 HTTP 调用，无需额外依赖。
 * 适用于单机部署，多实例部署需使用消息队列 + 分布式 Webhook 服务。
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class LocalWebhookService implements WebhookService {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private final WebhookRepository repository;
    private final AfgCoreProperties.WebhookConfig config;
    private final HttpClient httpClient;

    /**
     * 构造函数
     *
     * @param repository Webhook 仓库
     * @param properties 核心配置属性（从中获取 Webhook 配置）
     */
    public LocalWebhookService(WebhookRepository repository, AfgCoreProperties properties) {
        this.repository = repository;
        this.config = properties.getWebhook();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeout()))
                .build();
    }

    @Override
    public List<WebhookDeliveryResult> dispatch(@NonNull String event, @NonNull Object payload) {
        List<WebhookRegistration> subscribers = repository.findByEvent(event);
        if (subscribers.isEmpty()) {
            log.debug("No webhook subscribers for event: {}", event);
            return List.of();
        }

        WebhookPayload webhookPayload = WebhookPayload.of(event, payload);
        String jsonBody = serializePayload(webhookPayload);

        List<WebhookDeliveryResult> results = new ArrayList<>();
        for (WebhookRegistration subscriber : subscribers) {
            WebhookDeliveryResult result = deliverToSubscriber(subscriber, jsonBody);
            results.add(result);
        }
        return results;
    }

    @Override
    public void register(@NonNull WebhookRegistration registration) {
        repository.register(registration);
    }

    @Override
    public void unregister(@NonNull String id) {
        repository.unregister(id);
    }

    /**
     * 向单个订阅者投递事件
     *
     * @param subscriber 订阅者
     * @param jsonBody   JSON 请求体
     * @return 投递结果
     */
    private WebhookDeliveryResult deliverToSubscriber(WebhookRegistration subscriber, String jsonBody) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(subscriber.getUrl()))
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .header("Content-Type", JSON_CONTENT_TYPE)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

            // 添加签名头
            if (subscriber.getSecret() != null && !subscriber.getSecret().isEmpty()) {
                String signature = computeHmac(jsonBody, subscriber.getSecret());
                requestBuilder.header(config.getSignatureHeader(), signature);
            }

            // 添加自定义请求头
            if (subscriber.getHeaders() != null) {
                subscriber.getHeaders().forEach(requestBuilder::header);
            }

            // 带重试的投递
            return deliverWithRetry(requestBuilder.build(), subscriber);
        } catch (Exception e) {
            log.warn("Failed to deliver webhook to url={}: {}", subscriber.getUrl(), e.getMessage());
            return WebhookDeliveryResult.failure(0, "Delivery failed: " + e.getMessage());
        }
    }

    /**
     * 带重试的投递
     *
     * @param request   HTTP 请求
     * @param subscriber 订阅者
     * @return 投递结果
     */
    private WebhookDeliveryResult deliverWithRetry(HttpRequest request, WebhookRegistration subscriber) {
        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.debug("Retrying webhook delivery to url={}, attempt {}/{}",
                            subscriber.getUrl(), attempt, maxRetries);
                    Thread.sleep(config.getRetryIntervalMs());
                }

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    log.debug("Webhook delivered successfully to url={}, status={}",
                            subscriber.getUrl(), statusCode);
                    return WebhookDeliveryResult.success(statusCode, response.body());
                }

                // 4xx 错误不重试（客户端错误）
                if (statusCode >= 400 && statusCode < 500) {
                    log.warn("Webhook delivery failed with client error, url={}, status={}, not retrying",
                            subscriber.getUrl(), statusCode);
                    return WebhookDeliveryResult.failure(statusCode, response.body());
                }

                // 5xx 错误继续重试
                lastException = new RuntimeException("HTTP " + statusCode + ": " + response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return WebhookDeliveryResult.failure(0, "Delivery interrupted");
            } catch (Exception e) {
                lastException = e;
            }
        }

        log.warn("Webhook delivery exhausted retries for url={}: {}",
                subscriber.getUrl(), lastException != null ? lastException.getMessage() : "unknown");
        return WebhookDeliveryResult.failure(0, "Exhausted retries: "
                + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    /**
     * 计算 HMAC-SHA256 签名
     *
     * @param payload 请求体
     * @param secret  签名密钥
     * @return 十六进制签名字符串
     */
    String computeHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(config.getSignatureAlgorithm());
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), config.getSignatureAlgorithm());
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                    new Object[]{"Webhook HMAC 签名失败"}, e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 序列化 Webhook 载荷为 JSON
     * <p>
     * 简单的 JSON 序列化，不依赖 Jackson。仅处理 WebhookPayload 的标准字段。
     *
     * @param payload Webhook 载荷
     * @return JSON 字符串
     */
    private String serializePayload(WebhookPayload payload) {
        // 使用简单的拼接方式，避免引入 Jackson 依赖到 API 包
        StringBuilder sb = new StringBuilder();
        sb.append("{\"event\":\"").append(escapeJson(payload.getEvent())).append("\"");
        sb.append(",\"timestamp\":\"").append(payload.getTimestamp()).append("\"");
        sb.append(",\"id\":\"").append(escapeJson(payload.getId())).append("\"");
        sb.append(",\"data\":").append(serializeData(payload.getData()));
        sb.append("}");
        return sb.toString();
    }

    /**
     * 序列化业务数据
     * <p>
     * 简单处理：字符串直接包装，其他类型调用 toString。
     * 生产环境建议使用 Jackson 序列化。
     */
    private String serializeData(Object data) {
        if (data == null) {
            return "null";
        }
        if (data instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        }
        if (data instanceof Number || data instanceof Boolean) {
            return data.toString();
        }
        // 复杂对象使用 toString，生产环境建议 Jackson
        return "\"" + escapeJson(data.toString()) + "\"";
    }

    /**
     * 转义 JSON 特殊字符
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
