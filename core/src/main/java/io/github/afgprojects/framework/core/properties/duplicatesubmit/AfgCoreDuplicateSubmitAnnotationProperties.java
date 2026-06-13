package io.github.afgprojects.framework.core.properties.duplicatesubmit;

import lombok.Data;

/**
 * 防重复提交注解配置。
 *
 * @since 1.0.0
 */
@Data
public class AfgCoreDuplicateSubmitAnnotationProperties {

    /**
     * 是否启用防重复提交注解切面。
     */
    private boolean enabled = true;
}
