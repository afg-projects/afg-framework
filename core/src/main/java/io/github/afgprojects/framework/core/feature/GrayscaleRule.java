package io.github.afgprojects.framework.core.feature;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * 灰度规则
 * <p>
 * 定义功能开关的灰度发布规则，包括百分比、用户白名单、租户白名单等
 * </p>
 */
public record GrayscaleRule(
        /**
         * 灰度策略
         */
        @Nullable GrayscaleStrategy strategy,

        /**
         * 百分比灰度值（0-100）
         */
        int percentage,

        /**
         * 用户ID白名单
         */
        @Nullable Set<Long> userIds,

        /**
         * 租户ID白名单
         */
        @Nullable Set<Long> tenantIds
) {

    /**
     * 全量发布规则
     */
    public static final GrayscaleRule ALL = new GrayscaleRule(GrayscaleStrategy.ALL, 100, null, null);

    /**
     * 全量关闭规则
     */
    public static final GrayscaleRule NONE = new GrayscaleRule(GrayscaleStrategy.NONE, 0, null, null);

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建百分比灰度规则
     *
     * @param percentage 百分比（0-100）
     * @return 灰度规则
     */
    public static GrayscaleRule ofPercentage(int percentage) {
        return new GrayscaleRule(GrayscaleStrategy.PERCENTAGE, percentage, null, null);
    }

    /**
     * 创建用户白名单规则
     *
     * @param userIds 用户ID集合
     * @return 灰度规则
     */
    public static GrayscaleRule ofUserWhitelist(Set<Long> userIds) {
        return new GrayscaleRule(GrayscaleStrategy.USER_WHITELIST, 0, new HashSet<>(userIds), null);
    }

    /**
     * 创建租户白名单规则
     *
     * @param tenantIds 租户ID集合
     * @return 灰度规则
     */
    public static GrayscaleRule ofTenantWhitelist(Set<Long> tenantIds) {
        return new GrayscaleRule(GrayscaleStrategy.TENANT_WHITELIST, 0, null, new HashSet<>(tenantIds));
    }

    /**
     * 判断功能是否启用
     *
     * @param context 灰度上下文
     * @return 是否启用
     */
    public boolean isEnabled(GrayscaleContext context) {
        GrayscaleStrategy strategy = this.strategy;
        if (strategy == null) {
            return false;
        }
        return strategy.isEnabled(context, this);
    }

    /**
     * 灰度规则构建器
     */
    public static class Builder {

        private @Nullable GrayscaleStrategy strategy;
        private int percentage;
        private @Nullable Set<Long> userIds;
        private @Nullable Set<Long> tenantIds;

        public Builder strategy(@Nullable GrayscaleStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder percentage(int percentage) {
            this.percentage = Math.max(0, Math.min(100, percentage));
            return this;
        }

        public Builder userIds(@Nullable Set<Long> userIds) {
            this.userIds = userIds != null ? new HashSet<>(userIds) : null;
            return this;
        }

        public Builder addUserIds(Long... userIds) {
            if (this.userIds == null) {
                this.userIds = new HashSet<>();
            }
            for (Long userId : userIds) {
                this.userIds.add(userId);
            }
            return this;
        }

        public Builder tenantIds(@Nullable Set<Long> tenantIds) {
            this.tenantIds = tenantIds != null ? new HashSet<>(tenantIds) : null;
            return this;
        }

        public Builder addTenantIds(Long... tenantIds) {
            if (this.tenantIds == null) {
                this.tenantIds = new HashSet<>();
            }
            for (Long tenantId : tenantIds) {
                this.tenantIds.add(tenantId);
            }
            return this;
        }

        public GrayscaleRule build() {
            return new GrayscaleRule(strategy, percentage, userIds, tenantIds);
        }
    }
}