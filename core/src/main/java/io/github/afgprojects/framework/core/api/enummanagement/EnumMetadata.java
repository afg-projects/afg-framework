package io.github.afgprojects.framework.core.api.enummanagement;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 枚举元数据。
 * <p>
 * 描述一个枚举类的结构信息，包括名称、值/标签字段、i18n 前缀和枚举项列表。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class EnumMetadata {

    /**
     * 枚举简单类名。
     */
    private String name;

    /**
     * 值字段名。
     * <p>
     * 对应 {@code @AfgEnum} 注解的 valueField 属性。
     */
    private String valueField;

    /**
     * 标签字段名。
     * <p>
     * 对应 {@code @AfgEnum} 注解的 labelField 属性。
     */
    private String labelField;

    /**
     * i18n 前缀。
     * <p>
     * 对应 {@code @AfgEnum} 注解的 i18nPrefix 属性。
     */
    private String i18nPrefix;

    /**
     * 枚举项列表。
     */
    @Builder.Default
    private List<EnumItem> items = List.of();
}
