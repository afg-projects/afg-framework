package io.github.afgprojects.framework.core.api.duplicatesubmit;

/**
 * 重复提交检查器接口
 * <p>
 * 定义统一的防重复提交检查接口，支持多种存储后端。
 * 核心语义：{@code tryAcquire} 尝试在指定间隔内获取去重标记，成功表示首次请求，失败表示重复请求。
 * </p>
 *
 * <pre>{@code
 * @Autowired
 * private DuplicateSubmitChecker checker;
 *
 * // 尝试获取去重标记（3秒内不允许重复）
 * boolean acquired = checker.tryAcquire("order:submit:123", 3000);
 * if (acquired) {
 *     // 首次请求，执行业务逻辑
 * } else {
 *     // 重复请求，拒绝
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DuplicateSubmitChecker {

    /**
     * 尝试获取去重标记
     * <p>
     * 如果指定 key 在间隔时间内没有被标记过，则标记并返回 {@code true}（首次请求）。
     * 如果指定 key 在间隔时间内已被标记，则返回 {@code false}（重复请求）。
     * </p>
     *
     * @param key        去重键
     * @param intervalMs 去重间隔（毫秒），在间隔内重复请求将被拒绝
     * @return {@code true} 表示获取成功（首次请求），{@code false} 表示获取失败（重复请求）
     */
    boolean tryAcquire(String key, long intervalMs);

    /**
     * 释放去重标记
     * <p>
     * 手动移除指定 key 的去重标记，允许下一次请求通过。
     * 通常不需要手动调用，去重标记会在间隔时间后自动过期。
     * </p>
     *
     * @param key 去重键
     */
    void release(String key);
}
