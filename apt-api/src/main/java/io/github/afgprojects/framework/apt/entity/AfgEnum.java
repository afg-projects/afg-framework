package io.github.afgprojects.framework.apt.entity;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举管理标记注解。
 * <p>
 * 标注在枚举类上，APT 处理器会生成枚举元数据类，用于：
 * <ul>
 *   <li>运行时枚举值列表查询</li>
 *   <li>前端下拉选项自动填充</li>
 *   <li>i18n 枚举描述</li>
 * </ul>
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @AfgEnum(valueField = "code", labelField = "description", i18nPrefix = "enum.user-status")
 * public enum UserStatus {
 *     ACTIVE(1, "活跃"),
 *     INACTIVE(0, "停用");
 *
 *     private final int code;
 *     private final String description;
 *
 *     UserStatus(int code, String description) {
 *         this.code = code;
 *         this.description = description;
 *     }
 * }
 * }</pre>
 *
 * <h2>使用默认字段名</h2>
 * <pre>{@code
 * @AfgEnum
 * public enum Status {
 *     ACTIVE("active", "活跃"),
 *     INACTIVE("inactive", "停用");
 *
 *     private final String value;  // 默认 valueField
 *     private final String label;  // 默认 labelField
 *
 *     Status(String value, String label) {
 *         this.value = value;
 *         this.label = label;
 *     }
 * }
 * }</pre>
 *
 * @see EnumMetadata
 * @see EnumValue
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AfgEnum {

    /**
     * 枚举值字段名。
     * <p>
     * 用于提取枚举的值部分。例如枚举有 code 字段表示编码，
     * 则设置 valueField = "code"。
     *
     * @return 值字段名，默认 "value"
     */
    String valueField() default "value";

    /**
     * 枚举标签字段名。
     * <p>
     * 用于提取枚举的标签部分。例如枚举有 description 字段表示描述，
     * 则设置 labelField = "description"。
     *
     * @return 标签字段名，默认 "label"
     */
    String labelField() default "label";

    /**
     * i18n 前缀。
     * <p>
     * 用于国际化翻译时的前缀。例如 i18nPrefix = "enum.user-status"，
     * 则翻译键为 enum.user-status.ACTIVE、enum.user-status.INACTIVE 等。
     *
     * @return i18n 前缀，默认空字符串表示不使用 i18n
     */
    String i18nPrefix() default "";
}
