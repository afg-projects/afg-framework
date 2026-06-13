package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP request node - sends HTTP requests and returns the response data.
 *
 * <p>Sends an HTTP request to the configured URL and provides the response
 * body, status code, and headers as workflow output data.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code url} (required) - the URL to request</li>
 *   <li>{@code method} (optional) - HTTP method, defaults to GET</li>
 *   <li>{@code headers} (optional) - Map of HTTP headers</li>
 *   <li>{@code body} (optional) - Request body for POST/PUT/PATCH</li>
 *   <li>{@code timeoutMs} (optional) - Request timeout in milliseconds, defaults to 30000</li>
 * </ul>
 */
@Slf4j
public class HttpRequestNode extends AbstractWorkflowNode {

    public static final String TYPE = "http-request";

    private final HttpClient httpClient;

    public HttpRequestNode(String nodeId) {
        super(nodeId, TYPE);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public HttpRequestNode(String nodeId, HttpClient httpClient) {
        super(nodeId, TYPE);
        this.httpClient = httpClient;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String url = getRequiredParam(params, "url");
        String method = getParam(params, "method", "GET");
        long timeoutMs = getLongParam(params, "timeoutMs", 30000L);

        log.debug("HttpRequestNode [{}] sending {} to {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs));

            // Add headers
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) params.get("headers");
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.header(header.getKey(), header.getValue());
                }
            }

            // Set method and body
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

            Map<String, String> responseHeaders = new LinkedHashMap<>();
            response.headers().map().forEach((key, values) ->
                    responseHeaders.put(key, String.join(", ", values)));
            result.put("headers", responseHeaders);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP request interrupted: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: " + url, e);
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
