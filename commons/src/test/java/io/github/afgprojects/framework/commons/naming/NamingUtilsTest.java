package io.github.afgprojects.framework.commons.naming;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NamingUtils 测试
 */
@DisplayName("NamingUtils 测试")
class NamingUtilsTest {

    @Nested
    @DisplayName("toSnakeCase 方法")
    class ToSnakeCaseTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(NamingUtils.toSnakeCase(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(NamingUtils.toSnakeCase("")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "userName, user_name",
            "isActive, is_active",
            "id, id",
            "URL, u_r_l",
            "deviceId, device_id",
            "lastLoginTime, last_login_time"
        })
        @DisplayName("camelCase → snake_case")
        void shouldConvertCamelCaseToSnakeCase(String input, String expected) {
            assertThat(NamingUtils.toSnakeCase(input)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("toCamelCase 方法")
    class ToCamelCaseTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(NamingUtils.toCamelCase(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(NamingUtils.toCamelCase("")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "user_name, userName",
            "is_active, isActive",
            "id, id",
            "device_id, deviceId",
            "last_login_time, lastLoginTime"
        })
        @DisplayName("snake_case → camelCase")
        void shouldConvertSnakeCaseToCamelCase(String input, String expected) {
            assertThat(NamingUtils.toCamelCase(input)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("capitalize 方法")
    class CapitalizeTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(NamingUtils.capitalize(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(NamingUtils.capitalize("")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "userName, UserName",
            "id, Id",
            "active, Active"
        })
        @DisplayName("首字母大写")
        void shouldCapitalize(String input, String expected) {
            assertThat(NamingUtils.capitalize(input)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("uncapitalize 方法")
    class UncapitalizeTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(NamingUtils.uncapitalize(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(NamingUtils.uncapitalize("")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "UserName, userName",
            "Id, id",
            "Active, active"
        })
        @DisplayName("首字母小写")
        void shouldUncapitalize(String input, String expected) {
            assertThat(NamingUtils.uncapitalize(input)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("双向转换")
    class RoundTripTests {

        @Test
        @DisplayName("camelCase → snake_case → camelCase 应保持一致")
        void shouldBeIdempotent() {
            String original = "userName";
            String snakeCase = NamingUtils.toSnakeCase(original);
            String backToCamel = NamingUtils.toCamelCase(snakeCase);
            assertThat(backToCamel).isEqualTo(original);
        }
    }
}
