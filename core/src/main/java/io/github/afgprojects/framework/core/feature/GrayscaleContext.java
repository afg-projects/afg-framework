package io.github.afgprojects.framework.core.feature;

import org.jspecify.annotations.Nullable;

/**
 * 灰度上下文
 * <p>
 * 包含当前请求的用户和租户信息，用于灰度策略判断
 * </p>
 */
public record GrayscaleContext(
        /**
         * 用户ID
         */
        @Nullable String userId,

        /**
         * 租户ID
         */
        @Nullable String tenantId,

        /**
         * 用户名
         */
        @Nullable String username,

        /**
         * 客户端IP
         */
        @Nullable String clientIp
) {

    /**
     * 空上下文
     */
    public static final GrayscaleContext EMPTY = new GrayscaleContext(null, null, null, null);

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 从用户ID创建上下文
     *
     * @param userId 用户ID
     * @return 灰度上下文
     */
    public static GrayscaleContext fromUserId(String userId) {
        return new GrayscaleContext(userId, null, null, null);
    }

    /**
     * 从用户ID和租户ID创建上下文
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 灰度上下文
     */
    public static GrayscaleContext of(String userId, String tenantId) {
        return new GrayscaleContext(userId, tenantId, null, null);
    }

    /**
     * 灰度上下文构建器
     */
    public static class Builder {

        private @Nullable String userId;
        private @Nullable String tenantId;
        private @Nullable String username;
        private @Nullable String clientIp;

        public Builder userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder username(@Nullable String username) {
            this.username = username;
            return this;
        }

        public Builder clientIp(@Nullable String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public GrayscaleContext build() {
            return new GrayscaleContext(userId, tenantId, username, clientIp);
        }
    }
}
