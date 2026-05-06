package io.github.afgprojects.framework.data.core;

import io.github.afgprojects.framework.data.core.query.Condition;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 实体写入操作接口
 * <p>
 * 提供实体的新增、更新、删除操作，职责单一。
 * <p>
 * 使用示例：
 * <pre>
 * // 保存单个实体
 * User saved = dataManager.entity(User.class).save(user);
 *
 * // 批量保存
 * List&lt;User&gt; saved = dataManager.entity(User.class).saveAll(users);
 *
 * // 删除
 * dataManager.entity(User.class).deleteById(1L);
 * </pre>
 *
 * @param <T> 实体类型
 * @see EntityReader 实体读取操作接口
 * @see EntityQuery 实体条件查询接口
 */
public interface EntityWriter<T> {

    /**
     * 保存实体（新增或更新）
     * <p>
     * 根据实体ID是否存在决定执行插入或更新操作。
     *
     * @param entity 实体实例
     * @return 保存后的实体（可能包含生成的主键）
     */
    @NonNull T save(@NonNull T entity);

    /**
     * 批量保存实体
     * <p>
     * 批量执行插入或更新操作，性能优于逐条保存。
     *
     * @param entities 实体集合
     * @return 保存后的实体列表
     */
    @NonNull List<T> saveAll(@NonNull Iterable<T> entities);

    /**
     * 插入实体
     * <p>
     * 强制执行插入操作，忽略ID是否存在。
     *
     * @param entity 实体实例
     * @return 插入后的实体（包含生成的主键）
     */
    @NonNull T insert(@NonNull T entity);

    /**
     * 批量插入实体
     * <p>
     * 使用批量插入优化，性能更高。
     *
     * @param entities 实体集合
     * @return 插入后的实体列表
     */
    @NonNull List<T> insertAll(@NonNull Iterable<T> entities);

    /**
     * 更新实体
     * <p>
     * 强制执行更新操作，实体必须存在ID。
     *
     * @param entity 实体实例
     * @return 更新后的实体
     */
    @NonNull T update(@NonNull T entity);

    /**
     * 批量更新实体
     *
     * @param entities 实体集合
     * @return 更新后的实体列表
     */
    @NonNull List<T> updateAll(@NonNull Iterable<T> entities);

    /**
     * 根据ID删除实体
     *
     * @param id 实体ID
     */
    void deleteById(@NonNull Object id);

    /**
     * 删除实体
     *
     * @param entity 实体实例
     */
    void delete(@NonNull T entity);

    /**
     * 根据多个ID批量删除
     *
     * @param ids ID集合
     */
    void deleteAllById(@NonNull Iterable<?> ids);

    /**
     * 批量删除实体
     *
     * @param entities 实体集合
     */
    void deleteAll(@NonNull Iterable<? extends T> entities);

    /**
     * 根据条件批量更新
     * <p>
     * 执行条件更新，返回受影响的行数。
     *
     * @param condition 更新条件
     * @param updates   更新字段映射（字段名 -> 新值）
     * @return 受影响行数
     */
    long updateAll(@NonNull Condition condition, @NonNull Map<String, Object> updates);

    /**
     * 根据条件批量删除
     * <p>
     * 执行条件删除，返回受影响的行数。
     *
     * @param condition 删除条件
     * @return 受影响行数
     */
    long deleteAll(@NonNull Condition condition);

    /**
     * 根据ID恢复删除（软删除场景）
     *
     * @param id 实体ID
     */
    void restoreById(@NonNull Object id);

    /**
     * 根据多个ID恢复删除（软删除场景）
     *
     * @param ids ID集合
     */
    void restoreAllById(@NonNull Iterable<?> ids);
}