package io.github.afgprojects.framework.ai.core.memory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Manages conversation history for AI agents.
 * <p>
 * ConversationMemory provides an abstraction for storing and retrieving
 * conversation messages. Implementations can use different storage backends:
 * <ul>
 *   <li>In-memory storage (for simple use cases)</li>
 *   <li>Redis (for distributed systems)</li>
 *   <li>Database (for persistent storage)</li>
 *   <li>Vector store (for semantic search)</li>
 * </ul>
 *
 * <p>Each conversation is identified by a unique session ID, allowing
 * multiple concurrent conversations to be managed independently.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConversationMemory memory = new InMemoryConversationMemory();
 *
 * // Add messages to a conversation
 * memory.addMessage("session-1", Message.user("Hello!"));
 * memory.addMessage("session-1", Message.assistant("Hi there!"));
 *
 * // Retrieve conversation history
 * List<Message> history = memory.getHistory("session-1");
 *
 * // Get recent messages for context window
 * List<Message> recent = memory.getRecentMessages("session-1", 10);
 *
 * // Clear a conversation
 * memory.clear("session-1");
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface ConversationMemory {

    /**
     * Adds a message to the conversation history.
     * <p>
     * The message is appended to the end of the conversation for the
     * specified session. If the session does not exist, it will be created.
     *
     * @param sessionId the unique identifier for the conversation session
     * @param message   the message to add
     * @throws NullPointerException if sessionId or message is null
     */
    void addMessage(@NonNull String sessionId, @NonNull Message message);

    /**
     * Retrieves the complete conversation history for a session.
     * <p>
     * Returns all messages in chronological order (oldest first).
     * If the session does not exist, returns an empty list.
     *
     * @param sessionId the unique identifier for the conversation session
     * @return the list of messages, or an empty list if the session does not exist
     * @throws NullPointerException if sessionId is null
     */
    @NonNull
    List<Message> getHistory(@NonNull String sessionId);

    /**
     * Clears the conversation history for a session.
     * <p>
     * Removes all messages for the specified session. If the session
     * does not exist, this method does nothing.
     *
     * @param sessionId the unique identifier for the conversation session
     * @throws NullPointerException if sessionId is null
     */
    void clear(@NonNull String sessionId);

    /**
     * Retrieves the most recent n messages from a conversation.
     * <p>
     * This is useful for managing context window limits in LLM calls.
     * Returns messages in chronological order (oldest first among the recent ones).
     * If the session has fewer than n messages, returns all available messages.
     * If the session does not exist, returns an empty list.
     *
     * @param sessionId the unique identifier for the conversation session
     * @param n         the maximum number of messages to retrieve
     * @return the list of recent messages, or an empty list if the session does not exist
     * @throws NullPointerException     if sessionId is null
     * @throws IllegalArgumentException if n is negative
     */
    @NonNull
    List<Message> getRecentMessages(@NonNull String sessionId, int n);

    /**
     * Checks if a conversation session exists.
     *
     * @param sessionId the unique identifier for the conversation session
     * @return true if the session exists and has messages, false otherwise
     * @throws NullPointerException if sessionId is null
     */
    default boolean exists(@NonNull String sessionId) {
        return !getHistory(sessionId).isEmpty();
    }

    /**
     * Gets the number of messages in a conversation.
     *
     * @param sessionId the unique identifier for the conversation session
     * @return the number of messages, or 0 if the session does not exist
     * @throws NullPointerException if sessionId is null
     */
    default int size(@NonNull String sessionId) {
        return getHistory(sessionId).size();
    }
}
