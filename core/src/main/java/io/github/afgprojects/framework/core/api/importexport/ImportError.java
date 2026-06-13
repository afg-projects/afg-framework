package io.github.afgprojects.framework.core.api.importexport;

import lombok.Builder;
import lombok.Data;

/**
 * 数据导入错误信息。
 * <p>
 * 描述导入过程中某行某字段的校验或解析错误。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class ImportError {

    /**
     * 错误所在行号（从 1 开始，不含标题行）。
     */
    private int row;

    /**
     * 错误字段名。
     */
    private String field;

    /**
     * 错误描述。
     */
    private String message;

    /**
     * 原始值。
     */
    private Object value;
}
