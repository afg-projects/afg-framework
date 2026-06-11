package io.github.afgprojects.framework.apt.entity;

/**
 * 枚举值数据记录。
 * <p>
 * 表示枚举的一个值项，包含值和标签。
 * 由 APT 处理器根据 @AfgEnum 注解自动生成。
 *
 * @param value 枚举值（从 valueField 字段提取）
 * @param label 枚举标签（从 labelField 字段提取）
 * @see EnumMetadata
 * @see AfgEnum
 */
public record EnumValue(Object value, String label) {
}
