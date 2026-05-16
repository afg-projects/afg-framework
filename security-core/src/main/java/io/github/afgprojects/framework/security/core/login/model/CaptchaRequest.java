package io.github.afgprojects.framework.security.core.login.model;

import org.jspecify.annotations.Nullable;

/**
 * 验证码请求 DTO。
 *
 * @param captchaType 验证码类型
 * @param target 目标（手机号或邮箱，图形验证码时可为空）
 * @param tenantId 租户 ID
 * @author afg-projects
 * @since 1.0.0
 */
public record CaptchaRequest(
        CaptchaType captchaType,
        @Nullable String target,
        @Nullable String tenantId) {

    /**
     * 创建图形验证码请求。
     *
     * @return 验证码请求
     */
    public static CaptchaRequest ofImage() {
        return new CaptchaRequest(CaptchaType.IMAGE, null, null);
    }

    /**
     * 创建图形验证码请求（带租户）。
     *
     * @param tenantId 租户 ID
     * @return 验证码请求
     */
    public static CaptchaRequest ofImage(@Nullable String tenantId) {
        return new CaptchaRequest(CaptchaType.IMAGE, null, tenantId);
    }

    /**
     * 创建短信验证码请求。
     *
     * @param mobile 手机号
     * @return 验证码请求
     */
    public static CaptchaRequest ofSms(String mobile) {
        return new CaptchaRequest(CaptchaType.SMS, mobile, null);
    }

    /**
     * 创建短信验证码请求（带租户）。
     *
     * @param mobile 手机号
     * @param tenantId 租户 ID
     * @return 验证码请求
     */
    public static CaptchaRequest ofSms(String mobile, @Nullable String tenantId) {
        return new CaptchaRequest(CaptchaType.SMS, mobile, tenantId);
    }

    /**
     * 创建邮箱验证码请求。
     *
     * @param email 邮箱
     * @return 验证码请求
     */
    public static CaptchaRequest ofEmail(String email) {
        return new CaptchaRequest(CaptchaType.EMAIL, email, null);
    }

    /**
     * 创建邮箱验证码请求（带租户）。
     *
     * @param email 邮箱
     * @param tenantId 租户 ID
     * @return 验证码请求
     */
    public static CaptchaRequest ofEmail(String email, @Nullable String tenantId) {
        return new CaptchaRequest(CaptchaType.EMAIL, email, tenantId);
    }
}
