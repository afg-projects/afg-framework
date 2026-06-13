package io.github.afgprojects.framework.core.api.enummanagement;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 枚举注册表降级实现。
 * <p>
 * 所有注册和查询操作均为空操作。
 * {@code register} 静默忽略，
 * {@code getMetadata} 返回 null，
 * {@code getAllMetadata} 返回空列表，
 * {@code getItems} 返回空列表。
 * <p>
 * 由 {@code EnumManagementAutoConfiguration} 在无其他 {@link EnumRegistry} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpEnumRegistry implements EnumRegistry {

    @Override
    public void register(@NonNull Class<? extends Enum<?>> enumClass) {
        // no-op
    }

    @Override
    public void register(@NonNull Class<? extends Enum<?>> enumClass,
                         @NonNull String valueField, @NonNull String labelField) {
        // no-op
    }

    @Override
    @Nullable
    public EnumMetadata getMetadata(@NonNull String enumName) {
        return null;
    }

    @Override
    @Nullable
    public EnumMetadata getMetadata(@NonNull Class<? extends Enum<?>> enumClass) {
        return null;
    }

    @Override
    @NonNull
    public List<EnumMetadata> getAllMetadata() {
        return Collections.emptyList();
    }

    @Override
    @NonNull
    public List<EnumItem> getItems(@NonNull String enumName) {
        return Collections.emptyList();
    }
}
