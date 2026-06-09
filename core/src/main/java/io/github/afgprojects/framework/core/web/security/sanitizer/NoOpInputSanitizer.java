package io.github.afgprojects.framework.core.web.security.sanitizer;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * 空操作输入安全检测器。
 * <p>
 * 当 AntiSamy 不在 classpath 上时作为降级实现使用。
 * 所有检测方法均返回 false（不检测），所有清洗方法均原样返回输入（不清洗）。
 * <p>
 * <b>安全警告：</b>此实现不提供任何安全防护。生产环境应确保 AntiSamy 依赖存在，
 * 以使用 {@link EnhancedInputSanitizer} 进行真正的安全检测。
 *
 * @see InputSecurityChecker
 * @see EnhancedInputSanitizer
 */
@Slf4j
public class NoOpInputSanitizer implements InputSecurityChecker {

    private static final String WARNING_MESSAGE =
            "NoOpInputSanitizer is being used - XSS detection is DISABLED. "
            + "Add 'antisamy' dependency to enable real protection via EnhancedInputSanitizer.";

    /**
     * 创建空操作输入安全检测器。
     * <p>
     * 记录一条警告日志，提示应添加 AntiSamy 依赖。
     */
    public NoOpInputSanitizer() {
        log.warn(WARNING_MESSAGE);
    }

    @Override
    public boolean containsXss(@Nullable String input) {
        return false;
    }

    @Override
    public @Nullable String sanitizeHtml(@Nullable String input) {
        return input;
    }
}
