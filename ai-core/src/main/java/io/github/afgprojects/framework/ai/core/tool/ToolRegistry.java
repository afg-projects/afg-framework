package io.github.afgprojects.framework.ai.core.tool;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for managing and discovering tools.
 * <p>
 * The ToolRegistry provides a central location for registering and
 * retrieving tools. It supports dynamic registration and lookup
 * by name.
 * </p>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface ToolRegistry {

    /**
     * Registers a tool in the registry.
     *
     * @param tool the tool to register
     * @param <I>  the input type
     * @param <O>  the output type
     * @throws IllegalArgumentException if a tool with the same name already exists
     */
    <I, O> void register(Tool<I, O> tool);

    /**
     * Registers a tool in the registry, replacing any existing tool with the same name.
     *
     * @param tool the tool to register
     * @param <I>  the input type
     * @param <O>  the output type
     */
    <I, O> void registerOrReplace(Tool<I, O> tool);

    /**
     * Retrieves a tool by name.
     *
     * @param name the name of the tool
     * @return an Optional containing the tool, or empty if not found
     */
    Optional<Tool<?, ?>> getTool(String name);

    /**
     * Retrieves a tool by name with type parameters.
     *
     * @param name         the name of the tool
     * @param inputType    the expected input type
     * @param outputType   the expected output type
     * @param <I>          the input type
     * @param <O>          the output type
     * @return an Optional containing the typed tool, or empty if not found or type mismatch
     */
    <I, O> Optional<Tool<I, O>> getTool(String name, Class<I> inputType, Class<O> outputType);

    /**
     * Gets all registered tools.
     *
     * @return a collection of all registered tools
     */
    Collection<Tool<?, ?>> getAllTools();

    
    /**
     * Checks if a tool with the given name exists.
     *
     * @param name the name of the tool
     * @return true if the tool exists, false otherwise
     */
    boolean exists(String name);

    /**
     * Unregisters a tool by name.
     *
     * @param name the name of the tool to unregister
     * @return true if the tool was unregistered, false if it didn't exist
     */
    boolean unregister(String name);

    /**
     * Clears all registered tools.
     */
    void clear();

    /**
     * Returns the number of registered tools.
     *
     * @return the count of registered tools
     */
    int size();
}
