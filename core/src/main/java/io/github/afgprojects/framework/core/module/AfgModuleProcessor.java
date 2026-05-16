package io.github.afgprojects.framework.core.module;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import io.github.afgprojects.framework.core.config.ModuleConfigLoader;
import io.github.afgprojects.framework.core.module.exception.ModuleContextPathConflictException;
import io.github.afgprojects.framework.core.module.exception.ModulePackageConflictException;
import io.github.afgprojects.framework.apt.module.AfgModuleAnnotation;

/**
 * AFG 模块注解处理器
 *
 * 自动扫描并注册带有 @AfgModuleAnnotation 注解的类到模块注册表
 * 同时加载模块的配置文件到 Spring Environment
 *
 * <p>实现 {@link PriorityOrdered} 接口，确保在 WebMvc 初始化之前处理模块配置类，
 * 这样 {@link io.github.afgprojects.framework.core.web.module.ModuleWebAutoConfiguration}
 * 可以在配置路径前缀时获取到已注册的模块列表。
 */
@Slf4j
public class AfgModuleProcessor implements BeanPostProcessor, PriorityOrdered {

    private final ModuleRegistry moduleRegistry;
    private final ApplicationContext applicationContext;
    private final ModuleConfigLoader configLoader;

    public AfgModuleProcessor(@NonNull ModuleRegistry moduleRegistry,
                              @NonNull ApplicationContext applicationContext) {
        this.moduleRegistry = moduleRegistry;
        this.applicationContext = applicationContext;
        // 初始化配置加载器
        this.configLoader = createConfigLoader();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ModuleConfigLoader createConfigLoader() {
        try {
            ConfigurableEnvironment environment = applicationContext.getBean(ConfigurableEnvironment.class);
            return new ModuleConfigLoader(environment);
        } catch (Exception e) {
            log.warn("Failed to create ModuleConfigLoader, module config loading disabled: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        // 获取目标类（处理 CGLIB 代理）
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        AfgModuleAnnotation annotation = AnnotationUtils.findAnnotation(targetClass, AfgModuleAnnotation.class);

        if (annotation != null) {
            registerModule(bean, targetClass, annotation);
        }

        return bean;
    }

    private void registerModule(Object bean, Class<?> targetClass, AfgModuleAnnotation annotation) {
        // 解析 basePackage，默认使用注解所在类的包名
        String basePackage = annotation.basePackage();
        if (basePackage.isEmpty()) {
            basePackage = targetClass.getPackage().getName();
        }

        // 解析 moduleId，默认使用包名的最后一部分
        String moduleId = annotation.id().isEmpty() ? annotation.value() : annotation.id();
        if (moduleId.isEmpty()) {
            moduleId = extractModuleIdFromPackage(basePackage);
            log.debug("@AfgModuleAnnotation on {} has no id specified, using '{}' from package {}",
                    targetClass.getName(), moduleId, basePackage);
        }

        String name = annotation.name().isEmpty() ? moduleId : annotation.name();

        // 解析 contextPath，默认为 /{moduleId}-api
        String contextPath = annotation.contextPath();
        if (contextPath.isEmpty()) {
            contextPath = "/" + moduleId + "-api";
        }

        List<String> dependencies = Arrays.asList(annotation.dependencies());
        String version = annotation.version();
        String description = annotation.description();
        String configFile = annotation.configFile();

        // 验证 basePackage 和 contextPath 唯一性
        validateBasePackage(moduleId, basePackage);
        validateContextPath(moduleId, contextPath);

        // 创建模块定义
        ModuleDefinition definition = ModuleDefinition.builder()
                .id(moduleId)
                .name(name)
                .dependencies(dependencies)
                .moduleInstance(new AnnotatedAfgModule(moduleId, name, dependencies, basePackage, contextPath, version, description))
                .basePackage(basePackage)
                .contextPath(contextPath)
                .configFile(configFile)
                .build();

        // 注册模块
        try {
            moduleRegistry.registerWithoutDependencyCheck(definition);
            log.info("Registered AFG module: {} (id={}, basePackage={}, contextPath={}, dependencies={})",
                    name, moduleId, basePackage, contextPath, dependencies);

            // 加载模块配置文件
            loadModuleConfig(definition);

            // 调用模块初始化回调
            ModuleContext context = applicationContext.getBean(ModuleContext.class);
            definition.moduleInstance().onRegister(context);
        } catch (Exception e) {
            log.warn("Failed to register module {}: {}", moduleId, e.getMessage());
        }
    }

    /**
     * 加载模块配置文件
     */
    private void loadModuleConfig(ModuleDefinition definition) {
        if (configLoader == null) {
            return;
        }
        try {
            configLoader.loadAndRegisterModuleConfig(definition);
        } catch (Exception e) {
            log.warn("Failed to load config for module {}: {}", definition.id(), e.getMessage());
        }
    }

    /**
     * 从包名提取模块 ID
     * 例如: io.github.afgprojects.auth -> auth
     */
    private String extractModuleIdFromPackage(String basePackage) {
        if (basePackage == null || basePackage.isEmpty()) {
            return "unknown";
        }
        int lastDot = basePackage.lastIndexOf('.');
        return lastDot >= 0 ? basePackage.substring(lastDot + 1) : basePackage;
    }

    /**
     * 验证 basePackage 唯一性
     */
    private void validateBasePackage(String moduleId, String basePackage) {
        if (basePackage.isEmpty()) return;

        for (ModuleDefinition existing : moduleRegistry.getAllModules()) {
            String existingPackage = existing.basePackage();
            if (existingPackage != null && !existingPackage.isEmpty()) {
                // 检查包名完全相同
                if (basePackage.equals(existingPackage)) {
                    throw new ModulePackageConflictException(moduleId, existing.id(), basePackage);
                }
                // 检查包包含关系（警告但不阻止）
                if (basePackage.startsWith(existingPackage + ".")) {
                    log.warn("Module '{}' basePackage '{}' is a sub-package of module '{}' basePackage '{}'",
                            moduleId, basePackage, existing.id(), existingPackage);
                } else if (existingPackage.startsWith(basePackage + ".")) {
                    log.warn("Module '{}' basePackage '{}' contains module '{}' basePackage '{}' as sub-package",
                            moduleId, basePackage, existing.id(), existingPackage);
                }
            }
        }
    }

    /**
     * 验证 contextPath 唯一性
     */
    private void validateContextPath(String moduleId, String contextPath) {
        if (contextPath.isEmpty()) return;

        for (ModuleDefinition existing : moduleRegistry.getAllModules()) {
            String existingPath = existing.contextPath();
            if (existingPath != null && !existingPath.isEmpty() && existingPath.equals(contextPath)) {
                throw new ModuleContextPathConflictException(moduleId, existing.id(), contextPath);
            }
        }
    }

    /**
     * 基于 @AfgModuleAnnotation 注解的 AfgModule 实现
     */
    private record AnnotatedAfgModule(
            String moduleId,
            String moduleName,
            List<String> dependencies,
            String basePackage,
            String contextPath,
            String version,
            String description) implements AfgModule {

        @Override
        public String moduleId() {
            return moduleId;
        }

        @Override
        public String moduleName() {
            return moduleName;
        }

        @Override
        public List<String> dependencies() {
            return dependencies;
        }

        @Override
        public String basePackage() {
            return basePackage;
        }

        @Override
        public String contextPath() {
            return contextPath;
        }
    }
}