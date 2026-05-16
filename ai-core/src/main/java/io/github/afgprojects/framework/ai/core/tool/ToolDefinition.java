package io.github.afgprojects.framework.ai.core.tool;

/**
 * Represents the definition of a tool for registration and discovery.
 * <p>
 * A ToolDefinition contains the metadata about a tool without the
 * actual implementation. This is useful for tool discovery and
 * for providing tool information to AI models.
 * </p>
 *
 * @param name        the unique name of the tool
 * @param description the human-readable description
 * @param inputSchema the JSON schema for input parameters
 * @author AFG Projects
 * @since 1.0.0
 */
public record ToolDefinition(
    String name,
    String description,
    String inputSchema
) {

    /**
     * Creates a ToolDefinition with validated parameters.
     *
     * @param name        the unique name of the tool
     * @param description the human-readable description
     * @param inputSchema the JSON schema for input parameters
     * @throws IllegalArgumentException if name is null or blank
     */
    public ToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
        if (description == null) {
            description = "";
        }
        if (inputSchema == null) {
            inputSchema = "{}";
        }
    }

    /**
     * Creates a ToolDefinition from a Tool instance.
     *
     * @param tool the tool to extract definition from
     * @param <I>  the input type
     * @param <O>  the output type
     * @return a new ToolDefinition instance
     */
    public static <I, O> ToolDefinition from(Tool<I, O> tool) {
        return new ToolDefinition(tool.name(), tool.description(), tool.inputSchema());
    }

    /**
     * Creates a ToolDefinition with an empty input schema.
     *
     * @param name        the unique name of the tool
     * @param description the human-readable description
     * @return a new ToolDefinition instance
     */
    public static ToolDefinition of(String name, String description) {
        return new ToolDefinition(name, description, "{}");
    }
}
