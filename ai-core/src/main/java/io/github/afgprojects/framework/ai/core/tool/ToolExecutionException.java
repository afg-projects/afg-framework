package io.github.afgprojects.framework.ai.core.tool;

/**
 * Exception thrown when tool execution fails.
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class ToolExecutionException extends RuntimeException {

    /**
     * Creates a new ToolExecutionException with a message.
     *
     * @param message the error message
     */
    public ToolExecutionException(String message) {
        super(message);
    }

    /**
     * Creates a new ToolExecutionException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
