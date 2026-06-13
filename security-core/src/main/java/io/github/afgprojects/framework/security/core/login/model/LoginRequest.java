package io.github.afgprojects.framework.security.core.login.model;

import org.jspecify.annotations.Nullable;

/**
 * 登录请求 DTO。
 *
 * @param loginType 登录类型
 * @param username 用户名（用户名登录时必填）
 * @param password 密码（用户名登录时必填）
 * @param mobile 手机号（手机号登录时必填）
 * @param email 邮箱（邮箱登录时必填）
 * @param captchaKey 验证码 key
 * @param captchaValue 验证码值
 * @param tenantId 租户 ID
 * @param deviceId 设备 ID
 * @param deviceName 设备名称
 * @param clientId 客户端 ID
 * @param ip 登录 IP 地址
 * @param extra 扩展信息
 * @author afg-projects
 * @since 1.0.0
 */
public record LoginRequest(
        LoginType loginType,
        @Nullable String username,
        @Nullable String password,
        @Nullable String mobile,
        @Nullable String email,
        @Nullable String captchaKey,
        @Nullable String captchaValue,
        @Nullable String tenantId,
        @Nullable String deviceId,
        @Nullable String deviceName,
        @Nullable String clientId,
        @Nullable String ip,
        @Nullable String extra) {

    /**
     * 登录类型枚举。
     */
    public enum LoginType {
        /**
         * 用户名密码登录
         */
        USERNAME,

        /**
         * 手机号登录
         */
        MOBILE,

        /**
         * 邮箱登录
         */
        EMAIL,

        /**
         * 第三方登录
         */
        THIRD_PARTY,

        /**
         * 微信登录
         */
        WECHAT,

        /**
         * 钉钉登录
         */
        DINGTALK,

        /**
         * 飞书登录
         */
        FEISHU,

        /**
         * 企业微信登录
         */
        WECOM
    }

    /**
     * 创建用户名密码登录请求。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录请求
     */
    public static LoginRequest ofUsername(String username, String password) {
        return new LoginRequest(LoginType.USERNAME, username, password, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 创建手机号登录请求。
     *
     * @param mobile 手机号
     * @param captchaValue 验证码值
     * @return 登录请求
     */
    public static LoginRequest ofMobile(String mobile, String captchaValue) {
        return new LoginRequest(LoginType.MOBILE, null, null, mobile, null, null, captchaValue, null, null, null, null, null, null);
    }

    /**
     * 创建邮箱登录请求。
     *
     * @param email 邮箱
     * @param captchaValue 验证码值
     * @return 登录请求
     */
    public static LoginRequest ofEmail(String email, String captchaValue) {
        return new LoginRequest(LoginType.EMAIL, null, null, null, email, null, captchaValue, null, null, null, null, null, null);
    }
}
