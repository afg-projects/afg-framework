package io.github.afgprojects.framework.ai.core.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for StructuredOutputParser interface.
 */
@DisplayName("StructuredOutputParser Interface Tests")
class StructuredOutputParserTest {

    @Test
    @DisplayName("should define required methods")
    void shouldDefineRequiredMethods() {
        // Create a simple test implementation
        StructuredOutputParser<String> parser = new TestParser();

        assertThat(parser.getTargetClass()).isEqualTo(String.class);
        assertThat(parser.getFormatInstructions()).isNotEmpty();

        String result = parser.parse("test");
        assertThat(result).isEqualTo("test");
    }

    @Test
    @DisplayName("parse should throw OutputParsingException for invalid input")
    void parseShouldThrowExceptionForInvalidInput() {
        StructuredOutputParser<String> parser = new FailingParser();

        assertThatThrownBy(() -> parser.parse("invalid"))
                .isInstanceOf(OutputParsingException.class)
                .hasMessage("Parsing failed");
    }

    // Test implementation
    static class TestParser implements StructuredOutputParser<String> {
        @Override
        public String parse(String content) {
            return content;
        }

        @Override
        public String getFormatInstructions() {
            return "Return a string value.";
        }

        @Override
        public Class<String> getTargetClass() {
            return String.class;
        }
    }

    // Failing parser for exception testing
    static class FailingParser implements StructuredOutputParser<String> {
        @Override
        public String parse(String content) {
            throw new OutputParsingException("Parsing failed");
        }

        @Override
        public String getFormatInstructions() {
            return "This parser always fails.";
        }

        @Override
        public Class<String> getTargetClass() {
            return String.class;
        }
    }
}
