package io.github.afgprojects.framework.core.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigRefresher;
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
