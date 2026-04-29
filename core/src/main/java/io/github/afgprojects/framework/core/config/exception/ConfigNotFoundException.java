package io.github.afgprojects.framework.core.config.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 配置不存在异常
 * 当请求的配置不存在时抛出
 */
public class ConfigNotFoundException extends ConfigException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConfigNotFoundException(String prefix) {
        super(CommonErrorCode.CONFIG_NOT_FOUND.getCode(), "Config not found: " + prefix, prefix);
    }
}
