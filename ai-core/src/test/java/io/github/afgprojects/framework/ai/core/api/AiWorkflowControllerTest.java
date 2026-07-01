package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.workflow.CreateWorkflowRequest;
import io.github.afgprojects.framework.ai.core.dto.workflow.UpdateWorkflowRequest;
import io.github.afgprojects.framework.ai.core.dto.workflow.WorkflowExecuteRequest;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowExecutionEntity;
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
 * AiWorkflowController 集成测试
 *
 * <p>测试工作流定义、执行记录、节点类型的 CRUD 和执行接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiWorkflowControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Workflow Definition CRUD ====================

    @Test
    void shouldCreateDefinition_whenPostValidRequest() {
        // Arrange
        CreateWorkflowRequest request = new CreateWorkflowRequest();
        request.setName("test-workflow-" + UUID.randomUUID());
        request.setDescription("Test workflow description");
        request.setDslContent("{\"nodes\":[],\"edges\":[]}");
        request.setVersion("1.0.0");
        request.setStatus("DRAFT");

        // Act
        WorkflowDefinitionEntity created = restClient().post()
            .uri("/workflows/definitions")
            .body(request)
            .retrieve()
            .body(WorkflowDefinitionEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(request.getName());
        assertThat(created.getDescription()).isEqualTo(request.getDescription());
        assertThat(created.getDslContent()).isEqualTo("{\"nodes\":[],\"edges\":[]}");
        assertThat(created.getVersion()).isEqualTo("1.0.0");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(created.getDeleted()).isFalse();

        // Cleanup
        dataManager.deleteById(WorkflowDefinitionEntity.class, created.getId());
    }

    @Test
    void shouldListDefinitions_whenGetAll() {
        // Arrange - create 2 workflow definitions via DataManager
        String prefix = "list-wf-" + UUID.randomUUID();

        WorkflowDefinitionEntity wf1 = new WorkflowDefinitionEntity();
        wf1.setName(prefix + "-1");
        wf1.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf1.setStatus("DRAFT");
        wf1 = dataManager.save(WorkflowDefinitionEntity.class, wf1);

        WorkflowDefinitionEntity wf2 = new WorkflowDefinitionEntity();
        wf2.setName(prefix + "-2");
        wf2.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf2.setStatus("DRAFT");
        wf2 = dataManager.save(WorkflowDefinitionEntity.class, wf2);

        // Act
        List<WorkflowDefinitionEntity> definitions = restClient().get()
            .uri("/workflows/definitions")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf1.getId());
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf2.getId());
    }

    @Test
    void shouldReturnDefinition_whenGetById() {
        // Arrange - create workflow definition via DataManager
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("get-wf-" + UUID.randomUUID());
        wf.setDescription("Get test workflow");
        wf.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf.setStatus("DRAFT");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        // Act
        WorkflowDefinitionEntity found = restClient().get()
            .uri("/workflows/definitions/{id}", wf.getId())
            .retrieve()
            .body(WorkflowDefinitionEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(wf.getId());
        assertThat(found.getName()).isEqualTo(wf.getName());
        assertThat(found.getDescription()).isEqualTo("Get test workflow");

        // Cleanup
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf.getId());
    }

    @Test
    void shouldUpdateDefinition_whenPutValidRequest() {
        // Arrange - create workflow definition
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("update-wf-" + UUID.randomUUID());
        wf.setDescription("Original description");
        wf.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf.setStatus("DRAFT");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        UpdateWorkflowRequest request = new UpdateWorkflowRequest();
        request.setName("updated-wf-" + UUID.randomUUID());
        request.setDescription("Updated description");
        request.setVersion("2.0.0");
        request.setStatus("PUBLISHED");

        // Act
        WorkflowDefinitionEntity updated = restClient().put()
            .uri("/workflows/definitions/{id}", wf.getId())
            .body(request)
            .retrieve()
            .body(WorkflowDefinitionEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(wf.getId());
        assertThat(updated.getName()).isEqualTo(request.getName());
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getVersion()).isEqualTo("2.0.0");
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");

        // Cleanup
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf.getId());
    }

    @Test
    void shouldSoftDeleteDefinition_whenDeleteById() {
        // Arrange - create workflow definition
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("delete-wf-" + UUID.randomUUID());
        wf.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf.setStatus("DRAFT");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/workflows/definitions/{id}", wf.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        String deletedWfId = wf.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/workflows/definitions/{id}", deletedWfId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Workflow Execution ====================

    @Test
    void shouldExecuteWorkflow_whenPostExecute() {
        // Arrange - create a real executable workflow: input -> output
        // (InputNode/OutputNode 纯透传，不依赖 AI/chat client，适合集成测试)
        // DSL 对齐前端 workflowToJson 产出：{version, nodes[{id,type,name,data,position}], edges[{id,source,target,sourceAnchor}]}
        String dsl = """
            {
              "version": "1.0",
              "nodes": [
                {"id":"n1","type":"input","name":"输入","data":{"data":{"hello":"world"}},"position":{"x":0,"y":0}},
                {"id":"n2","type":"output","name":"输出","data":{"format":"raw","data":"${n1.hello}"},"position":{"x":300,"y":0}}
              ],
              "edges": [
                {"id":"e1","source":"n1","target":"n2","sourceAnchor":"output"}
              ]
            }
            """;
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("exec-wf-" + UUID.randomUUID());
        wf.setDescription("Workflow for execution test (input->output)");
        wf.setDslContent(dsl);
        wf.setStatus("PUBLISHED");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        WorkflowExecuteRequest request = new WorkflowExecuteRequest();
        request.setStream(false);
        request.setInputs(Map.of("input1", "test"));

        // Act & Assert - 真实执行不抛 ParamBindingException（验证前端产出的 DSL 能被后端解析执行）
        try {
            Object result = restClient().post()
                .uri("/workflows/definitions/{id}/execute", wf.getId())
                .body(request)
                .retrieve()
                .body(Object.class);

            // 执行成功：result 非空（DagResult 序列化为 Map）
            assertThat(result).as("工作流执行应返回结果").isNotNull();
        } catch (Exception e) {
            // ParamBindingException 表示前端 DSL 与后端 Params 不对齐——这是契约失败，不应发生
            throw new AssertionError(
                "工作流执行失败，可能为前端 DSL 与后端节点 Params 不对齐: " + e.getMessage(), e);
        } finally {
            dataManager.deleteById(WorkflowDefinitionEntity.class, wf.getId());
        }
    }

    @Test
    void shouldListExecutions_whenGetByDefinition() {
        // Arrange - create workflow definition and execution record via DataManager
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("exec-list-wf-" + UUID.randomUUID());
        wf.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf.setStatus("PUBLISHED");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        WorkflowExecutionEntity exec1 = new WorkflowExecutionEntity();
        exec1.setWorkflowDefinitionId(wf.getId());
        exec1.setStatus("COMPLETED");
        exec1 = dataManager.save(WorkflowExecutionEntity.class, exec1);

        WorkflowExecutionEntity exec2 = new WorkflowExecutionEntity();
        exec2.setWorkflowDefinitionId(wf.getId());
        exec2.setStatus("FAILED");
        exec2 = dataManager.save(WorkflowExecutionEntity.class, exec2);

        // Act
        List<WorkflowExecutionEntity> executions = restClient().get()
            .uri("/workflows/executions?definitionId={id}", wf.getId())
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(executions).isNotNull();
        assertThat(executions.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(WorkflowExecutionEntity.class, exec1.getId());
        dataManager.deleteById(WorkflowExecutionEntity.class, exec2.getId());
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf.getId());
    }

    @Test
    void shouldReturnExecution_whenGetById() {
        // Arrange - create workflow definition and execution record via DataManager
        WorkflowDefinitionEntity wf = new WorkflowDefinitionEntity();
        wf.setName("exec-get-wf-" + UUID.randomUUID());
        wf.setDslContent("{\"nodes\":[],\"edges\":[]}");
        wf.setStatus("PUBLISHED");
        wf = dataManager.save(WorkflowDefinitionEntity.class, wf);

        WorkflowExecutionEntity exec = new WorkflowExecutionEntity();
        exec.setWorkflowDefinitionId(wf.getId());
        exec.setStatus("COMPLETED");
        exec.setInput("{\"query\":\"test\"}");
        exec.setOutput("{\"result\":\"success\"}");
        exec = dataManager.save(WorkflowExecutionEntity.class, exec);

        // Act
        WorkflowExecutionEntity found = restClient().get()
            .uri("/workflows/executions/{id}", exec.getId())
            .retrieve()
            .body(WorkflowExecutionEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(exec.getId());
        assertThat(found.getWorkflowDefinitionId()).isEqualTo(wf.getId());
        assertThat(found.getStatus()).isEqualTo("COMPLETED");

        // Cleanup
        dataManager.deleteById(WorkflowExecutionEntity.class, exec.getId());
        dataManager.deleteById(WorkflowDefinitionEntity.class, wf.getId());
    }

    // ==================== Node Types ====================

    @Test
    void shouldListNodeTypes_whenGetNodeTypes() {
        // Act
        List<Map> nodeTypes = restClient().get()
            .uri("/workflows/node-types")
            .retrieve()
            .body(List.class);

        // Assert - node types list should not be null (may be empty if no types registered)
        assertThat(nodeTypes).isNotNull();
        if (nodeTypes.isEmpty()) {
            return;
        }

        // Every registered node type must carry non-null displayName, category,
        // displayNameZh (Chinese) and editorMeta (icon + color). Sample at least
        // 5 entries covering different categories.
        List<Map> sample = nodeTypes.size() >= 5 ? nodeTypes.subList(0, 5) : nodeTypes;
        for (Map<?, ?> nt : sample) {
            assertThat(nt.get("type")).as("type").isNotNull();
            assertThat(nt.get("displayName")).as("displayName").isNotNull();
            assertThat(nt.get("category")).as("category").isNotNull();
            assertThat(nt.get("displayNameZh")).as("displayNameZh")
                .as("displayNameZh should be Chinese, not null").isNotNull();
            assertThat(nt.get("editorMeta")).as("editorMeta").isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) nt.get("editorMeta");
            assertThat(meta.get("icon")).as("editorMeta.icon").isNotNull();
            assertThat(meta.get("color")).as("editorMeta.color").isNotNull();
        }

        // Spot-check a known node: ai-chat must exist with Chinese name and icon.
        Map<?, ?> aiChat = nodeTypes.stream()
            .filter(nt -> "ai-chat".equals(nt.get("type")))
            .findFirst()
            .orElse(null);
        assertThat(aiChat).as("ai-chat node type registered").isNotNull();
        assertThat(aiChat.get("displayNameZh")).isEqualTo("AI 对话");
    }

    @Test
    void shouldListNodeTypesByCategory_whenGetNodeTypesWithCategory() {
        // Act
        List<Map> nodeTypes = restClient().get()
            .uri("/workflows/node-types?category=control")
            .retrieve()
            .body(List.class);

        // Assert - node types list should not be null
        assertThat(nodeTypes).isNotNull();
    }
}
