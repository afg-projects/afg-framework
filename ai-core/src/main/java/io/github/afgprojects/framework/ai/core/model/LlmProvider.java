package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;

/**
 * Interface for creating LLM clients.
 * <p>
 * LlmProvider is a factory for creating LLM client instances.
 * Each provider implementation supports a specific LLM service
 * (e.g., OpenAI, Anthropic, Azure OpenAI).
 *
 * <p>Example usage:
 * <pre>{@code
 * // Get a provider
 * LlmProvider provider = LlmProvider.getProvider("openai");
 *
 * // Create a client
 * LlmConfig config = new LlmConfig("sk-xxx", "gpt-4", null, Duration.ofSeconds(30));
 * LlmClient client = provider.createClient(config);
 *
 * // Use the client
 * LlmResponse response = client.chat(request);
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface LlmProvider {

    /**
     * Gets the unique name of this provider.
     * <p>
     * The name should be lowercase and hyphen-separated,
     * e.g., "openai", "anthropic", "azure-openai".
     *
     * @return the provider name
     */
    @NonNull
    String getName();

    /**
     * Creates a new LLM client with the given configuration.
     * <p>
     * The returned client is ready to use and does not require
     * additional initialization.
     *
     * @param config the configuration for the client
     * @return a new LLM client instance
     * @throws IllegalArgumentException if config is null or invalid
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if client creation fails
     */
    @NonNull
    LlmClient createClient(@NonNull LlmConfig config);

    /**
     * Validates the configuration for this provider.
     * <p>
     * This method should check that all required configuration
     * parameters are present and valid for this provider.
     *
     * @param config the configuration to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
    default void validateConfig(@NonNull LlmConfig config) {
        // Default: no validation
    }

    /**
     * Gets the default base URL for this provider.
     * <p>
     * This is used when no base URL is specified in the configuration.
     *
     * @return the default base URL, or null if not applicable
     */
    default String getDefaultBaseUrl() {
        return null;
    }

    /**
     * Checks if this provider requires an API key.
     *
     * @return true if an API key is required
     */
    default boolean requiresApiKey() {
        return true;
    }

    /**
     * Gets the list of models supported by this provider.
     * <p>
     * This is informational and may not be exhaustive.
     *
     * @return a list of supported model identifiers
     */
    default java.util.List<String> getSupportedModels() {
        return java.util.List.of();
    }

    /**
     * Checks if a model is supported by this provider.
     *
     * @param model the model identifier
     * @return true if the model is supported
     */
    default boolean supportsModel(@NonNull String model) {
        return true; // Default: assume all models are supported
    }
}
