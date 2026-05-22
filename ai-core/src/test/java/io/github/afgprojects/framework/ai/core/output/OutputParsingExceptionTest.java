package io.github.afgprojects.framework.ai.core.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OutputParsingException.
 */
@DisplayName("OutputParsingException Tests")
class OutputParsingExceptionTest {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateExceptionWithMessage() {
        OutputParsingException ex = new OutputParsingException("Test message");

        assertThat(ex.getMessage()).isEqualTo("Test message");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Cause");
        OutputParsingException ex = new OutputParsingException("Test message", cause);

        assertThat(ex.getMessage()).isEqualTo("Test message");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should create exception with cause")
    void shouldCreateExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Cause");
        OutputParsingException ex = new OutputParsingException(cause);

        assertThat(ex.getMessage()).contains("Cause");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
