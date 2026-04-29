package io.github.afgprojects.framework.core.feature;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * 功能开关更新请求 DTO
 * <p>
 * 更新时所有字段都是可选的，只更新提供的字段
 * </p>
 */
public record FeatureFlagUpdateRequest(
        /**
         * 是否启用
         */
        @Nullable Boolean enabled,

        /**
         * 灰度策略
         */
        @Nullable String strategy,

        /**
         * 百分比（0-100）
         */
        @Nullable Integer percentage,

        /**
         * 用户ID白名单
         */
        @Nullable Set<Long> userIds,

        /**
         * 租户ID白名单
         */
        @Nullable Set<Long> tenantIds,

        /**
         * 功能描述
         */
        @Nullable String description,

        /**
         * 更新者
         */
        @Nullable String updatedBy) {

    /**
     * 转换为灰度规则
     *
     * @param existingRule 现有规则
     * @return 灰度规则
     */
    public @Nullable GrayscaleRule toGrayscaleRule(@Nullable GrayscaleRule existingRule) {
        // 如果没有提供任何灰度相关字段，保留现有规则
        if (strategy == null && percentage == null && userIds == null && tenantIds == null) {
            return existingRule;
        }

        GrayscaleStrategy grayscaleStrategy = null;
        if (strategy != null) {
            try {
                grayscaleStrategy = GrayscaleStrategy.valueOf(strategy.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 忽略无效的策略名称
            }
        }

        // 如果策略有效，使用新策略；否则使用现有策略
        if (grayscaleStrategy == null && existingRule != null) {
            grayscaleStrategy = existingRule.strategy();
        }

        return GrayscaleRule.builder()
                .strategy(grayscaleStrategy)
                .percentage(percentage != null ? percentage : (existingRule != null ? existingRule.percentage() : 0))
                .userIds(userIds != null ? userIds : (existingRule != null ? existingRule.userIds() : null))
                .tenantIds(
                        tenantIds != null ? tenantIds : (existingRule != null ? existingRule.tenantIds() : null))
                .build();
    }
}
