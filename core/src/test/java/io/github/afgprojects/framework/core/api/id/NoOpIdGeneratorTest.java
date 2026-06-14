package io.github.afgprojects.framework.core.api.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoOpIdGenerator")
class NoOpIdGeneratorTest {

    private final NoOpIdGenerator generator = new NoOpIdGenerator();

    @Nested
    @DisplayName("nextId")
    class NextId {

        @Test
        @DisplayName("should generate monotonically increasing IDs starting from 1")
        void shouldGenerateMonotonicallyIncreasingIdsStartingFrom1() {
            assertThat(generator.nextId()).isEqualTo(1);
            assertThat(generator.nextId()).isEqualTo(2);
            assertThat(generator.nextId()).isEqualTo(3);
        }

        @Test
        @DisplayName("should always generate positive IDs")
        void shouldAlwaysGeneratePositiveIds() {
            assertThat(generator.nextId()).isPositive();
        }
    }

    @Nested
    @DisplayName("nextIdAsString")
    class NextIdAsString {

        @Test
        @DisplayName("should return string representation of nextId")
        void shouldReturnStringRepresentationOfNextId() {
            assertThat(generator.nextIdAsString()).isEqualTo("1");
            assertThat(generator.nextIdAsString()).isEqualTo("2");
        }

        @Test
        @DisplayName("should generate non-empty string IDs")
        void shouldGenerateNonEmptyStringIds() {
            String idStr = generator.nextIdAsString();
            assertThat(idStr).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("getType")
    class GetType {

        @Test
        @DisplayName("should return NONE type")
        void shouldReturnNoneType() {
            assertThat(generator.getType()).isEqualTo(IdGeneratorType.NONE);
        }
    }
}
