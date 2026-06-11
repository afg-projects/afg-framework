package io.github.afgprojects.framework.commons.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字符串工具类。
 * <p>提供常用字符串判空、截断、拼接、拆分操作。
 *
 * <p>使用示例：
 * <pre>{@code
 * StringUtils.isBlank("")                  // → true
 * StringUtils.truncate("hello world", 5)   // → "hello..."
 * StringUtils.join(list, ",")              // → "a,b,c"
 * StringUtils.splitAndTrim("a, b, c", ",") // → ["a", "b", "c"]
 * }</pre>
 */
public final class StringUtils {

    private StringUtils() {
        // 工具类禁止实例化
    }

    /**
     * 判断字符串是否为 null、空或纯空白
     *
     * @param text 字符串
     * @return true 如果字符串为 null、空或纯空白
     */
    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * 判断字符串是否不为 null 且不为空白
     *
     * @param text 字符串
     * @return true 如果字符串不为 null 且不为空白
     */
    public static boolean isNotBlank(String text) {
        return !isBlank(text);
    }

    /**
     * 截断字符串到指定长度，超出部分用 "..." 替代
     *
     * @param text       原始字符串
     * @param maxLength  最大长度（不含 "..."）
     * @return 截断后的字符串
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 用分隔符拼接字符串列表
     *
     * @param parts    字符串列表
     * @param separator 分隔符
     * @return 拼接后的字符串
     */
    public static String join(List<String> parts, String separator) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        return parts.stream().collect(Collectors.joining(separator));
    }

    /**
     * 用分隔符拼接字符串数组
     *
     * @param parts     字符串数组
     * @param separator 分隔符
     * @return 拼接后的字符串
     */
    public static String join(String[] parts, String separator) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        return Arrays.stream(parts).collect(Collectors.joining(separator));
    }

    /**
     * 按分隔符拆分字符串并去除每个元素的前后空白
     *
     * @param text      原始字符串
     * @param separator 分隔符
     * @return 去空白后的字符串列表
     */
    public static List<String> splitAndTrim(String text, String separator) {
        if (isBlank(text)) {
            return List.of();
        }
        return Arrays.stream(text.split(separator))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}