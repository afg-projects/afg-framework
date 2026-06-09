package io.github.afgprojects.framework.commons.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCategory 测试
 */
@DisplayName("ErrorCategory 测试")
class ErrorCategoryTest {

    @Nested
    @DisplayName("getPrefix() 方法")
    class GetPrefixTests {

        @ParameterizedTest
        @CsvSource({
                "BUSINESS, B",
                "SYSTEM, S",
                "NETWORK, N",
                "SECURITY, A"
        })
        @DisplayName("每个枚举值应有正确的 prefix")
        void shouldHaveCorrectPrefix(ErrorCategory category, String expectedPrefix) {
            assertThat(category.getPrefix()).isEqualTo(expectedPrefix);
        }
    }

    @Nested
    @DisplayName("枚举值完整性")
    class EnumCompletenessTests {

        @Test
        @DisplayName("应包含 4 个枚举值")
        void shouldContainFourValues() {
            assertThat(ErrorCategory.values()).hasSize(4);
            assertThat(ErrorCategory.values())
                    .containsExactly(ErrorCategory.BUSINESS, ErrorCategory.SYSTEM, ErrorCategory.NETWORK, ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("valueOf 应正确解析枚举名称")
        void shouldParseEnumByName() {
            assertThat(ErrorCategory.valueOf("BUSINESS")).isEqualTo(ErrorCategory.BUSINESS);
            assertThat(ErrorCategory.valueOf("SYSTEM")).isEqualTo(ErrorCategory.SYSTEM);
            assertThat(ErrorCategory.valueOf("NETWORK")).isEqualTo(ErrorCategory.NETWORK);
            assertThat(ErrorCategory.valueOf("SECURITY")).isEqualTo(ErrorCategory.SECURITY);
        }
    }
}
