package io.github.afgprojects.framework.core.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JacksonUtils")
class JacksonUtilsTest {

    @BeforeEach
    @AfterEach
    void resetObjectMapper() {
        JacksonUtils.reset();
    }

    @Nested
    @DisplayName("toJson")
    class ToJson {

        @Test
        @DisplayName("should serialize simple object to JSON")
        void shouldSerializeSimpleObjectToJson() {
            TestDto dto = new TestDto("hello", 42);
            String json = JacksonUtils.toJson(dto);

            assertThat(json).contains("\"name\":\"hello\"");
            assertThat(json).contains("\"age\":42");
        }

        @Test
        @DisplayName("should serialize null to JSON")
        void shouldSerializeNullToJson() {
            String json = JacksonUtils.toJson(null);

            assertThat(json).isEqualTo("null");
        }

        @Test
        @DisplayName("should serialize list to JSON")
        void shouldSerializeListToJson() {
            String json = JacksonUtils.toJson(List.of("a", "b", "c"));

            assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("should serialize map to JSON")
        void shouldSerializeMapToJson() {
            String json = JacksonUtils.toJson(Map.of("key", "value"));

            assertThat(json).contains("\"key\":\"value\"");
        }

        @Test
        @DisplayName("should ignore null fields by default")
        void shouldIgnoreNullFieldsByDefault() {
            TestDto dto = new TestDto(null, 42);
            String json = JacksonUtils.toJson(dto);

            assertThat(json).doesNotContain("name");
            assertThat(json).contains("\"age\":42");
        }

        @Test
        @DisplayName("should throw JsonProcessingException on invalid object")
        void shouldThrowJsonProcessingException_onInvalidObject() {
            // An object that causes Jackson to fail (e.g., self-referencing)
            SelfRefObject obj = new SelfRefObject();
            obj.self = obj;

            assertThatThrownBy(() -> JacksonUtils.toJson(obj))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("should parse JSON to object")
        void shouldParseJsonToObject() {
            String json = "{\"name\":\"hello\",\"age\":42}";
            TestDto dto = JacksonUtils.parse(json, TestDto.class);

            assertThat(dto.name).isEqualTo("hello");
            assertThat(dto.age).isEqualTo(42);
        }

        @Test
        @DisplayName("should parse JSON with unknown properties")
        void shouldParseJsonWithUnknownProperties() {
            String json = "{\"name\":\"hello\",\"age\":42,\"unknownField\":\"value\"}";
            TestDto dto = JacksonUtils.parse(json, TestDto.class);

            assertThat(dto.name).isEqualTo("hello");
            assertThat(dto.age).isEqualTo(42);
        }

        @Test
        @DisplayName("should throw JsonProcessingException on invalid JSON")
        void shouldThrowJsonProcessingException_onInvalidJson() {
            assertThatThrownBy(() -> JacksonUtils.parse("not json", TestDto.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("should throw JsonProcessingException on type mismatch")
        void shouldThrowJsonProcessingException_onTypeMismatch() {
            String json = "{\"name\":\"hello\",\"age\":\"not-a-number\"}";
            assertThatThrownBy(() -> JacksonUtils.parse(json, TestDto.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("parseList")
    class ParseList {

        @Test
        @DisplayName("should parse JSON array to list")
        void shouldParseJsonArrayToList() {
            String json = "[\"a\",\"b\",\"c\"]";
            List<String> list = JacksonUtils.parseList(json, String.class);

            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("should parse JSON array of objects to list")
        void shouldParseJsonArrayOfObjectsToList() {
            String json = "[{\"name\":\"hello\",\"age\":42}]";
            List<TestDto> list = JacksonUtils.parseList(json, TestDto.class);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).name).isEqualTo("hello");
        }

        @Test
        @DisplayName("should throw JsonProcessingException on invalid JSON")
        void shouldThrowJsonProcessingException_onInvalidJson() {
            assertThatThrownBy(() -> JacksonUtils.parseList("not json", String.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("parseMap")
    class ParseMap {

        @Test
        @DisplayName("should parse JSON to map")
        void shouldParseJsonToMap() {
            String json = "{\"key\":\"value\",\"number\":42}";
            Map<String, Object> map = JacksonUtils.parseMap(json);

            assertThat(map.get("key")).isEqualTo("value");
            assertThat(map.get("number")).isEqualTo(42);
        }

        @Test
        @DisplayName("should throw JsonProcessingException on invalid JSON")
        void shouldThrowJsonProcessingException_onInvalidJson() {
            assertThatThrownBy(() -> JacksonUtils.parseMap("not json"))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("toMap")
    class ToMap {

        @Test
        @DisplayName("should convert object to map")
        void shouldConvertObjectToMap() {
            TestDto dto = new TestDto("hello", 42);
            Map<String, Object> map = JacksonUtils.toMap(dto);

            assertThat(map.get("name")).isEqualTo("hello");
            assertThat(map.get("age")).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNull_forNullInput() {
            // Jackson convertValue(null, ...) returns null with NON_NULL inclusion
            assertThat(JacksonUtils.toMap(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toObject")
    class ToObject {

        @Test
        @DisplayName("should convert map to object")
        void shouldConvertMapToObject() {
            Map<String, Object> map = Map.of("name", "hello", "age", 42);
            TestDto dto = JacksonUtils.toObject(map, TestDto.class);

            assertThat(dto.name).isEqualTo("hello");
            assertThat(dto.age).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("deepCopy")
    class DeepCopy {

        @Test
        @DisplayName("should create independent copy")
        void shouldCreateIndependentCopy() {
            TestDto original = new TestDto("hello", 42);
            TestDto copy = JacksonUtils.deepCopy(original, TestDto.class);

            assertThat(copy.name).isEqualTo("hello");
            assertThat(copy.age).isEqualTo(42);

            // Modify copy and verify independence
            copy.name = "modified";
            assertThat(original.name).isEqualTo("hello");
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNull_forNullInput() {
            assertThat(JacksonUtils.deepCopy(null, TestDto.class)).isNull();
        }
    }

    @Nested
    @DisplayName("setObjectMapper")
    class SetObjectMapper {

        @Test
        @DisplayName("should allow setting ObjectMapper once")
        void shouldAllowSettingObjectMapperOnce() {
            com.fasterxml.jackson.databind.ObjectMapper customMapper =
                    JacksonMapper.builder().ignoreNull(false).build();
            JacksonUtils.setObjectMapper(customMapper);

            assertThat(JacksonUtils.getObjectMapper()).isSameAs(customMapper);
        }

        @Test
        @DisplayName("should throw when setting different ObjectMapper")
        void shouldThrow_whenSettingDifferentObjectMapper() {
            com.fasterxml.jackson.databind.ObjectMapper first =
                    JacksonMapper.builder().build();
            com.fasterxml.jackson.databind.ObjectMapper second =
                    JacksonMapper.builder().ignoreNull(false).build();

            JacksonUtils.setObjectMapper(first);

            assertThatThrownBy(() -> JacksonUtils.setObjectMapper(second))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ObjectMapper has already been initialized");
        }

        @Test
        @DisplayName("should be idempotent for same instance")
        void shouldBeIdempotent_forSameInstance() {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    JacksonMapper.builder().build();
            JacksonUtils.setObjectMapper(mapper);
            JacksonUtils.setObjectMapper(mapper); // Should not throw

            assertThat(JacksonUtils.getObjectMapper()).isSameAs(mapper);
        }
    }

    // Test DTO
    public static class TestDto {
        public String name;
        public int age;

        public TestDto() {}

        public TestDto(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    // Self-referencing object for serialization failure test
    public static class SelfRefObject {
        public SelfRefObject self;
    }
}