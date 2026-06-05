package io.github.afgprojects.framework.security.core.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IpRestrictionRule 测试
 */
@DisplayName("IpRestrictionRule 测试")
class IpRestrictionRuleTest {

    @Nested
    @DisplayName("matches 方法")
    class MatchesTests {

        @Test
        @DisplayName("精确匹配应返回 true")
        void shouldMatchExactIp() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "Test rule"
            );

            assertThat(rule.matches("192.168.1.100")).isTrue();
            assertThat(rule.matches("192.168.1.101")).isFalse();
        }

        @Test
        @DisplayName("单段通配符应匹配该段所有 IP")
        void shouldMatchSingleWildcard() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "Test rule"
            );

            assertThat(rule.matches("192.168.1.0")).isTrue();
            assertThat(rule.matches("192.168.1.100")).isTrue();
            assertThat(rule.matches("192.168.1.255")).isTrue();
            assertThat(rule.matches("192.168.2.100")).isFalse();
        }

        @Test
        @DisplayName("多段通配符应匹配多段")
        void shouldMatchMultipleWildcards() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "192.168.*.*", "Test rule"
            );

            assertThat(rule.matches("192.168.0.0")).isTrue();
            assertThat(rule.matches("192.168.1.100")).isTrue();
            assertThat(rule.matches("192.168.255.255")).isTrue();
            assertThat(rule.matches("192.169.1.100")).isFalse();
        }

        @Test
        @DisplayName("全通配符 * 应匹配单个数字段")
        void shouldMatchSingleNumberSegmentWithAsterisk() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "*", "Test rule"
            );

            assertThat(rule.matches("123")).isTrue();
            assertThat(rule.matches("0")).isTrue();
        }

        @Test
        @DisplayName("null ipPattern 应返回 false")
        void shouldReturnFalseWhenPatternIsNull() {
            IpRestrictionRule rule = new IpRestrictionRule();
            rule.setIpPattern(null);

            assertThat(rule.matches("192.168.1.100")).isFalse();
        }

        @Test
        @DisplayName("null ip 参数应返回 false")
        void shouldReturnFalseWhenIpIsNull() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "Test rule"
            );

            assertThat(rule.matches(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Getter 和 Setter")
    class GetterSetterTests {

        @Test
        @DisplayName("应正确设置和获取 type")
        void shouldSetAndGetType() {
            IpRestrictionRule rule = new IpRestrictionRule();
            rule.setType(IpRestrictionRule.Type.BLACKLIST);

            assertThat(rule.getType()).isEqualTo(IpRestrictionRule.Type.BLACKLIST);
        }

        @Test
        @DisplayName("应正确设置和获取 ipPattern")
        void shouldSetAndGetIpPattern() {
            IpRestrictionRule rule = new IpRestrictionRule();
            rule.setIpPattern("10.0.0.*");

            assertThat(rule.getIpPattern()).isEqualTo("10.0.0.*");
        }

        @Test
        @DisplayName("应正确设置和获取 description")
        void shouldSetAndGetDescription() {
            IpRestrictionRule rule = new IpRestrictionRule();
            rule.setDescription("Internal network");

            assertThat(rule.getDescription()).isEqualTo("Internal network");
        }
    }

    @Nested
    @DisplayName("Type 枚举")
    class TypeEnumTests {

        @Test
        @DisplayName("应包含 WHITELIST 和 BLACKLIST")
        void shouldContainWhitelistAndBlacklist() {
            IpRestrictionRule.Type[] types = IpRestrictionRule.Type.values();

            assertThat(types).hasSize(2);
            assertThat(types).containsExactlyInAnyOrder(
                    IpRestrictionRule.Type.WHITELIST,
                    IpRestrictionRule.Type.BLACKLIST
            );
        }
    }

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("全参数构造函数应正确设置所有字段")
        void shouldSetAllFieldsWithFullConstructor() {
            IpRestrictionRule rule = new IpRestrictionRule(
                    IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "Internal network"
            );

            assertThat(rule.getType()).isEqualTo(IpRestrictionRule.Type.WHITELIST);
            assertThat(rule.getIpPattern()).isEqualTo("192.168.1.*");
            assertThat(rule.getDescription()).isEqualTo("Internal network");
        }
    }
}
