package io.github.afgprojects.framework.core.api.webhook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sun.net.httpserver.HttpServer;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

@DisplayName("LocalWebhookService")
class LocalWebhookServiceTest {

    private InMemoryWebhookRepository repository;
    private LocalWebhookService service;
    private AfgCoreProperties properties;
    private HttpServer httpServer;
    private int port;
    private String lastRequestBody;
    private String lastSignatureHeader;
    private int responseCode;

    @BeforeEach
    void setUp() throws IOException {
        repository = new InMemoryWebhookRepository();
        properties = new AfgCoreProperties();
        properties.getWebhook().setConnectTimeout(3000);
        properties.getWebhook().setReadTimeout(3000);
        properties.getWebhook().setMaxRetries(1);
        properties.getWebhook().setRetryIntervalMs(100);
        service = new LocalWebhookService(repository, properties);
        responseCode = 200;

        // Start a simple HTTP server for testing
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = httpServer.getAddress().getPort();
        httpServer.createContext("/webhook", exchange -> {
            // Read request body
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            lastRequestBody = new String(bodyBytes, StandardCharsets.UTF_8);

            // Capture signature header
            lastSignatureHeader = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");

            // Send response
            String response = "OK";
            exchange.sendResponseHeaders(responseCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("should deliver event to registered subscriber")
        void shouldDeliverEventToRegisteredSubscriber() {
            String url = "http://127.0.0.1:" + port + "/webhook";
            repository.register(createRegistration("wh-1", "order.created", url));

            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isTrue();
            assertThat(results.get(0).getHttpStatus()).isEqualTo(200);
            assertThat(lastRequestBody).contains("order.created");
        }

        @Test
        @DisplayName("should return empty list when no subscribers")
        void shouldReturnEmptyListWhenNoSubscribers() {
            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should deliver to multiple subscribers")
        void shouldDeliverToMultipleSubscribers() {
            String url1 = "http://127.0.0.1:" + port + "/webhook";
            String url2 = "http://127.0.0.1:" + port + "/webhook";
            repository.register(createRegistration("wh-1", "order.created", url1));
            repository.register(createRegistration("wh-2", "order.created", url2));

            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(WebhookDeliveryResult::isSuccess);
        }

        @Test
        @DisplayName("should only deliver to subscribers matching the event")
        void shouldOnlyDeliverToSubscribersMatchingTheEvent() {
            String url = "http://127.0.0.1:" + port + "/webhook";
            repository.register(createRegistration("wh-1", "order.created", url));
            repository.register(createRegistration("wh-2", "order.updated", url));

            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("should return failure for 5xx response")
        void shouldReturnFailureFor5xxResponse() {
            responseCode = 500;
            String url = "http://127.0.0.1:" + port + "/webhook";
            repository.register(createRegistration("wh-1", "order.created", url));

            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isFalse();
        }

        @Test
        @DisplayName("should return failure for unreachable URL")
        void shouldReturnFailureForUnreachableUrl() {
            String url = "http://127.0.0.1:1/unreachable";
            repository.register(createRegistration("wh-1", "order.created", url));

            List<WebhookDeliveryResult> results = service.dispatch("order.created", "test data");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isSuccess()).isFalse();
            assertThat(results.get(0).getHttpStatus()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("HMAC signature")
    class HmacSignature {

        @Test
        @DisplayName("should include signature header when secret is configured")
        void shouldIncludeSignatureHeaderWhenSecretIsConfigured() {
            String url = "http://127.0.0.1:" + port + "/webhook";
            WebhookRegistration registration = WebhookRegistration.builder()
                    .id("wh-1")
                    .event("order.created")
                    .url(url)
                    .secret("my-signing-secret")
                    .active(true)
                    .build();
            repository.register(registration);

            service.dispatch("order.created", "test data");

            assertThat(lastSignatureHeader).isNotNull();
            assertThat(lastSignatureHeader).isNotEmpty();
        }

        @Test
        @DisplayName("should not include signature header when secret is not configured")
        void shouldNotIncludeSignatureHeaderWhenSecretIsNotConfigured() {
            String url = "http://127.0.0.1:" + port + "/webhook";
            repository.register(createRegistration("wh-1", "order.created", url));

            service.dispatch("order.created", "test data");

            assertThat(lastSignatureHeader).isNull();
        }

        @Test
        @DisplayName("should compute consistent HMAC for same input")
        void shouldComputeConsistentHmacForSameInput() {
            String payload = "test-payload";
            String secret = "test-secret";

            String sig1 = service.computeHmac(payload, secret);
            String sig2 = service.computeHmac(payload, secret);

            assertThat(sig1).isEqualTo(sig2);
        }

        @Test
        @DisplayName("should compute different HMAC for different input")
        void shouldComputeDifferentHmacForDifferentInput() {
            String secret = "test-secret";

            String sig1 = service.computeHmac("payload-1", secret);
            String sig2 = service.computeHmac("payload-2", secret);

            assertThat(sig1).isNotEqualTo(sig2);
        }
    }

    @Nested
    @DisplayName("register and unregister")
    class RegisterAndUnregister {

        @Test
        @DisplayName("should register webhook via service")
        void shouldRegisterWebhookViaService() {
            WebhookRegistration registration = createRegistration("wh-1", "order.created", "https://example.com");

            service.register(registration);

            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("should unregister webhook via service")
        void shouldUnregisterWebhookViaService() {
            service.register(createRegistration("wh-1", "order.created", "https://example.com"));

            service.unregister("wh-1");

            assertThat(repository.findAll()).isEmpty();
        }
    }

    private static WebhookRegistration createRegistration(String id, String event, String url) {
        return WebhookRegistration.builder()
                .id(id)
                .event(event)
                .url(url)
                .active(true)
                .build();
    }
}
