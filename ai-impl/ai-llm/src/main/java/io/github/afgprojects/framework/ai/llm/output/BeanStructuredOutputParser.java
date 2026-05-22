package io.github.afgprojects.framework.ai.llm.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.output.OutputParsingException;
import io.github.afgprojects.framework.ai.core.output.StructuredOutputParser;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean-based structured output parser using Spring AI's BeanOutputConverter.
 * <p>
 * This parser leverages Spring AI's {@link BeanOutputConverter} to:
 * <ul>
 *   <li>Generate accurate JSON schemas from Java classes</li>
 *   <li>Parse JSON responses into Java objects</li>
 * </ul>
 *
 * <p>Compared to {@link JsonStructuredOutputParser}, this parser provides:
 * <ul>
 *   <li>More accurate JSON schema generation</li>
 *   <li>Better handling of complex types (generics, nested objects)</li>
 *   <li>Integration with Spring AI's ecosystem</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a parser
 * BeanStructuredOutputParser<Person> parser =
 *     new BeanStructuredOutputParser<>(Person.class);
 *
 * // Get format instructions with JSON schema
 * String instructions = parser.getFormatInstructions();
 *
 * // Parse response
 * Person person = parser.parse(response.content());
 * }</pre>
 *
 * @param <T> the type of object to parse
 * @author AFG Projects
 * @since 1.0.0
 */
public class BeanStructuredOutputParser<T> implements StructuredOutputParser<T> {

    private static final Logger log = LoggerFactory.getLogger(BeanStructuredOutputParser.class);

    private static final Pattern MARKDOWN_CODE_BLOCK =
            Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```", Pattern.CASE_INSENSITIVE);

    private final Class<T> targetClass;
    private final BeanOutputConverter<T> beanOutputConverter;
    private final ObjectMapper objectMapper;
    private final String formatInstructions;

    /**
     * Creates a new BeanStructuredOutputParser.
     *
     * @param targetClass the class to parse into
     */
    public BeanStructuredOutputParser(@NonNull Class<T> targetClass) {
        this.targetClass = targetClass;
        this.beanOutputConverter = new BeanOutputConverter<>(targetClass);
        this.objectMapper = new ObjectMapper();
        this.formatInstructions = generateFormatInstructions();
    }

    @Override
    @NonNull
    public T parse(@NonNull String content) {
        log.debug("Parsing content for type: {}", targetClass.getSimpleName());

        // Clean and extract JSON
        String json = extractJson(content);

        if (json == null || json.isBlank()) {
            throw new OutputParsingException(
                    "No valid JSON found in response. Content: " + truncate(content, 200));
        }

        try {
            // Use BeanOutputConverter for parsing
            return beanOutputConverter.convert(json);
        } catch (Exception e) {
            // Fallback to direct ObjectMapper parsing if BeanOutputConverter fails
            log.debug("BeanOutputConverter failed, falling back to ObjectMapper: {}", e.getMessage());
            try {
                return objectMapper.readValue(json, targetClass);
            } catch (Exception ex) {
                OutputParsingException ope = new OutputParsingException(
                        "Failed to parse JSON to " + targetClass.getSimpleName() + ": " + ex.getMessage() +
                        ". JSON: " + truncate(json, 200), ex);
                ope.addSuppressed(e);
                throw ope;
            }
        }
    }

    @Override
    @NonNull
    public String getFormatInstructions() {
        return formatInstructions;
    }

    @Override
    @NonNull
    public Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Gets the JSON schema for the target class.
     * <p>
     * This can be useful for documentation or custom prompt engineering.
     *
     * @return the JSON schema as a string
     */
    @NonNull
    public String getJsonSchema() {
        return beanOutputConverter.getFormat();
    }

    /**
     * Extracts JSON from the content.
     * <p>
     * Handles:
     * <ul>
     *   <li>Markdown code blocks (```json ... ```)</li>
     *   <li>Raw JSON objects or arrays</li>
     *   <li>JSON embedded in text</li>
     * </ul>
     *
     * @param content the raw content
     * @return the extracted JSON string, or null if not found
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String trimmed = content.trim();

        // Try to extract from markdown code block first
        Matcher markdownMatcher = MARKDOWN_CODE_BLOCK.matcher(trimmed);
        if (markdownMatcher.find()) {
            String extracted = markdownMatcher.group(1).trim();
            if (!extracted.isBlank()) {
                return extracted;
            }
        }

        // Check if the entire content looks like JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        // Try to find JSON object in the content
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        // Try to find JSON array
        start = trimmed.indexOf('[');
        end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return null;
    }

    /**
     * Generates format instructions for the target class.
     */
    private String generateFormatInstructions() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nYour response must be a valid JSON object that matches the following schema:\n\n");
        sb.append("```json\n");
        sb.append(beanOutputConverter.getFormat());
        sb.append("\n```\n\n");
        sb.append("Important:\n");
        sb.append("- Return ONLY the JSON object, no additional text or explanation\n");
        sb.append("- Ensure all required fields are present\n");
        sb.append("- Use proper JSON syntax with double quotes\n");
        sb.append("- Use null for missing optional fields\n");

        return sb.toString();
    }

    /**
     * Truncates a string for error messages.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
