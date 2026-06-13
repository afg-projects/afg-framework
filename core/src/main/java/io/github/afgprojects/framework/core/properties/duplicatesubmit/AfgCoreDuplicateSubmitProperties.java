package io.github.afgprojects.framework.core.properties.duplicatesubmit;

import lombok.Data;

/**
 * 防重复提交配置。
 *
 * @since 1.0.0
 */
@Data
public class AfgCoreDuplicateSubmitProperties {

    /**
     * 是否启用防重复提交。
     */
    private boolean enabled = true;

    /**
     * 去重键前缀。
     */
    private String keyPrefix = "afg:duplicate-submit";

    /**
     * 默认去重间隔（毫秒）。
     */
    private long defaultInterval = 3000;

    /**
     * 注解相关配置。
     */
    private AfgCoreDuplicateSubmitAnnotationProperties annotations = new AfgCoreDuplicateSubmitAnnotationProperties();
}
