package io.github.afgprojects.framework.core.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import io.github.afgprojects.framework.core.module.ModuleDefinition;

/**
 * 模块配置加载器
 *
 * 负责加载模块的配置文件并添加到 Spring Environment
 * 与 @AfgModuleAnnotation 注解联动，在模块注册时加载配置
 */
@Slf4j
public class ModuleConfigLoader {

    /**
     * 默认模块配置文件名模式
     * 使用 module-{moduleId}.yml 避免与 Spring Boot 的 application- 前缀冲突
     */
    private static final String DEFAULT_CONFIG_PATTERN = "module-%s.yml";
    private static final String PROPERTY_SOURCE_NAME = "applicationConfig: [classpath:application.yml]";

    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final ConfigurableEnvironment environment;

    public ModuleConfigLoader(@NonNull ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * 加载模块配置并添加到 Environment
     *
     * @param module 模块定义
     */
    public void loadAndRegisterModuleConfig(@NonNull ModuleDefinition module) {
        String configFileName = determineConfigFileName(module);
        String moduleId = module.id();

        List<PropertySource<?>> sources = loadConfigFile(moduleId, configFileName);

        if (sources.isEmpty()) {
            log.debug("No config file found for module: {} (looking for: {})", moduleId, configFileName);
            return;
        }

        // 添加到 Environment，优先级低于主应用配置
        addModuleConfigsToEnvironment(sources);

        log.info("Loaded {} config source(s) for module: {}", sources.size(), moduleId);
    }

    /**
     * 确定模块配置文件名
     */
    @NonNull
    private String determineConfigFileName(@NonNull ModuleDefinition module) {
        // 优先使用注解指定的配置文件
        if (module.configFile() != null && !module.configFile().isEmpty()) {
            return module.configFile();
        }
        // 默认使用 application-{moduleId}.yml
        return String.format(DEFAULT_CONFIG_PATTERN, module.id());
    }

    /**
     * 加载配置文件
     */
    @NonNull
    private List<PropertySource<?>> loadConfigFile(@NonNull String moduleId, @NonNull String configFileName) {
        String location = "classpath*:" + configFileName;

        try {
            Resource[] resources = resourceResolver.getResources(location);
            List<PropertySource<?>> result = new java.util.ArrayList<>();
            for (Resource resource : resources) {
                if (resource.exists() && resource.isReadable()) {
                    PropertySource<?> source = loadYamlResource(moduleId, resource);
                    if (source != null) {
                        result.add(source);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.debug("No config found at location: {} ({})", location, e.getMessage());
            return List.of();
        }
    }

    /**
     * 加载 YAML 资源为 PropertySource
     */
    @Nullable
    private PropertySource<?> loadYamlResource(@NonNull String moduleId, @NonNull Resource resource) {
        try {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource);
            factory.setSingleton(false);

            var properties = factory.getObject();
            if (properties != null && !properties.isEmpty()) {
                String sourceName = "module-config:[" + moduleId + "]:" + resource.getFilename();
                return new PropertiesPropertySource(sourceName, properties);
            }
        } catch (Exception e) {
            log.warn("Failed to parse YAML config for module {}: {}", moduleId, e.getMessage());
        }

        return null;
    }

    /**
     * 将模块配置添加到 Environment
     *
     * 配置优先级（从低到高）：
     * 1. 模块配置 (module-{moduleId}.yml)
     * 2. 主应用配置 (application.yml)
     * 3. Profile 配置 (application-{profile}.yml)
     * 4. 环境变量
     */
    private void addModuleConfigsToEnvironment(@NonNull List<PropertySource<?>> sources) {
        MutablePropertySources propertySources = environment.getPropertySources();

        // 找到合适的插入位置：在 applicationConfig 之前（优先级更低）
        // 这样 profile 配置会覆盖模块配置，符合预期
        String insertAfterName = findInsertPosition(propertySources);

        for (PropertySource<?> source : sources) {
            // 避免重复添加
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
            log.debug("Added module config: {} with priority lower than main application config", source.getName());
        }
    }

    /**
     * 找到模块配置的插入位置
     * 返回应该插入在其后面的 PropertySource 名称
     */
    @Nullable
    private String findInsertPosition(@NonNull MutablePropertySources propertySources) {
        // 查找 [applicationConfig: classpath:application.yml] 或类似名称
        // 模块配置应该插入在它后面，这样 profile 配置会覆盖模块配置
        for (PropertySource<?> source : propertySources) {
            String name = source.getName();
            // 找到 applicationConfig 但不是 profile 配置
            if (name.startsWith("applicationConfig") && !name.contains("profile")) {
                return name;
            }
        }

        // 如果找不到，尝试找其他配置源
        // 通常配置源的顺序是：命令行 > 环境变量 > profile配置 > 主配置
        // 我们想让模块配置优先级最低
        String lastConfigSource = null;
        for (PropertySource<?> source : propertySources) {
            String name = source.getName();
            if (name.contains("application") || name.contains("config")) {
                lastConfigSource = name;
            }
        }

        return lastConfigSource;
    }
}
