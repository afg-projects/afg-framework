package io.github.afgprojects.framework.ai.core.workflow.node.output;

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
 * Webhook node - sends data to a webhook URL.
 *
 * <p>Posts data to a configured webhook URL, typically for integration
 * with external systems like CI/CD pipelines, monitoring, or messaging platforms.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * An {@link HttpClient} is a construction-time dependency; the no-arg
 * constructor creates one internally.</p>
 */
@Slf4j
public class WebhookNode extends AbstractWorkflowNode<WebhookNode.Params> {

    public static final String TYPE = "webhook";

    /** Strongly-typed parameters for {@link WebhookNode}. */
    public record Params(
            @Param(displayName = "URL", description = "Webhook URL", required = true)
            String url,
            @Param(displayName = "Payload", description = "JSON payload to send", defaultValue = "{}")
            String payload,
            @Param(displayName = "Method", description = "HTTP method", defaultValue = "POST")
            String method,
            @Param(displayName = "Headers", description = "Additional HTTP headers")
            Map<String, String> headers,
            @Param(displayName = "Timeout (ms)", description = "Request timeout in milliseconds", defaultValue = "10000")
            Long timeoutMs
    ) {
        /** Effective payload, defaulting to "{}". */
        public String effectivePayload() {
            return payload == null ? "{}" : payload;
        }

        /** Effective method, defaulting to POST. */
        public String effectiveMethod() {
            return method == null || method.isBlank() ? "POST" : method.toUpperCase();
        }

        /** Effective timeout, defaulting to 10000ms. */
        public long effectiveTimeoutMs() {
            return timeoutMs == null ? 10000L : timeoutMs;
        }
    }

    /** Output descriptor for {@link WebhookNode}. */
    public record Output(
            @Out(description = "Status code") int statusCode,
            @Out(description = "URL") String url,
            @Out(description = "Whether successful") boolean success
    ) {}

    private final HttpClient httpClient;

    public WebhookNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public WebhookNode(String nodeId, HttpClient httpClient) {
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
        String payload = params.effectivePayload();
        String method = params.effectiveMethod();
        long timeoutMs = params.effectiveTimeoutMs();

        log.debug("WebhookNode [{}] sending {} to {}", getNodeId(), method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json");

            Map<String, String> headers = params.headers();
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
}
