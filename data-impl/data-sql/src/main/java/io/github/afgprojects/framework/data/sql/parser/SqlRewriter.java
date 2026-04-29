package io.github.afgprojects.framework.data.sql.parser;

import io.github.afgprojects.framework.data.core.sql.SqlRewriteContext;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider;
import io.github.afgprojects.framework.data.sql.scope.DataScopeProcessor;
import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * SQL 改写器
 * <p>
 * 支持数据权限、多租户、软删除等场景的 SQL 改写。
 * 数据权限支持以下占位符：
 * <ul>
 *   <li>#{currentUserId} - 当前用户ID</li>
 *   <li>#{currentDeptId} - 当前用户部门ID</li>
 *   <li>#{currentUserDeptIds} - 当前用户部门ID列表</li>
 *   <li>#{currentUserDeptAndChildIds} - 当前用户部门及子部门ID列表</li>
 *   <li>#{currentTenantId} - 当前租户ID</li>
 * </ul>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class SqlRewriter {

    private final Statement statement;
    private final SqlRewriteContext context;
    private final @Nullable DataScopeProcessor dataScopeProcessor;

    /**
     * 创建 SQL 改写器（不解析占位符）
     *
     * @param statement SQL 语句
     * @param context   改写上下文
     */
    public SqlRewriter(Statement statement, SqlRewriteContext context) {
        this(statement, context, null);
    }

    /**
     * 创建 SQL 改写器（支持占位符解析）
     *
     * @param statement           SQL 语句
     * @param context             改写上下文
     * @param contextProvider 数据权限上下文提供者
     */
    public SqlRewriter(Statement statement, SqlRewriteContext context,
                       @Nullable DataScopeContextProvider contextProvider) {
        this.statement = statement;
        this.context = context;
        this.dataScopeProcessor = contextProvider != null
                ? new DataScopeProcessor(contextProvider)
                : null;
    }

    public @NonNull String rewrite() {
        Expression additionalConditions = buildAdditionalConditions();
        if (additionalConditions == null) {
            return statement.toString();
        }

        if (statement instanceof Select select) {
            PlainSelect plainSelect = select.getPlainSelect();
            if (plainSelect != null) {
                Expression where = plainSelect.getWhere();
                if (where != null) {
                    plainSelect.setWhere(new AndExpression(where, additionalConditions));
                } else {
                    plainSelect.setWhere(additionalConditions);
                }
            }
        } else if (statement instanceof Update update) {
            Expression where = update.getWhere();
            if (where != null) {
                update.setWhere(new AndExpression(where, additionalConditions));
            } else {
                update.setWhere(additionalConditions);
            }
        } else if (statement instanceof Delete delete) {
            Expression where = delete.getWhere();
            if (where != null) {
                delete.setWhere(new AndExpression(where, additionalConditions));
            } else {
                delete.setWhere(additionalConditions);
            }
        }

        return statement.toString();
    }

    private Expression buildAdditionalConditions() {
        Expression result = null;

        if (!context.isIgnoreTenant() && context.getTenantId() != null) {
            Expression tenantCondition = createEqualsCondition("tenant_id", context.getTenantId());
            result = appendCondition(result, tenantCondition);
        }

        if (context.isSoftDeleteFilter()) {
            String column = context.getSoftDeleteColumn();
            String value = context.getDeletedValue();
            if (column != null && value != null) {
                Expression softDeleteCondition = createNotEqualsCondition(column, value);
                result = appendCondition(result, softDeleteCondition);
            }
        }

        if (!context.isIgnoreDataScope()) {
            List<DataScope> dataScopes = context.getDataScopes();
            for (DataScope scope : dataScopes) {
                Expression scopeCondition = buildDataScopeCondition(scope);
                if (scopeCondition != null) {
                    result = appendCondition(result, scopeCondition);
                }
            }
        }

        return result;
    }

    private Expression buildDataScopeCondition(DataScope scope) {
        String column = scope.column();
        String alias = context.getTableAlias(scope.table());
        String fullColumn = alias != null ? alias + "." + column : column;

        // 如果有 DataScopeProcessor，使用它来解析占位符
        if (dataScopeProcessor != null) {
            return buildDataScopeConditionWithProcessor(scope, fullColumn);
        }

        // 否则使用占位符字符串（旧逻辑，保持兼容）
        return switch (scope.scopeType()) {
            case ALL -> null;
            case SELF -> createEqualsCondition(fullColumn, "#{currentUserId}");
            case DEPT -> createEqualsCondition(fullColumn, "#{currentDeptId}");
            case DEPT_AND_CHILD -> createInCondition(fullColumn, "#{currentUserDeptAndChildIds}");
            case CUSTOM -> scope.customCondition() != null ? parseExpression(scope.customCondition()) : null;
        };
    }

    /**
     * 使用 DataScopeProcessor 构建数据权限条件
     */
    private Expression buildDataScopeConditionWithProcessor(DataScope scope, String fullColumn) {
        DataScopeUserContext userContext = getUserContext();

        // 如果用户拥有全部数据权限，不添加条件
        if (userContext.isAllDataPermission()) {
            return null;
        }

        return switch (scope.scopeType()) {
            case ALL -> null;
            case SELF -> buildSelfCondition(fullColumn, userContext);
            case DEPT -> buildDeptCondition(fullColumn, userContext);
            case DEPT_AND_CHILD -> buildDeptAndChildCondition(fullColumn, userContext);
            case CUSTOM -> buildCustomCondition(scope, userContext);
        };
    }

    /**
     * 获取用户上下文
     */
    private DataScopeUserContext getUserContext() {
        if (dataScopeProcessor == null) {
            return DataScopeUserContext.empty();
        }
        return dataScopeProcessor.provideUserContext();
    }

    /**
     * 构建仅本人数据条件
     */
    private Expression buildSelfCondition(String fullColumn, DataScopeUserContext userContext) {
        Long userId = userContext.getUserId();
        if (userId == null) {
            // 无法获取用户ID，返回一个不可能匹配的条件
            return createEqualsCondition(fullColumn, String.valueOf(Long.MIN_VALUE));
        }
        EqualsTo equals = new EqualsTo();
        equals.setLeftExpression(new Column(fullColumn));
        equals.setRightExpression(new LongValue(userId));
        return equals;
    }

    /**
     * 构建本部门数据条件
     */
    private Expression buildDeptCondition(String fullColumn, DataScopeUserContext userContext) {
        Long deptId = userContext.getDeptId();
        if (deptId == null) {
            // 无法获取部门ID，返回一个不可能匹配的条件
            return createEqualsCondition(fullColumn, String.valueOf(Long.MIN_VALUE));
        }
        EqualsTo equals = new EqualsTo();
        equals.setLeftExpression(new Column(fullColumn));
        equals.setRightExpression(new LongValue(deptId));
        return equals;
    }

    /**
     * 构建本部门及子部门数据条件
     */
    private Expression buildDeptAndChildCondition(String fullColumn, DataScopeUserContext userContext) {
        Set<Long> deptIds = userContext.getAccessibleDeptIds();

        if (deptIds == null || deptIds.isEmpty()) {
            // 如果没有可访问部门，尝试使用本部门
            Long deptId = userContext.getDeptId();
            if (deptId == null) {
                // 无法获取任何部门ID，返回一个不可能匹配的条件
                return createEqualsCondition(fullColumn, String.valueOf(Long.MIN_VALUE));
            }
            EqualsTo equals = new EqualsTo();
            equals.setLeftExpression(new Column(fullColumn));
            equals.setRightExpression(new LongValue(deptId));
            return equals;
        }

        // 构建 IN 条件
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(new Column(fullColumn));

        List<Expression> expressions = deptIds.stream()
                .map(LongValue::new)
                .map(Expression.class::cast)
                .toList();
        inExpression.setRightExpression(new ParenthesedExpressionList<>(new ExpressionList<>(expressions)));

        return inExpression;
    }

    /**
     * 构建自定义条件
     */
    private Expression buildCustomCondition(DataScope scope, DataScopeUserContext userContext) {
        String customCondition = scope.customCondition();
        if (customCondition == null || customCondition.isEmpty()) {
            return null;
        }

        // 解析占位符
        if (dataScopeProcessor != null) {
            customCondition = dataScopeProcessor.resolvePlaceholders(customCondition);
        }

        return parseExpression(customCondition);
    }

    private Expression createEqualsCondition(String column, String value) {
        EqualsTo equals = new EqualsTo();
        equals.setLeftExpression(new Column(column));
        equals.setRightExpression(new StringValue(value));
        return equals;
    }

    private Expression createNotEqualsCondition(String column, String value) {
        NotEqualsTo notEquals = new NotEqualsTo();
        notEquals.setLeftExpression(new Column(column));
        notEquals.setRightExpression(new StringValue(value));
        return notEquals;
    }

    private Expression createInCondition(String column, String value) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(new Column(column));
        // 使用 ParenthesedExpressionList 表示 IN 条件的值列表
        // value 会在运行时被替换为实际的部门 ID 列表
        ParenthesedExpressionList<Expression> expressionList = new ParenthesedExpressionList<>(new StringValue(value));
        inExpression.setRightExpression(expressionList);
        return inExpression;
    }

    private Expression parseExpression(String expr) {
        try {
            return CCJSqlParserUtil.parseCondExpression(expr);
        } catch (Exception e) {
            return null;
        }
    }

    private Expression appendCondition(Expression existing, Expression newCondition) {
        if (existing == null) {
            return newCondition;
        }
        return new AndExpression(existing, newCondition);
    }
}
