package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.memory.Message;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Advisor adapter for managing conversation memory.
 * <p>
 * This adapter wraps the framework's ConversationMemory interface and provides
 * automatic loading of conversation history before requests and saving new
 * messages after responses.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConversationMemory memory = new InMemoryConversationMemory();
 * ChatMemoryAdvisorAdapter advisor = new ChatMemoryAdvisorAdapter(
 *     memory,
 *     "session-123",
 *     10  // Keep last 10 messages
 * );
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ChatMemoryAdvisorAdapter implements LlmAdvisor {

    private final ConversationMemory conversationMemory;
    private final String sessionId;
    private final int maxHistorySize;
    private final boolean saveResponse;

    /**
     * Creates an advisor with default settings.
     *
     * @param conversationMemory the conversation memory backend
     * @param sessionId          the session identifier
     */
    public ChatMemoryAdvisorAdapter(@NonNull ConversationMemory conversationMemory, @NonNull String sessionId) {
        this(conversationMemory, sessionId, 20, true);
    }

    /**
     * Creates an advisor with custom settings.
     *
     * @param conversationMemory the conversation memory backend
     * @param sessionId          the session identifier
     * @param maxHistorySize     the maximum number of historical messages to load
     * @param saveResponse       whether to save assistant responses to memory
     */
    public ChatMemoryAdvisorAdapter(
            @NonNull ConversationMemory conversationMemory,
            @NonNull String sessionId,
            int maxHistorySize,
            boolean saveResponse) {
        this.conversationMemory = conversationMemory;
        this.sessionId = sessionId;
        this.maxHistorySize = maxHistorySize;
        this.saveResponse = saveResponse;
    }

    @Override
    public int getOrder() {
        // Execute after system prompt but before other advisors
        return 100;
    }

    @Override
    public String getName() {
        return "ChatMemoryAdvisorAdapter";
    }

    @Override
    @NonNull
    public List<org.springframework.ai.chat.messages.Message> apply(
            @NonNull List<org.springframework.ai.chat.messages.Message> messages,
            @NonNull AdvisorContext context) {
        // Load conversation history
        List<Message> history = maxHistorySize > 0
                ? conversationMemory.getRecentMessages(sessionId, maxHistorySize)
                : conversationMemory.getHistory(sessionId);

        if (history.isEmpty()) {
            return messages;
        }

        // Convert framework messages to Spring AI messages
        List<org.springframework.ai.chat.messages.Message> result = new ArrayList<>();
        for (Message msg : history) {
            result.add(convertToSpringAiMessage(msg));
        }

        // Add existing request messages
        result.addAll(messages);

        return result;
    }

    @Override
    public void onResponse(@NonNull LlmResponse response, @NonNull AdvisorContext context) {
        if (!saveResponse) {
            return;
        }

        // Save the user message
        var request = context.getRequest();
        if (request != null && !request.messages().isEmpty()) {
            Message lastMessage = request.messages().get(request.messages().size() - 1);
            if (lastMessage.role() == Message.Role.USER) {
                conversationMemory.addMessage(sessionId, lastMessage);
            }
        }

        // Save assistant response
        if (response.content() != null && !response.content().isBlank()) {
            Message assistantMessage = Message.assistant(response.content());
            conversationMemory.addMessage(sessionId, assistantMessage);
        }
    }

    /**
     * Converts a framework Message to a Spring AI Message.
     */
    private org.springframework.ai.chat.messages.Message convertToSpringAiMessage(Message msg) {
        return switch (msg.role()) {
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(msg.content());
            case USER -> new UserMessage(msg.content());
            case ASSISTANT -> {
                if (!msg.toolCalls().isEmpty()) {
                    List<AssistantMessage.ToolCall> toolCalls = msg.toolCalls().stream()
                            .map(tc -> new AssistantMessage.ToolCall(tc.id(), "function", tc.name(),
                                    tc.arguments() != null ? tc.arguments().toString() : "{}"))
                            .toList();
                    yield AssistantMessage.builder()
                            .content(msg.content())
                            .toolCalls(toolCalls)
                            .build();
                }
                yield new AssistantMessage(msg.content());
            }
            case TOOL -> {
                if (!msg.toolResults().isEmpty()) {
                    List<ToolResponseMessage.ToolResponse> responses = msg.toolResults().stream()
                            .map(tr -> new ToolResponseMessage.ToolResponse(
                                    tr.toolCallId(), tr.toolName(),
                                    tr.output() != null ? tr.output() : ""))
                            .toList();
                    yield ToolResponseMessage.builder()
                            .responses(responses)
                            .build();
                }
                yield new AssistantMessage("");
            }
        };
    }

    /**
     * Gets the session ID.
     *
     * @return the session identifier
     */
    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Clears the conversation history for this session.
     */
    public void clearHistory() {
        conversationMemory.clear(sessionId);
    }
}
