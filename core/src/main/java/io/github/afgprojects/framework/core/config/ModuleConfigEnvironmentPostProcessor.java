package io.github.afgprojects.framework.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * 模块配置环境后处理器
 *
 * 在 Spring Boot 启动早期阶段加载模块配置文件，
 * 确保模块配置在 Bean 初始化之前就可用。
 *
 * <p>配置优先级（从低到高）：
 * <ol>
 *   <li>模块配置 (module-{moduleId}.yml)</li>
 *   <li>主应用配置 (application.yml)</li>
 *   <li>Profile 配置 (application-{profile}.yml)</li>
 *   <li>环境变量</li>
 * </ol>
 *
 * <p>索引文件格式：
 * <pre>
 * moduleId:configFile:className
 * auth:module-auth.yml:io.github.afgprojects.auth.AuthModuleConfig
 * </pre>
 */
@Slf4j
public class ModuleConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String INDEX_FILE = "META-INF/afg-modules.index";

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment,
                                        @NonNull SpringApplication application) {
        // 使用 Logger 输出调试信息
        log.info("ModuleConfigEnvironmentPostProcessor invoked, checking for module configs...");

        List<ModuleInfo> modules = loadModuleIndex();

        if (modules.isEmpty()) {
            log.info("No AFG module index found");
            return;
        }

        log.info("Loading configurations for {} AFG modules: {}", modules.size(), modules);

        List<PropertySource<?>> moduleConfigs = new ArrayList<>();

        for (ModuleInfo module : modules) {
            PropertySource<?> source = loadModuleConfig(module);
            if (source != null) {
                moduleConfigs.add(source);
                log.info("Loaded module config: {} -> {}", module.moduleId(), module.configFile());
            } else {
                log.warn("Module config not found for module: {} (expected: {})", module.moduleId(), module.configFile());
            }
        }

        // 添加到 Environment，优先级低于主应用配置
        addModuleConfigsToEnvironment(environment, moduleConfigs);

        log.info("Module configs added to Environment. Total property sources: {}", environment.getPropertySources().size());
    }

    /**
     * 从索引文件加载模块信息
     */
    private List<ModuleInfo> loadModuleIndex() {
        List<ModuleInfo> modules = new ArrayList<>();

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources(INDEX_FILE);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            ModuleInfo info = parseModuleEntry(line);
                            if (info != null) {
                                modules.add(info);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load module index: {}", e.getMessage());
        }

        return modules;
    }

    /**
     * 解析模块条目
     * 格式: moduleId:configFile:className
     */
    private ModuleInfo parseModuleEntry(String entry) {
        String[] parts = entry.split(":", 3);
        if (parts.length >= 3) {
            return new ModuleInfo(parts[0], parts[1], parts[2]);
        } else if (parts.length == 1) {
            // 旧格式兼容：只有类名
            // 从类名提取 moduleId
            String className = parts[0];
            int lastDot = className.lastIndexOf('.');
            String moduleId = lastDot >= 0 ? className.substring(lastDot + 1).replace("ModuleConfig", "").toLowerCase() : className;
            return new ModuleInfo(moduleId, "module-" + moduleId + ".yml", className);
        }
        return null;
    }

    /**
     * 加载模块配置文件
     */
    private PropertySource<?> loadModuleConfig(ModuleInfo module) {
        String location = "classpath*:" + module.configFile();

        try {
            Resource[] resources = resourceResolver.getResources(location);
            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    PropertySource<?> source = loadYamlResource(module.moduleId(), resource);
                    if (source != null) {
                        return source;
                    }
                }
            }
        } catch (IOException e) {
            log.debug("No config found for module {} at {}", module.moduleId(), location);
        }

        return null;
    }

    /**
     * 加载 YAML 资源为 PropertySource
     */
    private PropertySource<?> loadYamlResource(String moduleId, Resource resource) {
        try {
            org.springframework.beans.factory.config.YamlPropertiesFactoryBean factory =
                    new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
            factory.setResources(resource);
            factory.setSingleton(false);

            var properties = factory.getObject();
            if (properties != null && !properties.isEmpty()) {
                String sourceName = "module-config:[" + moduleId + "]";
                return new PropertiesPropertySource(sourceName, properties);
            }
        } catch (Exception e) {
            log.warn("Failed to parse YAML config for module {}: {}", moduleId, e.getMessage());
        }

        return null;
    }

    /**
     * 将模块配置添加到 Environment
     */
    private void addModuleConfigsToEnvironment(ConfigurableEnvironment environment,
                                                List<PropertySource<?>> sources) {
        if (sources.isEmpty()) {
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();

        // 找到 applicationConfig 的位置，在其后面插入模块配置
        // 这样主应用配置会覆盖模块配置
        String insertAfterName = findInsertPosition(propertySources);

        for (PropertySource<?> source : sources) {
            if (propertySources.contains(source.getName())) {
                log.debug("Module config already exists: {}", source.getName());
                continue;
            }

            if (insertAfterName != null) {
                propertySources.addAfter(insertAfterName, source);
            } else {
                // 如果找不到合适位置，添加到最后（最低优先级）
                propertySources.addLast(source);
            }
        }
    }

    /**
     * 找到模块配置的插入位置
     */
    private String findInsertPosition(MutablePropertySources propertySources) {
        for (PropertySource<?> source : propertySources) {
            String name = source.getName();
            // 找到 applicationConfig 但不是 profile 配置
            if (name.contains("applicationConfig") && !name.contains("profile")) {
                return name;
            }
        }

        // 如果找不到，尝试找其他配置源
        String lastConfigSource = null;
        for (PropertySource<?> source : propertySources) {
            String name = source.getName();
            if (name.contains("application") || name.contains("config")) {
                lastConfigSource = name;
            }
        }

        return lastConfigSource;
    }

    /**
     * 模块信息记录
     */
    private record ModuleInfo(String moduleId, String configFile, String className) {}
}
