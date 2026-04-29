package io.github.afgprojects.framework.core.feature;

import java.time.Instant;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * 功能开关响应 DTO
 */
public record FeatureFlagResponse(
        /**
         * 功能名称
         */
        String name,

        /**
         * 是否启用
         */
        boolean enabled,

        /**
         * 灰度策略
         */
        @Nullable String strategy,

        /**
         * 百分比
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
         * 创建时间
         */
        @Nullable Instant createdAt,

        /**
         * 最后更新时间
         */
        @Nullable Instant updatedAt,

        /**
         * 最后更新者
         */
        @Nullable String updatedBy) {

    /**
     * 从 FeatureFlag 转换为响应 DTO
     *
     * @param flag 功能开关
     * @return 响应 DTO
     */
    public static FeatureFlagResponse from(FeatureFlag flag) {
        GrayscaleRule rule = flag.grayscaleRule();
        return new FeatureFlagResponse(
                flag.name(),
                flag.enabled(),
                rule != null && rule.strategy() != null ? rule.strategy().name() : null,
                rule != null ? rule.percentage() : null,
                rule != null ? rule.userIds() : null,
                rule != null ? rule.tenantIds() : null,
                flag.description(),
                flag.createdAt(),
                flag.updatedAt(),
                flag.updatedBy());
    }
}
