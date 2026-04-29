package io.github.afgprojects.framework.core.api.scheduler;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 动态任务配置源接口
 *
 * <p>提供动态任务配置的加载能力，支持从配置中心、数据库等外部源获取任务配置
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>实现类应支持配置的动态刷新</li>
 *   <li>配置变更时应通知注册的监听器</li>
 *   <li>应考虑分布式环境下的配置一致性</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface DynamicTaskConfigSource {

    /**
     * 加载所有任务配置
     *
     * @return 任务定义列表
     */
    @NonNull List<TaskDefinition> loadAll();

    /**
     * 加载指定任务的配置
     *
     * @param taskId 任务 ID
     * @return 任务定义，不存在返回 empty
     */
    @NonNull Optional<TaskDefinition> load(@NonNull String taskId);

    /**
     * 加载指定分组的任务配置
     *
     * @param taskGroup 任务分组
     * @return 任务定义列表
     */
    @NonNull List<TaskDefinition> loadByGroup(@NonNull String taskGroup);

    /**
     * 保存任务配置
     *
     * @param definition 任务定义
     */
    void save(@NonNull TaskDefinition definition);

    /**
     * 删除任务配置
     *
     * @param taskId 任务 ID
     */
    void delete(@NonNull String taskId);

    /**
     * 注册配置变更监听器
     *
     * @param listener 监听器
     */
    void addChangeListener(@NonNull ConfigChangeListener listener);

    /**
     * 移除配置变更监听器
     *
     * @param listener 监听器
     */
    void removeChangeListener(@NonNull ConfigChangeListener listener);

    /**
     * 刷新配置
     *
     * <p>从配置源重新加载配置
     */
    void refresh();

    /**
     * 获取配置源名称
     *
     * @return 配置源名称
     */
    @NonNull String getName();

    /**
     * 配置变更监听器
     */
    @FunctionalInterface
    interface ConfigChangeListener {
        /**
         * 配置变更回调
         *
         * @param eventType 事件类型
         * @param definition 变更的任务定义
         */
        void onConfigChange(@NonNull ConfigChangeEvent event, @Nullable TaskDefinition definition);

        /**
         * 配置变更事件类型
         */
        enum ConfigChangeEvent {
            /**
             * 新增
             */
            CREATED,
            /**
             * 更新
             */
            UPDATED,
            /**
             * 删除
             */
            DELETED,
            /**
             * 刷新
             */
            REFRESHED
        }
    }
}
