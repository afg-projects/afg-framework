package io.github.afgprojects.framework.core.feature;

import java.util.Set;

/**
 * 灰度策略
 * <p>
 * 定义功能开关的灰度发布策略
 * </p>
 */
public enum GrayscaleStrategy {

    /**
     * 全量发布
     * <p>
     * 所有用户都能访问该功能
     * </p>
     */
    ALL {
        @Override
        public boolean isEnabled(GrayscaleContext context, GrayscaleRule rule) {
            return true;
        }
    },

    /**
     * 百分比灰度
     * <p>
     * 按照配置的百分比随机开放功能
     * </p>
     */
    PERCENTAGE {
        @Override
        public boolean isEnabled(GrayscaleContext context, GrayscaleRule rule) {
            int percentage = rule.percentage();
            if (percentage <= 0) {
                return false;
            }
            if (percentage >= 100) {
                return true;
            }
            // 使用用户ID的哈希值进行确定性分配
            Long userId = context.userId();
            if (userId != null) {
                // 基于用户ID的确定性分配，同一用户始终得到相同结果
                return Math.abs(userId.hashCode()) % 100 < percentage;
            }
            // 如果没有用户ID，使用随机分配
            return Math.random() * 100 < percentage;
        }
    },

    /**
     * 用户白名单
     * <p>
     * 只有白名单中的用户才能访问该功能
     * </p>
     */
    USER_WHITELIST {
        @Override
        public boolean isEnabled(GrayscaleContext context, GrayscaleRule rule) {
            Long userId = context.userId();
            if (userId == null) {
                return false;
            }
            Set<Long> whitelist = rule.userIds();
            return whitelist != null && whitelist.contains(userId);
        }
    },

    /**
     * 租户白名单
     * <p>
     * 只有白名单中的租户才能访问该功能
     * </p>
     */
    TENANT_WHITELIST {
        @Override
        public boolean isEnabled(GrayscaleContext context, GrayscaleRule rule) {
            Long tenantId = context.tenantId();
            if (tenantId == null) {
                return false;
            }
            Set<Long> whitelist = rule.tenantIds();
            return whitelist != null && whitelist.contains(tenantId);
        }
    },

    /**
     * 关闭
     * <p>
     * 所有用户都不能访问该功能
     * </p>
     */
    NONE {
        @Override
        public boolean isEnabled(GrayscaleContext context, GrayscaleRule rule) {
            return false;
        }
    };

    /**
     * 判断功能是否启用
     *
     * @param context 灰度上下文
     * @param rule    灰度规则
     * @return 是否启用
     */
    public abstract boolean isEnabled(GrayscaleContext context, GrayscaleRule rule);
}