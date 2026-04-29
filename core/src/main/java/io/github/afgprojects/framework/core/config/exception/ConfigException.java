package io.github.afgprojects.framework.core.config.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.exception.AfgException;
import lombok.Getter;

/**
 * 配置异常基类
 */
@Getter
public class ConfigException extends AfgException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String prefix;

    public ConfigException(int code, String message) {
        super(code, message);
        this.prefix = null;
    }

    public ConfigException(int code, String message, String prefix) {
        super(code, message);
        this.prefix = prefix;
    }

    public ConfigException(int code, String message, Throwable cause) {
        super(code, message, cause);
        this.prefix = null;
    }

    public ConfigException(int code, String message, String prefix, Throwable cause) {
        super(code, message, cause);
        this.prefix = prefix;
    }
}
