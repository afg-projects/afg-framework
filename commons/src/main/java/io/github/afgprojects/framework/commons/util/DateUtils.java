package io.github.afgprojects.framework.commons.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类。
 * <p>提供常用日期格式化、解析、比较操作。
 *
 * <p>使用示例：
 * <pre>{@code
 * DateUtils.format(Instant.now(), "yyyy-MM-dd")           // → "2026-06-11"
 * DateUtils.parse("2026-06-11", "yyyy-MM-dd")             // → LocalDate
 * DateUtils.between(start, end, Instant.now())             // → true
 * DateUtils.isExpired(Instant.now().minusSeconds(60))      // → true
 * }</pre>
 */
public final class DateUtils {

    private DateUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将 Instant 格式化为字符串
     *
     * @param instant 时间戳
     * @param pattern 格式模式
     * @return 格式化后的字符串
     */
    public static String format(Instant instant, String pattern) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * 将 LocalDateTime 格式化为字符串
     *
     * @param dateTime 日期时间
     * @param pattern  格式模式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern).format(dateTime);
    }

    /**
     * 将字符串解析为 LocalDate
     *
     * @param text    日期字符串
     * @param pattern 格式模式
     * @return LocalDate
     */
    public static LocalDate parseLocalDate(String text, String pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将字符串解析为 LocalDateTime
     *
     * @param text    日期时间字符串
     * @param pattern 格式模式
     * @return LocalDateTime
     */
    public static LocalDateTime parseLocalDateTime(String text, String pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 判断时间是否在范围内
     *
     * @param start 范围起始（含）
     * @param end   范围结束（含）
     * @param value 待判断时间
     * @return true 如果 value 在 [start, end] 范围内
     */
    public static boolean between(Instant start, Instant end, Instant value) {
        if (start == null || end == null || value == null) {
            return false;
        }
        return !value.isBefore(start) && !value.isAfter(end);
    }

    /**
     * 判断时间是否已过期（早于当前时间）
     *
     * @param time 待判断时间
     * @return true 如果 time 早于当前时间
     */
    public static boolean isExpired(Instant time) {
        if (time == null) {
            return true;
        }
        return time.isBefore(Instant.now());
    }

    /**
     * 判断时间是否已过期（早于指定参考时间）
     *
     * @param time      待判断时间
     * @param reference 参考时间
     * @return true 如果 time 早于 reference
     */
    public static boolean isExpired(Instant time, Instant reference) {
        if (time == null || reference == null) {
            return true;
        }
        return time.isBefore(reference);
    }
}