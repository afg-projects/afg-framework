package io.github.afgprojects.framework.security.auth.login.strategy;

import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import org.jspecify.annotations.NonNull;

/**
 * 手机号验证码登录策略。
 *
 * @since 1.0.0
 */
public class MobileCaptchaLoginStrategy implements LoginStrategy {

    private final AfgUserDetailsService userDetailsService;
    private final CaptchaService captchaService;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param captchaService 验证码服务
     */
    public MobileCaptchaLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull CaptchaService captchaService) {
        this.userDetailsService = userDetailsService;
        this.captchaService = captchaService;
    }

    @Override
    public String getLoginType() {
        return "MOBILE";
    }

    @Override
    public AfgUserDetails authenticate(LoginRequest request) {
        String mobile = request.mobile();
        String captchaValue = request.captchaValue();

        // 验证验证码
        String captchaKey = "sms:" + mobile;
        if (!captchaService.validate(captchaKey, captchaValue)) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 加载用户详情
        AfgUserDetails userDetails = userDetailsService.loadUserByMobile(mobile);

        // 检查账号状态
        validateAccountStatus(userDetails);

        return userDetails;
    }

    /**
     * 验证账号状态。
     */
    private void validateAccountStatus(AfgUserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new IllegalArgumentException("账号已被锁定");
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new IllegalArgumentException("账号已过期");
        }
    }
}