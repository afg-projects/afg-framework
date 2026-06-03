package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.dto.agent.AgentExecuteRequest;
import io.github.afgprojects.framework.ai.core.dto.agent.CreateAgentRequest;
import io.github.afgprojects.framework.ai.core.dto.agent.UpdateAgentRequest;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentSessionEntity;
import io.github.afgprojects.framework.ai.core.service.AgentService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI Agent 管理控制器
 * <p>
 * 提供 Agent 定义、会话、执行的 CRUD 和执行接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AiAgentController {

    private final DataManager dataManager;
    private final AgentService agentService;

    // ==================== Agent 定义 CRUD ====================

    /**
     * 创建 Agent 定义
     */
    @PostMapping("/definitions")
    @Transactional
    public AgentDefinitionEntity createDefinition(@Valid @RequestBody CreateAgentRequest request) {
        AgentDefinitionEntity entity = new AgentDefinitionEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setChatClientName(request.getChatClientName());
        entity.setModelName(request.getModelName());
        entity.setTemperature(request.getTemperature());
        entity.setMaxTokens(request.getMaxTokens());
        entity.setMaxIterations(request.getMaxIterations());
        entity.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        entity.setToolIds(request.getToolIds());
        entity.setConfig(request.getConfig());
        return dataManager.save(AgentDefinitionEntity.class, entity);
    }

    /**
     * 列出所有 Agent 定义
     */
    @GetMapping("/definitions")
    public List<AgentDefinitionEntity> listDefinitions() {
        return dataManager.entity(AgentDefinitionEntity.class)
            .query()
            .orderByDesc(AgentDefinitionEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个 Agent 定义
     */
    @GetMapping("/definitions/{id}")
    public ResponseEntity<AgentDefinitionEntity> getDefinition(@PathVariable Long id) {
        return dataManager.findById(AgentDefinitionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新 Agent 定义
     */
    @PutMapping("/definitions/{id}")
    @Transactional
    public AgentDefinitionEntity updateDefinition(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateAgentRequest request) {
        AgentDefinitionEntity entity = dataManager.findById(AgentDefinitionEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Agent definition not found: " + id));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            entity.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getChatClientName() != null) {
            entity.setChatClientName(request.getChatClientName());
        }
        if (request.getModelName() != null) {
            entity.setModelName(request.getModelName());
        }
        if (request.getTemperature() != null) {
            entity.setTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            entity.setMaxTokens(request.getMaxTokens());
        }
        if (request.getMaxIterations() != null) {
            entity.setMaxIterations(request.getMaxIterations());
        }
        if (request.getKnowledgeBaseIds() != null) {
            entity.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        }
        if (request.getToolIds() != null) {
            entity.setToolIds(request.getToolIds());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }

        return dataManager.save(AgentDefinitionEntity.class, entity);
    }

    /**
     * 删除 Agent 定义（软删除）
     */
    @DeleteMapping("/definitions/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        AgentDefinitionEntity entity = dataManager.findById(AgentDefinitionEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.markDeleted();
        dataManager.save(AgentDefinitionEntity.class, entity);
        return ResponseEntity.noContent().build();
    }

    // ==================== Agent 会话管理 ====================

    /**
     * 创建 Agent 会话
     */
    @PostMapping("/definitions/{id}/sessions")
    @Transactional
    public AgentSessionEntity createSession(@PathVariable Long id) {
        // 验证 Agent 定义存在
        AgentDefinitionEntity definition = dataManager.findById(AgentDefinitionEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Agent definition not found: " + id));

        AgentSessionEntity session = new AgentSessionEntity();
        session.setAgentDefinitionId(id);
        session.setStatus("CREATED");
        session.setUserId(getCurrentUserId());
        return dataManager.save(AgentSessionEntity.class, session);
    }

    /**
     * 列出 Agent 会话（可选按 userId 筛选）
     */
    @GetMapping("/sessions")
    public List<AgentSessionEntity> listSessions(@RequestParam(required = false) String userId) {
        if (userId != null && !userId.isEmpty()) {
            return dataManager.entity(AgentSessionEntity.class)
                .query()
                .where(Conditions.builder(AgentSessionEntity.class)
                    .eq(AgentSessionEntity::getUserId, userId)
                    .build())
                .orderByDesc(AgentSessionEntity::getCreatedAt)
                .list();
        }
        return dataManager.entity(AgentSessionEntity.class)
            .query()
            .orderByDesc(AgentSessionEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个 Agent 会话
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<AgentSessionEntity> getSession(@PathVariable Long id) {
        return dataManager.findById(AgentSessionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除 Agent 会话（软删除）
     */
    @DeleteMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        AgentSessionEntity session = dataManager.findById(AgentSessionEntity.class, id)
            .orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        session.markDeleted();
        dataManager.save(AgentSessionEntity.class, session);
        return ResponseEntity.noContent().build();
    }

    // ==================== Agent 执行 ====================

    /**
     * 执行 Agent
     */
    @PostMapping("/sessions/{id}/execute")
    @Transactional
    public AgentResponse executeAgent(@PathVariable Long id,
                                       @Valid @RequestBody AgentExecuteRequest request) {
        return agentService.execute(id, request.getUserInput());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前用户 ID
     * <p>
     * TODO: 从 SecurityContext 获取实际用户 ID
     */
    private String getCurrentUserId() {
        return "system";
    }
}
