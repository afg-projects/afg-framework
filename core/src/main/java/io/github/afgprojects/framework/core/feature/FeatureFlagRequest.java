package io.github.afgprojects.framework.core.feature;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;

/**
 * 功能开关请求 DTO
 */
public record FeatureFlagRequest(
        /**
         * 功能名称
         */
        @NotBlank(message = "功能名称不能为空") String name,

        /**
         * 是否启用
         */
        boolean enabled,

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
     * @return 灰度规则
     */
    public @Nullable GrayscaleRule toGrayscaleRule() {
        if (strategy == null) {
            return enabled ? GrayscaleRule.ALL : GrayscaleRule.NONE;
        }

        GrayscaleStrategy grayscaleStrategy = parseStrategy(strategy);
        if (grayscaleStrategy == null) {
            return enabled ? GrayscaleRule.ALL : GrayscaleRule.NONE;
        }

        return GrayscaleRule.builder()
                .strategy(grayscaleStrategy)
                .percentage(percentage != null ? percentage : 0)
                .userIds(userIds)
                .tenantIds(tenantIds)
                .build();
    }

    private @Nullable GrayscaleStrategy parseStrategy(String strategyName) {
        try {
            return GrayscaleStrategy.valueOf(strategyName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
