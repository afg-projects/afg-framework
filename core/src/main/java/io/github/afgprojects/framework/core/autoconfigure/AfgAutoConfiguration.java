package io.github.afgprojects.framework.core.autoconfigure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigRefresher;
import io.github.afgprojects.framework.core.module.AfgModuleProcessor;
import io.github.afgprojects.framework.core.module.ModuleContext;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.util.JacksonMapper;
import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * AFG 平台自动配置类
 * 自动注册核心组件
 */
@AutoConfiguration
public class AfgAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AfgAutoConfiguration.class);
    private static final String INDEX_FILE = "META-INF/afg-modules.index";
    private static final String MODULE_DEFINITIONS_PROPERTY = "afg.modules.definitions";

    // 静态初始化块：在类加载时提前加载模块定义
    static {
        loadModuleDefinitionsEarly();
    }

    /**
     * 提前加载模块定义
     * 在任何 bean 创建之前，从索引文件加载模块信息并存储到系统属性
     */
    private static void loadModuleDefinitionsEarly() {
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

            if (!modules.isEmpty()) {
                System.setProperty(MODULE_DEFINITIONS_PROPERTY, serializeModules(modules));
                log.info("Pre-loaded {} module definitions from index: {}", modules.size(),
                        modules.stream().map(m -> m.moduleId).toList());
            }
        } catch (IOException e) {
            log.warn("Failed to pre-load module index: {}", e.getMessage());
        }
    }

    private static ModuleDefinitionInfo parseModuleDefinition(String line) {
        String[] parts = line.split(":");
        if (parts.length >= 3) {
            String moduleId = parts[0];
            String configFile = parts[1];
            String className = parts[2];
            String basePackage = extractBasePackage(className);
            String contextPath = "/" + moduleId + "-api";
            return new ModuleDefinitionInfo(moduleId, basePackage, contextPath, configFile, className);
        }
        return null;
    }

    private static String extractBasePackage(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : className;
    }

    private static String serializeModules(List<ModuleDefinitionInfo> modules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) sb.append(";");
            ModuleDefinitionInfo m = modules.get(i);
            sb.append(m.moduleId).append(",")
              .append(m.basePackage).append(",")
              .append(m.contextPath);
        }
        return sb.toString();
    }

    /**
     * 模块定义信息
     */
    record ModuleDefinitionInfo(
            String moduleId,
            String basePackage,
            String contextPath,
            String configFile,
            String className
    ) {}

    @Bean
    @ConditionalOnMissingBean
    public ModuleRegistry moduleRegistry() {
        return new ModuleRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AfgConfigRegistry afgConfigRegistry() {
        return new AfgConfigRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigRefresher configRefresher(AfgConfigRegistry configRegistry) {
        return new ConfigRefresher(configRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleContext moduleContext(ModuleRegistry moduleRegistry, ApplicationContext applicationContext) {
        return new ModuleContext(moduleRegistry, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return JacksonMapper.builder().build();
    }

    @Bean
    public JacksonUtilsBeanPostProcessor jacksonUtilsBeanPostProcessor() {
        return new JacksonUtilsBeanPostProcessor();
    }

    @Bean
    public AfgModuleProcessor afgModuleProcessor(ModuleRegistry moduleRegistry, ApplicationContext applicationContext) {
        return new AfgModuleProcessor(moduleRegistry, applicationContext);
    }

    /**
     * BeanPostProcessor to inject ObjectMapper into JacksonUtils
     * <p>
     * 幂等操作：如果 JacksonUtils 已初始化且是相同实例，则忽略；
     * 如果是不同实例则记录警告并跳过（支持多上下文场景如测试）。
     */
    public static class JacksonUtilsBeanPostProcessor implements BeanPostProcessor {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(JacksonUtilsBeanPostProcessor.class);

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
            if (bean instanceof ObjectMapper mapper) {
                try {
                    JacksonUtils.setObjectMapper(mapper);
                } catch (IllegalStateException e) {
                    // 多上下文场景（如测试）下，不同上下文可能创建不同的 ObjectMapper
                    // 此时记录警告并跳过，不影响当前上下文正常工作
                    log.debug("JacksonUtils already initialized with a different ObjectMapper instance, " +
                             "current context will use its own ObjectMapper bean");
                }
            }
            return bean;
        }
    }
}
