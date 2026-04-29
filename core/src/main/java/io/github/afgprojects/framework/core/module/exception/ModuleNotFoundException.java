package io.github.afgprojects.framework.core.module.exception;

import java.io.Serial;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 模块不存在异常
 * 当依赖的模块未注册时抛出
 */
public class ModuleNotFoundException extends ModuleException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ModuleNotFoundException(String moduleId) {
        super(CommonErrorCode.MODULE_NOT_FOUND.getCode(), "Module not found: " + moduleId, moduleId);
    }

    public ModuleNotFoundException(String moduleId, String dependentModuleId) {
        super(
                CommonErrorCode.MODULE_NOT_FOUND.getCode(),
                "Module not found: " + moduleId + ", required by: " + dependentModuleId,
                moduleId);
    }
}
