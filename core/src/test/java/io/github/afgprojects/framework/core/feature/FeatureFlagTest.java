package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * FeatureFlag 测试
 */
@DisplayName("FeatureFlag 测试")
class FeatureFlagTest extends BaseUnitTest {

    @Test
    @DisplayName("of 应创建简单启用/禁用开关")
    void of_shouldCreateSimpleFlag() {
        FeatureFlag enabled = FeatureFlag.of("test-feature", true);
        assertThat(enabled.name()).isEqualTo("test-feature");
        assertThat(enabled.enabled()).isTrue();
        assertThat(enabled.grayscaleRule()).isNotNull();

        FeatureFlag disabled = FeatureFlag.of("test-feature", false);
        assertThat(disabled.enabled()).isFalse();
        assertThat(disabled.grayscaleRule()).isEqualTo(GrayscaleRule.NONE);
    }

    @Test
    @DisplayName("of 应创建带灰度规则的开关")
    void of_withRule_shouldCreateFlagWithRule() {
        GrayscaleRule rule = GrayscaleRule.ofPercentage(50);
        FeatureFlag flag = FeatureFlag.of("test-feature", rule);

        assertThat(flag.name()).isEqualTo("test-feature");
        assertThat(flag.enabled()).isTrue();
        assertThat(flag.grayscaleRule()).isEqualTo(rule);
    }

    @Test
    @DisplayName("Builder 应正确构建开关")
    void builder_shouldBuildFlag() {
        Instant created = Instant.now().minusSeconds(3600);
        Instant updated = Instant.now();
        GrayscaleRule rule = GrayscaleRule.ofPercentage(30);

        FeatureFlag flag = FeatureFlag.builder()
                .name("custom-feature")
                .enabled(true)
                .grayscaleRule(rule)
                .description("自定义功能")
                .createdAt(created)
                .updatedAt(updated)
                .updatedBy("admin")
                .build();

        assertThat(flag.name()).isEqualTo("custom-feature");
        assertThat(flag.enabled()).isTrue();
        assertThat(flag.grayscaleRule()).isEqualTo(rule);
        assertThat(flag.description()).isEqualTo("自定义功能");
        assertThat(flag.createdAt()).isEqualTo(created);
        assertThat(flag.updatedAt()).isEqualTo(updated);
        assertThat(flag.updatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Builder 未设置时间时自动填充")
    void builder_shouldAutoFillTimestamps() {
        FeatureFlag flag = FeatureFlag.builder().name("auto-time").build();

        assertThat(flag.createdAt()).isNotNull();
        assertThat(flag.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("isEnabledFor 禁用时返回 false")
    void isEnabledFor_disabledFlag_shouldReturnFalse() {
        FeatureFlag flag = FeatureFlag.of("disabled-feature", false);

        assertThat(flag.isEnabledFor(GrayscaleContext.EMPTY)).isFalse();
        assertThat(flag.isEnabledFor(GrayscaleContext.of(123L, 456L))).isFalse();
    }

    @Test
    @DisplayName("isEnabledFor 启用但无规则时返回 true")
    void isEnabledFor_enabledWithoutRule_shouldReturnTrue() {
        FeatureFlag flag = FeatureFlag.builder()
                .name("no-rule-feature")
                .enabled(true)
                .grayscaleRule(null)
                .build();

        assertThat(flag.isEnabledFor(GrayscaleContext.EMPTY)).isTrue();
        assertThat(flag.isEnabledFor(GrayscaleContext.of(123L, 456L))).isTrue();
    }

    @Test
    @DisplayName("isEnabledFor 启用且有规则时按规则判断")
    void isEnabledFor_enabledWithRule_shouldFollowRule() {
        // 50% 规则，特定用户
        FeatureFlag flag = FeatureFlag.builder()
                .name("rule-feature")
                .enabled(true)
                .grayscaleRule(GrayscaleRule.ofUserWhitelist(java.util.Set.of(123L)))
                .build();

        // 白名单用户
        assertThat(flag.isEnabledFor(GrayscaleContext.fromUserId(123L))).isTrue();

        // 非白名单用户
        assertThat(flag.isEnabledFor(GrayscaleContext.fromUserId(999L))).isFalse();
    }
}