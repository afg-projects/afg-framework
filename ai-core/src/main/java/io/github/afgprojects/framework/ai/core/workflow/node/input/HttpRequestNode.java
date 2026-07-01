package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class HttpRequestNode extends AbstractWorkflowNode<HttpRequestNode.Params> {

    public static final String TYPE = "http-request";

    /** Strongly-typed parameters for {@link HttpRequestNode}. */
    public record Params(
            @Param(displayName = "URL", description = "The URL to request", required = true)
            String url,
            @Param(displayName = "HTTP method", description = "HTTP method",
                    type = io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType.ENUM,
                    enumValues = {"GET", "POST", "PUT", "DELETE", "PATCH"}, defaultValue = "GET")
            String method,
            @Param(displayName = "Headers", description = "Map of HTTP headers")
            Map<String, String> headers,
            @Param(displayName = "Body", description = "Request body for POST/PUT/PATCH")
            String body,
            @Param(displayName = "Timeout (ms)", description = "Request timeout in milliseconds", defaultValue = "30000")
            Long timeoutMs
    ) {
        /** Effective method, defaulting to GET. */
        public String effectiveMethod() {
            return method == null || method.isBlank() ? "GET" : method.toUpperCase();
        }

        /** Effective timeout, defaulting to 30000ms. */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 30000L : timeoutMs;
        }
    }

    /** Output descriptor for {@link HttpRequestNode}. */
    public record Output(
            @Out(description = "Status code") int statusCode,
            @Out(description = "Response body") String body,
            @Out(description = "Response headers") Map<String, String> headers
    ) {}

    private final HttpClient httpClient;

    public HttpRequestNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public HttpRequestNode(String nodeId, HttpClient httpClient) {
        super(nodeId, TYPE, Params.class);
        this.httpClient = httpClient;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String url = params.url();
        String method = params.effectiveMethod();
        long timeoutMs = params.effectiveTimeoutMs();

        log.debug("HttpRequestNode [{}] sending {} to {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs));

            // Add headers
            Map<String, String> headers = params.headers();
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.header(header.getKey(), header.getValue());
                }
            }

            // Set method and body
            String body = params.body();
            HttpRequest.BodyPublisher bodyPublisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();

            switch (method) {
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
}
