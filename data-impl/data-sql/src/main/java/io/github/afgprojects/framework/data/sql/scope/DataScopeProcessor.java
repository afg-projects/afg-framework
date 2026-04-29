package io.github.afgprojects.framework.data.sql.scope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据权限处理器
 * <p>
 * 负责解析数据权限中的占位符，将其替换为实际的 SQL 表达式。
 * <p>
 * 支持的占位符：
 * <ul>
 *   <li>#{currentUserId} - 当前用户ID</li>
 *   <li>#{currentUserDeptIds} - 当前用户部门ID列表</li>
 *   <li>#{currentUserDeptAndChildIds} - 当前用户部门及子部门ID列表</li>
 *   <li>#{currentTenantId} - 当前租户ID</li>
 *   <li>#{currentDeptId} - 当前用户部门ID</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * DataScopeProcessor processor = new DataScopeProcessor(contextProvider);
 *
 * // 解析占位符表达式
 * Expression result = processor.resolvePlaceholder("#{currentUserId}");
 * // 结果: LongValue(123)
 *
 * // 解析带占位符的字符串
 * String sql = processor.resolvePlaceholders("user_id = #{currentUserId}");
 * // 结果: "user_id = 123"
 * </pre>
 */
public class DataScopeProcessor {

    /**
     * 占位符模式：#{...}
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{([^}]+)}");

    /**
     * 当前用户ID占位符
     */
    public static final String CURRENT_USER_ID = "#{currentUserId}";

    /**
     * 当前用户部门ID占位符
     */
    public static final String CURRENT_DEPT_ID = "#{currentDeptId}";

    /**
     * 当前用户部门ID列表占位符（仅本部门）
     */
    public static final String CURRENT_USER_DEPT_IDS = "#{currentUserDeptIds}";

    /**
     * 当前用户部门及子部门ID列表占位符
     */
    public static final String CURRENT_USER_DEPT_AND_CHILD_IDS = "#{currentUserDeptAndChildIds}";

    /**
     * 当前租户ID占位符
     */
    public static final String CURRENT_TENANT_ID = "#{currentTenantId}";

    /**
     * 数据权限上下文提供者
     */
    private final DataScopeContextProvider contextProvider;

    /**
     * 创建数据权限处理器
     *
     * @param contextProvider 上下文提供者
     */
    public DataScopeProcessor(@NonNull DataScopeContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    /**
     * 解析占位符表达式
     * <p>
     * 将占位符转换为实际的 JSqlParser 表达式。
     *
     * @param placeholder 占位符字符串，如 "#{currentUserId}"
     * @return 解析后的表达式，如果无法解析则返回原始字符串值
     */
    public @NonNull Expression resolvePlaceholder(@NonNull String placeholder) {
        // 提取占位符内容
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);
        if (!matcher.matches()) {
            // 不是占位符，返回原始字符串
            return new StringValue(placeholder);
        }

        String placeholderName = matcher.group(1);
        DataScopeUserContext context = getUserContext();

        return switch (placeholderName) {
            case "currentUserId" -> resolveUserId(context);
            case "currentDeptId" -> resolveDeptId(context);
            case "currentUserDeptIds" -> resolveDeptIds(context, false);
            case "currentUserDeptAndChildIds" -> resolveDeptIds(context, true);
            case "currentTenantId" -> resolveTenantId(context);
            default -> new StringValue(placeholder); // 未知占位符，保持原样
        };
    }

