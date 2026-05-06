package io.github.afgprojects.framework.core.module.exception;

/**
 * 模块包名冲突异常
 * 当多个模块声明相同或存在包含关系的 basePackage 时抛出
 */
public class ModulePackageConflictException extends RuntimeException {

    private final String moduleId;
    private final String conflictingModuleId;
    private final String basePackage;

    public ModulePackageConflictException(String moduleId, String conflictingModuleId, String basePackage) {
        super(String.format("Module '%s' basePackage '%s' conflicts with module '%s'",
                moduleId, basePackage, conflictingModuleId));
        this.moduleId = moduleId;
        this.conflictingModuleId = conflictingModuleId;
        this.basePackage = basePackage;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getConflictingModuleId() {
        return conflictingModuleId;
    }

    public String getBasePackage() {
        return basePackage;
    }
}
