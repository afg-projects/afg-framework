package io.github.afgprojects.framework.core.api.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@DisplayName("LogNotificationService")
class LogNotificationServiceTest {

    private LogNotificationService service;

    @BeforeEach
    void setUp() {
        service = new LogNotificationService();
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("should return success result when sending notification")
        void shouldReturnSuccessResultWhenSendingNotification() {
            Notification notification = Notification.builder()
                    .to("user-123")
                    .channel(NotificationChannel.EMAIL)
                    .subject("Test Subject")
                    .content("Test Content")
                    .build();

            NotificationResult result = service.send(notification);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessageId()).startsWith("log-");
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("should return success result for each channel type")
        void shouldReturnSuccessResultForEachChannelType() {
            for (NotificationChannel channel : NotificationChannel.values()) {
                Notification notification = Notification.builder()
                        .to("user-123")
                        .channel(channel)
                        .build();

                NotificationResult result = service.send(notification);

                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getChannel()).isEqualTo(channel);
            }
        }

        @Test
        @DisplayName("should handle notification with template variables")
        void shouldHandleNotificationWithTemplateVariables() {
            Notification notification = Notification.builder()
                    .to("user-123")
                    .channel(NotificationChannel.SMS)
                    .template("welcome")
                    .build();
            notification.variable("username", "张三");
            notification.variable("code", "123456");

            NotificationResult result = service.send(notification);

            assertThat(result.isSuccess()).isTrue();
            assertThat(notification.getVariables()).containsEntry("username", "张三");
            assertThat(notification.getVariables()).containsEntry("code", "123456");
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
                    Notification.builder().to("user-2").channel(NotificationChannel.SMS).build(),
                    Notification.builder().to("user-3").channel(NotificationChannel.DINGTALK).build()
            );

            List<NotificationResult> results = service.sendBatch(notifications);

            assertThat(results).hasSize(3);
            assertThat(results).allMatch(NotificationResult::isSuccess);
            assertThat(results.get(0).getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(results.get(1).getChannel()).isEqualTo(NotificationChannel.SMS);
            assertThat(results.get(2).getChannel()).isEqualTo(NotificationChannel.DINGTALK);
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            List<NotificationResult> results = service.sendBatch(List.of());

            assertThat(results).isEmpty();
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
