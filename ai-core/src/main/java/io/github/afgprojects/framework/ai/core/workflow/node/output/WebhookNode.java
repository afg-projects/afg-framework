package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook node - sends data to a webhook URL.
 *
 * <p>Posts data to a configured webhook URL, typically for integration
 * with external systems like CI/CD pipelines, monitoring, or messaging platforms.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code url} (required) - webhook URL</li>
 *   <li>{@code payload} (optional) - JSON payload to send</li>
 *   <li>{@code method} (optional) - HTTP method, defaults to POST</li>
 *   <li>{@code headers} (optional) - additional HTTP headers</li>
 *   <li>{@code timeoutMs} (optional) - request timeout, defaults to 10000</li>
 * </ul>
 */
@Slf4j
public class WebhookNode extends AbstractWorkflowNode {

    public static final String TYPE = "webhook";

    private final HttpClient httpClient;

    public WebhookNode(String nodeId) {
        super(nodeId, TYPE);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public WebhookNode(String nodeId, HttpClient httpClient) {
        super(nodeId, TYPE);
        this.httpClient = httpClient;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String url = getRequiredParam(params, "url");
        String payload = getParam(params, "payload", "{}");
        String method = getParam(params, "method", "POST");
        long timeoutMs = getLongParam(params, "timeoutMs", 10000L);

        log.debug("WebhookNode [{}] sending {} to {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json");

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) params.get("headers");
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload));
            } else if ("PUT".equalsIgnoreCase(method)) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(payload));
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", response.statusCode());
            result.put("url", url);
            result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Webhook interrupted: " + url, e);
        } catch (Exception e) {
            throw new RuntimeException("Webhook failed: " + url, e);
        }
    }

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.longValue();
        return Long.parseLong(value.toString());
    }
}
