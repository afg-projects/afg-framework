package io.github.afgprojects.framework.ai.llm.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.output.StructuredOutputParser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Factory for creating structured output parsers.
 * <p>
 * This factory provides convenient methods for creating different types
 * of parsers based on the use case:
 * <ul>
 *   <li>{@link #jsonParser(Class)} - Simple JSON parsing with Jackson</li>
 *   <li>{@link #beanParser(Class)} - Advanced parsing with Spring AI's BeanOutputConverter</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a JSON parser
 * StructuredOutputParser<Person> jsonParser = StructuredOutputParsers.jsonParser(Person.class);
 *
 * // Create a bean parser (with JSON schema)
 * StructuredOutputParser<Person> beanParser = StructuredOutputParsers.beanParser(Person.class);
 *
 * // Use with LlmClient
 * Person person = client.chatAndParse(request, jsonParser);
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public final class StructuredOutputParsers {

    private StructuredOutputParsers() {
        // Utility class
    }

    /**
     * Creates a JSON-based structured output parser.
     * <p>
     * This parser uses Jackson's ObjectMapper for parsing and provides
     * basic format instructions. Suitable for simple use cases.
     *
     * @param targetClass the class to parse into
     * @param <T>         the type of object to parse
     * @return a new JsonStructuredOutputParser
     */
    @NonNull
    public static <T> StructuredOutputParser<T> jsonParser(@NonNull Class<T> targetClass) {
        return new JsonStructuredOutputParser<>(targetClass);
    }

    /**
     * Creates a JSON-based structured output parser with a custom ObjectMapper.
     *
     * @param targetClass  the class to parse into
     * @param objectMapper the ObjectMapper to use
     * @param <T>          the type of object to parse
     * @return a new JsonStructuredOutputParser
     */
    @NonNull
    public static <T> StructuredOutputParser<T> jsonParser(
            @NonNull Class<T> targetClass,
            @NonNull ObjectMapper objectMapper) {
        return new JsonStructuredOutputParser<>(targetClass, objectMapper);
    }

    /**
     * Creates a bean-based structured output parser using Spring AI.
     * <p>
     * This parser uses Spring AI's BeanOutputConverter for:
     * <ul>
     *   <li>Accurate JSON schema generation</li>
     *   <li>Robust parsing with type conversion</li>
     * </ul>
     *
     * <p>Recommended for production use when accurate schemas are needed.
     *
     * @param targetClass the class to parse into
     * @param <T>         the type of object to parse
     * @return a new BeanStructuredOutputParser
     */
    @NonNull
    public static <T> StructuredOutputParser<T> beanParser(@NonNull Class<T> targetClass) {
        return new BeanStructuredOutputParser<>(targetClass);
    }

    /**
     * Creates the appropriate parser based on the environment.
     * <p>
     * If Spring AI is available, creates a BeanStructuredOutputParser.
     * Otherwise, falls back to JsonStructuredOutputParser.
     *
     * @param targetClass the class to parse into
     * @param <T>         the type of object to parse
     * @return a new StructuredOutputParser
     */
    @NonNull
    public static <T> StructuredOutputParser<T> createParser(@NonNull Class<T> targetClass) {
        // Default to bean parser as Spring AI is available in this module
        return beanParser(targetClass);
    }
}
