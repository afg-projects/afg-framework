package io.github.afgprojects.framework.core.importexport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CSV Sheet 注解。
 * <p>
 * 标注在导出/导入 VO 类上，定义 CSV 文件的结构。
 * 与 {@link ExcelColumn} 配合使用，复用列定义。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @CsvSheet(name = "用户列表", delimiter = ',', charset = "UTF-8")
 * public class UserExportVO {
 *     @ExcelColumn(name = "用户名", order = 1)
 *     private String username;
 *     @ExcelColumn(name = "邮箱", order = 2)
 *     private String email;
 * }
 * }</pre>
 *
 * @see ExcelColumn
 * @see ExcelSheet
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CsvSheet {

    /**
     * CSV 文件名称（不含扩展名）。
     *
     * @return 文件名称
     */
    String name();

    /**
     * 分隔符。
     *
     * @return 分隔符，默认逗号
     */
    char delimiter() default ',';

    /**
     * 字符编码。
     *
     * @return 字符编码，默认 UTF-8
     */
    String charset() default "UTF-8";
}
