package io.github.afgprojects.framework.data.core.sensitive;

/**
 * NoOp 脱敏上下文实现（默认降级）。
 * <p>
 * 始终返回 true，即对所有标记了 {@code @SensitiveField} 的字段始终执行脱敏。
 * 当业务应用实现自己的 {@link MaskingContext} 时，此 NoOp 实现通过
 * {@code @ConditionalOnMissingBean} 自动退让。
 *
 * @see MaskingContext
 */
public class NoOpMaskingContext implements MaskingContext {

    @Override
    public boolean shouldMask(String fieldName, String sensitiveType) {
        return true;
    }
}
