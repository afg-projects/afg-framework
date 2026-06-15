package io.github.afgprojects.framework.data.core.security;

import java.util.Collections;
import java.util.Set;

/**
 * NoOp 字段级访问控制实现（默认降级）。
 * <p>
 * 不执行任何字段级访问限制，所有字段均可读写。
 * 当业务应用实现了自己的 {@link FieldAccessControl} 时，此 NoOp 通过
 * {@code @ConditionalOnMissingBean} 自动退让。
 *
 * <p>注意：返回空集合表示"无限制"（所有字段均可读写），
 * 并非表示"无字段可读写"。空集合的语义是"无需过滤"。
 *
 * @see FieldAccessControl
 */
public class NoOpFieldAccessControl implements FieldAccessControl {

    @Override
    public Set<String> getReadableFields(Class<?> entityClass) {
        return Collections.emptySet(); // empty = all fields readable
    }

    @Override
    public Set<String> getWritableFields(Class<?> entityClass) {
        return Collections.emptySet(); // empty = all fields writable
    }
}
