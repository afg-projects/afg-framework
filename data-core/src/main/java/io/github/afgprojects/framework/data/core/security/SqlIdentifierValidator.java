package io.github.afgprojects.framework.data.core.security;

import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 标识符统一验证器
 * <p>
 * 集中管理所有 SQL 标识符（列名、表名、别名、CTE 名称等）的验证逻辑，
 * 消除散落在各 Builder 和 Converter 中的重复正则定义。
 * <p>
 * <b>安全性说明：</b>所有标识符在拼接到 SQL 之前必须经过验证，防止 SQL 注入。
 * 验证规则基于白名单：只允许字母、数字、下划线和点（用于 table.column 格式）。
 *
 * @since 1.0.0
 */
public final class SqlIdentifierValidator {

    /**
     * 列名/字段名正则：字母/下划线开头，后跟字母/数字/下划线/点（支持 table.column 格式）
     */
    private static final Pattern COLUMN_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    /**
     * 表名正则：字母/下划线开头，后跟字母/数字/下划线/点/中划线
     * <p>
     * 比列名稍宽松，因为某些数据库支持中划线（需引用）
     */
    private static final Pattern TABLE_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.\\-]*$");

    /**
     * 别名正则：与列名相同规则
     */
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * CTE 名称正则：与别名相同规则
     */
    private static final Pattern CTE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * 简单标识符正则（不含点）：用于 INSERT/UPDATE/DELETE 的列名和表名
     */
    private static final Pattern SIMPLE_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * SQL 条件片段中禁止出现的关键字（DDL 和危险操作）
     */
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "DROP", "ALTER", "CREATE", "TRUNCATE", "GRANT", "REVOKE",
            "EXEC", "EXECUTE", "XP_", "SP_",
            "UNION", "INTERSECT", "EXCEPT",
            "INTO", "OUTFILE", "DUMPFILE",
            "LOAD_FILE", "BENCHMARK", "SLEEP", "WAITFOR",
            "INFORMATION_SCHEMA", "PG_CATALOG"
    );

    /**
     * 占位符模式：#{...}
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{[^}]+}");

    /**
     * SQL 条件片段中允许的安全操作符
     */
    private static final Pattern SAFE_CONDITION_TOKEN_PATTERN = Pattern.compile(
            // 占位符 #{...}
            "#\\{[^}]+}"
            // 或：列名（table.column 格式）
            + "|[a-zA-Z_][a-zA-Z0-9_.]*"
            // 或：数字字面量（整数和小数）
            + "|\\d+(?:\\.\\d+)?"
            // 或：SQL 操作符
            + "|(?:=|!=|<>|<|>|<=|>=)"
            // 或：SQL 关键字
            + "|(?:AND|OR|NOT|IN|NOT\\s+IN|BETWEEN|NOT\\s+BETWEEN|IS\\s+NULL|IS\\s+NOT\\s+NULL|LIKE|NOT\\s+LIKE)"
            // 或：括号和逗号
            + "|[(),]"
            // 或：空白
            + "|\\s+"
    );

    private SqlIdentifierValidator() {
        // 工具类，禁止实例化
    }

    /**
     * 验证列名/字段名合法性
     * <p>
     * 列名必须以字母或下划线开头，后跟字母、数字、下划线或点（支持 table.column 格式）。
     *
     * @param identifier 列名
     * @throws IllegalArgumentException 如果列名非法
     */
    public static void validateColumn(@NonNull String identifier) {
        validate(identifier, COLUMN_PATTERN, "column name");
    }

    /**
     * 验证表名合法性
     * <p>
     * 表名必须以字母或下划线开头，后跟字母、数字、下划线、点或中划线。
     *
     * @param identifier 表名
     * @throws IllegalArgumentException 如果表名非法
     */
    public static void validateTable(@NonNull String identifier) {
        validate(identifier, TABLE_PATTERN, "table name");
    }

    /**
     * 验证别名合法性
     * <p>
     * 别名必须以字母或下划线开头，后跟字母、数字或下划线。
     *
     * @param identifier 别名
     * @throws IllegalArgumentException 如果别名非法
     */
    public static void validateAlias(@NonNull String identifier) {
        validate(identifier, ALIAS_PATTERN, "alias");
    }

    /**
     * 验证 CTE 名称合法性
     * <p>
     * CTE 名称必须以字母或下划线开头，后跟字母、数字或下划线。
     *
     * @param identifier CTE 名称
     * @throws IllegalArgumentException 如果 CTE 名称非法
     */
    public static void validateCteName(@NonNull String identifier) {
        validate(identifier, CTE_NAME_PATTERN, "CTE name");
    }

    /**
     * 验证简单标识符合法性（不含点，用于 INSERT/UPDATE/DELETE）
     * <p>
     * 标识符必须以字母或下划线开头，后跟字母、数字或下划线。
     *
     * @param identifier 标识符
     * @throws IllegalArgumentException 如果标识符非法
     */
    public static void validateSimpleIdentifier(@NonNull String identifier) {
        validate(identifier, SIMPLE_IDENTIFIER_PATTERN, "identifier");
    }

    /**
     * 验证简单标识符合法性（含类型描述）
     *
     * @param identifier 标识符
     * @param type       标识符类型描述（用于错误消息）
     * @throws IllegalArgumentException 如果标识符非法
     */
    public static void validateSimpleIdentifier(@NonNull String identifier, @NonNull String type) {
        validate(identifier, SIMPLE_IDENTIFIER_PATTERN, type);
    }

    /**
     * 验证 SQL 条件片段安全性
     * <p>
     * 用于验证 {@code DataScope.customCondition} 等允许有限 SQL 片段的场景。
     * <p>
     * <b>白名单策略：</b>只允许以下内容：
     * <ul>
     *   <li>占位符：{@code #{...}}</li>
     *   <li>列名：匹配列名正则</li>
     *   <li>SQL 操作符：=, !=, &lt;&gt;, &lt;, &gt;, &lt;=, &gt;=</li>
     *   <li>SQL 关键字：AND, OR, NOT, IN, BETWEEN, IS NULL, IS NOT NULL, LIKE</li>
     *   <li>数字字面量</li>
     *   <li>括号和逗号</li>
     * </ul>
     * <p>
     * <b>拒绝：</b>子查询、注释、分号、UNION、DDL 关键字等。
     *
     * @param fragment SQL 条件片段
     * @throws IllegalArgumentException 如果片段包含不安全内容
     */
    public static void validateSqlConditionFragment(@NonNull String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return;
        }

        // 1. 检查禁止的关键字
        String upperFragment = fragment.toUpperCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperFragment.contains(keyword)) {
                throw new IllegalArgumentException(
                        "SQL condition fragment contains forbidden keyword '" + keyword + "': " + fragment);
            }
        }

        // 2. 检查注释和分号
        if (fragment.contains("--") || fragment.contains("/*") || fragment.contains(";")) {
            throw new IllegalArgumentException(
                    "SQL condition fragment contains comment or semicolon: " + fragment);
        }

        // 3. 检查子查询
        if (upperFragment.contains("SELECT") || upperFragment.contains("INSERT")
                || upperFragment.contains("UPDATE") || upperFragment.contains("DELETE")) {
            throw new IllegalArgumentException(
                    "SQL condition fragment contains subquery or DML: " + fragment);
        }

        // 4. 令牌化验证：确保所有令牌都是安全的
        // 将占位符替换为安全标记，然后逐令牌验证
        String sanitized = PLACEHOLDER_PATTERN.matcher(fragment).replaceAll("_placeholder_");
        StringBuilder remaining = new StringBuilder(sanitized);

        // 逐个匹配安全令牌并移除
        java.util.regex.Matcher matcher = SAFE_CONDITION_TOKEN_PATTERN.matcher(sanitized);
        while (matcher.find()) {
            // 安全令牌，继续
        }

        // 检查是否有未匹配的字符（不安全的内容）
        String reconstructed = matcher.reset().replaceAll("");
        if (!reconstructed.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL condition fragment contains unrecognized tokens: " + fragment);
        }
    }

    /**
     * 通用验证方法
     *
     * @param identifier 标识符
     * @param pattern    正则模式
     * @param type       标识符类型描述
     * @throws IllegalArgumentException 如果标识符非法
     */
    private static void validate(String identifier, Pattern pattern, String type) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException(type + " cannot be null or empty");
        }
        if (!pattern.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid " + type + ": '" + identifier + "'. "
                    + "Identifier must start with a letter or underscore, "
                    + "followed by letters, digits, underscores"
                    + (pattern == COLUMN_PATTERN ? ", or dots" : "")
                    + (pattern == TABLE_PATTERN ? ", dots, or hyphens" : "")
                    + ".");
        }
    }

    /**
     * 检查标识符是否为合法列名（不抛异常版本）
     *
     * @param identifier 标识符
     * @return 是否合法
     */
    public static boolean isValidColumn(@NonNull String identifier) {
        return identifier != null && !identifier.isEmpty() && COLUMN_PATTERN.matcher(identifier).matches();
    }

    /**
     * 检查标识符是否为合法表名（不抛异常版本）
     *
     * @param identifier 标识符
     * @return 是否合法
     */
    public static boolean isValidTable(@NonNull String identifier) {
        return identifier != null && !identifier.isEmpty() && TABLE_PATTERN.matcher(identifier).matches();
    }

    /**
     * 检查标识符是否为合法简单标识符（不抛异常版本）
     *
     * @param identifier 标识符
     * @return 是否合法
     */
    public static boolean isValidSimpleIdentifier(@NonNull String identifier) {
        return identifier != null && !identifier.isEmpty() && SIMPLE_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }
}
