package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * GrayscaleStrategy 测试
 */
@DisplayName("GrayscaleStrategy 测试")
class GrayscaleStrategyTest extends BaseUnitTest {

    private GrayscaleContext context;

    @BeforeEach
    void setUp() {
        context = GrayscaleContext.of(123L, 456L);
    }

    @Test
    @DisplayName("ALL 策略应始终返回 true")
    void all_shouldAlwaysReturnTrue() {
        GrayscaleRule rule = GrayscaleRule.ALL;
        assertThat(GrayscaleStrategy.ALL.isEnabled(context, rule)).isTrue();
        assertThat(GrayscaleStrategy.ALL.isEnabled(GrayscaleContext.EMPTY, rule)).isTrue();
    }

    @Test
    @DisplayName("NONE 策略应始终返回 false")
    void none_shouldAlwaysReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.NONE;
        assertThat(GrayscaleStrategy.NONE.isEnabled(context, rule)).isFalse();
        assertThat(GrayscaleStrategy.NONE.isEnabled(GrayscaleContext.EMPTY, rule)).isFalse();
    }

    @Test
    @DisplayName("PERCENTAGE 策略 - 0% 应返回 false")
    void percentage_zeroPercent_shouldReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(0);
        assertThat(GrayscaleStrategy.PERCENTAGE.isEnabled(context, rule)).isFalse();
    }

    @Test
    @DisplayName("PERCENTAGE 策略 - 100% 应返回 true")
    void percentage_hundredPercent_shouldReturnTrue() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(100);
        assertThat(GrayscaleStrategy.PERCENTAGE.isEnabled(context, rule)).isTrue();
    }

    @Test
    @DisplayName("PERCENTAGE 策略 - 同一用户应得到确定性结果")
    void percentage_sameUser_shouldBeDeterministic() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(50);

        // 同一用户多次调用应得到相同结果
        boolean first = GrayscaleStrategy.PERCENTAGE.isEnabled(context, rule);
        for (int i = 0; i < 10; i++) {
            assertThat(GrayscaleStrategy.PERCENTAGE.isEnabled(context, rule)).isEqualTo(first);
        }
    }

    @Test
    @DisplayName("PERCENTAGE 策略 - 不同用户可能有不同结果")
    void percentage_differentUsers_mayHaveDifferentResults() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(50);

        // 不同用户可能有不同结果
        int trueCount = 0;
        int falseCount = 0;
        for (long userId = 1; userId <= 100; userId++) {
            GrayscaleContext ctx = GrayscaleContext.fromUserId(userId);
            if (GrayscaleStrategy.PERCENTAGE.isEnabled(ctx, rule)) {
                trueCount++;
            } else {
                falseCount++;
            }
        }

        // 100 个用户，50% 灰度，应该大致各占一半
        assertThat(trueCount).isBetween(30, 70);
        assertThat(falseCount).isBetween(30, 70);
    }

    @Test
    @DisplayName("PERCENTAGE 策略 - 无用户ID时使用随机分配")
    void percentage_noUserId_shouldUseRandom() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(50);
        GrayscaleContext emptyContext = GrayscaleContext.EMPTY;

        // 无用户ID时，每次调用可能不同（概率性）
        // 但我们只测试不会抛异常
        for (int i = 0; i < 10; i++) {
            GrayscaleStrategy.PERCENTAGE.isEnabled(emptyContext, rule);
        }
    }

    @Test
    @DisplayName("USER_WHITELIST 策略 - 白名单用户应返回 true")
    void userWhitelist_whitelistUser_shouldReturnTrue() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.USER_WHITELIST)
                .addUserIds(123L, 789L)
                .build();

        assertThat(GrayscaleStrategy.USER_WHITELIST.isEnabled(context, rule)).isTrue();
    }

    @Test
    @DisplayName("USER_WHITELIST 策略 - 非白名单用户应返回 false")
    void userWhitelist_nonWhitelistUser_shouldReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.USER_WHITELIST)
                .addUserIds(999L, 888L)
                .build();

        assertThat(GrayscaleStrategy.USER_WHITELIST.isEnabled(context, rule)).isFalse();
    }

    @Test
    @DisplayName("USER_WHITELIST 策略 - 无用户ID应返回 false")
    void userWhitelist_noUserId_shouldReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.USER_WHITELIST)
                .addUserIds(123L)
                .build();

        assertThat(GrayscaleStrategy.USER_WHITELIST.isEnabled(GrayscaleContext.EMPTY, rule)).isFalse();
    }

    @Test
    @DisplayName("TENANT_WHITELIST 策略 - 白名单租户应返回 true")
    void tenantWhitelist_whitelistTenant_shouldReturnTrue() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.TENANT_WHITELIST)
                .addTenantIds(456L, 789L)
                .build();

        assertThat(GrayscaleStrategy.TENANT_WHITELIST.isEnabled(context, rule)).isTrue();
    }

    @Test
    @DisplayName("TENANT_WHITELIST 策略 - 非白名单租户应返回 false")
    void tenantWhitelist_nonWhitelistTenant_shouldReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.TENANT_WHITELIST)
                .addTenantIds(999L, 888L)
                .build();

        assertThat(GrayscaleStrategy.TENANT_WHITELIST.isEnabled(context, rule)).isFalse();
    }

    @Test
    @DisplayName("TENANT_WHITELIST 策略 - 无租户ID应返回 false")
    void tenantWhitelist_noTenantId_shouldReturnFalse() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.TENANT_WHITELIST)
                .addTenantIds(456L)
                .build();

        assertThat(GrayscaleStrategy.TENANT_WHITELIST.isEnabled(GrayscaleContext.EMPTY, rule)).isFalse();
    }
}