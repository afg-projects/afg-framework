package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.scope.TenantScope;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * 数据操作管理器
 * <p>
 * 无泛型全局门面，作为数据操作的唯一入口。
 * <ul>
 *   <li>{@code entity(Class)} - 获取 EntityProxy 进行实体操作</li>
 *   <li>{@code query()} - 获取 SqlQueryBuilder 进行复杂查询</li>
 *   <li>{@code update()} - 获取 SqlUpdateBuilder 进行更新</li>
 *   <li>{@code insert()} - 获取 SqlInsertBuilder 进行插入</li>
 *   <li>{@code delete()} - 获取 SqlDeleteBuilder 进行删除</li>
 * </ul>
 */
public interface DataManager {

    // ==================== 实体操作 ====================

    /**
     * 获取实体操作代理
     *
     * @param entityClass 实体类型
     * @return 实体操作代理
     */
    <T> @NonNull EntityProxy<T> entity(@NonNull Class<T> entityClass);

    /**
     * 获取实体元数据
     *
     * @param entityClass 实体类型
     * @return 实体元数据
     */
    <T> @NonNull EntityMetadata<T> getEntityMetadata(@NonNull Class<T> entityClass);

    // ==================== SQL 构建 ====================

    /**
     * 创建 SQL 查询构建器
     *
     * @return SQL 查询构建器
     */
    @NonNull SqlQueryBuilder query();

    /**
     * 创建 SQL 更新构建器
     *
     * @return SQL 更新构建器
     */
    @NonNull SqlUpdateBuilder update();

    /**
     * 创建 SQL 插入构建器
     *
     * @return SQL 插入构建器
     */
    @NonNull SqlInsertBuilder insert();

    /**
     * 创建 SQL 删除构建器
     *
     * @return SQL 删除构建器
     */
    @NonNull SqlDeleteBuilder delete();

    // ==================== 事务管理 ====================

    /**
     * 在事务中执行
     *
     * @param action 要执行的操作
     */
    void executeInTransaction(@NonNull Runnable action);

    /**
     * 在事务中执行并返回结果
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    <T> T executeInTransaction(@NonNull Supplier<T> action);

    /**
     * 在只读事务中执行
     *
     * @param action 要执行的操作
     * @return 操作结果
     */
    <T> T executeInReadOnly(@NonNull Supplier<T> action);

    // ==================== 租户管理 ====================

    /**
     * 创建租户作用域（try-with-resources 自动恢复）
     *
     * @param tenantId 租户ID
     * @return 租户作用域
     */
    @NonNull TenantScope tenantScope(@NonNull String tenantId);

    /**
     * 获取租户上下文持有者
     *
     * @return 租户上下文持有者
     */
    @NonNull TenantContextHolder getTenantContextHolder();

    // ==================== 数据库信息 ====================

    /**
     * 获取当前数据库类型
     *
     * @return 数据库类型
     */
    @NonNull DatabaseType getDatabaseType();

    /**
     * 获取事务管理器
     *
     * @return 事务管理器
     */
    @NonNull Object getTransactionManager();
}