package io.github.afgprojects.framework.core.properties.validation;

import lombok.Data;

/**
 * Bean Validation 配置。
 *
 * @since 1.0.0
 */
@Data
public class AfgCoreValidationProperties {

    /**
     * 是否启用 Bean Validation（含统一异常处理）。
     */
    private boolean enabled = true;

    /**
     * 是否在错误响应中包含字段错误详情。
     */
    private boolean includeFieldErrors = true;

    /**
     * 参数校验失败时的默认错误消息。
     */
    private String defaultErrorMessage = "参数校验失败";
}
