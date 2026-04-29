package io.github.afgprojects.framework.data.jdbc.security;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * LIKE 注入安全测试
 */
@DisplayName("LIKE 注入安全测试")
@Tag("security")
class LikeSecurityTest extends SecurityTestBase {

    @Nested
    @DisplayName("通配符注入测试")
    class WildcardInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.LikePayloads#wildcardInjection")
        @DisplayName("应防止 LIKE 通配符注入")
        void shouldPreventWildcardInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll(Conditions.like("email", payload))
            );
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("UNION 注入测试")
    class UnionInjectionTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.LikePayloads#unionInjection")
        @DisplayName("应防止 LIKE UNION 注入")
        void shouldPreventUnionInjection(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll(Conditions.like("name", payload))
            );
            assertDatabaseIntegrity();
            assertDataUnchanged();
        }
    }

    @Nested
    @DisplayName("转义绕过测试")
    class EscapeBypassTests {

        @ParameterizedTest(name = "[{index}] Payload: {0}")
        @MethodSource("io.github.afgprojects.framework.data.jdbc.security.payload.LikePayloads#escapeBypass")
        @DisplayName("应防止 LIKE 转义绕过")
        void shouldPreventEscapeBypass(String payload) {
            assertThatNoException().isThrownBy(() ->
                userProxy.findAll(Conditions.like("email", payload))
            );
            assertDatabaseIntegrity();
        }
    }

    @Nested
    @DisplayName("正常 LIKE 查询测试")
    class NormalLikeTests {

        @org.junit.jupiter.api.Test
        @DisplayName("正常 LIKE 查询应返回正确结果")
        void shouldReturnCorrectResultsForNormalLike() {
            var results = userProxy.findAll(Conditions.like("email", "test"));
            assertThat(results).hasSize(3);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("LIKE 查询带通配符应正常工作")
        void shouldWorkWithWildcards() {
            var results = userProxy.findAll(Conditions.like("name", "a%"));
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("alice");
        }
    }
}
