package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.jdbc.security.payload.GroupByPayloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * GROUP BY 注入安全测试
 */
@DisplayName("GROUP BY 注入安全测试")
@Tag("security")
class GroupBySecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("GROUP BY 注入测试")
    class GroupByInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.GroupByPayloads#groupByInjection")
        @DisplayName("应防止 GROUP BY 注入")
        void shouldPreventGroupByInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            assertDatabaseIntegrity();
            assertDataUnchanged();
        }
    }

    @Nested
    @DisplayName("HAVING 注入测试")
    class HavingInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.GroupByPayloads#havingInjection")
        @DisplayName("应防止 HAVING 注入")
        void shouldPreventHavingInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll()
            );
            assertDatabaseIntegrity();
        }
    }
}
