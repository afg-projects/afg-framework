package io.github.afgprojects.framework.ai.chat.autoconfigure;

import io.github.afgprojects.framework.ai.core.autoconfigure.AiConfigurationProperties;
import io.github.afgprojects.framework.ai.chat.DefaultAfgChatClient;
import io.github.afgprojects.framework.ai.chat.DefaultChatClientRegistry;
import io.github.afgprojects.framework.ai.chat.model.DefaultModelRegistry;
import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.model.ModelType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ChatClient 自动配置
 *
 * <p>当 classpath 上存在 {@link ChatClient} 且 {@code afg.ai.chat.enabled=true} 时自动激活。
 * 在 Spring AI 各 Provider 的自动配置之后执行。
 *
 * <p>LLM Provider 配置全部走 Spring AI 原生：
 * <pre>{@code
 * spring:
 *   ai:
 *     openai:
 *       api-key: ${OPENAI_API_KEY}
 *       chat:
 *         options:
 *           model: gpt-4
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties({AiConfigurationProperties.class, ChatClientProperties.class})
@ConditionalOnClass(ChatClient.class)
@ConditionalOnProperty(prefix = "afg.ai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatModel chatModel,
                                  ChatClientProperties properties,
                                  ObjectProvider<org.springframework.ai.chat.client.advisor.api.Advisor> advisors) {
        var builder = ChatClient.builder(chatModel);
        if (properties.getDefaultSystemPrompt() != null) {
            builder.defaultSystem(properties.getDefaultSystemPrompt());
        }
        advisors.orderedStream().forEach(builder::defaultAdvisors);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(AfgChatClient.class)
    @ConditionalOnBean(ChatClient.class)
    public AfgChatClient afgChatClient(ChatClient chatClient) {
        return new DefaultAfgChatClient(chatClient);
    }

    @Bean
    @ConditionalOnMissingBean(ChatClientRegistry.class)
    public ChatClientRegistry chatClientRegistry() {
        return new DefaultChatClientRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(ModelRegistry.class)
    public ModelRegistry modelRegistry() {
        return new DefaultModelRegistry();
    }

    @Bean
    @ConditionalOnBean({ChatModel.class, ChatClientRegistry.class, ModelRegistry.class})
    public ChatModelRegistrar chatModelRegistrar(
            ChatModel chatModel,
            ChatClientRegistry registry,
            ModelRegistry modelRegistry,
            ChatClientProperties properties) {
        return new ChatModelRegistrar(chatModel, registry, modelRegistry, properties);
    }

    static class ChatModelRegistrar {

        ChatModelRegistrar(
                ChatModel chatModel,
                ChatClientRegistry registry,
                ModelRegistry modelRegistry,
                ChatClientProperties properties) {
            var client = new DefaultAfgChatClient(ChatClient.create(chatModel));
            var name = properties.getDefaultName() != null ? properties.getDefaultName() : "default-chat";
            registry.register(name, client);

            var info = DefaultModelInfo.of(name, ModelType.CHAT);
            modelRegistry.registerModel(name, info);
            modelRegistry.setDefault(name, ModelType.CHAT);
        }
    }
}