package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.security.DefaultIpRestrictionChecker;
import io.github.afgprojects.framework.security.core.security.model.IpRestrictionRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("白名单模式")
    class WhitelistTests {

        @Test
        @DisplayName("白名单中的 IP 应被识别为白名单")
        void shouldRecognizeWhitelistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "特定 IP");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();
        }

        @Test
        @DisplayName("不在白名单中的 IP 不应被识别")
        void shouldNotRecognizeNonWhitelistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "特定 IP");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("10.0.0.1")).isFalse();
        }

        @Test
        @DisplayName("白名单中的 IP 应被允许访问")
        void shouldAllowWhitelistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "特定 IP");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isAllowed("192.168.1.100", null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("黑名单模式")
    class BlacklistTests {

        @Test
        @DisplayName("黑名单中的 IP 应被识别为黑名单")
        void shouldRecognizeBlacklistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "封禁 IP");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.isBlacklisted("10.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("黑名单中的 IP 应被拒绝访问")
        void shouldDenyBlacklistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "封禁 IP");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.isAllowed("10.0.0.1", null, null)).isFalse();
        }

        @Test
        @DisplayName("不在黑名单中的 IP 应被允许访问")
        void shouldAllowNonBlacklistedIp() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "封禁 IP");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.isAllowed("192.168.1.1", null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("白名单优先于黑名单")
    class WhitelistPriorityTests {

        @Test
        @DisplayName("白名单中的 IP 即使在黑名单中也应被允许")
        void shouldAllowWhitelistedIpEvenIfBlacklisted() {
            IpRestrictionRule whitelist = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "白名单");
            IpRestrictionRule blacklist = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "192.168.1.100", "黑名单");
            checker.setWhitelistRules(List.of(whitelist));
            checker.setBlacklistRules(List.of(blacklist));

            assertThat(checker.isAllowed("192.168.1.100", null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("CIDR 匹配")
    class CidrMatchTests {

        @Test
        @DisplayName("/24 子网应正确匹配白名单")
        void shouldMatch24SubnetWhitelist() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.0/24", "内网");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("192.168.1.0")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.128")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.255")).isTrue();
            assertThat(checker.isWhitelisted("192.168.2.0")).isFalse();
        }

        @Test
        @DisplayName("/8 子网应正确匹配")
        void shouldMatch8Subnet() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "10.0.0.0/8", "内网");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("10.0.0.1")).isTrue();
            assertThat(checker.isWhitelisted("10.255.255.255")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.1")).isFalse();
        }

        @Test
        @DisplayName("/24 子网应正确匹配黑名单")
        void shouldMatch24SubnetBlacklist() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.0/24", "封禁段");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.isBlacklisted("10.0.0.50")).isTrue();
            assertThat(checker.isBlacklisted("10.0.1.1")).isFalse();
        }
    }

    @Nested
    @DisplayName("通配符匹配")
    class WildcardMatchTests {

        @Test
        @DisplayName("192.168.1.* 应匹配 192.168.1.0-255")
        void shouldMatchWildcardForLastOctet() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.*", "内网 C 段");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("192.168.1.0")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.128")).isTrue();
            assertThat(checker.isWhitelisted("192.168.1.255")).isTrue();
            assertThat(checker.isWhitelisted("192.168.2.1")).isFalse();
        }

        @Test
        @DisplayName("* 应匹配所有 IP")
        void shouldMatchAllWithSingleAsterisk() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "*", "封禁所有");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.isBlacklisted("192.168.1.1")).isTrue();
            assertThat(checker.isBlacklisted("10.0.0.1")).isTrue();
        }
    }

    @Nested
    @DisplayName("空规则和边界")
    class EdgeCaseTests {

        @Test
        @DisplayName("无规则时应默认允许")
        void shouldAllowByDefaultWhenNoRules() {
            assertThat(checker.isAllowed("192.168.1.1", null, null)).isTrue();
        }

        @Test
        @DisplayName("空白名单和黑名单应默认允许")
        void shouldAllowWhenEmptyRules() {
            checker.setWhitelistRules(List.of());
            checker.setBlacklistRules(List.of());

            assertThat(checker.isAllowed("192.168.1.1", null, null)).isTrue();
        }

        @Test
        @DisplayName("setWhitelistRules 应能更新白名单")
        void shouldUpdateWhitelistRules() {
            IpRestrictionRule rule1 = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.0/24", "规则1");
            checker.setWhitelistRules(List.of(rule1));
            assertThat(checker.isWhitelisted("192.168.1.1")).isTrue();

            IpRestrictionRule rule2 = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "10.0.0.0/24", "规则2");
            checker.setWhitelistRules(List.of(rule2));
            assertThat(checker.isWhitelisted("192.168.1.1")).isFalse();
            assertThat(checker.isWhitelisted("10.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("setBlacklistRules 应能更新黑名单")
        void shouldUpdateBlacklistRules() {
            IpRestrictionRule rule1 = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "规则1");
            checker.setBlacklistRules(List.of(rule1));
            assertThat(checker.isBlacklisted("10.0.0.1")).isTrue();

            IpRestrictionRule rule2 = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.2", "规则2");
            checker.setBlacklistRules(List.of(rule2));
            assertThat(checker.isBlacklisted("10.0.0.1")).isFalse();
            assertThat(checker.isBlacklisted("10.0.0.2")).isTrue();
        }

        @Test
        @DisplayName("IPv4 回环地址应正确处理")
        void shouldHandleLoopbackAddress() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "127.0.0.1", "回环");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.isWhitelisted("127.0.0.1")).isTrue();
            assertThat(checker.isWhitelisted("127.0.0.2")).isFalse();
        }
    }

    @Nested
    @DisplayName("缓存管理")
    class CacheTests {

        @Test
        @DisplayName("clearCache 应清除缓存")
        void shouldClearCache() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "特定 IP");
            checker.setWhitelistRules(List.of(rule));

            // 先查询一次，缓存结果
            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();

            // 清除缓存后仍应返回相同结果（重新计算）
            checker.clearCache();
            assertThat(checker.isWhitelisted("192.168.1.100")).isTrue();
        }

        @Test
        @DisplayName("getWhitelistRules 应返回当前白名单规则")
        void shouldReturnWhitelistRules() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.WHITELIST, "192.168.1.100", "特定 IP");
            checker.setWhitelistRules(List.of(rule));

            assertThat(checker.getWhitelistRules()).isNotNull();
            assertThat(checker.getWhitelistRules()).hasSize(1);
        }

        @Test
        @DisplayName("getBlacklistRules 应返回当前黑名单规则")
        void shouldReturnBlacklistRules() {
            IpRestrictionRule rule = new IpRestrictionRule(IpRestrictionRule.Type.BLACKLIST, "10.0.0.1", "封禁 IP");
            checker.setBlacklistRules(List.of(rule));

            assertThat(checker.getBlacklistRules()).isNotNull();
            assertThat(checker.getBlacklistRules()).hasSize(1);
        }
    }
}
