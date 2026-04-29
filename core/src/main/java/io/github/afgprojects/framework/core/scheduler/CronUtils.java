package io.github.afgprojects.framework.core.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Cron 表达式工具类
 *
 * <p>提供简单的 Cron 表达式解析和计算功能
 *
 * <h3>支持的 Cron 格式</h3>
 * <pre>
 * ┌───────────── second (0-59)
 * │ ┌───────────── minute (0-59)
 * │ │ ┌───────────── hour (0-23)
 * │ │ │ ┌───────────── day of month (1-31)
 * │ │ │ │ ┌───────────── month (1-12)
 * │ │ │ │ │ ┌───────────── day of week (0-6, 0=Sunday)
 * │ │ │ │ │ │
 * * * * * * *
 * </pre>
 *
 * @since 1.0.0
 */
public final class CronUtils {

    private CronUtils() {
        // 工具类，不允许实例化
    }

    // Cron 字段位置
    private static final int SECOND = 0;
    private static final int MINUTE = 1;
    private static final int HOUR = 2;
    private static final int DAY_OF_MONTH = 3;
    private static final int MONTH = 4;
    private static final int DAY_OF_WEEK = 5;

    // 默认值
    private static final int[] DEFAULTS = {0, 0, 0, 1, 1, 0};
    private static final int[] MAX_VALUES = {59, 59, 23, 31, 12, 6};

    /**
     * 获取下一次执行的延迟时间
     *
     * @param cron Cron 表达式
     * @return 距离下一次执行的延迟时间
     */
    @NonNull
    public static Duration getNextExecutionDelay(@NonNull String cron) {
        ZonedDateTime next = getNextExecutionTime(cron, ZonedDateTime.now());
        return Duration.between(ZonedDateTime.now(), next);
    }

    /**
     * 获取下一次执行时间
     *
     * @param cron Cron 表达式
     * @param from  起始时间
     * @return 下一次执行时间
     */
    @NonNull
    public static ZonedDateTime getNextExecutionTime(@NonNull String cron, @NonNull ZonedDateTime from) {
        CronExpression expression = parse(cron);
        return expression.nextExecutionTime(from);
    }

    /**
     * 获取执行周期
     *
     * <p>对于固定周期的 Cron 表达式，返回周期时长
     *
     * @param cron Cron 表达式
     * @return 周期时长
     */
    @NonNull
    public static Duration getPeriod(@NonNull String cron) {
        CronExpression expression = parse(cron);

        // 计算连续两次执行的时间差
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next1 = expression.nextExecutionTime(now);
        ZonedDateTime next2 = expression.nextExecutionTime(next1);

        return Duration.between(next1, next2);
    }

