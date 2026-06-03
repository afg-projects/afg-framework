package io.github.afgprojects.framework.ai.core.memory;

import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.memory.ConversationMemory;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存对话记忆实现 - 默认实现
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class InMemoryConversationMemory implements ConversationMemory {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<AiMessage>> sessions = new ConcurrentHashMap<>();
    private final int maxHistoryPerSession;

    public InMemoryConversationMemory() {
        this(1000);
    }

    public InMemoryConversationMemory(int maxHistoryPerSession) {
        this.maxHistoryPerSession = maxHistoryPerSession;
    }

    @Override
    public void addMessage(@NonNull String sessionId, @NonNull AiMessage message) {
        sessions.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(message);
        var history = sessions.get(sessionId);
        if (history.size() > maxHistoryPerSession) {
            history.subList(0, history.size() - maxHistoryPerSession).clear();
        }
    }

    @Override
    @NonNull
    public List<AiMessage> getHistory(@NonNull String sessionId) {
        return Collections.unmodifiableList(sessions.getOrDefault(sessionId, new CopyOnWriteArrayList<>()));
    }

    @Override
    public void clear(@NonNull String sessionId) {
        sessions.remove(sessionId);
        log.debug("Conversation memory cleared for session: {}", sessionId);
    }

    @Override
    @NonNull
    public List<AiMessage> getRecentMessages(@NonNull String sessionId, int n) {
        var history = sessions.getOrDefault(sessionId, new CopyOnWriteArrayList<>());
        if (history.size() <= n) {
            return Collections.unmodifiableList(history);
        }
        return Collections.unmodifiableList(history.subList(history.size() - n, history.size()));
    }
}
