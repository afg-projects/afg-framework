package io.github.afgprojects.framework.ai.llm.output;

import io.github.afgprojects.framework.ai.core.output.OutputParsingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for BeanStructuredOutputParser.
 */
@DisplayName("BeanStructuredOutputParser Tests")
class BeanStructuredOutputParserTest {

    private BeanStructuredOutputParser<Address> parser;

    @BeforeEach
    void setUp() {
        parser = new BeanStructuredOutputParser<>(Address.class);
    }

    @Nested
    @DisplayName("parse() method")
    class ParseMethod {

        @Test
        @DisplayName("should parse valid JSON object")
        void shouldParseValidJsonObject() {
            String json = "{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"zipCode\":\"12345\"}";

            Address address = parser.parse(json);

            assertThat(address.getStreet()).isEqualTo("123 Main St");
            assertThat(address.getCity()).isEqualTo("Springfield");
            assertThat(address.getZipCode()).isEqualTo("12345");
        }

        @Test
        @DisplayName("should parse JSON from markdown code block")
        void shouldParseJsonFromMarkdownCodeBlock() {
            String content = "```json\n{\"street\":\"456 Oak Ave\",\"city\":\"Boston\",\"zipCode\":\"02101\"}\n```";

            Address address = parser.parse(content);

            assertThat(address.getStreet()).isEqualTo("456 Oak Ave");
            assertThat(address.getCity()).isEqualTo("Boston");
            assertThat(address.getZipCode()).isEqualTo("02101");
        }

        @Test
        @DisplayName("should handle missing optional fields")
        void shouldHandleMissingOptionalFields() {
            String json = "{\"street\":\"789 Pine Rd\"}";

            Address address = parser.parse(json);

            assertThat(address.getStreet()).isEqualTo("789 Pine Rd");
            assertThat(address.getCity()).isNull();
            assertThat(address.getZipCode()).isNull();
        }

        @Test
        @DisplayName("should throw exception for invalid JSON")
        void shouldThrowExceptionForInvalidJson() {
            String invalidJson = "Not JSON at all";

            assertThatThrownBy(() -> parser.parse(invalidJson))
                    .isInstanceOf(OutputParsingException.class);
        }
    }

    @Nested
    @DisplayName("getFormatInstructions() method")
    class GetFormatInstructionsMethod {

        @Test
        @DisplayName("should return format instructions with JSON schema")
        void shouldReturnFormatInstructionsWithJsonSchema() {
            String instructions = parser.getFormatInstructions();

            assertThat(instructions).isNotEmpty();
            assertThat(instructions).contains("JSON");
            assertThat(instructions).contains("schema");
        }

        @Test
        @DisplayName("should include JSON schema")
        void shouldIncludeJsonSchema() {
            String instructions = parser.getFormatInstructions();

            // Should contain schema elements
            assertThat(instructions).containsAnyOf("type", "properties", "required");
        }
    }

    @Nested
    @DisplayName("getJsonSchema() method")
    class GetJsonSchemaMethod {

        @Test
        @DisplayName("should return JSON schema")
        void shouldReturnJsonSchema() {
            String schema = parser.getJsonSchema();

            assertThat(schema).isNotEmpty();
            assertThat(schema).containsAnyOf("type", "properties", "Address");
        }
    }

    @Nested
    @DisplayName("getTargetClass() method")
    class GetTargetClassMethod {

        @Test
        @DisplayName("should return target class")
        void shouldReturnTargetClass() {
            Class<Address> targetClass = parser.getTargetClass();

            assertThat(targetClass).isEqualTo(Address.class);
        }
    }

    // Test data class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Address {
        private String street;
        private String city;
        private String zipCode;
    }
}