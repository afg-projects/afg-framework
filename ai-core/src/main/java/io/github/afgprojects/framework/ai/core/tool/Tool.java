package io.github.afgprojects.framework.ai.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jspecify.annotations.Nullable;

/**
 * Tool interface for AI agent function calling.
 * <p>
 * A Tool represents a callable function that an AI agent can invoke.
 * Tools are the primary mechanism for agents to interact with external
 * systems, perform actions, and retrieve information.
 * </p>
 *
 * @param <I> the input type for the tool
 * @param <O> the output type for the tool
 * @author AFG Projects
 * @since 1.0.0
 */
public interface Tool<I, O> {

    /**
     * Returns the unique name of this tool.
     * <p>
     * Tool names should be descriptive and follow a naming convention
     * such as "verb_noun" (e.g., "get_weather", "send_email").
     * </p>
     *
     * @return the tool name
     */
    String name();

    /**
     * Returns a human-readable description of what this tool does.
     * <p>
     * This description is used by AI models to understand when and
     * how to use the tool. It should be clear and concise.
     * </p>
     *
     * @return the tool description
     */
    String description();

    /**
     * Returns the JSON schema for the tool's input parameters.
     * <p>
     * The schema follows JSON Schema specification and describes
     * the expected structure of the input. Default is an empty object.
     * </p>
     *
     * @return the input schema as a JSON string, defaults to "{}"
     */
    default String inputSchema() {
        return "{}";
    }

    /**
     * Returns the TypeReference for the input type.
     * <p>
     * This is used for precise JSON deserialization of tool inputs.
     * If not provided, the input will be deserialized as Map&lt;String, Object&gt;.
     * </p>
     *
     * @return the TypeReference for the input type, or null to use default Map type
     */
    default @Nullable TypeReference<I> inputType() {
        return null;
    }

    /**
     * Executes the tool with the given input.
     *
     * @param input the input for the tool
     * @return the output from the tool execution
     * @throws ToolExecutionException if execution fails
     */
    O execute(I input);
}
