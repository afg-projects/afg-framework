package io.github.afgprojects.framework.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonMapper")
class JacksonMapperTest {

    @Nested
    @DisplayName("builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should create mapper with default settings")
        void shouldCreateMapperWithDefaultSettings() {
            ObjectMapper mapper = JacksonMapper.builder().build();

            assertThat(mapper).isNotNull();
        }
    }

    @Nested
    @DisplayName("builder configuration")
    class BuilderConfiguration {

        @Test
        @DisplayName("should create mapper with ignoreNull=false")
        void shouldCreateMapperWithIgnoreNullFalse() {
            ObjectMapper mapper = JacksonMapper.builder().ignoreNull(false).build();

            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("should create mapper with ignoreUnknownProperties=true")
        void shouldCreateMapperWithIgnoreUnknownPropertiesTrue() {
            ObjectMapper mapper = JacksonMapper.builder().ignoreUnknownProperties(true).build();

            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("should create mapper with dateFormat")
        void shouldCreateMapperWithDateFormat() {
            ObjectMapper mapper = JacksonMapper.builder()
                    .dateFormat("yyyy-MM-dd HH:mm:ss")
                    .build();

            assertThat(mapper).isNotNull();
        }

        @Test
        @DisplayName("should create mapper with snake_case naming strategy")
        void shouldCreateMapperWithSnakeCaseNamingStrategy() {
            ObjectMapper mapper = JacksonMapper.builder()
                    .namingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .build();

            assertThat(mapper).isNotNull();
        }
    }

    @Nested
    @DisplayName("builder produces different instances")
    class BuilderProducesDifferentInstances {

        @Test
        @DisplayName("should create different ObjectMapper instances")
        void shouldCreateDifferentObjectMapperInstances() {
            ObjectMapper mapper1 = JacksonMapper.builder().build();
            ObjectMapper mapper2 = JacksonMapper.builder().build();

            assertThat(mapper1).isNotSameAs(mapper2);
        }
    }
}