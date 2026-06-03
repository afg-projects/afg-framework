package io.github.afgprojects.framework.ai.core.scenario;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.chat.ChatRequest;
import io.github.afgprojects.framework.ai.core.dto.chat.CreateConversationRequest;
import io.github.afgprojects.framework.ai.core.dto.model.CreateModelConfigRequest;
import io.github.afgprojects.framework.ai.core.dto.model.CreateProviderRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationRequest;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelConfigEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelProviderEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 全流程端到端场景测试
 *
 * <p>验证完整的业务链路：Model Provider → Model Config → Application → Conversation → Chat。
 * 每一步依赖上一步的结果，确保各模块协同工作。
 *
 * <p>需要 Ollama 运行在 localhost:11434，否则自动跳过。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiFullFlowTest extends AbstractAiWebTest {

    @Test
    void shouldCompleteFullFlow_whenCreateModelToChat() {
        assumeOllamaAvailable();

        // Step 1: Create Model Provider
        CreateProviderRequest providerReq = new CreateProviderRequest();
        providerReq.setProviderName("Full Flow Test Ollama");
        providerReq.setProviderType("OLLAMA");
        providerReq.setBaseUrl("http://localhost:11434");
        providerReq.setEnabled(true);

        ModelProviderEntity provider = restClient().post()
            .uri("/models/providers")
            .body(providerReq)
            .retrieve()
            .body(ModelProviderEntity.class);

        assertThat(provider).isNotNull();
        assertThat(provider.getId()).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("Full Flow Test Ollama");
        assertThat(provider.getProviderType()).isEqualTo("OLLAMA");

        // Step 2: Create Model Config
        CreateModelConfigRequest configReq = new CreateModelConfigRequest();
        configReq.setProviderId(provider.getId());
        configReq.setModelName("qwen3:0.6b");
        configReq.setDisplayName("Qwen3 0.6B");
        configReq.setModelType("CHAT");
        configReq.setEnabled(true);

        ModelConfigEntity config = restClient().post()
            .uri("/models/configs")
            .body(configReq)
            .retrieve()
            .body(ModelConfigEntity.class);

        assertThat(config).isNotNull();
        assertThat(config.getId()).isNotNull();
        assertThat(config.getModelName()).isEqualTo("qwen3:0.6b");
        assertThat(config.getProviderId()).isEqualTo(provider.getId());

        // Step 3: Create Application
        CreateApplicationRequest appReq = new CreateApplicationRequest();
        appReq.setName("Full Flow Test App");
        appReq.setType("CHAT");
        appReq.setDescription("End-to-end full flow test application");

        ApplicationEntity app = restClient().post()
            .uri("/resources/applications")
            .body(appReq)
            .retrieve()
            .body(ApplicationEntity.class);

        assertThat(app).isNotNull();
        assertThat(app.getId()).isNotNull();
        assertThat(app.getName()).isEqualTo("Full Flow Test App");

        // Step 4: Create Conversation
        CreateConversationRequest convReq = new CreateConversationRequest();
        convReq.setApplicationId(app.getId().toString());
        convReq.setTitle("Full Flow Test Conversation");

        @SuppressWarnings("unchecked")
        Map<String, Object> conversation = restClient().post()
            .uri("/chat/conversations")
            .body(convReq)
            .retrieve()
            .body(Map.class);

        assertThat(conversation).isNotNull();
        assertThat(conversation.get("conversationId")).isNotNull();
        assertThat(conversation.get("state")).isEqualTo("ACTIVE");

        String conversationId = (String) conversation.get("conversationId");

        try {
            // Step 5: Send Chat Message (calls Ollama)
            ChatRequest chatReq = new ChatRequest();
            chatReq.setMessage("Hello, please respond with just 'hi'");
            chatReq.setStream(false);

            @SuppressWarnings("unchecked")
            Map<String, Object> chatResult = restClient().post()
                .uri("/chat/conversations/{id}/messages", conversationId)
                .body(chatReq)
                .retrieve()
                .body(Map.class);

            assertThat(chatResult).isNotNull();
            assertThat(chatResult.get("content")).isNotNull();
            assertThat(chatResult.get("content").toString()).isNotBlank();
        } finally {
            // Cleanup: delete conversation
            restClient().delete()
                .uri("/chat/conversations/{id}", conversationId)
                .retrieve()
                .toBodilessEntity();
        }
    }
}
