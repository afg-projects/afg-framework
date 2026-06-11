package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentSessionEntity;
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
 * AiAgentController 集成测试
 *
 * <p>测试 Agent 定义 CRUD、会话管理、执行等接口。
 * AI 依赖的测试（Agent 执行）在 Ollama 不可用时自动跳过。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiAgentControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Agent Definition CRUD (no AI needed) ====================

    @Test
    void shouldCreateDefinition_whenPostValidRequest() {
        // Arrange
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("test-agent-" + UUID.randomUUID());
        definition.setDescription("Test agent description");
        definition.setSystemPrompt("You are a helpful assistant.");
        definition.setMaxIterations(5);

        // Act
        AgentDefinitionEntity created = restClient().post()
            .uri("/agents/definitions")
            .body(definition)
            .retrieve()
            .body(AgentDefinitionEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(definition.getName());
        assertThat(created.getDescription()).isEqualTo("Test agent description");
        assertThat(created.getSystemPrompt()).isEqualTo("You are a helpful assistant.");
        assertThat(created.getMaxIterations()).isEqualTo(5);

        // Cleanup
        dataManager.deleteById(AgentDefinitionEntity.class, created.getId());
    }

    @Test
    void shouldListDefinitions_whenGetAll() {
        // Arrange - create 2 definitions via DataManager
        String prefix = "list-agent-" + UUID.randomUUID();

        AgentDefinitionEntity def1 = new AgentDefinitionEntity();
        def1.setName(prefix + "-1");
        def1 = dataManager.save(AgentDefinitionEntity.class, def1);

        AgentDefinitionEntity def2 = new AgentDefinitionEntity();
        def2.setName(prefix + "-2");
        def2 = dataManager.save(AgentDefinitionEntity.class, def2);

        try {
            // Act
            List<AgentDefinitionEntity> definitions = restClient().get()
                .uri("/agents/definitions")
                .retrieve()
                .body(List.class);

            // Assert
            assertThat(definitions).isNotNull();
            assertThat(definitions.size()).isGreaterThanOrEqualTo(2);
        } finally {
            // Cleanup
            dataManager.deleteById(AgentDefinitionEntity.class, def1.getId());
            dataManager.deleteById(AgentDefinitionEntity.class, def2.getId());
        }
    }

    @Test
    void shouldReturnDefinition_whenGetById() {
        // Arrange - create definition via DataManager
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("get-agent-" + UUID.randomUUID());
        definition.setDescription("Get test agent");
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        try {
            // Act
            AgentDefinitionEntity found = restClient().get()
                .uri("/agents/definitions/{id}", definition.getId())
                .retrieve()
                .body(AgentDefinitionEntity.class);

            // Assert
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(definition.getId());
            assertThat(found.getName()).isEqualTo(definition.getName());
            assertThat(found.getDescription()).isEqualTo("Get test agent");
        } finally {
            // Cleanup
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }

    @Test
    void shouldUpdateDefinition_whenPutValidRequest() {
        // Arrange - create definition
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("update-agent-" + UUID.randomUUID());
        definition.setDescription("Original description");
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        try {
            // Update via PUT - the controller sets id on the body and saves
            definition.setName("updated-agent-" + UUID.randomUUID());
            definition.setDescription("Updated description");
            definition.setSystemPrompt("New system prompt");

            // Act
            AgentDefinitionEntity updated = restClient().put()
                .uri("/agents/definitions/{id}", definition.getId())
                .body(definition)
                .retrieve()
                .body(AgentDefinitionEntity.class);

            // Assert
            assertThat(updated).isNotNull();
            assertThat(updated.getId()).isEqualTo(definition.getId());
            assertThat(updated.getName()).isEqualTo(definition.getName());
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getSystemPrompt()).isEqualTo("New system prompt");
        } finally {
            // Cleanup
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }

    @Test
    void shouldSoftDeleteDefinition_whenDeleteById() {
        // Arrange - create definition
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("delete-agent-" + UUID.randomUUID());
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/agents/definitions/{id}", definition.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedDefId = definition.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/agents/definitions/{id}", deletedDefId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Session Management (no AI needed) ====================

    @Test
    void shouldCreateSession_whenPostToDefinition() {
        // Arrange - create definition first
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("session-agent-" + UUID.randomUUID());
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        try {
            AgentSessionEntity sessionRequest = new AgentSessionEntity();
            sessionRequest.setAgentDefinitionId(definition.getId());
            sessionRequest.setUserId("test-user");
            sessionRequest.setTitle("Test Session");
            sessionRequest.setStatus("CREATED");

            // Act
            AgentSessionEntity created = restClient().post()
                .uri("/agents/sessions")
                .body(sessionRequest)
                .retrieve()
                .body(AgentSessionEntity.class);

            // Assert
            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull();
            assertThat(created.getAgentDefinitionId()).isEqualTo(definition.getId());
            assertThat(created.getUserId()).isEqualTo("test-user");
            assertThat(created.getTitle()).isEqualTo("Test Session");
            assertThat(created.getStatus()).isEqualTo("CREATED");

            // Cleanup session
            dataManager.deleteById(AgentSessionEntity.class, created.getId());
        } finally {
            // Cleanup definition
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }

    @Test
    void shouldListSessions_whenGetAll() {
        // Arrange - create definition + 2 sessions
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("list-session-agent-" + UUID.randomUUID());
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        AgentSessionEntity session1 = new AgentSessionEntity();
        session1.setAgentDefinitionId(definition.getId());
        session1.setStatus("CREATED");
        session1 = dataManager.save(AgentSessionEntity.class, session1);

        AgentSessionEntity session2 = new AgentSessionEntity();
        session2.setAgentDefinitionId(definition.getId());
        session2.setStatus("CREATED");
        session2 = dataManager.save(AgentSessionEntity.class, session2);

        try {
            // Act
            List<AgentSessionEntity> sessions = restClient().get()
                .uri("/agents/sessions?agentDefinitionId={definitionId}", definition.getId())
                .retrieve()
                .body(List.class);

            // Assert
            assertThat(sessions).isNotNull();
            assertThat(sessions.size()).isGreaterThanOrEqualTo(2);
        } finally {
            // Cleanup
            dataManager.deleteById(AgentSessionEntity.class, session1.getId());
            dataManager.deleteById(AgentSessionEntity.class, session2.getId());
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }

    @Test
    void shouldReturnSession_whenGetById() {
        // Arrange - create definition + session
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("get-session-agent-" + UUID.randomUUID());
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        AgentSessionEntity session = new AgentSessionEntity();
        session.setAgentDefinitionId(definition.getId());
        session.setUserId("get-test-user");
        session.setTitle("Get Test Session");
        session.setStatus("CREATED");
        session = dataManager.save(AgentSessionEntity.class, session);

        try {
            // Act
            AgentSessionEntity found = restClient().get()
                .uri("/agents/sessions/{id}", session.getId())
                .retrieve()
                .body(AgentSessionEntity.class);

            // Assert
            assertThat(found).isNotNull();
            assertThat(found.getId()).isEqualTo(session.getId());
            assertThat(found.getAgentDefinitionId()).isEqualTo(definition.getId());
            assertThat(found.getUserId()).isEqualTo("get-test-user");
            assertThat(found.getTitle()).isEqualTo("Get Test Session");
        } finally {
            // Cleanup
            dataManager.deleteById(AgentSessionEntity.class, session.getId());
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }

    @Test
    void shouldSoftDeleteSession_whenDeleteById() {
        // Arrange - create definition + session
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("delete-session-agent-" + UUID.randomUUID());
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        AgentSessionEntity session = new AgentSessionEntity();
        session.setAgentDefinitionId(definition.getId());
        session.setStatus("CREATED");
        session = dataManager.save(AgentSessionEntity.class, session);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/agents/sessions/{id}", session.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedSessionId = session.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/agents/sessions/{id}", deletedSessionId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);

        // Cleanup definition
        dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
    }

    // ==================== Agent Execution (needs Ollama) ====================

    @Test
    void shouldExecuteAgent_whenPostExecute() {
        assumeOllamaAvailable();

        // Arrange - create definition + session
        AgentDefinitionEntity definition = new AgentDefinitionEntity();
        definition.setName("execute-agent-" + UUID.randomUUID());
        definition.setDescription("Agent for execution test");
        definition.setSystemPrompt("You are a helpful assistant.");
        definition.setMaxIterations(3);
        definition = dataManager.save(AgentDefinitionEntity.class, definition);

        AgentSessionEntity session = new AgentSessionEntity();
        session.setAgentDefinitionId(definition.getId());
        session.setUserId("execute-test-user");
        session.setStatus("CREATED");
        session = dataManager.save(AgentSessionEntity.class, session);

        try {
            // Act
            Map<String, String> executeRequest = Map.of("message", "hello");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient().post()
                .uri("/agents/sessions/{id}/execute", session.getId())
                .body(executeRequest)
                .retrieve()
                .body(Map.class);

            // Assert - should return an AgentResponse
            assertThat(response).isNotNull();
            // The response should have a status field (COMPLETED or ERROR are both valid)
            assertThat(response.get("status")).isNotNull();
        } finally {
            // Cleanup
            dataManager.deleteById(AgentSessionEntity.class, session.getId());
            dataManager.deleteById(AgentDefinitionEntity.class, definition.getId());
        }
    }
}
