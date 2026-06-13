package io.github.afgprojects.framework.core.importexport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 列注解。
 * <p>
 * 标注在导出/导入 VO 的字段上，定义列标题、顺序、格式和校验规则。
 * 与 {@link ExcelSheet} 配合使用。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @ExcelSheet(name = "用户列表")
 * public class UserExportVO {
 *     @ExcelColumn(name = "用户名", order = 1, required = true)
 *     private String username;
 *
 *     @ExcelColumn(name = "创建时间", order = 2, format = "yyyy-MM-dd HH:mm:ss")
 *     private LocalDateTime createdAt;
 *
 *     @ExcelColumn(name = "状态", order = 3, enumConverter = UserStatus.class)
 *     private Integer status;
 * }
 * }</pre>
 *
 * @see ExcelSheet
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * 列标题。
     *
     * @return 列标题名称
     */
    String name();

    /**
     * 列顺序（升序排列）。
     *
     * @return 列顺序，默认 0
     */
    int order() default 0;

    /**
     * 格式（如日期格式 "yyyy-MM-dd"）。
     *
     * @return 格式字符串，默认空字符串
     */
    String format() default "";

    /**
     * 枚举转换器类型。
     * <p>
     * 用于导入时将枚举标签转换为枚举值，导出时将枚举值转换为标签。
     * 指定的类必须是枚举类型且标注了 {@code @AfgEnum} 注解。
     *
     * @return 枚举转换器类型，默认 Void.class 表示不使用枚举转换
     */
    Class<?> enumConverter() default Void.class;

    /**
     * 导入时是否必填。
     *
     * @return 是否必填，默认 false
     */
    boolean required() default false;
}
