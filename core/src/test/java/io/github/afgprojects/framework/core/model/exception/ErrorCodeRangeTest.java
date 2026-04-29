package io.github.afgprojects.framework.core.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ErrorCodeRangeTest {

    @Nested
    @DisplayName("范围检查测试")
    class ContainsTests {

        @Test
        @DisplayName("应正确判断错误码是否在范围内")
        void shouldCheckIfCodeIsInRange() {
            assertThat(ErrorCodeRange.COMMON.contains(10000)).isTrue();
            assertThat(ErrorCodeRange.COMMON.contains(15000)).isTrue();
            assertThat(ErrorCodeRange.COMMON.contains(19999)).isTrue();
            assertThat(ErrorCodeRange.COMMON.contains(20000)).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
            "10000, COMMON",
            "15000, COMMON",
            "20000, AUTH",
            "30000, BUSINESS",
            "90000, SYSTEM"
        })
        @DisplayName("应根据错误码获取正确的范围")
        void shouldGetRangeFromCode(int code, ErrorCodeRange expectedRange) {
            assertThat(ErrorCodeRange.fromCode(code)).isEqualTo(expectedRange);
        }

        @Test
        @DisplayName("未知错误码应返回 SYSTEM 范围")
        void unknownCodeShouldReturnSystem() {
            assertThat(ErrorCodeRange.fromCode(1)).isEqualTo(ErrorCodeRange.SYSTEM);
            assertThat(ErrorCodeRange.fromCode(999999)).isEqualTo(ErrorCodeRange.SYSTEM);
        }
    }

    @Nested
    @DisplayName("属性测试")
    class PropertyTests {

        @Test
        @DisplayName("应返回正确的范围边界")
        void shouldReturnCorrectBoundaries() {
            assertThat(ErrorCodeRange.COMMON.getStart()).isEqualTo(10000);
            assertThat(ErrorCodeRange.COMMON.getEnd()).isEqualTo(19999);
            assertThat(ErrorCodeRange.COMMON.getDescription()).isEqualTo("通用模块");
        }
    }
}
