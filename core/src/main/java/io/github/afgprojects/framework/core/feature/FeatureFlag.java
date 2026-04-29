package io.github.afgprojects.framework.core.feature;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * 功能开关状态
 * <p>
 * 存储功能开关的完整状态信息，包括启用状态、灰度规则、元数据等
 * </p>
 */
public record FeatureFlag(
        /**
         * 功能名称
         */
        String name,

        /**
         * 是否启用
         */
        boolean enabled,

        /**
         * 灰度规则
         */
        @Nullable GrayscaleRule grayscaleRule,

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
        @Nullable String updatedBy
) {

    /**
     * 创建一个简单的启用/禁用功能开关
     *
     * @param name    功能名称
     * @param enabled 是否启用
     * @return 功能开关
     */
    public static FeatureFlag of(String name, boolean enabled) {
        return new FeatureFlag(
                name,
                enabled,
                enabled ? GrayscaleRule.ALL : GrayscaleRule.NONE,
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    /**
     * 创建一个带灰度规则的功能开关
     *
     * @param name           功能名称
     * @param grayscaleRule  灰度规则
     * @return 功能开关
     */
    public static FeatureFlag of(String name, GrayscaleRule grayscaleRule) {
        return new FeatureFlag(
                name,
                true,
                grayscaleRule,
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 判断功能是否对给定上下文启用
     *
     * @param context 灰度上下文
     * @return 是否启用
     */
    public boolean isEnabledFor(GrayscaleContext context) {
        if (!enabled) {
            return false;
        }
        GrayscaleRule rule = grayscaleRule;
        if (rule == null) {
            return true;
        }
        return rule.isEnabled(context);
    }

    /**
     * 功能开关构建器
     */
    public static class Builder {

        private String name;
        private boolean enabled = true;
        private @Nullable GrayscaleRule grayscaleRule;
        private @Nullable String description;
        private @Nullable Instant createdAt;
        private @Nullable Instant updatedAt;
        private @Nullable String updatedBy;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder grayscaleRule(@Nullable GrayscaleRule grayscaleRule) {
            this.grayscaleRule = grayscaleRule;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(@Nullable Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(@Nullable Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder updatedBy(@Nullable String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public FeatureFlag build() {
            Instant now = Instant.now();
            return new FeatureFlag(
                    name,
                    enabled,
                    grayscaleRule,
                    description,
                    createdAt != null ? createdAt : now,
                    updatedAt != null ? updatedAt : now,
                    updatedBy);
        }
    }
}