package io.github.afgprojects.framework.core.api.enummanagement;

import lombok.Builder;
import lombok.Data;

/**
 * 枚举项。
 * <p>
 * 表示枚举的一个值项，包含常量名、值、标签和序号。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class EnumItem {

    /**
     * 枚举常量名（如 ACTIVE）。
     */
    private String name;

    /**
     * 枚举值（从 valueField 字段提取）。
     */
    private Object value;

    /**
     * 枚举标签（从 labelField 字段提取）。
     */
    private String label;

    /**
     * 枚举序号（ordinal）。
     */
    private int ordinal;
}
