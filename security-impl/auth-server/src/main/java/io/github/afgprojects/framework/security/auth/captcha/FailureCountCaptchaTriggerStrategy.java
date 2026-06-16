package io.github.afgprojects.framework.security.auth.captcha;

import io.github.afgprojects.framework.security.core.login.strategy.CaptchaTriggerStrategy;
import io.github.afgprojects.framework.security.core.security.LoginFailureTracker;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 基于登录失败次数的验证码触发策略。
 *
 * <p>当用户密码错误达到指定次数后，要求输入图形验证码。
 * 默认阈值为 3 次。
 *
 * <p>可通过构造函数自定义阈值。
 *
 * @since 1.1.0
 */
@Slf4j
public class FailureCountCaptchaTriggerStrategy implements CaptchaTriggerStrategy {

    /**
     * 默认触发阈值
     */
    private static final int DEFAULT_THRESHOLD = 3;

    private final LoginFailureTracker failureTracker;
    private final int threshold;

    /**
     * 使用默认阈值（3 次）构造。
     *
     * @param failureTracker 登录失败追踪器
     */
    public FailureCountCaptchaTriggerStrategy(@NonNull LoginFailureTracker failureTracker) {
        this(failureTracker, DEFAULT_THRESHOLD);
    }

    /**
     * 使用自定义阈值构造。
     *
     * @param failureTracker 登录失败追踪器
     * @param threshold 触发阈值
     */
    public FailureCountCaptchaTriggerStrategy(@NonNull LoginFailureTracker failureTracker, int threshold) {
        this.failureTracker = failureTracker;
        this.threshold = threshold;
        log.info("Initialized FailureCountCaptchaTriggerStrategy with threshold={}", threshold);
    }

    @Override
    public boolean shouldRequireCaptcha(@NonNull String username, @Nullable String tenantId, @NonNull String ip) {
        int failureCount = failureTracker.getFailureCount(username, tenantId);
        boolean requireCaptcha = failureCount >= threshold;
        if (requireCaptcha) {
            log.debug("Captcha required for user {}: failureCount={}, threshold={}", username, failureCount, threshold);
        }
        return requireCaptcha;
    }

    @Override
    @NonNull
    public String getCaptchaType() {
        return "IMAGE";
    }

    @Override
    @Nullable
    public String getTriggerReason(@NonNull String username, @Nullable String tenantId, @NonNull String ip) {
        int failureCount = failureTracker.getFailureCount(username, tenantId);
        if (failureCount >= threshold) {
            return "密码错误次数过多（" + failureCount + "次），请输入验证码";
        }
        return null;
    }
}
