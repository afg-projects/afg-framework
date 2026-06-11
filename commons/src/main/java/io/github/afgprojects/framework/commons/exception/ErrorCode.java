package io.github.afgprojects.framework.commons.exception;

import java.text.MessageFormat;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 错误码接口。
 * <p>枚举实现此接口即可定义错误码。支持国际化消息和参数化模板。
 *
 * <p>消息模板使用 {@link MessageFormat} 语法，占位符为 {0}、{1} 等：
 * <pre>{@code
 * // 在枚举中定义带占位符的消息
 * ENTITY_NOT_FOUND(11000, "实体 {0} 不存在", ErrorCategory.BUSINESS)
 *
 * // 使用时传入参数
 * errorCode.getMessage(new Object[]{"User"}, null)  // → "实体 User 不存在"
 * }</pre>
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
     * 获取指定语言的错误消息。
     * <p>默认实现返回 {@link #getMessage()}，子类可覆盖以支持 i18n。
     *
     * @param locale 语言，为 null 时使用默认语言
     * @return 国际化消息
     */
    @NonNull
    default String getMessage(@Nullable Locale locale) {
        return getMessage();
    }

    /**
     * 获取带参数的指定语言错误消息。
     * <p>使用 {@link MessageFormat} 对消息模板进行参数替换。
     * 消息模板中可使用 {0}、{1} 等占位符。
     *
     * @param args 消息参数，用于消息模板中的占位符替换；为 null 或空时不做替换
     * @param locale 语言，为 null 时使用默认语言
     * @return 格式化后的消息
     */
    @NonNull
    default String getMessage(@Nullable Object[] args, @Nullable Locale locale) {
        String template = getMessage(locale);
        if (args == null || args.length == 0) {
            return template;
        }
        return MessageFormat.format(template, args);
    }
}
