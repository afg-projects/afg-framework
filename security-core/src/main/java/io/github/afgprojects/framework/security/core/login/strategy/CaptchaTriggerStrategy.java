package io.github.afgprojects.framework.security.core.login.strategy;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 验证码触发策略接口。
 *
 * <p>用于决定在何种条件下需要用户输入验证码。
 * 实现类可以基于多种因素（登录失败次数、IP 风险、设备指纹等）来决定是否需要验证码。
 *
 * <p>默认实现：密码错误 3 次后要求图形验证码。
 *
 * <p>扩展点：实现此接口并注册为 Spring Bean 即可替换默认策略。
 *
 * @since 1.1.0
 */
public interface CaptchaTriggerStrategy {

    /**
     * 判断是否需要验证码。
     *
     * @param username 用户名，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param ip 客户端 IP，永不为 null
     * @return 如果需要验证码则返回 true
     */
    boolean shouldRequireCaptcha(@NonNull String username, @Nullable String tenantId, @NonNull String ip);

    /**
     * 获取验证码类型。
     *
     * <p>返回当前策略要求的验证码类型，默认为图形验证码。
     *
     * @return 验证码类型，默认为 "IMAGE"
     */
    @NonNull
    default String getCaptchaType() {
        return "IMAGE";
    }

    /**
     * 获取触发原因描述。
     *
     * <p>用于向前端返回为什么需要验证码，便于展示提示信息。
     *
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param ip 客户端 IP
     * @return 触发原因描述，如"密码错误次数过多，请输入验证码"
     */
    @Nullable
    default String getTriggerReason(@NonNull String username, @Nullable String tenantId, @NonNull String ip) {
        return null;
    }
}
