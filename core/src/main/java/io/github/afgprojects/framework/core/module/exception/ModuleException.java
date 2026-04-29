package io.github.afgprojects.framework.core.module.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.exception.AfgException;
import lombok.Getter;

/**
 * 模块异常基类
 * 所有模块相关异常的父类
 */
@Getter
public class ModuleException extends AfgException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String moduleId;

    public ModuleException(int code, String message) {
        super(code, message);
        this.moduleId = null;
    }

    public ModuleException(int code, String message, String moduleId) {
        super(code, message);
        this.moduleId = moduleId;
    }

    public ModuleException(int code, String message, Throwable cause) {
        super(code, message, cause);
        this.moduleId = null;
    }

    public ModuleException(int code, String message, String moduleId, Throwable cause) {
        super(code, message, cause);
        this.moduleId = moduleId;
    }
}
