package io.github.afgprojects.framework.core.api.id;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UuidIdGenerator")
class UuidIdGeneratorTest {

    private final UuidIdGenerator generator = new UuidIdGenerator();

    @Nested
    @DisplayName("nextId")
    class NextId {

        @Test
        @DisplayName("should throw UnsupportedOperationException for numeric ID")
        void shouldThrowUnsupportedOperationExceptionForNumericId() {
            assertThatThrownBy(() -> generator.nextId())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("UUID generator does not support numeric ID");
        }
    }

    @Nested
    @DisplayName("nextIdAsString")
    class NextIdAsString {

        @Test
        @DisplayName("should generate 32-character hex string without hyphens")
        void shouldGenerate32CharacterHexStringWithoutHyphens() {
            String id = generator.nextIdAsString();

            assertThat(id).hasSize(32);
            assertThat(id).matches("[0-9a-f]{32}");
        }

        @Test
        @DisplayName("should generate unique IDs")
        void shouldGenerateUniqueIds() {
            String id1 = generator.nextIdAsString();
            String id2 = generator.nextIdAsString();

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("getType")
    class GetType {

        @Test
        @DisplayName("should return UUID type")
        void shouldReturnUuidType() {
            assertThat(generator.getType()).isEqualTo(IdGeneratorType.UUID);
        }
    }
}
