package io.github.afgprojects.framework.core.env;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import io.github.afgprojects.framework.core.module.ModuleDefinitionLoader;
import io.github.afgprojects.framework.core.module.ModuleDefinitionInfo;

/**
 * 模块上下文初始化器
 *
 * <p>在 Spring 上下文初始化阶段（早于任何 bean 创建）加载模块定义信息，
 * 并存储到环境属性中，供 {@link io.github.afgprojects.framework.core.web.module.ModuleWebAutoConfiguration} 使用。
 */
@Slf4j
public class ModuleApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        log.info("ModuleApplicationContextInitializer.initialize called");
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        List<ModuleDefinitionInfo> modules = ModuleDefinitionLoader.loadModuleDefinitions();

        if (!modules.isEmpty()) {
            environment.getSystemProperties().put(ModuleDefinitionLoader.MODULE_DEFINITIONS_PROPERTY, modules);
            log.info("Loaded {} module definitions from index: {}", modules.size(),
                    modules.stream().map(ModuleDefinitionInfo::moduleId).toList());
        } else {
            log.warn("No module definitions found in index");
        }
    }
}
