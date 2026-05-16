package io.github.afgprojects.framework.security.core.login.model;

/**
 * 验证码类型枚举。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public enum CaptchaType {
    /**
     * 图形验证码
     */
    IMAGE,

    /**
     * 短信验证码
     */
    SMS,

    /**
     * 邮箱验证码
     */
    EMAIL
}
