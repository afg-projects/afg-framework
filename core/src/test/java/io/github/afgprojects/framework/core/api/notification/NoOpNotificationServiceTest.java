package io.github.afgprojects.framework.core.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@DisplayName("NoOpNotificationService")
class NoOpNotificationServiceTest {

    private NoOpNotificationService service;

    @BeforeEach
    void setUp() {
        service = new NoOpNotificationService();
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("should return success result with noop messageId")
        void shouldReturnSuccessResultWithNoopMessageId() {
            Notification notification = Notification.builder()
                    .to("user-123")
                    .channel(NotificationChannel.EMAIL)
                    .build();

            NotificationResult result = service.send(notification);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).startsWith("noop-");
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        }
    }

    @Nested
    @DisplayName("sendBatch")
    class SendBatch {

        @Test
        @DisplayName("should return success results for all notifications")
        void shouldReturnSuccessResultsForAllNotifications() {
            List<Notification> notifications = List.of(
                    Notification.builder().to("user-1").channel(NotificationChannel.EMAIL).build(),
                    Notification.builder().to("user-2").channel(NotificationChannel.SMS).build()
            );

            List<NotificationResult> results = service.sendBatch(notifications);

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(NotificationResult::isSuccess);
        }
    }

    @Nested
    @DisplayName("supports")
    class Supports {

        @Test
        @DisplayName("should support all channels")
        void shouldSupportAllChannels() {
            for (NotificationChannel channel : NotificationChannel.values()) {
                assertThat(service.supports(channel)).isTrue();
            }
        }
    }
}
