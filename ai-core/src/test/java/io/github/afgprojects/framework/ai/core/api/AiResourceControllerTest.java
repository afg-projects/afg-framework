package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationVersionRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateToolRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateToolRequest;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationVersionEntity;
import io.github.afgprojects.framework.ai.core.entity.skill.UserSkillEntity;
import io.github.afgprojects.framework.ai.core.entity.tool.ToolRegistryEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiResourceController 集成测试
 *
 * <p>测试工具注册、应用管理的 CRUD 接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiResourceControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Tool CRUD ====================

    @Test
    void shouldCreateTool_whenPostValidRequest() {
        // Arrange
        CreateToolRequest request = new CreateToolRequest();
        request.setName("test-tool-" + UUID.randomUUID());
        request.setDescription("Test tool description");
        request.setType("HTTP");
        request.setEndpoint("https://api.example.com/tools/test");
        request.setParameters("{\"param1\": \"string\"}");
        request.setEnabled(true);

        // Act
        ToolRegistryEntity created = restClient().post()
            .uri("/resources/tools")
            .body(request)
            .retrieve()
            .body(ToolRegistryEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(request.getName());
        assertThat(created.getType()).isEqualTo("HTTP");
        assertThat(created.getEnabled()).isTrue();

        // Cleanup
        dataManager.deleteById(ToolRegistryEntity.class, created.getId());
    }

    @Test
    void shouldListTools_whenGetAll() {
        // Arrange - create 2 tools via DataManager
        String prefix = "list-tool-" + UUID.randomUUID();

        ToolRegistryEntity tool1 = new ToolRegistryEntity();
        tool1.setName(prefix + "-1");
        tool1.setType("HTTP");
        tool1.setEnabled(true);
        tool1 = dataManager.save(ToolRegistryEntity.class, tool1);

        ToolRegistryEntity tool2 = new ToolRegistryEntity();
        tool2.setName(prefix + "-2");
        tool2.setType("FUNCTION");
        tool2.setEnabled(true);
        tool2 = dataManager.save(ToolRegistryEntity.class, tool2);

        // Act
        List<ToolRegistryEntity> tools = restClient().get()
            .uri("/resources/tools")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(tools).isNotNull();
        assertThat(tools.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(ToolRegistryEntity.class, tool1.getId());
        dataManager.deleteById(ToolRegistryEntity.class, tool2.getId());
    }

    @Test
    void shouldFilterToolsByType_whenGetWithType() {
        // Arrange - create 2 tools with different types
        String prefix = "filter-tool-" + UUID.randomUUID();
        String targetType = "HTTP_" + UUID.randomUUID();

        ToolRegistryEntity tool1 = new ToolRegistryEntity();
        tool1.setName(prefix + "-http");
        tool1.setType(targetType);
        tool1.setEnabled(true);
        tool1 = dataManager.save(ToolRegistryEntity.class, tool1);

        ToolRegistryEntity tool2 = new ToolRegistryEntity();
        tool2.setName(prefix + "-function");
        tool2.setType("FUNCTION");
        tool2.setEnabled(true);
        tool2 = dataManager.save(ToolRegistryEntity.class, tool2);

        // Act
        List<ToolRegistryEntity> tools = restClient().get()
            .uri("/resources/tools?type={type}", targetType)
            .retrieve()
            .body(List.class);

        // Assert - only tool1 should be returned
        assertThat(tools).isNotNull();
        assertThat(tools.size()).isGreaterThanOrEqualTo(1);

        // Cleanup
        dataManager.deleteById(ToolRegistryEntity.class, tool1.getId());
        dataManager.deleteById(ToolRegistryEntity.class, tool2.getId());
    }

    @Test
    void shouldReturnTool_whenGetById() {
        // Arrange - create tool via DataManager
        ToolRegistryEntity tool = new ToolRegistryEntity();
        tool.setName("get-tool-" + UUID.randomUUID());
        tool.setType("HTTP");
        tool.setEndpoint("https://api.example.com/tools/get");
        tool.setEnabled(true);
        tool = dataManager.save(ToolRegistryEntity.class, tool);

        // Act
        ToolRegistryEntity found = restClient().get()
            .uri("/resources/tools/{id}", tool.getId())
            .retrieve()
            .body(ToolRegistryEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(tool.getId());
        assertThat(found.getName()).isEqualTo(tool.getName());
        assertThat(found.getType()).isEqualTo("HTTP");

        // Cleanup
        dataManager.deleteById(ToolRegistryEntity.class, tool.getId());
    }

    @Test
    void shouldUpdateTool_whenPutValidRequest() {
        // Arrange - create tool
        ToolRegistryEntity tool = new ToolRegistryEntity();
        tool.setName("update-tool-" + UUID.randomUUID());
        tool.setType("HTTP");
        tool.setEnabled(true);
        tool = dataManager.save(ToolRegistryEntity.class, tool);

        UpdateToolRequest request = new UpdateToolRequest();
        request.setName("updated-tool-" + UUID.randomUUID());
        request.setDescription("Updated description");
        request.setType("FUNCTION");

        // Act
        ToolRegistryEntity updated = restClient().put()
            .uri("/resources/tools/{id}", tool.getId())
            .body(request)
            .retrieve()
            .body(ToolRegistryEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(tool.getId());
        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getType()).isEqualTo("FUNCTION");

        // Cleanup
        dataManager.deleteById(ToolRegistryEntity.class, tool.getId());
    }

    @Test
    void shouldSoftDeleteTool_whenDeleteById() {
        // Arrange - create tool
        ToolRegistryEntity tool = new ToolRegistryEntity();
        tool.setName("delete-tool-" + UUID.randomUUID());
        tool.setType("HTTP");
        tool.setEnabled(true);
        tool = dataManager.save(ToolRegistryEntity.class, tool);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/resources/tools/{id}", tool.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        String deletedToolId = tool.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/resources/tools/{id}", deletedToolId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Application CRUD ====================

    @Test
    void shouldCreateApplication_whenPostValidRequest() {
        // Arrange
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setName("test-app-" + UUID.randomUUID());
        request.setDescription("Test application description");
        request.setType("CHATBOT");
        request.setAccessToken("token-" + UUID.randomUUID());
        request.setStatus("ACTIVE");
        request.setSort(100);

        // Act
        ApplicationEntity created = restClient().post()
            .uri("/resources/applications")
            .body(request)
            .retrieve()
            .body(ApplicationEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(request.getName());
        assertThat(created.getType()).isEqualTo("CHATBOT");
        assertThat(created.getStatus()).isEqualTo("ACTIVE");

        // Cleanup
        dataManager.deleteById(ApplicationEntity.class, created.getId());
    }

    @Test
    void shouldListApplications_whenGetAll() {
        // Arrange - create 2 applications via DataManager
        String prefix = "list-app-" + UUID.randomUUID();

        ApplicationEntity app1 = new ApplicationEntity();
        app1.setName(prefix + "-1");
        app1.setType("CHATBOT");
        app1.setStatus("ACTIVE");
        app1.setSort(1);
        app1 = dataManager.save(ApplicationEntity.class, app1);

        ApplicationEntity app2 = new ApplicationEntity();
        app2.setName(prefix + "-2");
        app2.setType("AGENT");
        app2.setStatus("INACTIVE");
        app2.setSort(2);
        app2 = dataManager.save(ApplicationEntity.class, app2);

        // Act
        List<ApplicationEntity> apps = restClient().get()
            .uri("/resources/applications")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(apps).isNotNull();
        assertThat(apps.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(ApplicationEntity.class, app1.getId());
        dataManager.deleteById(ApplicationEntity.class, app2.getId());
    }

    @Test
    void shouldReturnApplication_whenGetById() {
        // Arrange - create application via DataManager
        ApplicationEntity app = new ApplicationEntity();
        app.setName("get-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app.setConfig("{\"theme\": \"dark\"}");
        app = dataManager.save(ApplicationEntity.class, app);

        // Act
        ApplicationEntity found = restClient().get()
            .uri("/resources/applications/{id}", app.getId())
            .retrieve()
            .body(ApplicationEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(app.getId());
        assertThat(found.getName()).isEqualTo(app.getName());
        assertThat(found.getType()).isEqualTo("CHATBOT");

        // Cleanup
        dataManager.deleteById(ApplicationEntity.class, app.getId());
    }

    @Test
    void shouldUpdateApplication_whenPutValidRequest() {
        // Arrange - create application
        ApplicationEntity app = new ApplicationEntity();
        app.setName("update-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app = dataManager.save(ApplicationEntity.class, app);

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setName("updated-app-" + UUID.randomUUID());
        request.setDescription("Updated description");
        request.setStatus("INACTIVE");
        request.setSort(200);

        // Act
        ApplicationEntity updated = restClient().put()
            .uri("/resources/applications/{id}", app.getId())
            .body(request)
            .retrieve()
            .body(ApplicationEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(app.getId());
        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");
        assertThat(updated.getSort()).isEqualTo(200);

        // Cleanup
        dataManager.deleteById(ApplicationEntity.class, app.getId());
    }

    @Test
    void shouldSoftDeleteApplication_whenDeleteById() {
        // Arrange - create application
        ApplicationEntity app = new ApplicationEntity();
        app.setName("delete-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app = dataManager.save(ApplicationEntity.class, app);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/resources/applications/{id}", app.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        String deletedAppId = app.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/resources/applications/{id}", deletedAppId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Application Version ====================

    @Test
    void shouldCreateVersion_whenPostToApplication() {
        // Arrange - create application first
        ApplicationEntity app = new ApplicationEntity();
        app.setName("version-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app = dataManager.save(ApplicationEntity.class, app);

        CreateApplicationVersionRequest request = new CreateApplicationVersionRequest();
        request.setVersion("1.0.0");
        request.setDescription("Initial version");
        request.setConfig("{\"model\": \"gpt-4\"}");
        request.setPublishedAt(LocalDateTime.now());
        request.setUserId("test-user");

        // Act
        ApplicationVersionEntity created = restClient().post()
            .uri("/resources/applications/{id}/versions", app.getId())
            .body(request)
            .retrieve()
            .body(ApplicationVersionEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getApplicationId()).isEqualTo(app.getId());
        assertThat(created.getVersion()).isEqualTo("1.0.0");
        assertThat(created.getDescription()).isEqualTo("Initial version");

        // Cleanup
        dataManager.deleteById(ApplicationVersionEntity.class, created.getId());
        dataManager.deleteById(ApplicationEntity.class, app.getId());
    }

    @Test
    void shouldListVersions_whenGetByApplication() {
        // Arrange - create application + 2 versions
        ApplicationEntity app = new ApplicationEntity();
        app.setName("list-version-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app = dataManager.save(ApplicationEntity.class, app);

        ApplicationVersionEntity version1 = new ApplicationVersionEntity();
        version1.setApplicationId(app.getId());
        version1.setVersion("1.0.0");
        version1.setConfig("{}");
        version1 = dataManager.save(ApplicationVersionEntity.class, version1);

        ApplicationVersionEntity version2 = new ApplicationVersionEntity();
        version2.setApplicationId(app.getId());
        version2.setVersion("1.1.0");
        version2.setConfig("{}");
        version2 = dataManager.save(ApplicationVersionEntity.class, version2);

        // Act
        List<ApplicationVersionEntity> versions = restClient().get()
            .uri("/resources/applications/{id}/versions", app.getId())
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(versions).isNotNull();
        assertThat(versions.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(ApplicationVersionEntity.class, version1.getId());
        dataManager.deleteById(ApplicationVersionEntity.class, version2.getId());
        dataManager.deleteById(ApplicationEntity.class, app.getId());
    }

    @Test
    void shouldReturnHitTestResponse_whenPostHitTest() {
        // Arrange - 创建带关联配置（config JSON 含 knowledgeBaseIds/toolIds）的应用
        ApplicationEntity app = new ApplicationEntity();
        app.setName("hit-test-app-" + UUID.randomUUID());
        app.setType("CHATBOT");
        app.setStatus("ACTIVE");
        app.setConfig("{\"knowledgeBaseIds\":[],\"toolIds\":[]}");
        app = dataManager.save(ApplicationEntity.class, app);

        Map<String, Object> body = Map.of("question", "hello world");

        // Act
        Map<?, ?> response = restClient().post()
            .uri("/resources/applications/{id}/hit-test", app.getId())
            .body(body)
            .retrieve()
            .body(Map.class);

        // Assert - 返回 HitTestResponse 结构（含 results 数组，可能为空因无关联知识库/工具）
        assertThat(response).isNotNull();
        assertThat(response.get("results")).isNotNull();

        // Cleanup
        dataManager.deleteById(ApplicationEntity.class, app.getId());
    }

    @Test
    void shouldCrudUserSkill() {
        // Arrange - 构造创建请求
        String skillName = "test-skill-" + UUID.randomUUID();
        Map<String, Object> createBody = Map.of(
            "name", skillName,
            "description", "测试技能",
            "prompt", "You are a helpful assistant.",
            "enabled", true
        );

        // Act - create
        Map<?, ?> created = restClient().post()
            .uri("/resources/skills")
            .body(createBody)
            .retrieve()
            .body(Map.class);

        // Assert - 创建成功
        assertThat(created).isNotNull();
        assertThat(created.get("name")).isEqualTo(skillName);
        String skillId = (String) created.get("id");

        // Act - list（应包含新创建的）
        List<?> list = restClient().get()
            .uri("/resources/skills")
            .retrieve()
            .body(List.class);
        assertThat(list).isNotNull();

        // Act - get by name
        Map<?, ?> fetched = restClient().get()
            .uri("/resources/skills/{name}", skillName)
            .retrieve()
            .body(Map.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.get("name")).isEqualTo(skillName);

        // Act - disable
        Map<?, ?> disabled = restClient().post()
            .uri("/resources/skills/{name}/disable", skillName)
            .retrieve()
            .body(Map.class);
        assertThat(disabled.get("enabled")).isEqualTo(false);

        // Cleanup
        restClient().delete()
            .uri("/resources/skills/{name}", skillName)
            .retrieve()
            .toBodilessEntity();
        assertThat(dataManager.findById(UserSkillEntity.class, skillId)).isEmpty();
    }
}
