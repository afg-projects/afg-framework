package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Configuration for an LLM client.
 * <p>
 * LlmConfig contains all the settings needed to create and configure
 * an LLM client instance:
 * <ul>
 *   <li>apiKey - the API key for authentication</li>
 *   <li>model - the model identifier (e.g., "gpt-4", "claude-3-opus")</li>
 *   <li>baseUrl - the base URL for the API endpoint</li>
 *   <li>timeout - the request timeout duration</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * LlmConfig config = new LlmConfig(
 *     "sk-xxx",
 *     "gpt-4",
 *     "https://api.openai.com/v1",
 *     Duration.ofSeconds(30)
 * );
 *
 * // Using builder pattern
 * LlmConfig config = LlmConfig.builder()
 *     .apiKey("sk-xxx")
 *     .model("gpt-4")
 *     .build();
 * }</pre>
 *
 * @param apiKey  the API key for authentication
 * @param model   the model identifier
 * @param baseUrl the base URL for the API endpoint
 * @param timeout the request timeout duration
 * @author AFG Projects
 * @since 1.0.0
 */
public record LlmConfig(
    @Nullable String apiKey,
    @NonNull String model,
    @Nullable String baseUrl,
    @NonNull Duration timeout
) {

    /**
     * Default timeout for LLM requests.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Creates an LlmConfig with validated parameters.
     * <p>
     * Null safety is ensured:
     * <ul>
     *   <li>model cannot be null or blank</li>
     *   <li>timeout defaults to 60 seconds if null</li>
     * </ul>
     *
     * @param apiKey  the API key for authentication
     * @param model   the model identifier
     * @param baseUrl the base URL for the API endpoint
     * @param timeout the request timeout duration
     * @throws IllegalArgumentException if model is null or blank
     */
    public LlmConfig {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model cannot be null or blank");
        }
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
    }

    /**
     * Creates an LlmConfig with only the required model parameter.
     *
     * @param model the model identifier
     * @return a new LlmConfig instance
     */
    @NonNull
    public static LlmConfig of(@NonNull String model) {
        return new LlmConfig(null, model, null, DEFAULT_TIMEOUT);
    }

    /**
     * Creates an LlmConfig with API key and model.
     *
     * @param apiKey the API key for authentication
     * @param model  the model identifier
     * @return a new LlmConfig instance
     */
    @NonNull
    public static LlmConfig of(@Nullable String apiKey, @NonNull String model) {
        return new LlmConfig(apiKey, model, null, DEFAULT_TIMEOUT);
    }

    /**
     * Creates an LlmConfig with all parameters.
     *
     * @param apiKey  the API key for authentication
     * @param model   the model identifier
     * @param baseUrl the base URL for the API endpoint
     * @param timeout the request timeout duration
     * @return a new LlmConfig instance
     */
    @NonNull
    public static LlmConfig of(
        @Nullable String apiKey,
        @NonNull String model,
        @Nullable String baseUrl,
        @NonNull Duration timeout
    ) {
        return new LlmConfig(apiKey, model, baseUrl, timeout);
    }

    /**
     * Creates a new LlmConfig with a different API key.
     *
     * @param apiKey the new API key
     * @return a new LlmConfig instance
     */
    @NonNull
    public LlmConfig withApiKey(@Nullable String apiKey) {
        return new LlmConfig(apiKey, this.model, this.baseUrl, this.timeout);
    }

    /**
     * Creates a new LlmConfig with a different model.
     *
     * @param model the new model identifier
     * @return a new LlmConfig instance
     */
    @NonNull
    public LlmConfig withModel(@NonNull String model) {
        return new LlmConfig(this.apiKey, model, this.baseUrl, this.timeout);
    }

    /**
     * Creates a new LlmConfig with a different base URL.
     *
     * @param baseUrl the new base URL
     * @return a new LlmConfig instance
     */
    @NonNull
    public LlmConfig withBaseUrl(@Nullable String baseUrl) {
        return new LlmConfig(this.apiKey, this.model, baseUrl, this.timeout);
    }

    /**
     * Creates a new LlmConfig with a different timeout.
     *
     * @param timeout the new timeout duration
     * @return a new LlmConfig instance
     */
    @NonNull
    public LlmConfig withTimeout(@NonNull Duration timeout) {
        return new LlmConfig(this.apiKey, this.model, this.baseUrl, timeout);
    }
}
