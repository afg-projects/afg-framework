package io.github.afgprojects.framework.core.api.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

@DisplayName("LocalSseConnectionManager")
class LocalSseConnectionManagerTest {

    private LocalSseConnectionManager manager;
    private AfgCoreProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AfgCoreProperties();
        // 使用较短的超时以加快测试
        properties.getSse().setTimeout(5000);
        properties.getSse().setMaxConnections(10);
        manager = new LocalSseConnectionManager(properties);
    }

    @Nested
    @DisplayName("createConnection")
    class CreateConnection {

        @Test
        @DisplayName("should create SSE emitter with configured timeout")
        void shouldCreateSseEmitterWithConfiguredTimeout() {
            var emitter = manager.createConnection("client-1");

            assertThat(emitter).isNotNull();
            assertThat(manager.isConnected("client-1")).isTrue();
        }

        @Test
        @DisplayName("should replace existing connection when creating for same client")
        void shouldReplaceExistingConnectionWhenCreatingForSameClient() {
            var emitter1 = manager.createConnection("client-1");
            var emitter2 = manager.createConnection("client-1");

            assertThat(manager.isConnected("client-1")).isTrue();
            assertThat(manager.getActiveConnectionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create multiple connections for different clients")
        void shouldCreateMultipleConnectionsForDifferentClients() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");
            manager.createConnection("client-3");

            assertThat(manager.getActiveConnectionCount()).isEqualTo(3);
            assertThat(manager.getActiveConnectionIds()).containsExactlyInAnyOrder("client-1", "client-2", "client-3");
        }

        @Test
        @DisplayName("should throw BusinessException when max connections exceeded")
        void shouldThrowBusinessExceptionWhenMaxConnectionsExceeded() {
            properties.getSse().setMaxConnections(2);
            manager = new LocalSseConnectionManager(properties);

            manager.createConnection("client-1");
            manager.createConnection("client-2");

            assertThatThrownBy(() -> manager.createConnection("client-3"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("sendEvent")
    class SendEvent {

        @Test
        @DisplayName("should send event to connected client without error")
        void shouldSendEventToConnectedClientWithoutError() {
            var emitter = manager.createConnection("client-1");

            // SseEmitter.send() 在非 Servlet 环境下可能抛异常，
            // 此处验证连接存在时调用不会导致连接丢失
            manager.sendEvent("client-1", org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data("test"));

            assertThat(manager.isConnected("client-1")).isTrue();
        }

        @Test
        @DisplayName("should not throw when sending to non-existent client")
        void shouldNotThrowWhenSendingToNonExistentClient() {
            // 不应抛异常，仅静默忽略
            manager.sendEvent("non-existent", org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data("test"));
        }
    }

    @Nested
    @DisplayName("sendEventToAll")
    class SendEventToAll {

        @Test
        @DisplayName("should broadcast event to all connected clients")
        void shouldBroadcastEventToAllConnectedClients() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");

            manager.sendEventToAll(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data("broadcast"));

            // 验证连接仍然活跃（发送未导致断开）
            assertThat(manager.getActiveConnectionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not throw when no active connections")
        void shouldNotThrowWhenNoActiveConnections() {
            // 无活跃连接时广播不应抛异常
            manager.sendEventToAll(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data("broadcast"));
        }
    }

    @Nested
    @DisplayName("closeConnection")
    class CloseConnection {

        @Test
        @DisplayName("should close and remove connection")
        void shouldCloseAndRemoveConnection() {
            manager.createConnection("client-1");
            assertThat(manager.isConnected("client-1")).isTrue();

            manager.closeConnection("client-1");
            assertThat(manager.isConnected("client-1")).isFalse();
            assertThat(manager.getActiveConnectionCount()).isZero();
        }

        @Test
        @DisplayName("should not throw when closing non-existent connection")
        void shouldNotThrowWhenClosingNonExistentConnection() {
            manager.closeConnection("non-existent");
        }

        @Test
        @DisplayName("should only close the specified connection")
        void shouldOnlyCloseTheSpecifiedConnection() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");

            manager.closeConnection("client-1");

            assertThat(manager.isConnected("client-1")).isFalse();
            assertThat(manager.isConnected("client-2")).isTrue();
            assertThat(manager.getActiveConnectionCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isConnected")
    class IsConnected {

        @Test
        @DisplayName("should return false when client is not connected")
        void shouldReturnFalseWhenClientIsNotConnected() {
            assertThat(manager.isConnected("non-existent")).isFalse();
        }

        @Test
        @DisplayName("should return true when client is connected")
        void shouldReturnTrueWhenClientIsConnected() {
            manager.createConnection("client-1");
            assertThat(manager.isConnected("client-1")).isTrue();
        }

        @Test
        @DisplayName("should return false after connection is closed")
        void shouldReturnFalseAfterConnectionIsClosed() {
            manager.createConnection("client-1");
            manager.closeConnection("client-1");
            assertThat(manager.isConnected("client-1")).isFalse();
        }
    }

    @Nested
    @DisplayName("getActiveConnectionCount")
    class GetActiveConnectionCount {

        @Test
        @DisplayName("should return 0 when no connections")
        void shouldReturnZeroWhenNoConnections() {
            assertThat(manager.getActiveConnectionCount()).isZero();
        }

        @Test
        @DisplayName("should return correct count after connections are created")
        void shouldReturnCorrectCountAfterConnectionsAreCreated() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");

            assertThat(manager.getActiveConnectionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should decrease count after connection is closed")
        void shouldDecreaseCountAfterConnectionIsClosed() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");
            manager.closeConnection("client-1");

            assertThat(manager.getActiveConnectionCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getActiveConnectionIds")
    class GetActiveConnectionIds {

        @Test
        @DisplayName("should return empty list when no connections")
        void shouldReturnEmptyListWhenNoConnections() {
            assertThat(manager.getActiveConnectionIds()).isEmpty();
        }

        @Test
        @DisplayName("should return all active client IDs")
        void shouldReturnAllActiveClientIds() {
            manager.createConnection("client-1");
            manager.createConnection("client-2");

            assertThat(manager.getActiveConnectionIds())
                    .containsExactlyInAnyOrder("client-1", "client-2");
        }
    }
}
