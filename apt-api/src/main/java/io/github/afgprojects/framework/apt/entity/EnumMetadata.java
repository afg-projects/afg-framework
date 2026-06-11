package io.github.afgprojects.framework.apt.entity;

import java.util.List;

/**
 * 枚举元数据接口。
 * <p>
 * 由 APT 处理器根据 @AfgEnum 注解自动生成的元数据类实现此接口。
 * 提供枚举类的类型信息、值/标签字段名、i18n 前缀和枚举值列表。
 *
 * @param <T> 枚举类型
 * @see AfgEnum
 * @see EnumValue
 */
public interface EnumMetadata<T extends Enum<T>> {

    /**
     * 获取枚举类型。
     *
     * @return 枚举类的 Class 对象
     */
    Class<T> enumType();

    /**
     * 获取枚举值字段名。
     * <p>
     * 对应 @AfgEnum 注解的 valueField 属性。
     *
     * @return 值字段名
     */
    String valueField();

    /**
     * 获取枚举标签字段名。
     * <p>
     * 对应 @AfgEnum 注解的 labelField 属性。
     *
     * @return 标签字段名
     */
    String labelField();

    /**
     * 获取 i18n 前缀。
     * <p>
     * 对应 @AfgEnum 注解的 i18nPrefix 属性。
     *
     * @return i18n 前缀，空字符串表示不使用 i18n
     */
    String i18nPrefix();

    /**
     * 获取枚举值列表。
     *
     * @return 枚举值列表
     */
    List<EnumValue> values();
}
