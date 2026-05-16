package io.github.afgprojects.framework.security.core.login.model;

/**
 * 登录结果枚举。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public enum LoginResult {
    /**
     * 登录成功
     */
    SUCCESS,

    /**
     * 登录失败
     */
    FAILURE,

    /**
     * 账号锁定
     */
    LOCKED,

    /**
     * 验证码错误
     */
    CAPTCHA_ERROR
}
