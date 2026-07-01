package io.github.afgprojects.framework.data.jdbc.metrics;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 原始 SQL 安全守卫
 * <p>
 * 在 {@code DataManager.executeUpdate()} 和 {@code DataManager.queryForList()} 等原始 SQL 方法执行前，
 * 对 SQL 语句进行安全检查，防止 DDL 操作和明显的注入模式。
 * <p>
 * <b>安全模式：</b>
 * <ul>
 *   <li><b>MODERATE</b>（默认）：允许 SELECT/INSERT/UPDATE/DELETE，拒绝 DDL（DROP/ALTER/CREATE/TRUNCATE）</li>
 *   <li><b>STRICT</b>：只允许 SELECT</li>
 *   <li><b>PERMISSIVE</b>：允许一切，但记录审计日志</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Getter
@Slf4j
public final class RawSqlSecurityGuard {

    /**
     * 安全模式
     */
    public enum Mode {
        /** 允许 SELECT/INSERT/UPDATE/DELETE，拒绝 DDL */
        MODERATE,
        /** 只允许 SELECT */
        STRICT,
        /** 允许一切，但记录审计日志 */
        PERMISSIVE
    }

    /**
     * DDL 关键字列表
     */
    private static final String[] DDL_KEYWORDS = {
            "DROP", "ALTER", "CREATE", "TRUNCATE", "GRANT", "REVOKE"
    };

    /**
     * DML 关键字列表（MODERATE 模式允许）
     */
    private static final String[] DML_KEYWORDS = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "WITH"
    };

    /**
     * 当前安全模式
     */
    private Mode mode = Mode.MODERATE;

    public RawSqlSecurityGuard() {
    }

    public RawSqlSecurityGuard(Mode mode) {
        this.mode = mode;
    }

    /**
     * 设置安全模式
     *
     * @param mode 安全模式
     */
    public void setMode(@NonNull Mode mode) {
        this.mode = mode;
    }

    /**
     * 检查 SQL 语句安全性
     * <p>
     * 根据当前安全模式，对 SQL 语句进行前置检查。
     *
     * @param sql         SQL 语句
     * @param callerInfo  调用方信息（类名，用于审计日志）
     * @throws SecurityException 如果 SQL 语句违反当前安全模式
     */
    public void check(@Nullable String sql, @NonNull String callerInfo) {
        if (sql == null || sql.isBlank()) {
            return;
        }

        String trimmedSql = sql.trim();
        String firstKeyword = extractFirstKeyword(trimmedSql);

        // 记录审计日志
        logAudit(sql, callerInfo, firstKeyword);

        switch (mode) {
            case STRICT -> checkStrict(firstKeyword, sql);
            case MODERATE -> checkModerate(trimmedSql, firstKeyword, sql);
            case PERMISSIVE -> { /* 允许一切，仅审计 */ }
        }
    }

    /**
     * STRICT 模式检查：只允许 SELECT
     */
    private void checkStrict(String firstKeyword, String sql) {
        if (!"SELECT".equalsIgnoreCase(firstKeyword) && !"WITH".equalsIgnoreCase(firstKeyword)) {
            throw new SecurityException(
                    "Raw SQL rejected in STRICT mode: only SELECT queries are allowed. "
                    + "First keyword: " + firstKeyword + ". SQL: " + truncate(sql, 200));
        }
    }

    /**
     * MODERATE 模式检查：允许 CRUD，拒绝 DDL
     */
    private void checkModerate(String trimmedSql, String firstKeyword, String sql) {
        // 检查是否为 DDL
        for (String ddlKeyword : DDL_KEYWORDS) {
            if (ddlKeyword.equalsIgnoreCase(firstKeyword)) {
                throw new SecurityException(
                        "Raw SQL rejected in MODERATE mode: DDL operations are not allowed. "
                        + "First keyword: " + firstKeyword + ". SQL: " + truncate(sql, 200));
            }
        }

        // 检查是否包含多语句（分号分隔）
        // 注意：简单的分号出现在字符串常量中是合法的，这里只检查裸分号
        String upperSql = trimmedSql.toUpperCase();
        if (upperSql.indexOf(';') > 0) {
            // 检查分号后是否有 SQL 关键字（多语句注入模式）
            String afterSemicolon = upperSql.substring(upperSql.indexOf(';') + 1).trim();
            String nextKeyword = extractFirstKeyword(afterSemicolon);
            if (!nextKeyword.isEmpty()) {
                throw new SecurityException(
                        "Raw SQL rejected in MODERATE mode: multi-statement SQL is not allowed. "
                        + "SQL: " + truncate(sql, 200));
            }
        }
    }

    /**
     * 记录审计日志
     */
    private void logAudit(String sql, String callerInfo, String firstKeyword) {
        log.debug("Raw SQL audit: caller={}, keyword={}, sql={}",
                callerInfo, firstKeyword, truncate(sql, 200));
    }

    /**
     * 提取 SQL 语句的第一个关键字
     */
    private static String extractFirstKeyword(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        String trimmed = sql.trim();
        // 找第一个空格之前的单词
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            return trimmed.substring(0, spaceIndex).toUpperCase();
        }
        return trimmed.toUpperCase();
    }

    /**
     * 截断字符串
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}