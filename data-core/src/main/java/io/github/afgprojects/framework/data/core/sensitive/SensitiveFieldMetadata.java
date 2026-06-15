package io.github.afgprojects.framework.data.core.sensitive;

import io.github.afgprojects.framework.apt.entity.SensitiveType;
import org.jspecify.annotations.Nullable;

/**
 * 敏感字段元数据。
 * <p>
 * 记录标注了 {@code @SensitiveField} 注解的字段信息，
 * 包括字段名、敏感数据类型和自定义策略 Bean 名称。
 *
 * @param fieldName     字段名（Java 属性名）
 * @param sensitiveType 敏感数据类型
 * @param strategy      自定义脱敏策略 Bean 名称（仅 CUSTOM 类型时非空）
 */
public record SensitiveFieldMetadata(
    String fieldName,
    SensitiveType sensitiveType,
    @Nullable String strategy
) {
}
