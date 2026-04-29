package io.github.afgprojects.framework.data.jdbc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * ORDER BY 注入安全测试
 */
@DisplayName("ORDER BY 注入安全测试")
@Tag("security")
class OrderBySecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("列名注入测试")
    class ColumnInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.OrderByPayloads#columnInjection")
        @DisplayName("应防止 ORDER BY 列名注入")
        void shouldPreventColumnInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            assertDatabaseIntegrity();
            assertDataUnchanged();
        }
    }

    @Nested
    @DisplayName("条件注入测试")
    class ConditionInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.OrderByPayloads#conditionInjection")
        @DisplayName("应防止 ORDER BY 条件注入")
        void shouldPreventConditionInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("时间盲注测试")
    class TimeBasedTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.OrderByPayloads#timeBased")
        @DisplayName("应检测 ORDER BY 时间盲注")
        void shouldDetectTimeBasedInjection(String payload) {
            long start = System.currentTimeMillis();
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            long duration = System.currentTimeMillis() - start;

            assertThat(duration).isLessThan(2000);
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("错误注入测试")
    class ErrorBasedTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.OrderByPayloads#errorBased")
        @DisplayName("应防止 ORDER BY 错误注入")
        void shouldPreventErrorBasedInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            assertDatabaseIntegrity();
        }
    }
}
