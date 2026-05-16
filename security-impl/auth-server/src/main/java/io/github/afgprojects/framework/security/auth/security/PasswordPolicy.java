package io.github.afgprojects.framework.security.auth.security;

/**
 * 密码策略配置。
 *
 * <p>定义密码强度校验的规则。
 *
 * @param minLength 最小长度
 * @param requireUppercase 是否需要大写字母
 * @param requireLowercase 是否需要小写字母
 * @param requireDigit 是否需要数字
 * @param requireSpecialChar 是否需要特殊字符
 * @since 1.0.0
 */
public record PasswordPolicy(
        int minLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigit,
        boolean requireSpecialChar) {

    /**
     * 默认密码策略。
     *
     * <p>最小长度8位，需要大小写字母、数字和特殊字符。
     */
    public static final PasswordPolicy DEFAULT = new PasswordPolicy(8, true, true, true, true);

    /**
     * 宽松密码策略。
     *
     * <p>最小长度6位，无其他要求。
     */
    public static final PasswordPolicy LOOSE = new PasswordPolicy(6, false, false, false, false);

    /**
     * 严格密码策略。
     *
     * <p>最小长度12位，需要大小写字母、数字和特殊字符。
     */
    public static final PasswordPolicy STRICT = new PasswordPolicy(12, true, true, true, true);

    /**
     * 创建密码策略构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 密码策略构建器。
     */
    public static class Builder {
        private int minLength = 8;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecialChar = true;

        /**
         * 设置最小长度。
         *
         * @param minLength 最小长度
         * @return 构建器
         */
        public Builder minLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        /**
         * 设置是否需要大写字母。
         *
         * @param require 是否需要
         * @return 构建器
         */
        public Builder requireUppercase(boolean require) {
            this.requireUppercase = require;
            return this;
        }

        /**
         * 设置是否需要小写字母。
         *
         * @param require 是否需要
         * @return 构建器
         */
        public Builder requireLowercase(boolean require) {
            this.requireLowercase = require;
            return this;
        }

        /**
         * 设置是否需要数字。
         *
         * @param require 是否需要
         * @return 构建器
         */
        public Builder requireDigit(boolean require) {
            this.requireDigit = require;
            return this;
        }

        /**
         * 设置是否需要特殊字符。
         *
         * @param require 是否需要
         * @return 构建器
         */
        public Builder requireSpecialChar(boolean require) {
            this.requireSpecialChar = require;
            return this;
        }

        /**
         * 构建密码策略。
         *
         * @return 密码策略
         */
        public PasswordPolicy build() {
            return new PasswordPolicy(minLength, requireUppercase, requireLowercase, requireDigit, requireSpecialChar);
        }
    }
}
