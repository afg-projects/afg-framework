package io.github.afgprojects.framework.core.model.exception;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 错误码接口
 * 枚举实现此接口即可定义错误码
 */
public interface ErrorCode {

    /**
     * 获取数字错误码
     * @return 数字错误码，如 10001、20001 等
     */
    int getCode();

    /**
     * 获取错误消息
     * @return 错误消息
     */
    String getMessage();

    /**
     * 获取错误分类
     * @return 错误分类
     */
    default ErrorCategory getCategory() {
        return ErrorCategory.BUSINESS;
    }

    /**
     * 获取格式化的错误码字符串
     * @return 格式为 "E{code}"，如 "E10001"
     */
    default String formatCode() {
        return "E" + getCode();
    }

    /**
     * 获取指定语言的错误消息
     * @param locale 语言，为 null 时使用默认语言
     * @return 国际化消息
     */
    @NonNull
    default String getMessage(@Nullable Locale locale) {
        return getMessage(null, locale);
    }

    /**
     * 获取带参数的指定语言错误消息
     * @param args 消息参数，用于消息模板中的占位符替换
     * @param locale 语言，为 null 时使用默认语言
     * @return 国际化消息
     */
    @NonNull
    default String getMessage(@Nullable Object[] args, @Nullable Locale locale) {
        return ErrorCodeMessageSource.getMessage(this, args, locale);
    }
}
