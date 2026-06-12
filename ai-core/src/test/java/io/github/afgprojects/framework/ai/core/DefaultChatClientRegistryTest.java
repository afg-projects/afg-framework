package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.DefaultChatClientRegistry;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultChatClientRegistry 纯单元测试
 *
 * <p>使用匿名 AfgChatClient 实现进行测试，不依赖真实 LLM。
 */
@DisplayName("DefaultChatClientRegistry")
class DefaultChatClientRegistryTest {

    /** 创建一个最小的 AfgChatClient 桩（非 mock，只是匿名实现） */
    private AfgChatClient stubClient() {
        return new AfgChatClient() {
            @Override public AiChatResponse chat(String userMessage) { return null; }
            @Override public AiChatResponse chat(String conversationId, String userMessage) { return null; }
            @Override public AiChatResponse chat(io.github.afgprojects.framework.ai.core.api.chat.AiMessage message) { return null; }
            @Override public AiChatResponse chat(java.util.List<io.github.afgprojects.framework.ai.core.api.chat.AiMessage> messages) { return null; }
            @Override public reactor.core.publisher.Flux<String> chatStream(String userMessage) { return reactor.core.publisher.Flux.empty(); }
            @Override public reactor.core.publisher.Flux<String> chatStream(String conversationId, String userMessage) { return reactor.core.publisher.Flux.empty(); }
            @Override public <T> T chat(String userMessage, Class<T> responseType) { return null; }
            @Override public ChatRequestSpec prompt(String userMessage) { return null; }
            @Override public AfgChatClient withSystemPrompt(String systemPrompt) { return this; }
            @Override public AfgChatClient withModel(String modelName) { return this; }
            @Override public AfgChatClient withConversationId(String conversationId) { return this; }
        };
    }

    private final DefaultChatClientRegistry registry = new DefaultChatClientRegistry();

    @Nested
    @DisplayName("register + get")
    class RegisterAndGet {

        @Test
        @DisplayName("应注册并获取 ChatClient")
        void shouldRegisterAndGetClient() {
            var client = stubClient();
            registry.register("openai", client);

            var result = registry.get("openai");

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(client);
        }

        @Test
        @DisplayName("未注册的名称应返回 empty")
        void shouldReturnEmpty_whenNotRegistered() {
            assertThat(registry.get("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefault")
    class GetDefault {

        @Test
        @DisplayName("第一个注册的应为默认")
        void shouldFirstRegisteredBeDefault() {
            var client1 = stubClient();
            var client2 = stubClient();
            registry.register("openai", client1);
            registry.register("anthropic", client2);

            assertThat(registry.getDefault()).isSameAs(client1);
        }

        @Test
        @DisplayName("无注册时应抛异常")
        void shouldThrow_whenNoClientRegistered() {
            assertThatThrownBy(registry::getDefault)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("setDefault")
    class SetDefault {

        @Test
        @DisplayName("应切换默认 ChatClient")
        void shouldSwitchDefault() {
            var client1 = stubClient();
            var client2 = stubClient();
            registry.register("openai", client1);
            registry.register("anthropic", client2);
            registry.setDefault("anthropic");

            assertThat(registry.getDefault()).isSameAs(client2);
        }

        @Test
        @DisplayName("设置未注册的名称为默认应抛异常")
        void shouldThrow_whenSettingUnregisteredAsDefault() {
            assertThatThrownBy(() -> registry.setDefault("nonexistent"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not registered");
        }
    }

    @Nested
    @DisplayName("listNames")
    class ListNames {

        @Test
        @DisplayName("应列出所有注册名称")
        void shouldListAllNames() {
            registry.register("openai", stubClient());
            registry.register("anthropic", stubClient());

            assertThat(registry.listNames()).containsExactlyInAnyOrder("openai", "anthropic");
        }

        @Test
        @DisplayName("空注册表应返回空列表")
        void shouldReturnEmptyList_whenNoClients() {
            assertThat(registry.listNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("应移除已注册的 ChatClient")
        void shouldRemoveRegisteredClient() {
            registry.register("openai", stubClient());
            registry.remove("openai");

            assertThat(registry.get("openai")).isEmpty();
        }

        @Test
        @DisplayName("移除不存在的名称应不抛异常")
        void shouldNotThrow_whenRemovingNonexistent() {
            assertThatCode(() -> registry.remove("nonexistent")).doesNotThrowAnyException();
        }
    }
}
