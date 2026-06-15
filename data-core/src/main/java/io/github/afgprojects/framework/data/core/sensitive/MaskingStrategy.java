package io.github.afgprojects.framework.data.core.sensitive;

import org.jspecify.annotations.Nullable;

/**
 * 数据脱敏策略 SPI 接口。
 * <p>
 * 定义字段值的脱敏方法。支持标准脱敏类型（PHONE、ID_CARD 等）和自定义策略。
 * 框架提供基于 GB/T 35273 的默认实现 {@code DefaultMaskingStrategy}。
 *
 * <h3>实现示例</h3>
 * <pre>{@code
 * @Component("myCustomStrategy")
 * public class MyCustomStrategy implements MaskingStrategy {
 *     @Override
 *     public String mask(String value, String sensitiveType, @Nullable String fieldName) {
 *         if (value == null) return null;
 *         return "DESENSITIZED";
 *     }
 * }
 * }</pre>
 *
 * <h3>自定义策略引用</h3>
 * <pre>{@code
 * @SensitiveField(type = SensitiveType.CUSTOM, strategy = "myCustomStrategy")
 * private String customField;
 * }</pre>
 *
 * @see io.github.afgprojects.framework.apt.entity.SensitiveField
 * @see io.github.afgprojects.framework.apt.entity.SensitiveType
 */
public interface MaskingStrategy {

    /**
     * 对字段值进行脱敏。
     *
     * @param value         原始值，可能为 null
     * @param sensitiveType 敏感数据类型（来自 {@code SensitiveType} 枚举名称）
     * @param fieldName     字段名（Java 属性名），可能为 null
     * @return 脱敏后的值，输入 null 时返回 null
     */
    @Nullable
    String mask(@Nullable String value, String sensitiveType, @Nullable String fieldName);
}
