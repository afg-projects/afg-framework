package io.github.afgprojects.framework.ai.core.tool;

/**
 * Represents a tool call request from an AI model.
 *
 * @param id        the unique identifier for this tool call
 * @param name      the name of the tool to call
 * @param arguments the arguments to pass to the tool
 * @author AFG Projects
 * @since 1.0.0
 */
public record ToolCall(
    String id,
    String name,
    java.util.Map<String, Object> arguments
) {

    /**
     * Creates a ToolCall with validated parameters.
     *
     * @param id        the unique identifier for this tool call
     * @param name      the name of the tool to call
     * @param arguments the arguments to pass to the tool
     * @throws IllegalArgumentException if id or name is null or blank
     */
    public ToolCall {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tool call id cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
        if (arguments == null) {
            arguments = java.util.Map.of();
        }
    }

    /**
     * Creates a ToolCall with empty arguments.
     *
     * @param id   the unique identifier for this tool call
     * @param name the name of the tool to call
     * @return a new ToolCall instance
     */
    public static ToolCall of(String id, String name) {
        return new ToolCall(id, name, java.util.Map.of());
    }

    /**
     * Creates a ToolCall with a single argument.
     *
     * @param id    the unique identifier for this tool call
     * @param name  the name of the tool to call
     * @param key   the argument key
     * @param value the argument value
     * @return a new ToolCall instance
     */
    public static ToolCall of(String id, String name, String key, Object value) {
        return new ToolCall(id, name, java.util.Map.of(key, value));
    }
}
