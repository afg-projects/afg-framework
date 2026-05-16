package io.github.afgprojects.framework.security.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.security.model.IpRestrictionRule;

/**
 * DefaultIpRestrictionChecker 测试
 */
@DisplayName("DefaultIpRestrictionChecker 测试")
class DefaultIpRestrictionCheckerTest {

    private DefaultIpRestrictionChecker checker;

    @BeforeEach
    void setUp() {
        checker = new DefaultIpRestrictionChecker();
    }

    @Nested
    @DisplayName("白名单检查测试")
    class WhitelistTests {

        @Test
        @DisplayName("IP 在白名单中应该返回 true")
        void shouldReturnTrueWhenIpInWhitelist() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP"),
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "10.0.0.1", "另一个允许的IP")));

            // when
            boolean result = checker.isWhitelisted("192.168.1.100");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("IP 不在白名单中应该返回 false")
        void shouldReturnFalseWhenIpNotInWhitelist() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));

            // when
            boolean result = checker.isWhitelisted("192.168.1.101");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("白名单为空时应该返回 false")
        void shouldReturnFalseWhenWhitelistEmpty() {
            // given
            checker.setWhitelistRules(List.of());

            // when
            boolean result = checker.isWhitelisted("192.168.1.100");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("黑名单检查测试")
    class BlacklistTests {

        @Test
        @DisplayName("IP 在黑名单中应该返回 true")
        void shouldReturnTrueWhenIpInBlacklist() {
            // given
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "192.168.1.100", "禁止的IP"),
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "另一个禁止的IP")));

            // when
            boolean result = checker.isBlacklisted("192.168.1.100");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("IP 不在黑名单中应该返回 false")
        void shouldReturnFalseWhenIpNotInBlacklist() {
            // given
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "192.168.1.100", "禁止的IP")));

            // when
            boolean result = checker.isBlacklisted("192.168.1.101");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("黑名单为空时应该返回 false")
        void shouldReturnFalseWhenBlacklistEmpty() {
            // given
            checker.setBlacklistRules(List.of());

            // when
            boolean result = checker.isBlacklisted("192.168.1.100");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("通配符匹配测试")
    class WildcardMatchTests {

        @Test
        @DisplayName("应该支持单段通配符匹配 - 192.168.1.*")
        void shouldSupportSingleSegmentWildcard() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "允许 192.168.1.0-255")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.1.0")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.255")).isTrue();
            assertThat(checker.isWhitelisted("192.168.2.1")).isFalse();
        }

        @Test
        @DisplayName("应该支持多段通配符匹配 - 192.168.*.*")
        void shouldSupportMultipleSegmentWildcard() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.*.*", "允许 192.168.0.0-255.0-255")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.0.1")).isTrue();
            assertThat(checker.isWhitelisted("192.168.100.200")).isTrue();
            assertThat(checker.isWhitelisted("192.168.255.255")).isTrue();
            assertThat(checker.isWhitelisted("192.169.1.1")).isFalse();
        }

        @Test
        @DisplayName("应该支持全通配符 - *")
        void shouldSupportFullWildcard() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "*", "允许所有IP")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.1.1")).isTrue();
            assertThat(checker.isWhitelisted("10.0.0.1")).isTrue();
            assertThat(checker.isWhitelisted("172.16.0.1")).isTrue();
        }

        @Test
        @DisplayName("黑名单也应该支持通配符匹配")
        void shouldSupportWildcardInBlacklist() {
            // given
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.*.*.*", "禁止所有 10.x.x.x")));

            // when & then
            assertThat(checker.isBlacklisted("10.0.0.1")).isTrue();
            assertThat(checker.isBlacklisted("10.255.255.255")).isTrue();
            assertThat(checker.isBlacklisted("192.168.1.1")).isFalse();
        }
    }

    @Nested
    @DisplayName("CIDR 匹配测试")
    class CidrMatchTests {

        @Test
        @DisplayName("应该支持 CIDR /24 匹配")
        void shouldSupportCidr24() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.0/24", "允许 192.168.1.0-255")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.1.0")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.255")).isTrue();
            assertThat(checker.isWhitelisted("192.168.2.1")).isFalse();
        }

        @Test
        @DisplayName("应该支持 CIDR /16 匹配")
        void shouldSupportCidr16() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.0.0/16", "允许 192.168.0.0-255.0-255")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.0.1")).isTrue();
            assertThat(checker.isWhitelisted("192.168.100.200")).isTrue();
            assertThat(checker.isWhitelisted("192.168.255.255")).isTrue();
            assertThat(checker.isWhitelisted("192.169.1.1")).isFalse();
        }

        @Test
        @DisplayName("应该支持 CIDR /32 精确匹配")
        void shouldSupportCidr32() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100/32", "只允许此IP")));

            // when & then
            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.101")).isFalse();
        }

        @Test
        @DisplayName("应该支持 CIDR /8 匹配")
        void shouldSupportCidr8() {
            // given
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.0/8", "禁止所有 10.x.x.x")));

            // when & then
            assertThat(checker.isBlacklisted("10.0.0.1")).isTrue();
            assertThat(checker.isBlacklisted("10.255.255.255")).isTrue();
            assertThat(checker.isBlacklisted("192.168.1.1")).isFalse();
        }
    }

    @Nested
    @DisplayName("综合访问检查测试")
    class IsAllowedTests {

        @Test
        @DisplayName("白名单优先：IP 在白名单中应该允许访问")
        void shouldAllowWhenInWhitelist() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));
            checker.setBlacklistRules(List.of());

            // when
            boolean result = checker.isAllowed("192.168.1.100", null, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("黑名单拒绝：IP 在黑名单中应该拒绝访问")
        void shouldDenyWhenInBlacklist() {
            // given
            checker.setWhitelistRules(List.of());
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "192.168.1.100", "禁止的IP")));

            // when
            boolean result = checker.isAllowed("192.168.1.100", null, null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("白名单优先于黑名单：IP 同时在白名单和黑名单中应该允许访问")
        void shouldAllowWhenInBothLists() {
            // given - 白名单优先
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "192.168.1.100", "禁止的IP")));

            // when
            boolean result = checker.isAllowed("192.168.1.100", null, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不在任何名单中：默认应该允许访问")
        void shouldAllowByDefaultWhenNotInAnyList() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));
            checker.setBlacklistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "禁止的IP")));

            // when
            boolean result = checker.isAllowed("172.16.0.1", null, null);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("名单都为空：应该允许所有访问")
        void shouldAllowAllWhenListsEmpty() {
            // given
            checker.setWhitelistRules(List.of());
            checker.setBlacklistRules(List.of());

            // when
            boolean result = checker.isAllowed("192.168.1.100", null, null);

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("缓存测试")
    class CacheTests {

        @Test
        @DisplayName("相同 IP 多次检查应该使用缓存")
        void shouldUseCacheForSameIp() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "允许的IP段")));

            // when - 多次检查同一 IP
            boolean result1 = checker.isWhitelisted("192.168.1.100");
            boolean result2 = checker.isWhitelisted("192.168.1.100");
            boolean result3 = checker.isWhitelisted("192.168.1.100");

            // then
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();
        }

        @Test
        @DisplayName("更新规则后应该清除缓存")
        void shouldClearCacheWhenRulesUpdated() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));

            // when - 第一次检查
            boolean result1 = checker.isWhitelisted("192.168.1.100");
            assertThat(result1).isTrue();

            // 更新规则
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.101", "新的允许IP")));

            // 再次检查
            boolean result2 = checker.isWhitelisted("192.168.1.100");

            // then
            assertThat(result2).isFalse();
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("null IP 应该返回 false")
        void shouldReturnFalseForNullIp() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "*", "允许所有IP")));

            // when
            boolean result = checker.isWhitelisted(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("空字符串 IP 应该返回 false")
        void shouldReturnFalseForEmptyIp() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "*", "允许所有IP")));

            // when
            boolean result = checker.isWhitelisted("");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("无效 IP 格式应该返回 false")
        void shouldReturnFalseForInvalidIpFormat() {
            // given
            checker.setWhitelistRules(List.of(
                    new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "允许的IP")));

            // when & then
            assertThat(checker.isWhitelisted("invalid-ip")).isFalse();
            assertThat(checker.isWhitelisted("192.168.1")).isFalse();
            assertThat(checker.isWhitelisted("192.168.1.100.200")).isFalse();
        }

        @Test
        @DisplayName("null 规则列表应该被视为空列表")
        void shouldTreatNullRulesAsEmptyList() {
            // given
            checker.setWhitelistRules(null);
            checker.setBlacklistRules(null);

            // when
            boolean whitelistResult = checker.isWhitelisted("192.168.1.100");
            boolean blacklistResult = checker.isBlacklisted("192.168.1.100");
            boolean allowedResult = checker.isAllowed("192.168.1.100", null, null);

            // then
            assertThat(whitelistResult).isFalse();
            assertThat(blacklistResult).isFalse();
            assertThat(allowedResult).isTrue();
        }
    }
}
