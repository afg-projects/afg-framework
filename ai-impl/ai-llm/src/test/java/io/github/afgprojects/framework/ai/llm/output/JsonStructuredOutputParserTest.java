package io.github.afgprojects.framework.ai.llm.output;

import io.github.afgprojects.framework.ai.core.output.OutputParsingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JsonStructuredOutputParser.
 */
@DisplayName("JsonStructuredOutputParser Tests")
class JsonStructuredOutputParserTest {

    private JsonStructuredOutputParser<Person> parser;

    @BeforeEach
    void setUp() {
        parser = new JsonStructuredOutputParser<>(Person.class);
    }

    @Nested
    @DisplayName("parse() method")
    class ParseMethod {

        @Test
        @DisplayName("should parse valid JSON object")
        void shouldParseValidJsonObject() {
            String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";

            Person person = parser.parse(json);

            assertThat(person.getName()).isEqualTo("John");
            assertThat(person.getAge()).isEqualTo(30);
            assertThat(person.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should parse JSON from markdown code block")
        void shouldParseJsonFromMarkdownCodeBlock() {
            String content = "Here is the result:\n```json\n{\"name\":\"Alice\",\"age\":25,\"email\":\"alice@example.com\"}\n```\nThat's it.";

            Person person = parser.parse(content);

            assertThat(person.getName()).isEqualTo("Alice");
            assertThat(person.getAge()).isEqualTo(25);
            assertThat(person.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("should parse JSON from markdown code block without json label")
        void shouldParseJsonFromMarkdownCodeBlockWithoutJsonLabel() {
            String content = "```\n{\"name\":\"Bob\",\"age\":40,\"email\":\"bob@example.com\"}\n```";

            Person person = parser.parse(content);

            assertThat(person.getName()).isEqualTo("Bob");
            assertThat(person.getAge()).isEqualTo(40);
            assertThat(person.getEmail()).isEqualTo("bob@example.com");
        }

        @Test
        @DisplayName("should parse JSON embedded in text")
        void shouldParseJsonEmbeddedInText() {
            String content = "The person data is: {\"name\":\"Charlie\",\"age\":35,\"email\":\"charlie@example.com\"} end of message.";

            Person person = parser.parse(content);

            assertThat(person.getName()).isEqualTo("Charlie");
            assertThat(person.getAge()).isEqualTo(35);
            assertThat(person.getEmail()).isEqualTo("charlie@example.com");
        }

        @Test
        @DisplayName("should handle whitespace and newlines")
        void shouldHandleWhitespaceAndNewlines() {
            String json = "{\n  \"name\"  :  \"David\"  ,\n  \"age\"  :  28  ,\n  \"email\"  :  \"david@example.com\"\n}";

            Person person = parser.parse(json);

            assertThat(person.getName()).isEqualTo("David");
            assertThat(person.getAge()).isEqualTo(28);
            assertThat(person.getEmail()).isEqualTo("david@example.com");
        }

        @Test
        @DisplayName("should throw exception for invalid JSON")
        void shouldThrowExceptionForInvalidJson() {
            String invalidJson = "This is not JSON at all";

            assertThatThrownBy(() -> parser.parse(invalidJson))
                    .isInstanceOf(OutputParsingException.class)
                    .hasMessageContaining("No valid JSON found");
        }

        @Test
        @DisplayName("should throw exception for malformed JSON")
        void shouldThrowExceptionForMalformedJson() {
            String malformedJson = "{\"name\":\"John\",\"age\":30,}"; // trailing comma

            assertThatThrownBy(() -> parser.parse(malformedJson))
                    .isInstanceOf(OutputParsingException.class)
                    .hasMessageContaining("Invalid JSON format");
        }

        @Test
        @DisplayName("should throw exception for empty content")
        void shouldThrowExceptionForEmptyContent() {
            assertThatThrownBy(() -> parser.parse(""))
                    .isInstanceOf(OutputParsingException.class)
                    .hasMessageContaining("No valid JSON found");
        }

        @Test
        @DisplayName("should throw exception for null content")
        void shouldThrowExceptionForNullContent() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should handle missing optional fields")
        void shouldHandleMissingOptionalFields() {
            String json = "{\"name\":\"Eve\"}"; // age and email missing

            Person person = parser.parse(json);

            assertThat(person.getName()).isEqualTo("Eve");
            assertThat(person.getAge()).isNull();
            assertThat(person.getEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("getFormatInstructions() method")
    class GetFormatInstructionsMethod {

        @Test
        @DisplayName("should return format instructions")
        void shouldReturnFormatInstructions() {
            String instructions = parser.getFormatInstructions();

            assertThat(instructions).isNotEmpty();
            assertThat(instructions).contains("JSON");
            assertThat(instructions).contains("Person");
        }

        @Test
        @DisplayName("should include important guidelines")
        void shouldIncludeImportantGuidelines() {
            String instructions = parser.getFormatInstructions();

            assertThat(instructions).contains("Return ONLY the JSON object");
            assertThat(instructions).contains("proper JSON syntax");
        }
    }

    @Nested
    @DisplayName("getTargetClass() method")
    class GetTargetClassMethod {

        @Test
        @DisplayName("should return target class")
        void shouldReturnTargetClass() {
            Class<Person> targetClass = parser.getTargetClass();

            assertThat(targetClass).isEqualTo(Person.class);
        }
    }

    // Test data class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Person {
        private String name;
        private Integer age;
        private String email;
    }
}