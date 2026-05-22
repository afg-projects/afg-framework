package io.github.afgprojects.framework.ai.core.memory;

import io.github.afgprojects.framework.ai.core.media.MediaContent;
import io.github.afgprojects.framework.ai.core.tool.ToolCall;
import io.github.afgprojects.framework.ai.core.tool.ToolResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a message in a conversation.
 * <p>
 * A Message is the fundamental unit of conversation history, containing:
 * <ul>
 *   <li>role - the sender's role (SYSTEM, USER, ASSISTANT, TOOL)</li>
 *   <li>content - the message text content</li>
 *   <li>toolCalls - tool calls made by the assistant (if any)</li>
 *   <li>toolResults - results from tool executions (if any)</li>
 *   <li>media - media content (images, audio) for USER messages</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // User message
 * Message userMessage = new Message(Message.Role.USER, "Hello!", List.of(), List.of(), List.of());
 *
 * // Assistant message with tool call
 * Message assistantMessage = new Message(
 *     Message.Role.ASSISTANT,
 *     "Let me check that for you.",
 *     List.of(toolCall),
 *     List.of(),
 *     List.of()
 * );
 *
 * // Tool result message
 * Message toolMessage = new Message(
 *     Message.Role.TOOL,
 *     "Result: 42",
 *     List.of(),
 *     List.of(toolResult),
 *     List.of()
 * );
 * }</pre>
 *
 * @param role        the role of the message sender
 * @param content     the text content of the message
 * @param toolCalls   the list of tool calls (for ASSISTANT messages)
 * @param toolResults the list of tool results (for TOOL messages)
 * @param media       the list of media content (for USER messages)
 * @author AFG Projects
 * @since 1.0.0
 */
public record Message(
    @NonNull Role role,
    @Nullable String content,
    @NonNull List<ToolCall> toolCalls,
    @NonNull List<ToolResult> toolResults,
    @NonNull List<MediaContent> media
) {

    /**
     * Creates a Message with validated parameters.
     * <p>
     * Null safety is ensured:
     * <ul>
     *   <li>role cannot be null</li>
     *   <li>content can be null (for tool calls without text)</li>
     *   <li>toolCalls, toolResults, and media default to empty lists if null</li>
     * </ul>
     *
     * @param role        the role of the message sender
     * @param content     the text content of the message
     * @param toolCalls   the list of tool calls
     * @param toolResults the list of tool results
     * @param media       the list of media content
     * @throws IllegalArgumentException if role is null
     */
    public Message {
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }
        if (toolCalls == null) {
            toolCalls = List.of();
        } else {
            toolCalls = Collections.unmodifiableList(new ArrayList<>(toolCalls));
        }
        if (toolResults == null) {
            toolResults = List.of();
        } else {
            toolResults = Collections.unmodifiableList(new ArrayList<>(toolResults));
        }
        if (media == null) {
            media = List.of();
        } else {
            media = Collections.unmodifiableList(new ArrayList<>(media));
        }
    }

    /**
     * Creates a simple user message.
     *
     * @param content the message content
     * @return a new Message with USER role
     */
    @NonNull
    public static Message user(@Nullable String content) {
        return new Message(Role.USER, content, List.of(), List.of(), List.of());
    }

    /**
     * Creates a user message with media content.
     *
     * @param content the message content
     * @param media   the media content list
     * @return a new Message with USER role and media
     */
    @NonNull
    public static Message userWithMedia(@Nullable String content, @NonNull List<MediaContent> media) {
        return new Message(Role.USER, content, List.of(), List.of(), media);
    }

    /**
     * Creates a simple system message.
     *
     * @param content the message content
     * @return a new Message with SYSTEM role
     */
    @NonNull
    public static Message system(@Nullable String content) {
        return new Message(Role.SYSTEM, content, List.of(), List.of(), List.of());
    }

    /**
     * Creates a simple assistant message.
     *
     * @param content the message content
     * @return a new Message with ASSISTANT role
     */
    @NonNull
    public static Message assistant(@Nullable String content) {
        return new Message(Role.ASSISTANT, content, List.of(), List.of(), List.of());
    }

    /**
     * Creates an assistant message with tool calls.
     *
     * @param content   the message content (can be null)
     * @param toolCalls the list of tool calls
     * @return a new Message with ASSISTANT role and tool calls
     */
    @NonNull
    public static Message assistantWithTools(@Nullable String content, @NonNull List<ToolCall> toolCalls) {
        return new Message(Role.ASSISTANT, content, toolCalls, List.of(), List.of());
    }

    /**
     * Creates a tool result message.
     *
     * @param content     the tool result content
     * @param toolResults the list of tool results
     * @return a new Message with TOOL role
     */
    @NonNull
    public static Message tool(@Nullable String content, @NonNull List<ToolResult> toolResults) {
        return new Message(Role.TOOL, content, List.of(), toolResults, List.of());
    }

    /**
     * Represents the role of a message sender in a conversation.
     *
     * @author AFG Projects
     * @since 1.0.0
     */
    public enum Role {
        /**
         * System message - sets the behavior and context for the assistant.
         */
        SYSTEM,

        /**
         * User message - input from the human user.
         */
        USER,

        /**
         * Assistant message - response from the AI assistant.
         */
        ASSISTANT,

        /**
         * Tool message - result from a tool execution.
         */
        TOOL
    }
}
