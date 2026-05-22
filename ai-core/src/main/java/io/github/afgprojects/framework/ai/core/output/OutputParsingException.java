package io.github.afgprojects.framework.ai.core.output;

import io.github.afgprojects.framework.ai.core.exception.AiException;
import org.jspecify.annotations.NonNull;

/**
 * Exception thrown when structured output parsing fails.
 * <p>
 * This exception is thrown by {@link StructuredOutputParser#parse(String)}
 * when the LLM response cannot be parsed into the expected format.
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class OutputParsingException extends AiException {

    private static final String ERROR_CODE = "OUTPUT_PARSING_FAILED";

    /**
     * Creates a new OutputParsingException with a message.
     *
     * @param message the error message
     */
    public OutputParsingException(@NonNull String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Creates a new OutputParsingException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public OutputParsingException(@NonNull String message, @NonNull Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Creates a new OutputParsingException with a cause.
     *
     * @param cause the underlying cause
     */
    public OutputParsingException(@NonNull Throwable cause) {
        super(cause.getMessage() != null ? cause.getMessage() : "Output parsing failed", ERROR_CODE, cause);
    }
}
