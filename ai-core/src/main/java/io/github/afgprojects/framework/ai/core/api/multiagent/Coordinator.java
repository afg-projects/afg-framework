package io.github.afgprojects.framework.ai.core.api.multiagent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 协调者接口
 *
 * <p>负责 Agent 之间的协调工作，包括状态同步、冲突解决和结果合并。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface Coordinator {

    /**
     * 获取协调者名称
     *
     * @return 名称
     */
    @NonNull String getName();

    /**
     * 协调多个 Agent 的执行
     *
     * <p>管理 Agent 之间的依赖关系，确保按正确顺序执行。
     *
     * @param workflowId 工作流 ID
     * @return 协调结果
     */
    @NonNull CompletableFuture<CoordinationResult> coordinate(@NonNull String workflowId);

    /**
     * 解决 Agent 之间的冲突
     *
     * @param conflict 冲突信息
     * @return 解决方案
     */
    @NonNull ConflictResolution resolveConflict(@NonNull Conflict conflict);

    /**
     * 合并多个 Agent 的执行结果
     *
     * @param results 执行结果列表
     * @return 合并后的结果
     */
    @NonNull Object mergeResults(@NonNull Iterable<?> results);

    /**
     * 同步 Agent 状态
     *
     * @param agentId Agent 唯一标识
     * @param status  状态信息
     */
    void syncStatus(@NonNull String agentId, @NonNull Object status);

    /**
     * 获取 Agent 状态
     *
     * @param agentId Agent 唯一标识
     * @return 状态信息，如果不存在返回 null
     */
    @Nullable Object getStatus(@NonNull String agentId);

    /**
     * 协调结果
     *
     * @param success   是否成功
     * @param result    协调结果
     * @param errors    错误信息
     * @param metadata  元数据
     */
    record CoordinationResult(
            boolean success,
            @Nullable Object result,
            @Nullable String errors,
            @Nullable Map<String, Object> metadata
    ) {
        /**
         * 创建成功的协调结果
         */
        public static @NonNull CoordinationResult success(@Nullable Object result) {
            return new CoordinationResult(true, result, null, null);
        }

        /**
         * 创建失败的协调结果
         */
        public static @NonNull CoordinationResult failure(@NonNull String errors) {
            return new CoordinationResult(false, null, errors, null);
        }
    }

    /**
     * 冲突信息
     *
     * @param conflictId   冲突 ID
     * @param agentIds     涉及的 Agent ID 列表
     * @param conflictType 冲突类型
     * @param description  冲突描述
     */
    record Conflict(
            @NonNull String conflictId,
            @NonNull List<String> agentIds,
            @NonNull String conflictType,
            @NonNull String description
    ) {}

    /**
     * 冲突解决方案
     *
     * @param resolved    是否已解决
     * @param resolution  解决方案描述
     * @param action      需要采取的行动
     */
    record ConflictResolution(
            boolean resolved,
            @Nullable String resolution,
            @Nullable String action
    ) {
        /**
         * 创建已解决的解决方案
         */
        public static @NonNull ConflictResolution resolved(@NonNull String resolution) {
            return new ConflictResolution(true, resolution, null);
        }

        /**
         * 创建需要人工干预的解决方案
         */
        public static @NonNull ConflictResolution needsIntervention(@NonNull String action) {
            return new ConflictResolution(false, null, action);
        }
    }
}
