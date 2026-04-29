package io.github.afgprojects.framework.core.module.exception;

import java.io.Serial;
import java.util.List;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import lombok.Getter;

/**
 * 模块循环依赖异常
 * 当检测到模块间存在循环依赖时抛出
 */
@Getter
public class ModuleCircularDependencyException extends ModuleException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> dependencyChain;

    public ModuleCircularDependencyException(List<String> dependencyChain) {
        super(
                CommonErrorCode.MODULE_CIRCULAR_DEPENDENCY.getCode(),
                "Circular dependency detected: " + String.join(" -> ", dependencyChain));
        this.dependencyChain = dependencyChain;
    }
}