    /**
     * 解析字符串中的所有占位符
     * <p>
     * 将字符串中的占位符替换为实际值。
     *
     * @param text 包含占位符的文本
     * @return 替换后的文本
     */
    public @NonNull String resolvePlaceholders(@NonNull String text) {
        DataScopeUserContext context = getUserContext();

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            String replacement = resolvePlaceholderValue(placeholderName, context);
            matcher.appendReplacement(result, replacement != null ? replacement : matcher.group(0));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 检查表达式是否包含占位符
     *
     * @param expression 表达式
     * @return 是否包含占位符
     */
    public boolean hasPlaceholder(@Nullable Expression expression) {
        if (expression == null) {
            return false;
        }

        if (expression instanceof StringValue stringValue) {
            return PLACEHOLDER_PATTERN.matcher(stringValue.getValue()).matches();
        }

        return false;
    }

    /**
     * 检查字符串是否是占位符
     *
     * @param value 字符串值
     * @return 是否是占位符
     */
    public boolean isPlaceholder(@Nullable String value) {
        if (value == null) {
            return false;
        }
        return PLACEHOLDER_PATTERN.matcher(value).matches();
    }

    /**
     * 获取用户上下文
     *
     * @return 用户上下文，如果无法获取则返回空上下文
     */
    public DataScopeUserContext provideUserContext() {
        DataScopeUserContext context = contextProvider.provide();
        return context != null ? context : DataScopeUserContext.empty();
    }

    /**
     * 获取用户上下文
     *
     * @return 用户上下文，如果无法获取则返回空上下文
     */
    private DataScopeUserContext getUserContext() {
        DataScopeUserContext context = contextProvider.provide();
        return context != null ? context : DataScopeUserContext.empty();
    }

    /**
     * 解析用户ID表达式
     */
    private Expression resolveUserId(DataScopeUserContext context) {
        Long userId = context.getUserId();
        if (userId != null) {
            return new LongValue(userId);
        }
        // 如果无法获取用户ID，返回一个不可能匹配的值
        return new LongValue(Long.MIN_VALUE);
    }

    /**
     * 解析部门ID表达式
     */
    private Expression resolveDeptId(DataScopeUserContext context) {
        Long deptId = context.getDeptId();
        if (deptId != null) {
            return new LongValue(deptId);
        }
        // 如果无法获取部门ID，返回一个不可能匹配的值
        return new LongValue(Long.MIN_VALUE);
    }

    /**
     * 解析部门ID列表表达式
     *
     * @param context       用户上下文
     * @param includeChilds 是否包含子部门
     * @return 部门ID列表表达式
     */
    private Expression resolveDeptIds(DataScopeUserContext context, boolean includeChilds) {
        Set<Long> deptIds = context.getAccessibleDeptIds();

        if (deptIds == null || deptIds.isEmpty()) {
            // 如果没有可访问部门，尝试使用本部门
            Long deptId = context.getDeptId();
            if (deptId != null) {
                return new LongValue(deptId);
            }
            // 返回一个不可能匹配的值
            return new LongValue(Long.MIN_VALUE);
        }

        // 将部门ID列表转换为表达式列表
        List<Expression> expressions = deptIds.stream()
                .map(LongValue::new)
                .map(Expression.class::cast)
                .toList();

        return new ParenthesedExpressionList<>(new ExpressionList<>(expressions));
    }

    /**
     * 解析租户ID表达式
     */
    private Expression resolveTenantId(DataScopeUserContext context) {
        Long tenantId = context.getTenantId();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        // 如果无法获取租户ID，返回一个不可能匹配的值
        return new LongValue(Long.MIN_VALUE);
    }

    /**
     * 解析占位符值为字符串
     *
     * @param placeholderName 占位符名称
     * @param context         用户上下文
     * @return 字符串值，如果无法解析则返回 null
     */
    private @Nullable String resolvePlaceholderValue(String placeholderName, DataScopeUserContext context) {
        return switch (placeholderName) {
            case "currentUserId" -> {
                Long userId = context.getUserId();
                yield userId != null ? String.valueOf(userId) : null;
            }
            case "currentDeptId" -> {
                Long deptId = context.getDeptId();
                yield deptId != null ? String.valueOf(deptId) : null;
            }
            case "currentUserDeptIds", "currentUserDeptAndChildIds" -> {
                Set<Long> deptIds = context.getAccessibleDeptIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    yield deptIds.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse(null);
                }
                Long deptId = context.getDeptId();
                yield deptId != null ? String.valueOf(deptId) : null;
            }
            case "currentTenantId" -> {
                Long tenantId = context.getTenantId();
                yield tenantId != null ? String.valueOf(tenantId) : null;
            }
            default -> null;
        };
    }
}