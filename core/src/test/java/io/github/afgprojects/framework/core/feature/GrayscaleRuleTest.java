package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * GrayscaleRule 测试
 */
@DisplayName("GrayscaleRule 测试")
class GrayscaleRuleTest extends BaseUnitTest {

    @Test
    @DisplayName("ALL 静态常量应启用所有用户")
    void all_shouldEnableAll() {
        GrayscaleRule rule = GrayscaleRule.ALL;

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.ALL);
        assertThat(rule.percentage()).isEqualTo(100);

        // 任何上下文都应启用
        assertThat(rule.isEnabled(GrayscaleContext.EMPTY)).isTrue();
        assertThat(rule.isEnabled(GrayscaleContext.of(123L, 456L))).isTrue();
    }

    @Test
    @DisplayName("NONE 静态常量应禁用所有用户")
    void none_shouldDisableAll() {
        GrayscaleRule rule = GrayscaleRule.NONE;

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.NONE);
        assertThat(rule.percentage()).isEqualTo(0);

        // 任何上下文都应禁用
        assertThat(rule.isEnabled(GrayscaleContext.EMPTY)).isFalse();
        assertThat(rule.isEnabled(GrayscaleContext.of(123L, 456L))).isFalse();
    }

    @Test
    @DisplayName("ofPercentage 应创建百分比规则")
    void ofPercentage_shouldCreatePercentageRule() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(50);

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.PERCENTAGE);
        assertThat(rule.percentage()).isEqualTo(50);
        assertThat(rule.userIds()).isNull();
        assertThat(rule.tenantIds()).isNull();
    }

    @Test
    @DisplayName("ofUserWhitelist 应创建用户白名单规则")
    void ofUserWhitelist_shouldCreateUserWhitelistRule() {
        Set<Long> userIds = Set.of(1L, 2L, 3L);
        GrayscaleRule rule = GrayscaleRule.ofUserWhitelist(userIds);

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.USER_WHITELIST);
        assertThat(rule.userIds()).containsExactlyInAnyOrderElementsOf(userIds);
        assertThat(rule.tenantIds()).isNull();
    }

    @Test
    @DisplayName("ofTenantWhitelist 应创建租户白名单规则")
    void ofTenantWhitelist_shouldCreateTenantWhitelistRule() {
        Set<Long> tenantIds = Set.of(100L, 200L);
        GrayscaleRule rule = GrayscaleRule.ofTenantWhitelist(tenantIds);

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.TENANT_WHITELIST);
        assertThat(rule.tenantIds()).containsExactlyInAnyOrderElementsOf(tenantIds);
        assertThat(rule.userIds()).isNull();
    }

    @Test
    @DisplayName("Builder 应正确构建规则")
    void builder_shouldBuildRule() {
        GrayscaleRule rule = GrayscaleRule.builder()
                .strategy(GrayscaleStrategy.PERCENTAGE)
                .percentage(75)
                .addUserIds(1L, 2L, 3L)
                .addTenantIds(100L, 200L)
                .build();

        assertThat(rule.strategy()).isEqualTo(GrayscaleStrategy.PERCENTAGE);
        assertThat(rule.percentage()).isEqualTo(75);
        assertThat(rule.userIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(rule.tenantIds()).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("Builder percentage 应限制在 0-100 范围内")
    void builder_percentage_shouldBeClamped() {
        GrayscaleRule over = GrayscaleRule.builder().percentage(150).build();
        assertThat(over.percentage()).isEqualTo(100);

        GrayscaleRule under = GrayscaleRule.builder().percentage(-10).build();
        assertThat(under.percentage()).isEqualTo(0);
    }

    @Test
    @DisplayName("isEnabled 策略为 null 时应返回 false")
    void isEnabled_nullStrategy_shouldReturnFalse() {
        GrayscaleRule rule = new GrayscaleRule(null, 50, null, null);

        assertThat(rule.isEnabled(GrayscaleContext.EMPTY)).isFalse();
    }
}