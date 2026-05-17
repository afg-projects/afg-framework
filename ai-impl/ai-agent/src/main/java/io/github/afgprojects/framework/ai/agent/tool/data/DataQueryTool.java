package io.github.afgprojects.framework.ai.agent.tool.data;

import io.github.afgprojects.framework.ai.core.tool.SecureTool;
import io.github.afgprojects.framework.ai.core.tool.ToolContext;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityQuery;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据查询工具基类。
 *
 * <p>提供数据查询工具的通用能力：
 * <ul>
 *   <li>自动应用数据权限</li>
 *   <li>租户隔离</li>
 *   <li>结果过滤</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * public class UserQueryTool extends DataQueryTool<User, UserQueryInput, UserQueryOutput> {
 *
 *     public UserQueryTool(DataManager dataManager) {
 *         super(dataManager, User.class);
 *     }
 *
 *     @Override
 *     public String name() { return "query_users"; }
 *
 *     @Override
 *     public String requiredPermission() { return "user:read"; }
 *
 *     @Override
 *     protected Condition buildCondition(UserQueryInput input, ToolContext context) {
 *         return Conditions.builder(User.class)
 *             .eqIf(User::getStatus, input.status())
 *             .build();
 *     }
 *
 *     @Override
 *     protected UserQueryOutput convertResult(List<User> entities, ToolContext context) {
 *         return new UserQueryOutput(entities);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 * @param <I> 输入类型
 * @param <O> 输出类型
 * @since 1.0.0
 */
public abstract class DataQueryTool<T, I, O> implements SecureTool<I, O> {

    protected final DataManager dataManager;
    protected final Class<T> entityClass;

    /**
     * 创建数据查询工具。
     *
     * @param dataManager 数据管理器
     * @param entityClass 实体类
     */
    protected DataQueryTool(@NonNull DataManager dataManager, @NonNull Class<T> entityClass) {
        this.dataManager = dataManager;
        this.entityClass = entityClass;
    }

    @Override
    public O execute(I input, @NonNull ToolContext context) {
        // 1. 创建查询（自动应用租户隔离）
        EntityQuery<T> query = createQuery(context);

        // 2. 应用查询条件
        Condition condition = buildCondition(input, context);
        if (condition != null) {
            query = query.where(condition);
        }

        // 3. 应用排序
        io.github.afgprojects.framework.data.core.query.Sort sort = buildSort(input, context);
        if (sort != null) {
            query = query.orderBy(sort);
        }

        // 4. 应用分页
        Integer limit = getLimit(input, context);
        if (limit != null && limit > 0) {
            query = query.limit(limit);
        }
        Integer offset = getOffset(input, context);
        if (offset != null && offset > 0) {
            query = query.offset(offset);
        }

        // 5. 执行查询
        List<T> entities = query.list();

        // 6. 转换结果
        return convertResult(entities, context);
    }

    /**
     * 创建带数据权限的查询。
     *
     * @param context 工具上下文
     * @return 查询构建器
     */
    protected @NonNull EntityQuery<T> createQuery(@NonNull ToolContext context) {
        EntityQuery<T> query = dataManager.entity(entityClass).query();

        // 应用租户隔离
        if (context.getTenantId() != null) {
            query = query.withTenant(context.getTenantId());
        }

        // 应用数据权限
        DataScope dataScope = getDataScope(context);
        if (dataScope != null && dataScope.scopeType() != DataScopeType.ALL) {
            query = query.withDataScope(dataScope);
        }

        return query;
    }

    /**
     * 构建查询条件。
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @return 查询条件，null 表示无条件
     */
    protected abstract @Nullable Condition buildCondition(I input, @NonNull ToolContext context);

    /**
     * 构建排序规则。
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @return 排序规则，null 表示不排序
     */
    protected io.github.afgprojects.framework.data.core.query.Sort buildSort(
            I input,
            @NonNull ToolContext context) {
        return null;
    }

    /**
     * 获取查询限制数量。
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @return 限制数量，null 表示不限制
     */
    protected @Nullable Integer getLimit(I input, @NonNull ToolContext context) {
        return null;
    }

    /**
     * 获取查询偏移量。
     *
     * @param input   输入参数
     * @param context 工具上下文
     * @return 偏移量，null 表示从 0 开始
     */
    protected @Nullable Integer getOffset(I input, @NonNull ToolContext context) {
        return null;
    }

    /**
     * 转换查询结果。
     *
     * @param entities 查询结果
     * @param context  工具上下文
     * @return 输出结果
     */
    protected abstract O convertResult(@NonNull List<T> entities, @NonNull ToolContext context);

    /**
     * 获取默认数据权限。
     *
     * <p>子类可以覆盖此方法提供默认的数据权限配置。
     *
     * @param context 工具上下文
     * @return 数据权限配置，null 表示不应用数据权限
     */
    @Override
    public @Nullable DataScope getDataScope(@NonNull ToolContext context) {
        // 默认不应用数据权限，子类可以覆盖
        return null;
    }

    /**
     * 获取实体类。
     *
     * @return 实体类
     */
    protected @NonNull Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取数据管理器。
     *
     * @return 数据管理器
     */
    protected @NonNull DataManager getDataManager() {
        return dataManager;
    }
}
