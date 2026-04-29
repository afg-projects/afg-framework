package io.github.afgprojects.framework.core.module.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 模块重复异常
 * 当注册已存在的模块ID时抛出
 */
public class ModuleDuplicateException extends ModuleException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ModuleDuplicateException(String moduleId) {
        super(CommonErrorCode.MODULE_DUPLICATE.getCode(), "Module already exists: " + moduleId, moduleId);
    }
}
