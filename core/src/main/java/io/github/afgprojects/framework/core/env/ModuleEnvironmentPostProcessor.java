package io.github.afgprojects.framework.core.env;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 模块环境后置处理器
 *
 * <p>在 Spring 环境初始化阶段（早于任何 bean 创建）加载模块定义信息，
 * 并存储到环境属性中，供 {@link io.github.afgprojects.framework.core.web.module.ModuleWebAutoConfiguration} 使用。
 *
 * <p>这样可以解决模块路径前缀配置的时机问题：
 * <ul>
 *   <li>模块信息在编译时通过注解处理器生成到 META-INF/afg-modules.index</li>
 *   <li>本处理器在环境初始化阶段读取索引文件</li>
 *   <li>ModuleWebAutoConfiguration 在 WebMvc 初始化时可以获取到模块列表</li>
 * </ul>
 *
 * <p>索引文件格式：{@code moduleId:configFile:className}
 */
public class ModuleEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ModuleEnvironmentPostProcessor.class);

    private static final String INDEX_FILE = "META-INF/afg-modules.index";
    private static final String MODULE_DEFINITIONS_PROPERTY = "afg.modules.definitions";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        log.info("ModuleEnvironmentPostProcessor.postProcessEnvironment called");
        List<ModuleDefinitionInfo> modules = loadModuleDefinitions();

        if (!modules.isEmpty()) {
            // 存储到环境属性中
            environment.getSystemProperties().put(MODULE_DEFINITIONS_PROPERTY, modules);
            log.info("Loaded {} module definitions from index: {}", modules.size(),
                    modules.stream().map(m -> m.moduleId).toList());
        } else {
            log.warn("No module definitions found in index");
        }
    }

    /**
     * 从编译时生成的索引文件加载模块定义
     */
    private List<ModuleDefinitionInfo> loadModuleDefinitions() {
        List<ModuleDefinitionInfo> modules = new ArrayList<>();

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
                            ModuleDefinitionInfo info = parseModuleDefinition(line);
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
     * 解析模块定义
     * 格式: moduleId:configFile:className
     */
    private ModuleDefinitionInfo parseModuleDefinition(String line) {
        String[] parts = line.split(":");
        if (parts.length >= 3) {
            String moduleId = parts[0];
            String configFile = parts[1];
            String className = parts[2];

            // 从类名推断 basePackage
            String basePackage = extractBasePackage(className);
            // 默认 contextPath 为 /{moduleId}-api
            String contextPath = "/" + moduleId + "-api";

            return new ModuleDefinitionInfo(moduleId, basePackage, contextPath, configFile, className);
        }
        return null;
    }

    /**
     * 从类名提取 basePackage
     * 例如: io.github.afgprojects.auth.AuthModuleConfig -> io.github.afgprojects.auth
     */
    private String extractBasePackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : className;
    }

    /**
     * 模块定义信息（简化版，只包含路径前缀配置所需的信息）
     *
     * @param moduleId   模块 ID
     * @param basePackage 基础包名
     * @param contextPath 上下文路径前缀
     * @param configFile  配置文件名
     * @param className   配置类名
     */
    public record ModuleDefinitionInfo(
            String moduleId,
            String basePackage,
            String contextPath,
            String configFile,
            String className
    ) {}
}
