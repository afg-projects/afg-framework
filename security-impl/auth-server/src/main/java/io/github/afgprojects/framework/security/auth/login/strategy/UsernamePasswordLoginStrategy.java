package io.github.afgprojects.framework.security.auth.login.strategy;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetailsService;
import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.strategy.LoginStrategy;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户名密码登录策略。
 *
 * <p>支持纯用户名密码登录和用户名密码验证码登录。
 *
 * @since 1.0.0
 */
public class UsernamePasswordLoginStrategy implements LoginStrategy {

    private final AfgUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    /**
     * 构造函数。
     *
     * @param userDetailsService 用户详情服务
     * @param passwordEncoder 密码编码器
     * @param captchaService 验证码服务（可选）
     */
    public UsernamePasswordLoginStrategy(
            @NonNull AfgUserDetailsService userDetailsService,
            @NonNull PasswordEncoder passwordEncoder,
            CaptchaService captchaService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
    }

    @Override
    public String getLoginType() {
        return "USERNAME";
    }

    @Override
    public AfgUserDetails authenticate(LoginRequest request) {
        String username = request.username();
        String password = request.password();

        // 验证验证码（如果提供）
        if (request.captchaKey() != null && request.captchaValue() != null) {
            if (captchaService == null) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "验证码服务未配置");
            }
            if (!captchaService.validate(request.captchaKey(), request.captchaValue())) {
                throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "验证码错误");
            }
        }

        // 加载用户详情
        AfgUserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 验证密码
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "密码错误");
        }

        // 检查账号状态
        validateAccountStatus(userDetails);

        return userDetails;
    }

    /**
     * 验证账号状态。
     */
    private void validateAccountStatus(AfgUserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_LOCKED, "账号已被锁定");
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new BusinessException(CommonErrorCode.ACCOUNT_DISABLED, "账号已过期");
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new BusinessException(CommonErrorCode.PASSWORD_EXPIRED, "凭证已过期");
        }
    }
}