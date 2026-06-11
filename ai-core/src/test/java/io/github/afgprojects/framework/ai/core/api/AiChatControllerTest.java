package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.chat.ChatRequest;
import io.github.afgprojects.framework.ai.core.dto.chat.CreateConversationRequest;
import io.github.afgprojects.framework.ai.core.entity.chat.ChatLogEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiChatController 集成测试
 *
 * <p>测试对话会话管理、消息发送、对话日志查询等接口。
 * AI 依赖的测试（消息发送）在 Ollama 不可用时自动跳过。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiChatControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Conversation Management (no AI needed) ====================

    @Test
    void shouldCreateConversation_whenPostValidRequest() {
        // Arrange
        CreateConversationRequest request = new CreateConversationRequest();
        request.setApplicationId("test-app-" + UUID.randomUUID());
        request.setTitle("Test Conversation");

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restClient().post()
            .uri("/chat/conversations")
            .body(request)
            .retrieve()
            .body(Map.class);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("conversationId")).isNotNull();
        assertThat(result.get("userId")).isEqualTo("system");
        assertThat(result.get("state")).isEqualTo("ACTIVE");
        assertThat(result.get("metadata")).isNotNull();

        // Cleanup - delete the session
        String conversationId = (String) result.get("conversationId");
        restClient().delete()
            .uri("/chat/conversations/{id}", conversationId)
            .retrieve()
            .toBodilessEntity();
    }

    @Test
    void shouldListConversations_whenGetAll() {
        // Arrange - create 2 conversations
        CreateConversationRequest request1 = new CreateConversationRequest();
        request1.setApplicationId("list-app-" + UUID.randomUUID());
        request1.setTitle("List Test 1");

        @SuppressWarnings("unchecked")
        Map<String, Object> conv1 = restClient().post()
            .uri("/chat/conversations")
            .body(request1)
            .retrieve()
            .body(Map.class);

        CreateConversationRequest request2 = new CreateConversationRequest();
        request2.setApplicationId("list-app-" + UUID.randomUUID());
        request2.setTitle("List Test 2");

        @SuppressWarnings("unchecked")
        Map<String, Object> conv2 = restClient().post()
            .uri("/chat/conversations")
            .body(request2)
            .retrieve()
            .body(Map.class);

        // Act
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conversations = restClient().get()
            .uri("/chat/conversations")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(conversations).isNotNull();
        assertThat(conversations.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        restClient().delete()
            .uri("/chat/conversations/{id}", conv1.get("conversationId"))
            .retrieve()
            .toBodilessEntity();
        restClient().delete()
            .uri("/chat/conversations/{id}", conv2.get("conversationId"))
            .retrieve()
            .toBodilessEntity();
    }

    @Test
    void shouldReturnConversation_whenGetById() {
        // Arrange - create conversation
        CreateConversationRequest request = new CreateConversationRequest();
        request.setApplicationId("get-app-" + UUID.randomUUID());
        request.setTitle("Get Test");

        @SuppressWarnings("unchecked")
        Map<String, Object> created = restClient().post()
            .uri("/chat/conversations")
            .body(request)
            .retrieve()
            .body(Map.class);

        String conversationId = (String) created.get("conversationId");

        // Act
        @SuppressWarnings("unchecked")
        Map<String, Object> found = restClient().get()
            .uri("/chat/conversations/{id}", conversationId)
            .retrieve()
            .body(Map.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.get("sessionId")).isEqualTo(conversationId);
        assertThat(found.get("userId")).isEqualTo("system");

        // Cleanup
        restClient().delete()
            .uri("/chat/conversations/{id}", conversationId)
            .retrieve()
            .toBodilessEntity();
    }

    @Test
    void shouldDeleteConversation_whenDeleteById() {
        // Arrange - create conversation
        CreateConversationRequest request = new CreateConversationRequest();
        request.setApplicationId("delete-app-" + UUID.randomUUID());
        request.setTitle("Delete Test");

        @SuppressWarnings("unchecked")
        Map<String, Object> created = restClient().post()
            .uri("/chat/conversations")
            .body(request)
            .retrieve()
            .body(Map.class);

        String conversationId = (String) created.get("conversationId");

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/chat/conversations/{id}", conversationId)
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        assertThatThrownBy(() -> restClient().get()
                .uri("/chat/conversations/{id}", conversationId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Chat (needs Ollama) ====================

    @Test
    void shouldSendMessage_whenPostMessage() {
        assumeOllamaAvailable();

        // Arrange - create conversation
        CreateConversationRequest convRequest = new CreateConversationRequest();
        convRequest.setApplicationId("chat-app-" + UUID.randomUUID());
        convRequest.setTitle("Chat Test");

        @SuppressWarnings("unchecked")
        Map<String, Object> conv = restClient().post()
            .uri("/chat/conversations")
            .body(convRequest)
            .retrieve()
            .body(Map.class);

        String conversationId = (String) conv.get("conversationId");

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage("hello");
        chatRequest.setStream(false);

        try {
            // Act
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restClient().post()
                .uri("/chat/conversations/{id}/messages", conversationId)
                .body(chatRequest)
                .retrieve()
                .body(Map.class);

            // Assert - should return a pipeline result with content
            assertThat(result).isNotNull();
            assertThat(result.get("content")).isNotNull();
        } finally {
            // Cleanup
            restClient().delete()
                .uri("/chat/conversations/{id}", conversationId)
                .retrieve()
                .toBodilessEntity();
        }
    }

    @Test
    void shouldStreamMessage_whenPostMessageWithStream() {
        assumeOllamaAvailable();

        // Arrange - create conversation
        CreateConversationRequest convRequest = new CreateConversationRequest();
        convRequest.setApplicationId("stream-app-" + UUID.randomUUID());
        convRequest.setTitle("Stream Test");

        @SuppressWarnings("unchecked")
        Map<String, Object> conv = restClient().post()
            .uri("/chat/conversations")
            .body(convRequest)
            .retrieve()
            .body(Map.class);

        String conversationId = (String) conv.get("conversationId");

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage("hello");
        chatRequest.setStream(true);

        try {
            // Act - stream request returns SSE (text/event-stream)
            // For SSE testing, we verify the endpoint accepts the request and returns a response
            ResponseEntity<String> response = restClient().post()
                .uri("/chat/conversations/{id}/messages", conversationId)
                .body(chatRequest)
                .retrieve()
                .toEntity(String.class);

            // Assert - should return some content (SSE or error, both are valid responses)
            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.PARTIAL_CONTENT);
        } finally {
            // Cleanup
            restClient().delete()
                .uri("/chat/conversations/{id}", conversationId)
                .retrieve()
                .toBodilessEntity();
        }
    }

    // ==================== Logs (no AI needed) ====================

    @Test
    void shouldListLogs_whenGetLogs() {
        // Arrange - create a ChatLogEntity via DataManager
        ChatLogEntity logEntity = new ChatLogEntity();
        logEntity.setApplicationId(1L);
        logEntity.setSessionId("session-" + UUID.randomUUID());
        logEntity.setUserId("test-user");
        logEntity.setQuestion("test question");
        logEntity.setAnswer("test answer");
        logEntity = dataManager.save(ChatLogEntity.class, logEntity);

        try {
            // Act
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> logs = restClient().get()
                .uri("/chat/logs")
                .retrieve()
                .body(List.class);

            // Assert
            assertThat(logs).isNotNull();
            assertThat(logs.size()).isGreaterThanOrEqualTo(1);
        } finally {
            // Cleanup
            dataManager.deleteById(ChatLogEntity.class, logEntity.getId());
        }
    }

    @Test
    void shouldVoteLog_whenPutVote() {
        // Arrange - create a ChatLogEntity via DataManager
        ChatLogEntity logEntity = new ChatLogEntity();
        logEntity.setSessionId("vote-session-" + UUID.randomUUID());
        logEntity.setUserId("test-user");
        logEntity.setQuestion("vote test question");
        logEntity.setAnswer("vote test answer");
        logEntity = dataManager.save(ChatLogEntity.class, logEntity);

        try {
            // Act - vote on the log
            Map<String, Object> voteRequest = Map.of("vote", 1, "reason", "helpful");

            ChatLogEntity voted = restClient().put()
                .uri("/chat/logs/{id}/vote", logEntity.getId())
                .body(voteRequest)
                .retrieve()
                .body(ChatLogEntity.class);

            // Assert
            assertThat(voted).isNotNull();
            assertThat(voted.getId()).isEqualTo(logEntity.getId());
            assertThat(voted.getVote()).isEqualTo(1);
            assertThat(voted.getVoteReason()).isEqualTo("helpful");
        } finally {
            // Cleanup
            dataManager.deleteById(ChatLogEntity.class, logEntity.getId());
        }
    }
}
