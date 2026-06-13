package io.github.afgprojects.framework.core.importexport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel Sheet 注解。
 * <p>
 * 标注在导出/导入 VO 类上，定义 Sheet 名称和序号。
 * 与 {@link ExcelColumn} 配合使用，描述 Excel 工作表的结构。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @ExcelSheet(name = "用户列表")
 * public class UserExportVO {
 *     @ExcelColumn(name = "用户名", order = 1)
 *     private String username;
 *     @ExcelColumn(name = "状态", order = 2, enumConverter = UserStatus.class)
 *     private Integer status;
 * }
 * }</pre>
 *
 * @see ExcelColumn
 * @see CsvSheet
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelSheet {

    /**
     * Sheet 名称。
     *
     * @return 工作表名称
     */
    String name();

    /**
     * Sheet 序号（从 0 开始）。
     *
     * @return 工作表序号，默认 0
     */
    int sheetNo() default 0;
}
