package io.github.afgprojects.framework.core.module.exception;

/**
 * 模块上下文路径冲突异常
 * 当多个模块声明相同的 contextPath 时抛出
 */
public class ModuleContextPathConflictException extends RuntimeException {

    private final String moduleId;
    private final String conflictingModuleId;
    private final String contextPath;

    public ModuleContextPathConflictException(String moduleId, String conflictingModuleId, String contextPath) {
        super(String.format("Module '%s' contextPath '%s' conflicts with module '%s'",
                moduleId, contextPath, conflictingModuleId));
        this.moduleId = moduleId;
        this.conflictingModuleId = conflictingModuleId;
        this.contextPath = contextPath;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getConflictingModuleId() {
        return conflictingModuleId;
    }

    public String getContextPath() {
        return contextPath;
    }
}
