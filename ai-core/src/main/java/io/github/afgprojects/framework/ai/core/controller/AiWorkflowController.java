package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.NodeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEvent;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagResult;
import io.github.afgprojects.framework.ai.core.api.workflow.node.NodeTypeRegistry;
import io.github.afgprojects.framework.ai.core.dto.workflow.CreateWorkflowRequest;
import io.github.afgprojects.framework.ai.core.dto.workflow.UpdateWorkflowRequest;
import io.github.afgprojects.framework.ai.core.dto.workflow.WorkflowExecuteRequest;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowExecutionEntity;
import io.github.afgprojects.framework.ai.core.service.WorkflowService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.core.authentication.AfgUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * AI 工作流管理控制器
 * <p>
 * 提供工作流定义、执行、节点类型的 CRUD 和执行接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class AiWorkflowController {

    private final DataManager dataManager;
    private final WorkflowService workflowService;
    private final NodeTypeRegistry nodeTypeRegistry;

    // ==================== 工作流定义 CRUD ====================

    /**
     * 创建工作流定义
     */
    @PostMapping("/definitions")
    @Transactional
    public WorkflowDefinitionEntity createDefinition(@Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDslContent(request.getDslContent());
        entity.setVersion(request.getVersion());
        entity.setStatus(request.getStatus());
        entity.setApplicationId(request.getApplicationId());
        return dataManager.save(WorkflowDefinitionEntity.class, entity);
    }

    /**
     * 列出所有工作流定义
     */
    @GetMapping("/definitions")
    public List<WorkflowDefinitionEntity> listDefinitions() {
        return dataManager.entity(WorkflowDefinitionEntity.class)
            .query()
            .orderByDesc(WorkflowDefinitionEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个工作流定义
     */
    @GetMapping("/definitions/{id}")
    public ResponseEntity<WorkflowDefinitionEntity> getDefinition(@PathVariable Long id) {
        return dataManager.findById(WorkflowDefinitionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新工作流定义
     */
    @PutMapping("/definitions/{id}")
    @Transactional
    public ResponseEntity<WorkflowDefinitionEntity> updateDefinition(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateWorkflowRequest request) {
        WorkflowDefinitionEntity entity = dataManager.findById(WorkflowDefinitionEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getDslContent() != null) {
            entity.setDslContent(request.getDslContent());
        }
        if (request.getVersion() != null) {
            entity.setVersion(request.getVersion());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getApplicationId() != null) {
            entity.setApplicationId(request.getApplicationId());
        }

        return ResponseEntity.ok(dataManager.save(WorkflowDefinitionEntity.class, entity));
    }

    /**
     * 删除工作流定义（软删除）
     */
    @DeleteMapping("/definitions/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        WorkflowDefinitionEntity entity = dataManager.findById(WorkflowDefinitionEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.markDeleted();
        dataManager.save(WorkflowDefinitionEntity.class, entity);
        return ResponseEntity.noContent().build();
    }

    // ==================== 工作流执行 ====================

    /**
     * 执行工作流
     * <p>
     * 如果 request.stream=true，返回 SSE 事件流；否则同步返回执行结果。
     */
    @PostMapping("/definitions/{id}/execute")
    public Object executeWorkflow(@PathVariable Long id,
                                   @RequestBody(required = false) WorkflowExecuteRequest request) {
        WorkflowExecuteRequest effectiveRequest = request != null ? request : new WorkflowExecuteRequest();

        String userId = getCurrentUserId();

        if (effectiveRequest.isStream()) {
            return workflowService.executeStream(id, effectiveRequest.getInputs(), userId);
        } else {
            return workflowService.execute(id, effectiveRequest.getInputs(), userId);
        }
    }

    /**
     * 流式执行工作流（SSE）
     */
    @GetMapping(value = "/definitions/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DagEvent> executeWorkflowStream(@PathVariable Long id,
                                                 @RequestParam(required = false) Map<String, Object> inputs) {
        String userId = getCurrentUserId();
        return workflowService.executeStream(id, inputs, userId);
    }

    // ==================== 工作流执行记录 ====================

    /**
     * 列出工作流执行记录（可选按 definitionId 筛选）
     */
    @GetMapping("/executions")
    public List<WorkflowExecutionEntity> listExecutions(@RequestParam(required = false) Long definitionId) {
        if (definitionId != null) {
            return dataManager.entity(WorkflowExecutionEntity.class)
                .query()
                .where(Conditions.builder(WorkflowExecutionEntity.class)
                    .eq(WorkflowExecutionEntity::getWorkflowDefinitionId, definitionId)
                    .build())
                .orderByDesc(WorkflowExecutionEntity::getCreatedAt)
                .list();
        }
        return dataManager.entity(WorkflowExecutionEntity.class)
            .query()
            .orderByDesc(WorkflowExecutionEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个工作流执行记录
     */
    @GetMapping("/executions/{id}")
    public ResponseEntity<WorkflowExecutionEntity> getExecution(@PathVariable Long id) {
        return dataManager.findById(WorkflowExecutionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 节点类型 ====================

    /**
     * 列出所有可用的节点类型
     */
    @GetMapping("/node-types")
    public Collection<NodeDefinition> listNodeTypes(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return nodeTypeRegistry.getByCategory(category);
        }
        return nodeTypeRegistry.getAll();
    }

    /**
     * 获取单个节点类型详情
     */
    @GetMapping("/node-types/{type}")
    public ResponseEntity<NodeDefinition> getNodeType(@PathVariable String type) {
        return nodeTypeRegistry.get(type)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前用户 ID
     *
     * <p>从 Spring SecurityContext 获取用户 ID，若未认证则返回 "system"。
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AfgUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return "system";
    }
}
