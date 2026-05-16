package io.github.afgprojects.framework.security.core.login.strategy;

/**
 * 登录类型枚举。
 *
 * <p>定义系统内置的登录类型。
 *
 * @since 1.0.0
 */
public enum LoginType {

    /**
     * 用户名密码登录
     */
    USERNAME,

    /**
     * 手机号验证码登录
     */
    MOBILE,

    /**
     * 邮箱验证码登录
     */
    EMAIL,

    /**
     * 第三方登录
     */
    THIRD_PARTY
}
