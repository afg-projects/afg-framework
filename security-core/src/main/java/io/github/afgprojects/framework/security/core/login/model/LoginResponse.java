package io.github.afgprojects.framework.security.core.login.model;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 登录响应 DTO。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @param tokenType 令牌类型（如 Bearer）
 * @param expiresIn 过期时间（秒）
 * @param userId 用户 ID
 * @param username 用户名
 * @param roles 角色列表
 * @param permissions 权限列表
 * @param tenantId 租户 ID
 * @author afg-projects
 * @since 1.0.0
 */
public record LoginResponse(
        String accessToken,
        @Nullable String refreshToken,
        String tokenType,
        long expiresIn,
        String userId,
        String username,
        @Nullable List<String> roles,
        @Nullable List<String> permissions,
        @Nullable String tenantId) {

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * LoginResponse Builder。
     */
    public static final class Builder {
        private String accessToken;
        private @Nullable String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private String userId;
        private String username;
        private @Nullable List<String> roles;
        private @Nullable List<String> permissions;
        private @Nullable String tenantId;

        private Builder() {
        }

        /**
         * 设置访问令牌。
         *
         * @param accessToken 访问令牌
         * @return Builder
         */
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * 设置刷新令牌。
         *
         * @param refreshToken 刷新令牌
         * @return Builder
         */
        public Builder refreshToken(@Nullable String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        /**
         * 设置令牌类型。
         *
         * @param tokenType 令牌类型
         * @return Builder
         */
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        /**
         * 设置过期时间。
         *
         * @param expiresIn 过期时间（秒）
         * @return Builder
         */
        public Builder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        /**
         * 设置用户 ID。
         *
         * @param userId 用户 ID
         * @return Builder
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置用户名。
         *
         * @param username 用户名
         * @return Builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * 设置角色列表。
         *
         * @param roles 角色列表
         * @return Builder
         */
        public Builder roles(@Nullable List<String> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * 设置权限列表。
         *
         * @param permissions 权限列表
         * @return Builder
         */
        public Builder permissions(@Nullable List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * 设置租户 ID。
         *
         * @param tenantId 租户 ID
         * @return Builder
         */
        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 构建 LoginResponse。
         *
         * @return LoginResponse 实例
         */
        public LoginResponse build() {
            return new LoginResponse(
                    accessToken,
                    refreshToken,
                    tokenType,
                    expiresIn,
                    userId,
                    username,
                    roles,
                    permissions,
                    tenantId);
        }
    }
}
