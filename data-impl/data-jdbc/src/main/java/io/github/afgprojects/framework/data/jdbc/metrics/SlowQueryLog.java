package io.github.afgprojects.framework.data.jdbc.metrics;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.StringJoiner;

/**
 * 慢查询日志记录
 * <p>
 * 记录慢查询的详细信息，包括 SQL 语句、执行时间、参数等
 *
 * @since 1.0.0
 */
public record SlowQueryLog(
        /**
         * SQL 语句
         */
        @NonNull String sql,

        /**
         * SQL 操作类型
         */
        @NonNull SqlOperationType operationType,

        /**
         * 执行时间
         */
        @NonNull Duration duration,

        /**
         * 参数列表（可能为 null）
         */
        @Nullable Object[] params,

        /**
         * 实体类名（可能为 null）
         */
        @Nullable String entityName,

        /**
         * 异常信息（如果执行失败）
         */
        @Nullable String errorMessage
) {

    /**
     * 创建慢查询日志
     *
     * @param sql           SQL 语句
     * @param operationType 操作类型
     * @param duration      执行时间
     * @param params        参数列表
     * @param entityName    实体类名
     * @return 慢查询日志
     */
    @NonNull
    public static SlowQueryLog of(@NonNull String sql,
                                  @NonNull SqlOperationType operationType,
                                  @NonNull Duration duration,
                                  @Nullable Object[] params,
                                  @Nullable String entityName) {
        return new SlowQueryLog(sql, operationType, duration, params, entityName, null);
    }

    /**
     * 创建执行失败的慢查询日志
     *
     * @param sql           SQL 语句
     * @param operationType 操作类型
     * @param duration      执行时间
     * @param params        参数列表
     * @param entityName    实体类名
     * @param errorMessage  异常信息
     * @return 慢查询日志
     */
    @NonNull
    public static SlowQueryLog ofError(@NonNull String sql,
                                       @NonNull SqlOperationType operationType,
                                       @NonNull Duration duration,
                                       @Nullable Object[] params,
                                       @Nullable String entityName,
                                       @NonNull String errorMessage) {
        return new SlowQueryLog(sql, operationType, duration, params, entityName, errorMessage);
    }

    /**
     * 获取执行时间（毫秒）
     *
     * @return 执行时间（毫秒）
     */
    public long durationMillis() {
        return duration.toMillis();
    }

    /**
     * 是否为执行失败的查询
     *
     * @return 是否失败
     */
    public boolean isError() {
        return errorMessage != null;
    }

    /**
     * 格式化参数列表（用于日志输出）
     *
     * @return 格式化后的参数字符串
     */
    @NonNull
    public String formatParams() {
        if (params == null || params.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Object param : params) {
            if (param == null) {
                joiner.add("null");
            } else if (param instanceof String) {
                // 字符串参数加引号
                joiner.add("'" + param + "'");
            } else {
                joiner.add(param.toString());
            }
        }
        return joiner.toString();
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SlowQueryLog{");
        sb.append("type=").append(operationType.getName());
        sb.append(", duration=").append(durationMillis()).append("ms");
        if (entityName != null) {
            sb.append(", entity=").append(entityName);
        }
        sb.append(", sql=").append(sql);
        if (params != null && params.length > 0) {
            sb.append(", params=").append(formatParams());
        }
        if (errorMessage != null) {
            sb.append(", error=").append(errorMessage);
        }
        sb.append("}");
        return sb.toString();
    }
}
