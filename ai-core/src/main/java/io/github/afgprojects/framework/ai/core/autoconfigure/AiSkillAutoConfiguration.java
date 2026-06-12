package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.skill.*;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.skill.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AFG AI 技能自动配置。
 *
 * <p>配置前缀：{@code afg.ai.skill}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiToolAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiSkillAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SkillRegistry.class)
    public DefaultSkillRegistry defaultSkillRegistry() {
        return new DefaultSkillRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(IntentAnalyzer.class)
    @ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IntentAnalyzer defaultIntentAnalyzer(
            SkillRegistry skillRegistry,
            @Autowired(required = false) AfgChatClient chatClient) {
        if (chatClient == null) {
            return new NoOpIntentAnalyzer();
        }
        return new DefaultIntentAnalyzer(skillRegistry, chatClient);
    }

    @Bean
    @ConditionalOnMissingBean(SkillExecutor.class)
    @ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SkillExecutor defaultSkillExecutor(
            SkillRegistry skillRegistry,
            @Autowired(required = false) AfgChatClient chatClient,
            @Autowired(required = false) ToolRegistry toolRegistry) {
        if (chatClient == null) {
            return new NoOpSkillExecutor(skillRegistry);
        }
        return new DefaultSkillExecutor(skillRegistry, chatClient, toolRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(SkillDispatcher.class)
    @ConditionalOnProperty(prefix = "afg.ai.skill", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SkillDispatcher defaultSkillDispatcher(
            IntentAnalyzer intentAnalyzer,
            SkillExecutor skillExecutor,
            SkillRegistry skillRegistry,
            @Autowired(required = false) AfgChatClient chatClient) {
        if (chatClient == null) {
            return new NoOpSkillDispatcher(intentAnalyzer, skillRegistry);
        }
        return new DefaultSkillDispatcher(intentAnalyzer, skillExecutor, skillRegistry, chatClient);
    }
}