package io.github.afgprojects.framework.core.config.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 配置绑定异常
 * 当配置绑定失败时抛出
 */
public class ConfigBindingException extends ConfigException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConfigBindingException(String prefix, String message) {
        super(
                CommonErrorCode.CONFIG_BINDING_ERROR.getCode(),
                "Config binding failed for prefix [" + prefix + "]: " + message,
                prefix);
    }

    public ConfigBindingException(String prefix, String message, Throwable cause) {
        super(
                CommonErrorCode.CONFIG_BINDING_ERROR.getCode(),
                "Config binding failed for prefix [" + prefix + "]: " + message,
                prefix,
                cause);
    }
}
