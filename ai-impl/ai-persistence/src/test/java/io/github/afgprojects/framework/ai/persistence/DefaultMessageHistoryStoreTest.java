package io.github.afgprojects.framework.ai.persistence;

import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultMessageHistoryStore 单元测试
 */
class DefaultMessageHistoryStoreTest {

    private DefaultMessageHistoryStore store;

    @BeforeEach
    void setUp() {
        store = new DefaultMessageHistoryStore();
    }

    @Test
    @DisplayName("添加消息")
    void addMessage() {
        MessageHistoryStore.Message message = createMessage(MessageHistoryStore.MessageRole.USER, "Hello");

        MessageHistoryStore.Message added = store.addMessage("session-001", message);

        assertThat(added.getMessageId()).isNotBlank();
        assertThat(added.getSessionId()).isEqualTo("session-001");
        assertThat(added.getRole()).isEqualTo(MessageHistoryStore.MessageRole.USER);
        assertThat(added.getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("获取会话消息")
    void getMessages() {
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello"));
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.ASSISTANT, "Hi there!"));

        java.util.List<MessageHistoryStore.Message> messages = store.getMessages("session-001");

        assertThat(messages).hasSize(2);
    }

    @Test
    @DisplayName("获取分页消息")
    void getMessages_pagination() {
        for (int i = 0; i < 10; i++) {
            store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Message " + i));
        }

        java.util.List<MessageHistoryStore.Message> page1 = store.getMessages("session-001", 0, 5);
        java.util.List<MessageHistoryStore.Message> page2 = store.getMessages("session-001", 5, 5);

        assertThat(page1).hasSize(5);
        assertThat(page2).hasSize(5);
    }

    @Test
    @DisplayName("获取最近消息")
    void getRecentMessages() {
        for (int i = 0; i < 10; i++) {
            store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Message " + i));
        }

        java.util.List<MessageHistoryStore.Message> recent = store.getRecentMessages("session-001", 3);

        assertThat(recent).hasSize(3);
    }

    @Test
    @DisplayName("更新消息")
    void updateMessage() {
        MessageHistoryStore.Message added = store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello"));

        added.setContent("Hello, updated!");
        store.updateMessage(added);

        MessageHistoryStore.Message updated = store.getMessage(added.getMessageId());

        assertThat(updated.getContent()).isEqualTo("Hello, updated!");
    }

    @Test
    @DisplayName("删除消息")
    void deleteMessage() {
        MessageHistoryStore.Message added = store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello"));

        store.deleteMessage(added.getMessageId());

        MessageHistoryStore.Message deleted = store.getMessage(added.getMessageId());

        assertThat(deleted.getStatus()).isEqualTo(MessageHistoryStore.MessageStatus.DELETED);
    }

    @Test
    @DisplayName("删除会话所有消息")
    void deleteSessionMessages() {
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello"));
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.ASSISTANT, "Hi!"));

        store.deleteSessionMessages("session-001");

        java.util.List<MessageHistoryStore.Message> messages = store.getMessages("session-001");

        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("搜索消息")
    void searchMessages() {
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello world"));
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.ASSISTANT, "Hi there!"));
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "How are you?"));

        java.util.List<MessageHistoryStore.Message> results = store.searchMessages("session-001", "Hello", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("获取消息数量")
    void getMessageCount() {
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.USER, "Hello"));
        store.addMessage("session-001", createMessage(MessageHistoryStore.MessageRole.ASSISTANT, "Hi!"));

        long count = store.getMessageCount("session-001");

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("获取 Token 统计")
    void getTokenStats() {
        MessageHistoryStore.Message msg1 = createMessageWithTokens(MessageHistoryStore.MessageRole.USER, "Hello", 10, 0);
        MessageHistoryStore.Message msg2 = createMessageWithTokens(MessageHistoryStore.MessageRole.ASSISTANT, "Hi!", 5, 20);

        store.addMessage("session-001", msg1);
        store.addMessage("session-001", msg2);

        MessageHistoryStore.TokenStats stats = store.getTokenStats("session-001");

        assertThat(stats.getTotalInputTokens()).isEqualTo(15);
        assertThat(stats.getTotalOutputTokens()).isEqualTo(20);
        assertThat(stats.getTotalTokens()).isEqualTo(35);
    }

    private MessageHistoryStore.Message createMessage(MessageHistoryStore.MessageRole role, String content) {
        return new MessageHistoryStore.Message() {
            @Override
            public String getMessageId() { return null; }
            @Override
            public String getSessionId() { return null; }
            @Override
            public MessageHistoryStore.MessageRole getRole() { return role; }
            @Override
            public String getContent() { return content; }
            @Override
            public void setContent(String newContent) {}
            @Override
            public java.time.Instant getCreatedAt() { return null; }
            @Override
            public MessageHistoryStore.TokenUsage getTokenUsage() { return null; }
            @Override
            public void setTokenUsage(MessageHistoryStore.TokenUsage usage) {}
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public Map<String, String> getMetadata() { return Map.of(); }
            @Override
            public String getParentMessageId() { return null; }
            @Override
            public MessageHistoryStore.MessageStatus getStatus() { return MessageHistoryStore.MessageStatus.NORMAL; }
            @Override
            public void setStatus(MessageHistoryStore.MessageStatus status) {}
        };
    }

    private MessageHistoryStore.Message createMessageWithTokens(MessageHistoryStore.MessageRole role, String content, long inputTokens, long outputTokens) {
        MessageHistoryStore.TokenUsage usage = new MessageHistoryStore.TokenUsage() {
            @Override
            public long getInputTokens() { return inputTokens; }
            @Override
            public long getOutputTokens() { return outputTokens; }
            @Override
            public long getTotalTokens() { return inputTokens + outputTokens; }
        };

        return new MessageHistoryStore.Message() {
            @Override
            public String getMessageId() { return null; }
            @Override
            public String getSessionId() { return null; }
            @Override
            public MessageHistoryStore.MessageRole getRole() { return role; }
            @Override
            public String getContent() { return content; }
            @Override
            public void setContent(String newContent) {}
            @Override
            public java.time.Instant getCreatedAt() { return null; }
            @Override
            public MessageHistoryStore.TokenUsage getTokenUsage() { return usage; }
            @Override
            public void setTokenUsage(MessageHistoryStore.TokenUsage u) {}
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public Map<String, String> getMetadata() { return Map.of(); }
            @Override
            public String getParentMessageId() { return null; }
            @Override
            public MessageHistoryStore.MessageStatus getStatus() { return MessageHistoryStore.MessageStatus.NORMAL; }
            @Override
            public void setStatus(MessageHistoryStore.MessageStatus status) {}
        };
    }
}