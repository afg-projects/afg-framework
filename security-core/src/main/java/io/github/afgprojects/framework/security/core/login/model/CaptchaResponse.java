package io.github.afgprojects.framework.security.core.login.model;

import org.jspecify.annotations.Nullable;

/**
 * 验证码响应 DTO。
 *
 * @param captchaKey 验证码 key（用于验证时提交）
 * @param captchaImage 验证码图片（Base64 编码，图形验证码时返回）
 * @param captchaType 验证码类型
 * @param expiresIn 过期时间（秒）
 * @author afg-projects
 * @since 1.0.0
 */
public record CaptchaResponse(
        String captchaKey,
        @Nullable String captchaImage,
        CaptchaType captchaType,
        long expiresIn) {

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * CaptchaResponse Builder。
     */
    public static final class Builder {
        private String captchaKey;
        private @Nullable String captchaImage;
        private CaptchaType captchaType;
        private long expiresIn = 300;

        private Builder() {
        }

        /**
         * 设置验证码 key。
         *
         * @param captchaKey 验证码 key
         * @return Builder
         */
        public Builder captchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
            return this;
        }

        /**
         * 设置验证码图片。
         *
         * @param captchaImage 验证码图片（Base64 编码）
         * @return Builder
         */
        public Builder captchaImage(@Nullable String captchaImage) {
            this.captchaImage = captchaImage;
            return this;
        }

        /**
         * 设置验证码类型。
         *
         * @param captchaType 验证码类型
         * @return Builder
         */
        public Builder captchaType(CaptchaType captchaType) {
            this.captchaType = captchaType;
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
         * 构建 CaptchaResponse。
         *
         * @return CaptchaResponse 实例
         */
        public CaptchaResponse build() {
            return new CaptchaResponse(captchaKey, captchaImage, captchaType, expiresIn);
        }
    }
}
