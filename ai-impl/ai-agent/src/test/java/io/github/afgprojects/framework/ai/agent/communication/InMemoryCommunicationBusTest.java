package io.github.afgprojects.framework.ai.agent.communication;

import io.github.afgprojects.framework.ai.core.multiagent.communication.AgentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCommunicationBusTest {

    private InMemoryCommunicationBus bus;

    @BeforeEach
    void setUp() {
        bus = new InMemoryCommunicationBus();
    }

    @Test
    @DisplayName("发送和接收消息")
    void sendAndReceive_shouldWork() {
        AgentMessage message = AgentMessage.taskRequest("agent-a", "agent-b", "task data");

        bus.send("agent-a", "agent-b", message);

        var received = bus.receive("agent-b", Duration.ofSeconds(1));
        assertThat(received).isPresent();
        assertThat(received.get().fromAgent()).isEqualTo("agent-a");
    }

    @Test
    @DisplayName("获取待处理消息")
    void getPendingMessages_returnsMessages() {
        bus.send("agent-a", "agent-b", AgentMessage.taskRequest("agent-a", "agent-b", "task1"));
        bus.send("agent-a", "agent-b", AgentMessage.taskRequest("agent-a", "agent-b", "task2"));

        var messages = bus.getPendingMessages("agent-b");
        assertThat(messages).hasSize(2);
    }

    @Test
    @DisplayName("清空消息队列")
    void clearMessages_shouldClear() {
        bus.send("agent-a", "agent-b", AgentMessage.taskRequest("agent-a", "agent-b", "task"));

        bus.clearMessages("agent-b");

        assertThat(bus.getPendingMessages("agent-b")).isEmpty();
    }

    @Test
    @DisplayName("发布订阅")
    void publishSubscribe_shouldWork() {
        AtomicReference<AgentMessage> received = new AtomicReference<>();
        bus.subscribe("topic-1", "agent-a", received::set);

        AgentMessage message = AgentMessage.notification("agent-b", "hello");
        bus.publish("topic-1", message);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().payload()).isEqualTo("hello");
    }

    @Test
    @DisplayName("取消订阅")
    void unsubscribe_shouldStopReceiving() {
        AtomicReference<AgentMessage> received = new AtomicReference<>();
        bus.subscribe("topic-1", "agent-a", received::set);
        bus.unsubscribe("topic-1", "agent-a");

        bus.publish("topic-1", AgentMessage.notification("agent-b", "hello"));

        assertThat(received.get()).isNull();
    }

    @Test
    @DisplayName("广播消息")
    void broadcast_shouldSendToAll() {
        // 先发送消息到 agent-a 和 agent-b 以创建它们的队列
        bus.send("coordinator", "agent-a", AgentMessage.heartbeat("coordinator"));
        bus.send("coordinator", "agent-b", AgentMessage.heartbeat("coordinator"));

        // 清空队列中的心跳消息
        bus.clearMessages("agent-a");
        bus.clearMessages("agent-b");

        // 广播消息
        bus.broadcast(AgentMessage.notification("coordinator", "broadcast message"));

        // 验证两个 agent 都收到了广播消息
        var msgA = bus.receive("agent-a", Duration.ofSeconds(1));
        var msgB = bus.receive("agent-b", Duration.ofSeconds(1));

        assertThat(msgA).isPresent();
        assertThat(msgB).isPresent();
        assertThat(msgA.get().payload()).isEqualTo("broadcast message");
        assertThat(msgB.get().payload()).isEqualTo("broadcast message");
    }

    @Test
    @DisplayName("接收消息超时返回空")
    void receive_timeout_returnsEmpty() {
        var received = bus.receive("non-existent-agent", Duration.ofMillis(100));
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("发送到 null 目标会广播")
    void send_nullTarget_broadcasts() {
        // 先发送消息以创建队列
        bus.send("coordinator", "agent-a", AgentMessage.heartbeat("coordinator"));
        bus.send("coordinator", "agent-b", AgentMessage.heartbeat("coordinator"));

        // 清空队列中的心跳消息
        bus.clearMessages("agent-a");
        bus.clearMessages("agent-b");

        // 发送消息到 null 目标（会触发广播）
        AgentMessage message = AgentMessage.notification("coordinator", "broadcast via send");
        bus.send("coordinator", null, message);

        // 验证广播效果（通过队列接收）
        var msgA = bus.receive("agent-a", Duration.ofMillis(100));
        var msgB = bus.receive("agent-b", Duration.ofMillis(100));

        assertThat(msgA).isPresent();
        assertThat(msgB).isPresent();
        assertThat(msgA.get().payload()).isEqualTo("broadcast via send");
        assertThat(msgB.get().payload()).isEqualTo("broadcast via send");
    }

    @Test
    @DisplayName("任务请求和结果消息")
    void taskRequestAndResult_shouldWork() {
        // 发送任务请求
        AgentMessage request = AgentMessage.taskRequest("agent-a", "agent-b", "do something");
        bus.send("agent-a", "agent-b", request);

        // 接收任务请求
        var receivedRequest = bus.receive("agent-b", Duration.ofSeconds(1));
        assertThat(receivedRequest).isPresent();
        assertThat(receivedRequest.get().type()).isEqualTo(AgentMessage.MessageType.TASK_REQUEST);

        // 发送任务结果
        AgentMessage result = AgentMessage.taskResult("agent-b", "agent-a", "done", receivedRequest.get().messageId());
        bus.send("agent-b", "agent-a", result);

        // 接收任务结果
        var receivedResult = bus.receive("agent-a", Duration.ofSeconds(1));
        assertThat(receivedResult).isPresent();
        assertThat(receivedResult.get().type()).isEqualTo(AgentMessage.MessageType.TASK_RESULT);
        assertThat(receivedResult.get().correlationId()).isEqualTo(receivedRequest.get().messageId());
    }

    @Test
    @DisplayName("查询和响应消息")
    void queryAndResponse_shouldWork() {
        // 发送查询
        AgentMessage query = AgentMessage.query("agent-a", "agent-b", "what is the status?");
        bus.send("agent-a", "agent-b", query);

        // 接收查询
        var receivedQuery = bus.receive("agent-b", Duration.ofSeconds(1));
        assertThat(receivedQuery).isPresent();
        assertThat(receivedQuery.get().type()).isEqualTo(AgentMessage.MessageType.QUERY);

        // 发送响应
        AgentMessage response = AgentMessage.response("agent-b", "agent-a", "status is ok", receivedQuery.get().messageId());
        bus.send("agent-b", "agent-a", response);

        // 接收响应
        var receivedResponse = bus.receive("agent-a", Duration.ofSeconds(1));
        assertThat(receivedResponse).isPresent();
        assertThat(receivedResponse.get().type()).isEqualTo(AgentMessage.MessageType.RESPONSE);
    }

    @Test
    @DisplayName("错误消息")
    void errorMessage_shouldWork() {
        AgentMessage error = AgentMessage.error("agent-b", "agent-a", "something went wrong", "correlation-123");
        bus.send("agent-b", "agent-a", error);

        var received = bus.receive("agent-a", Duration.ofSeconds(1));
        assertThat(received).isPresent();
        assertThat(received.get().type()).isEqualTo(AgentMessage.MessageType.ERROR);
        assertThat(received.get().payload()).isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("心跳消息")
    void heartbeatMessage_shouldWork() {
        AgentMessage heartbeat = AgentMessage.heartbeat("agent-a");
        bus.send("agent-a", "agent-b", heartbeat);

        var received = bus.receive("agent-b", Duration.ofSeconds(1));
        assertThat(received).isPresent();
        assertThat(received.get().type()).isEqualTo(AgentMessage.MessageType.HEARTBEAT);
    }

    @Test
    @DisplayName("多个订阅者接收同一主题消息")
    void multipleSubscribers_shouldAllReceive() {
        AtomicReference<AgentMessage> received1 = new AtomicReference<>();
        AtomicReference<AgentMessage> received2 = new AtomicReference<>();
        AtomicReference<AgentMessage> received3 = new AtomicReference<>();

        bus.subscribe("topic-x", "agent-1", received1::set);
        bus.subscribe("topic-x", "agent-2", received2::set);
        bus.subscribe("topic-x", "agent-3", received3::set);

        AgentMessage message = AgentMessage.notification("publisher", "important update");
        bus.publish("topic-x", message);

        assertThat(received1.get()).isNotNull();
        assertThat(received2.get()).isNotNull();
        assertThat(received3.get()).isNotNull();
        assertThat(received1.get().messageId()).isEqualTo(received2.get().messageId());
        assertThat(received2.get().messageId()).isEqualTo(received3.get().messageId());
    }

    @Test
    @DisplayName("消息处理器异常不影响其他订阅者")
    void handlerException_shouldNotAffectOtherSubscribers() {
        AtomicReference<AgentMessage> received = new AtomicReference<>();

        bus.subscribe("topic-y", "agent-1", msg -> {
            throw new RuntimeException("Handler error");
        });
        bus.subscribe("topic-y", "agent-2", received::set);

        AgentMessage message = AgentMessage.notification("publisher", "test");
        bus.publish("topic-y", message);

        assertThat(received.get()).isNotNull();
    }
}