    /**
     * 验证 Cron 表达式是否有效
     *
     * @param cron Cron 表达式
     * @return 是否有效
     */
    public static boolean isValid(@Nullable String cron) {
        if (cron == null || cron.isEmpty()) {
            return false;
        }

        try {
            parse(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 Cron 表达式
     *
     * @param cron Cron 表达式
     * @return CronExpression 对象
     */
    @NonNull
    public static CronExpression parse(@NonNull String cron) {
        String[] fields = cron.trim().split("\\s+");

        if (fields.length != 6) {
            throw new IllegalArgumentException(
                "Invalid cron expression: expected 6 fields but got " + fields.length);
        }

        return new CronExpression(
            parseField(fields[SECOND], 0, 59),
            parseField(fields[MINUTE], 0, 59),
            parseField(fields[HOUR], 0, 23),
            parseField(fields[DAY_OF_MONTH], 1, 31),
            parseField(fields[MONTH], 1, 12),
            parseField(fields[DAY_OF_WEEK], 0, 6)
        );
    }

    /**
     * 解析单个字段
     */
    private static CronField parseField(String field, int min, int max) {
        if ("*".equals(field) || "?".equals(field)) {
            return new CronField(min, max, true);
        }

        // 处理 */n 或 n/m 格式（步进表达式）
        if (field.startsWith("*/")) {
            int step = Integer.parseInt(field.substring(2));
            return new CronField(min, max, step);
        }
        if (field.contains("/") && !field.startsWith("*")) {
            String[] parts = field.split("/");
            int start = Integer.parseInt(parts[0]);
            int step = Integer.parseInt(parts[1]);
            return new CronField(start, max, step);
        }

        // 处理 n-m 格式
        if (field.contains("-")) {
            String[] range = field.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return new CronField(start, end, false);
        }

        // 处理 n,m,o 格式
        if (field.contains(",")) {
            String[] values = field.split(",");
            int[] nums = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                nums[i] = Integer.parseInt(values[i]);
            }
            return new CronField(nums);
        }

        // 处理单个数字
        int value = Integer.parseInt(field);
        return new CronField(value, value, false);
    }

    /**
     * Cron 表达式对象
     */
    public static class CronExpression {
        private final CronField second;
        private final CronField minute;
        private final CronField hour;
        private final CronField dayOfMonth;
        private final CronField month;
        private final CronField dayOfWeek;

        CronExpression(CronField second, CronField minute, CronField hour,
                       CronField dayOfMonth, CronField month, CronField dayOfWeek) {
            this.second = second;
            this.minute = minute;
            this.hour = hour;
            this.dayOfMonth = dayOfMonth;
            this.month = month;
            this.dayOfWeek = dayOfWeek;
        }

        /**
         * 计算下一次执行时间
         */
        @NonNull
        public ZonedDateTime nextExecutionTime(@NonNull ZonedDateTime from) {
            ZonedDateTime next = from.withNano(0).plusSeconds(1);
            ZonedDateTime result = null;

            // 最多尝试 366 天
            for (int i = 0; i < 366 && result == null; i++) {
                // 调整月份
                if (!month.matches(next.getMonthValue())) {
                    int nextMonth = next.getMonthValue() + 1;
                    if (nextMonth > 12) {
                        nextMonth = 1;
                    }
                    next = next.withMonth(nextMonth)
                        .withDayOfMonth(1)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0);
                    continue;
                }

                // 调整日
                if (!dayOfMonth.matches(next.getDayOfMonth()) ||
                    !dayOfWeek.matches(next.getDayOfWeek().getValue() % 7)) {
                    next = next.plusDays(1)
                        .withHour(0)
                        .withMinute(0)
                        .withSecond(0);
                    continue;
                }

                // 调整小时
                if (!hour.matches(next.getHour())) {
                    int nextHour = next.getHour() + 1;
                    if (nextHour > 23) {
                        next = next.plusDays(1).withHour(0).withMinute(0).withSecond(0);
                    } else {
                        next = next.withHour(nextHour).withMinute(0).withSecond(0);
                    }
                    continue;
                }

                // 调整分钟
                if (!minute.matches(next.getMinute())) {
                    int nextMinute = next.getMinute() + 1;
                    if (nextMinute > 59) {
                        next = next.withHour(next.getHour() + 1).withMinute(0).withSecond(0);
                    } else {
                        next = next.withMinute(nextMinute).withSecond(0);
                    }
                    continue;
                }

                // 调整秒
                if (!second.matches(next.getSecond())) {
                    next = next.plusSeconds(1);
                    continue;
                }

                // 找到匹配的时间
                result = next;
            }

            if (result != null) {
                return result;
            }

            throw new IllegalStateException("Unable to find next execution time within 366 days");
        }
    }

    /**
     * Cron 字段
     */
    public static class CronField {
        private final int[] values;
        private final boolean isAll;

        CronField(int min, int max, boolean isAll) {
            this.isAll = isAll;
            if (isAll) {
                this.values = null;
            } else {
                this.values = new int[max - min + 1];
                for (int i = 0; i < values.length; i++) {
                    values[i] = min + i;
                }
            }
        }

        CronField(int min, int max, int step) {
            this.isAll = false;
            int count = (max - min) / step + 1;
            this.values = new int[count];
            for (int i = 0; i < count; i++) {
                values[i] = min + i * step;
            }
        }

        CronField(int[] values) {
            this.isAll = false;
            this.values = values;
        }

        boolean matches(int value) {
            if (isAll) {
                return true;
            }

            for (int v : values) {
                if (v == value) {
                    return true;
                }
            }

            return false;
        }
    }
}
