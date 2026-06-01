package io.github.afgprojects.framework.ai.agent.skill.autoconfigure;

import io.github.afgprojects.framework.ai.agent.skill.DefaultSkillExecutor;
import io.github.afgprojects.framework.ai.agent.skill.DefaultSkillRegistry;
import io.github.afgprojects.framework.ai.agent.skill.SkillExecutor;
import io.github.afgprojects.framework.ai.agent.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
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
@AutoConfiguration(after = io.github.afgprojects.framework.ai.agent.autoconfigure.AiAutoConfiguration.class)
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
     * <p>使用 AfgChatClient 进行对话
     */
    @Bean
    @ConditionalOnMissingBean(SkillExecutor.class)
    public SkillExecutor skillExecutor(
            @NonNull SkillRegistry skillRegistry,
            @Nullable AfgChatClient chatClient,
            @Nullable ToolRegistry toolRegistry) {

        if (chatClient == null) {
            throw new IllegalStateException("No AfgChatClient available");
        }

        log.info("Creating default skill executor with AfgChatClient");
        return new DefaultSkillExecutor(skillRegistry, chatClient, toolRegistry);
    }
}
