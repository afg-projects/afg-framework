package io.github.afgprojects.framework.core.web.module;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;

/**
 * 模块化 Web MVC 自动配置
 *
 * 使用 Spring MVC 原生的 PathMatchConfigurer 为模块 Controller 添加路径前缀
 *
 * <p>模块信息通过 {@link io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration}
 * 在类加载时提前加载，存储在系统属性 {@code afg.modules.definitions} 中。
 * 本配置类在 WebMvc 初始化时从系统属性读取模块定义，配置路径前缀。
 *
 * <p>路径前缀规则：
 * <ul>
 *   <li>原始路径（如 /auth/login）会被替换为带前缀的路径</li>
 *   <li>最终只注册带前缀的路径（如 /auth-api/auth/login）</li>
 *   <li>监控端点（如 /actuator/mappings）只显示带前缀的路径</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@Slf4j
public class ModuleWebAutoConfiguration implements WebMvcConfigurer {

    private static final String MODULE_DEFINITIONS_PROPERTY = "afg.modules.definitions";

    private final Environment environment;
    private final ModuleRegistry moduleRegistry;

    public ModuleWebAutoConfiguration(@NonNull Environment environment, @NonNull ModuleRegistry moduleRegistry) {
        this.environment = environment;
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        log.info("Configuring path prefix matcher for module controllers");

        // 从系统属性获取模块定义（在 AfgAutoConfiguration 类加载时提前加载）
        String moduleDefsStr = System.getProperty(MODULE_DEFINITIONS_PROPERTY);
        log.info("Module definitions from system property: {}", moduleDefsStr);

        if (moduleDefsStr != null && !moduleDefsStr.isEmpty()) {
            List<ModuleInfo> moduleDefs = parseModuleDefinitions(moduleDefsStr);
            log.info("Parsed {} module definitions", moduleDefs.size());
            for (ModuleInfo module : moduleDefs) {
                addPathPrefixForModule(configurer, module.moduleId, module.basePackage, module.contextPath);
            }
        } else {
            // 回退到从 ModuleRegistry 获取
            List<ModuleDefinition> modules = moduleRegistry.getAllModules();
            log.info("Found {} modules from ModuleRegistry", modules.size());
            for (ModuleDefinition module : modules) {
                addPathPrefixForModule(configurer, module.id(), module.basePackage(), module.contextPath());
            }
        }
    }

    /**
     * 解析序列化的模块定义
     * 格式: moduleId,basePackage,contextPath;moduleId2,basePackage2,contextPath2
     */
    private List<ModuleInfo> parseModuleDefinitions(String moduleDefsStr) {
        List<ModuleInfo> modules = new ArrayList<>();
        String[] moduleParts = moduleDefsStr.split(";");
        for (String modulePart : moduleParts) {
            String[] fields = modulePart.split(",");
            if (fields.length >= 3) {
                modules.add(new ModuleInfo(fields[0], fields[1], fields[2]));
            }
        }
        return modules;
    }

    private void addPathPrefixForModule(PathMatchConfigurer configurer, String moduleId, String basePackage, String contextPath) {
        if (basePackage != null && !basePackage.isEmpty()
                && contextPath != null && !contextPath.isEmpty()) {
            configurer.addPathPrefix(contextPath,
                    clazz -> clazz.getPackageName().startsWith(basePackage)
                            && isController(clazz));
            log.info("Added path prefix '{}' for module '{}' (package: {})", contextPath, moduleId, basePackage);
        }
    }

    /**
     * 检查类是否为 Controller（带有 @RestController 或 @Controller 注解）
     */
    private boolean isController(Class<?> clazz) {
        return AnnotationUtils.findAnnotation(clazz, RestController.class) != null
                || AnnotationUtils.findAnnotation(clazz, Controller.class) != null;
    }

    /**
     * 简化的模块信息
     */
    private record ModuleInfo(String moduleId, String basePackage, String contextPath) {}
}
