package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import org.jspecify.annotations.NonNull;

/**
 * NoOp 验证码服务降级实现。
 * <p>
 * 验证码总是验证失败，生成返回空结果。
 *
 * @since 1.0.0
 */
public class NoOpCaptchaService implements CaptchaService {

    @Override
    public @NonNull CaptchaResponse generate(@NonNull CaptchaRequest request) {
        return CaptchaResponse.builder()
                .captchaKey("noop")
                .build();
    }

    @Override
    public boolean validate(@NonNull String captchaKey, @NonNull String captchaValue) {
        return false;
    }

    @Override
    public void delete(@NonNull String captchaKey) {
        // no-op
    }
}
