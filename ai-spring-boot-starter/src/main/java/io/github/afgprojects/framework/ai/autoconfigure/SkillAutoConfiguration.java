package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.agent.skill.DefaultSkillExecutor;
import io.github.afgprojects.framework.ai.agent.skill.DefaultSkillRegistry;
import io.github.afgprojects.framework.ai.agent.skill.SkillExecutor;
import io.github.afgprojects.framework.ai.agent.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmClientRegistry;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Skill 自动配置
 *
 * <p>自动配置 SkillRegistry 和 SkillExecutor。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiAutoConfiguration.class)
@EnableConfigurationProperties(SkillProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true")
public class SkillAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SkillAutoConfiguration.class);

    /**
     * 配置 Skill 注册表
     */
    @Bean
    @ConditionalOnMissingBean(SkillRegistry.class)
    public SkillRegistry skillRegistry(SkillProperties properties) {
        log.info("Creating default skill registry");
        DefaultSkillRegistry registry = new DefaultSkillRegistry();

        // 从配置的目录加载 skills
        String path = properties.getSkillsPath();
        if (path != null && !path.isEmpty()) {
            // 支持 classpath 加载
            if (path.startsWith("classpath:")) {
                loadFromClasspath(registry, path);
            } else {
                try {
                    int count = registry.loadFromDirectory(java.nio.file.Path.of(path));
                    log.info("Loaded {} skills from: {}", count, path);
                } catch (IOException e) {
                    log.warn("Failed to load skills from: {}", path, e);
                }
            }
        }

        return registry;
    }

    /**
     * 从 classpath 加载 skills
     */
    private void loadFromClasspath(DefaultSkillRegistry registry, String classpathLocation) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = classpathLocation.replace("classpath:", "classpath*:") + "/*.yaml";
            Resource[] resources = resolver.getResources(pattern);

            int count = 0;
            for (Resource resource : resources) {
                try {
                    registry.loadFromYaml(resource.getInputStream());
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to load skill from: {}", resource.getFilename(), e);
                }
            }

            log.info("Loaded {} skills from classpath: {}", count, classpathLocation);
        } catch (IOException e) {
            log.warn("Failed to load skills from classpath: {}", classpathLocation, e);
        }
    }

    /**
     * 配置 Skill 执行器
     *
     * <p>优先使用 LlmClientRegistry.getDefault()，其次使用单独的 LlmClient
     */
    @Bean
    @ConditionalOnMissingBean(SkillExecutor.class)
    public SkillExecutor skillExecutor(
            @NonNull SkillRegistry skillRegistry,
            @Nullable LlmClient llmClient,
            @Nullable LlmClientRegistry llmClientRegistry,
            @Nullable ToolRegistry toolRegistry) {

        // 优先使用 LlmClientRegistry
        LlmClient client;
        if (llmClientRegistry != null) {
            client = llmClientRegistry.getDefault();
            log.info("Creating default skill executor with LlmClientRegistry");
        } else if (llmClient != null) {
            client = llmClient;
            log.info("Creating default skill executor with LlmClient");
        } else {
            throw new IllegalStateException("No LlmClient or LlmClientRegistry available");
        }

        return new DefaultSkillExecutor(skillRegistry, client, toolRegistry);
    }
}
