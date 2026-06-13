package io.github.afgprojects.framework.ai.core.workflow.node.tool;

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
 * HTTP call node - makes an HTTP API call (tool variant).
 *
 * <p>Similar to HttpRequestNode but positioned in the TOOL category.
 * Sends an HTTP request to an external API and returns the response.
 * Intended for calling external tool/service APIs.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code url} (required) - the URL to call</li>
 *   <li>{@code method} (optional) - HTTP method, defaults to POST</li>
 *   <li>{@code headers} (optional) - Map of HTTP headers</li>
 *   <li>{@code body} (optional) - request body</li>
 *   <li>{@code contentType} (optional) - Content-Type header, defaults to "application/json"</li>
 *   <li>{@code timeoutMs} (optional) - request timeout in milliseconds, defaults to 30000</li>
 * </ul>
 */
@Slf4j
public class HttpCallNode extends AbstractWorkflowNode {

    public static final String TYPE = "http-call";

    private final HttpClient httpClient;

    public HttpCallNode(String nodeId) {
        super(nodeId, TYPE);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public HttpCallNode(String nodeId, HttpClient httpClient) {
        super(nodeId, TYPE);
        this.httpClient = httpClient;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String url = getRequiredParam(params, "url");
        String method = getParam(params, "method", "POST");
        String contentType = getParam(params, "contentType", "application/json");
        long timeoutMs = getLongParam(params, "timeoutMs", 30000L);

        log.debug("HttpCallNode [{}] calling {} {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", contentType);

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) params.get("headers");
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            String body = getParam(params, "body", null);
            HttpRequest.BodyPublisher bodyPublisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();

            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(bodyPublisher);
                case "PUT" -> requestBuilder.PUT(bodyPublisher);
                case "DELETE" -> requestBuilder.DELETE();
                case "PATCH" -> requestBuilder.method("PATCH", bodyPublisher);
                default -> requestBuilder.method(method, bodyPublisher);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("statusCode", response.statusCode());
            result.put("body", response.body());
            result.put("url", url);
            result.put("method", method);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP call interrupted: " + url, e);
        } catch (Exception e) {
            throw new RuntimeException("HTTP call failed: " + url, e);
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
