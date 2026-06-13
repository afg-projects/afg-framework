package io.github.afgprojects.framework.core.api.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@DisplayName("InMemoryWebhookRepository")
class InMemoryWebhookRepositoryTest {

    private InMemoryWebhookRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryWebhookRepository();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register webhook and find it by ID")
        void shouldRegisterWebhookAndFindItById() {
            WebhookRegistration registration = createRegistration("wh-1", "order.created", "https://example.com/webhook");

            repository.register(registration);

            List<WebhookRegistration> all = repository.findAll();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getId()).isEqualTo("wh-1");
        }

        @Test
        @DisplayName("should replace existing registration with same ID")
        void shouldReplaceExistingRegistrationWithSameId() {
            repository.register(createRegistration("wh-1", "order.created", "https://old.example.com"));
            repository.register(createRegistration("wh-1", "order.updated", "https://new.example.com"));

            List<WebhookRegistration> all = repository.findAll();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getEvent()).isEqualTo("order.updated");
            assertThat(all.get(0).getUrl()).isEqualTo("https://new.example.com");
        }

        @Test
        @DisplayName("should register multiple webhooks with different IDs")
        void shouldRegisterMultipleWebhooksWithDifferentIds() {
            repository.register(createRegistration("wh-1", "order.created", "https://example1.com"));
            repository.register(createRegistration("wh-2", "order.updated", "https://example2.com"));
            repository.register(createRegistration("wh-3", "order.deleted", "https://example3.com"));

            assertThat(repository.findAll()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("should remove registered webhook")
        void shouldRemoveRegisteredWebhook() {
            repository.register(createRegistration("wh-1", "order.created", "https://example.com"));
            assertThat(repository.findAll()).hasSize(1);

            repository.unregister("wh-1");

            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("should not throw when unregistering non-existent webhook")
        void shouldNotThrowWhenUnregisteringNonExistentWebhook() {
            repository.unregister("non-existent");
        }

        @Test
        @DisplayName("should only remove the specified webhook")
        void shouldOnlyRemoveTheSpecifiedWebhook() {
            repository.register(createRegistration("wh-1", "order.created", "https://example1.com"));
            repository.register(createRegistration("wh-2", "order.updated", "https://example2.com"));

            repository.unregister("wh-1");

            List<WebhookRegistration> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getId()).isEqualTo("wh-2");
        }
    }

    @Nested
    @DisplayName("findByEvent")
    class FindByEvent {

        @Test
        @DisplayName("should return webhooks matching the event")
        void shouldReturnWebhooksMatchingTheEvent() {
            repository.register(createRegistration("wh-1", "order.created", "https://example1.com"));
            repository.register(createRegistration("wh-2", "order.updated", "https://example2.com"));
            repository.register(createRegistration("wh-3", "order.created", "https://example3.com"));

            List<WebhookRegistration> results = repository.findByEvent("order.created");

            assertThat(results).hasSize(2);
            assertThat(results).allSatisfy(r -> assertThat(r.getEvent()).isEqualTo("order.created"));
        }

        @Test
        @DisplayName("should return empty list when no webhooks match the event")
        void shouldReturnEmptyListWhenNoWebhooksMatchTheEvent() {
            repository.register(createRegistration("wh-1", "order.created", "https://example.com"));

            List<WebhookRegistration> results = repository.findByEvent("order.deleted");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should not return inactive webhooks")
        void shouldNotReturnInactiveWebhooks() {
            WebhookRegistration active = createRegistration("wh-1", "order.created", "https://example1.com");
            WebhookRegistration inactive = WebhookRegistration.builder()
                    .id("wh-2").event("order.created").url("https://example2.com").active(false).build();

            repository.register(active);
            repository.register(inactive);

            List<WebhookRegistration> results = repository.findByEvent("order.created");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("wh-1");
        }

        @Test
        @DisplayName("should return empty list when no registrations exist")
        void shouldReturnEmptyListWhenNoRegistrationsExist() {
            List<WebhookRegistration> results = repository.findByEvent("order.created");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return all registrations including inactive")
        void shouldReturnAllRegistrationsIncludingInactive() {
            WebhookRegistration active = createRegistration("wh-1", "order.created", "https://example1.com");
            WebhookRegistration inactive = WebhookRegistration.builder()
                    .id("wh-2").event("order.updated").url("https://example2.com").active(false).build();

            repository.register(active);
            repository.register(inactive);

            List<WebhookRegistration> all = repository.findAll();

            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no registrations exist")
        void shouldReturnEmptyListWhenNoRegistrationsExist() {
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
