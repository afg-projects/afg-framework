package io.github.afgprojects.framework.core.api.duplicatesubmit;

/**
 * NoOp 重复提交检查器实现
 * <p>
 * 本地降级实现，所有去重检查总是成功获取（总是允许请求通过）。
 * 适用于不需要防重复提交的场景，注解式去重会"透传"（不阻塞业务逻辑）。
 * </p>
 * <p>
 * 由 {@code DuplicateSubmitAutoConfiguration} 在无其他 {@link DuplicateSubmitChecker} 实现时自动注册。
 * </p>
 *
 * @since 1.0.0
 */
public class NoOpDuplicateSubmitChecker implements DuplicateSubmitChecker {

    @Override
    public boolean tryAcquire(String key, long intervalMs) {
        return true;
    }

    @Override
    public void release(String key) {
        // no-op
    }
}
