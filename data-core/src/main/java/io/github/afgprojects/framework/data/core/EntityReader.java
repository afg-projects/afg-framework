package io.github.afgprojects.framework.data.core;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * 实体读取操作接口
 * <p>
 * 提供实体的查询和读取相关操作，职责单一，便于理解和维护。
 * <p>
 * 使用示例：
 * <pre>
 * // 基础查询
 * Optional&lt;User&gt; user = dataManager.entity(User.class).findById(1L);
 * List&lt;User&gt; users = dataManager.entity(User.class).findAll();
 *
 * // 条件查询（通过 EntityQuery 扩展）
 * List&lt;User&gt; users = dataManager.entity(User.class)
 *     .query()
 *     .where(Conditions.builder().eq("status", 1).build())
 *     .list();
 * </pre>
 *
 * @param <T> 实体类型
 * @see EntityQuery 实体条件查询接口
 * @see EntityWriter 实体写入操作接口
 */
public interface EntityReader<T> {

    /**
     * 根据ID查询实体
     *
     * @param id 实体ID
     * @return 实体（可能为空）
     */
    @NonNull Optional<T> findById(@NonNull Object id);

    /**
     * 根据多个ID查询实体列表
     *
     * @param ids ID集合
     * @return 实体列表
     */
    @NonNull List<T> findAllById(@NonNull Iterable<?> ids);

    /**
     * 查询所有实体
     *
     * @return 实体列表
     */
    @NonNull List<T> findAll();

    /**
     * 统计实体总数
     *
     * @return 实体总数
     */
    long count();

    /**
     * 判断实体是否存在
     *
     * @param id 实体ID
     * @return 是否存在
     */
    boolean existsById(@NonNull Object id);

    /**
     * 获取条件查询接口
     * <p>
     * 返回条件查询构建器，支持更复杂的查询操作。
     *
     * @return 条件查询接口
     */
    @NonNull EntityQuery<T> query();
}