package io.github.afgprojects.framework.core.web.module;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.AnnotationUtils;
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
 * 注意：PathMatchConfigurer.addPathPrefix() 会为匹配的 Controller 创建两个可访问的路径：
 * 1. 原始路径（如 /auth/login）
 * 2. 带前缀的路径（如 /auth-api/auth/login）
 *
 * 这是 Spring MVC 的设计行为，允许同一个 Controller 通过多个路径访问。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
public class ModuleWebAutoConfiguration implements WebMvcConfigurer {

    private final ModuleRegistry moduleRegistry;

    public ModuleWebAutoConfiguration(@NonNull ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        // 为每个模块添加路径前缀
        List<ModuleDefinition> modules = moduleRegistry.getAllModules();
        for (ModuleDefinition module : modules) {
            String basePackage = module.basePackage();
            String contextPath = module.contextPath();

            if (basePackage != null && !basePackage.isEmpty()
                    && contextPath != null && !contextPath.isEmpty()) {
                // 使用 Spring MVC 原生方式添加路径前缀
                // 仅对带有 @RestController 或 @Controller 注解的类生效
                configurer.addPathPrefix(contextPath,
                        clazz -> clazz.getPackageName().startsWith(basePackage)
                                && isController(clazz));
            }
        }
    }

    /**
     * 检查类是否为 Controller（带有 @RestController 或 @Controller 注解）
     */
    private boolean isController(Class<?> clazz) {
        return AnnotationUtils.findAnnotation(clazz, RestController.class) != null
                || AnnotationUtils.findAnnotation(clazz, Controller.class) != null;
    }
}
