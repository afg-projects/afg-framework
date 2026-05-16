package io.github.afgprojects.framework.core.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
import io.github.afgprojects.framework.core.module.ModuleDefinitionLoader;
import io.github.afgprojects.framework.core.module.ModuleDefinitionInfo;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.util.JacksonMapper;
import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * AFG 平台自动配置类
 * 自动注册核心组件
 */
@AutoConfiguration
@Slf4j
public class AfgAutoConfiguration {

    static {
        loadModuleDefinitionsEarly();
    }

    private static void loadModuleDefinitionsEarly() {
        java.util.List<ModuleDefinitionInfo> modules = ModuleDefinitionLoader.loadModuleDefinitions();

        if (!modules.isEmpty()) {
            System.setProperty(ModuleDefinitionLoader.MODULE_DEFINITIONS_PROPERTY,
                    ModuleDefinitionLoader.serializeModules(modules));
            log.info("Pre-loaded {} module definitions from index: {}", modules.size(),
                    modules.stream().map(ModuleDefinitionInfo::moduleId).toList());
        }
    }

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
     */
    public static class JacksonUtilsBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
            if (bean instanceof ObjectMapper mapper) {
                try {
                    JacksonUtils.setObjectMapper(mapper);
                } catch (IllegalStateException e) {
                    log.debug("JacksonUtils already initialized with a different ObjectMapper instance, " +
                             "current context will use its own ObjectMapper bean");
                }
            }
            return bean;
        }
    }
}
