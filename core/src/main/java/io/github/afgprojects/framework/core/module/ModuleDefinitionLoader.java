package io.github.afgprojects.framework.core.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 模块定义加载器
 *
 * <p>从编译时生成的索引文件加载模块定义信息。
 * 索引文件格式：{@code moduleId:configFile:className}
 */
@Slf4j
public final class ModuleDefinitionLoader {

    public static final String INDEX_FILE = "META-INF/afg-modules.index";
    public static final String MODULE_DEFINITIONS_PROPERTY = "afg.modules.definitions";

    private ModuleDefinitionLoader() {
    }

    /**
     * 从索引文件加载模块定义
     */
    public static List<ModuleDefinitionInfo> loadModuleDefinitions() {
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
     *
     * <p>支持两种索引格式：
     * <ul>
     *   <li>新格式（含 contextPath）: moduleId:configFile:className:contextPath</li>
     *   <li>旧格式（不含 contextPath）: moduleId:configFile:className，contextPath 默认为 "/{moduleId}-api"</li>
     * </ul>
     */
    public static ModuleDefinitionInfo parseModuleDefinition(String line) {
        String[] parts = line.split(":");
        if (parts.length >= 3) {
            String moduleId = parts[0];
            String configFile = parts[1];
            String className = parts[2];

            String basePackage = extractBasePackage(className);
            String contextPath = parts.length >= 4 ? parts[3] : "/" + moduleId + "-api";

            return new ModuleDefinitionInfo(moduleId, basePackage, contextPath, configFile, className);
        }
        return null;
    }

    /**
     * 从类名提取 basePackage
     */
    public static String extractBasePackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : className;
    }

    /**
     * 序列化模块列表为字符串
     */
    public static String serializeModules(List<ModuleDefinitionInfo> modules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) sb.append(";");
            ModuleDefinitionInfo m = modules.get(i);
            sb.append(m.moduleId()).append(",")
              .append(m.basePackage()).append(",")
              .append(m.contextPath());
        }
        return sb.toString();
    }
}
