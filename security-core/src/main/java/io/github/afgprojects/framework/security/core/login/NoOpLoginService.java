package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import org.jspecify.annotations.NonNull;

/**
 * NoOp 登录服务降级实现。
 * <p>
 * 登录操作总是返回失败响应（空令牌），验证码操作返回空结果。
 * 适用于未配置认证服务器的场景。
 *
 * @since 1.0.0
 */
public class NoOpLoginService implements LoginService {

    @Override
    public @NonNull LoginResponse login(@NonNull LoginRequest request) {
        return LoginResponse.builder()
                .build();
    }

    @Override
    public void logout(@NonNull String token) {
        // no-op
    }

    @Override
    public @NonNull LoginResponse refreshToken(@NonNull String refreshToken) {
        return LoginResponse.builder()
                .build();
    }

    @Override
    public @NonNull CaptchaResponse generateCaptcha(@NonNull CaptchaRequest request) {
        return CaptchaResponse.builder()
                .captchaKey("noop")
                .build();
    }

    @Override
    public boolean validateCaptcha(@NonNull String captchaKey, @NonNull String captchaValue) {
        return false;
    }
}
