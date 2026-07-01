package io.github.afgprojects.framework.ai.core.workflow.node.tool;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * An {@link HttpClient} is a construction-time dependency; the no-arg
 * constructor creates one internally.</p>
 */
@Slf4j
public class HttpCallNode extends AbstractWorkflowNode<HttpCallNode.Params> {

    public static final String TYPE = "http-call";

    /** Strongly-typed parameters for {@link HttpCallNode}. */
    public record Params(
            @Param(displayName = "URL", description = "The URL to call", required = true)
            String url,
            @Param(displayName = "HTTP method", description = "HTTP method",
                    type = ParamType.ENUM,
                    enumValues = {"GET", "POST", "PUT", "DELETE", "PATCH"},
                    defaultValue = "POST")
            String method,
            @Param(displayName = "Headers", description = "Map of HTTP headers")
            Map<String, String> headers,
            @Param(displayName = "Body", description = "Request body")
            String body,
            @Param(displayName = "Content-Type", description = "Content-Type header", defaultValue = "application/json")
            String contentType,
            @Param(displayName = "Timeout (ms)", description = "Request timeout in milliseconds", defaultValue = "30000")
            Long timeoutMs
    ) {
        /** Effective method, defaulting to POST. */
        public String effectiveMethod() {
            return method == null || method.isBlank() ? "POST" : method.toUpperCase();
        }

        /** Effective Content-Type, defaulting to application/json. */
        public String effectiveContentType() {
            return contentType == null || contentType.isBlank() ? "application/json" : contentType;
        }

        /** Effective timeout, defaulting to 30000ms. */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 30000L : timeoutMs;
        }
    }

    /** Output descriptor for {@link HttpCallNode}. */
    public record Output(
            @Out(description = "Status code") int statusCode,
            @Out(description = "Response body") String body,
            @Out(description = "URL") String url,
            @Out(description = "HTTP method") String method
    ) {}

    private final HttpClient httpClient;

    public HttpCallNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public HttpCallNode(String nodeId, HttpClient httpClient) {
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
        String contentType = params.effectiveContentType();
        long timeoutMs = params.effectiveTimeoutMs();

        log.debug("HttpCallNode [{}] calling {} {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", contentType);

            Map<String, String> headers = params.headers();
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

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
}
