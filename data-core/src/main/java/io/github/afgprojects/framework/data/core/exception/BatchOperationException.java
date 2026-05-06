package io.github.afgprojects.framework.data.core.exception;

/**
 * 批量操作异常
 * <p>
 * 当批量操作部分失败时抛出此异常，包含成功数量和失败详情。
 */
public class BatchOperationException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 总数量
     */
    private final int totalCount;

    /**
     * 成功数量
     */
    private final int successCount;

    /**
     * 失败数量
     */
    private final int failureCount;

    /**
     * 创建批量操作异常
     *
     * @param totalCount   总数量
     * @param successCount 成功数量
     * @param failureCount 失败数量
     */
    public BatchOperationException(int totalCount, int successCount, int failureCount) {
        super(String.format("Batch operation completed with %d successes and %d failures out of %d total",
                successCount, failureCount, totalCount));
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    /**
     * 创建批量操作异常
     *
     * @param message      错误消息
     * @param totalCount   总数量
     * @param successCount 成功数量
     * @param failureCount 失败数量
     */
    public BatchOperationException(String message, int totalCount, int successCount, int failureCount) {
        super(message);
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    /**
     * 创建批量操作异常
     *
     * @param message      错误消息
     * @param totalCount   总数量
     * @param successCount 成功数量
     * @param failureCount 失败数量
     * @param cause        原因
     */
    public BatchOperationException(String message, int totalCount, int successCount, int failureCount, Throwable cause) {
        super(message, cause);
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    /**
     * 获取总数量
     *
     * @return 总数量
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 获取成功数量
     *
     * @return 成功数量
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * 获取失败数量
     *
     * @return 失败数量
     */
    public int getFailureCount() {
        return failureCount;
    }
}