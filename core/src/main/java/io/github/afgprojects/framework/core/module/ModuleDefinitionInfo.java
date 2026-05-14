package io.github.afgprojects.framework.core.module;

/**
 * 模块定义信息
 *
 * @param moduleId    模块 ID
 * @param basePackage  基础包名
 * @param contextPath  上下文路径前缀
 * @param configFile   配置文件名
 * @param className    配置类名
 */
public record ModuleDefinitionInfo(
        String moduleId,
        String basePackage,
        String contextPath,
        String configFile,
        String className
) {}
