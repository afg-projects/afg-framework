package io.github.afgprojects.framework.core.model.exception;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 错误码消息解析器
 * 集成 Spring MessageSource 实现错误码消息国际化
 *
 * <p>使用方式：
 * <pre>
 * // 获取默认语言消息
 * String message = ErrorCodeMessageSource.getMessage(CommonErrorCode.PARAM_ERROR);
 *
 * // 获取指定语言消息
 * String message = ErrorCodeMessageSource.getMessage(CommonErrorCode.PARAM_ERROR, Locale.ENGLISH);
 *
 * // 带参数的消息
 * String message = ErrorCodeMessageSource.getMessage(CommonErrorCode.NOT_FOUND, new Object[]{"用户"}, Locale.CHINA);
 * </pre>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class ErrorCodeMessageSource {

    private static MessageSource messageSource;

    private ErrorCodeMessageSource() {}

    /**
     * 设置 MessageSource 实例
     * 由 Spring 容器在启动时注入，也可用于测试时手动设置
     */
    public static void setMessageSource(@NonNull MessageSource source) {
        messageSource = source;
    }

    /**
     * 获取错误码消息（使用当前上下文 Locale）
     *
     * @param errorCode 错误码
     * @return 国际化消息
     */
    @NonNull
    public static String getMessage(@NonNull ErrorCode errorCode) {
        return getMessage(errorCode, (Object[]) null, getCurrentLocale());
    }

    /**
     * 获取错误码消息（指定 Locale）
     *
     * @param errorCode 错误码
     * @param locale 语言
     * @return 国际化消息
     */
    @NonNull
    public static String getMessage(@NonNull ErrorCode errorCode, @Nullable Locale locale) {
        return getMessage(errorCode, (Object[]) null, locale);
    }

    /**
     * 获取错误码消息（带参数，使用当前上下文 Locale）
     *
     * @param errorCode 错误码
     * @param args 消息参数
     * @return 国际化消息
     */
    @NonNull
    public static String getMessage(@NonNull ErrorCode errorCode, @Nullable Object[] args) {
        return getMessage(errorCode, args, getCurrentLocale());
    }

    /**
     * 获取错误码消息（带参数，指定 Locale）
     *
     * @param errorCode 错误码
     * @param args 消息参数，可为 null
     * @param locale 语言，为 null 时使用默认语言
     * @return 国际化消息
     */
    @NonNull
    public static String getMessage(
            @NonNull ErrorCode errorCode, @Nullable Object[] args, @Nullable Locale locale) {
        if (messageSource == null) {
            // MessageSource 未初始化时返回默认消息
            return errorCode.getMessage();
        }

        String code = String.valueOf(errorCode.getCode());
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();

        try {
            return messageSource.getMessage(code, args, errorCode.getMessage(), effectiveLocale);
        } catch (Exception e) {
            // 解析失败时返回默认消息
            return errorCode.getMessage();
        }
    }

    /**
     * 获取当前上下文的 Locale
     * 优先使用 Spring LocaleContextHolder，其次使用系统默认
     */
    @NonNull
    private static Locale getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.getDefault();
    }
}
