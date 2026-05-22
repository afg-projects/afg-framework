package io.github.afgprojects.framework.ai.core.output;

import org.jspecify.annotations.NonNull;

/**
 * Interface for parsing LLM responses into structured Java objects.
 * <p>
 * StructuredOutputParser provides methods to:
 * <ul>
 *   <li>Parse raw LLM response content into a typed Java object</li>
 *   <li>Generate format instructions to guide the LLM's output format</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a parser for a specific type
 * StructuredOutputParser<Person> parser = createParser(Person.class);
 *
 * // Get format instructions to include in the prompt
 * String instructions = parser.getFormatInstructions();
 * String prompt = "Extract person information. " + instructions;
 *
 * // Parse the LLM response
 * LlmResponse response = client.chat(LlmRequest.ofUserMessage(prompt));
 * Person person = parser.parse(response.content());
 * }</pre>
 *
 * @param <T> the type of object to parse
 * @author AFG Projects
 * @since 1.0.0
 */
public interface StructuredOutputParser<T> {

    /**
     * Parses the LLM response content into a typed object.
     * <p>
     * The content should be in the format specified by {@link #getFormatInstructions()}.
     * Implementations should handle common parsing issues like:
     * <ul>
     *   <li>Extra whitespace or newlines</li>
     *   <li>Markdown code blocks (e.g., ```json ... ```)</li>
     *   <li>Partial or malformed content</li>
     * </ul>
     *
     * @param content the raw LLM response content
     * @return the parsed object
     * @throws OutputParsingException if parsing fails
     */
    @NonNull
    T parse(@NonNull String content);

    /**
     * Generates format instructions to include in the prompt.
     * <p>
     * These instructions tell the LLM how to format its response
     * so that it can be correctly parsed by {@link #parse(String)}.
     * <p>
     * The instructions typically include:
     * <ul>
     *   <li>The expected output format (e.g., JSON)</li>
     *   <li>The schema or structure of the expected output</li>
     *   <li>Examples or constraints</li>
     * </ul>
     *
     * @return format instructions to append to the prompt
     */
    @NonNull
    String getFormatInstructions();

    /**
     * Gets the target class that this parser produces.
     *
     * @return the target class
     */
    @NonNull
    Class<T> getTargetClass();
}
