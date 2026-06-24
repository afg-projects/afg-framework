package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentSessionEntity;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Agent 控制器
 * <p>
 * 提供 Agent 定义的 CRUD、会话管理、执行和流式执行等功能。
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
    private final io.github.afgprojects.framework.ai.core.service.AgentService agentService;

    // ==================== Agent 定义 ====================

    /**
     * 创建 Agent 定义
     */
    @PostMapping("/definitions")
    public AgentDefinitionEntity createDefinition(@RequestBody AgentDefinitionEntity definition) {
        return dataManager.save(AgentDefinitionEntity.class, definition);
    }

    /**
     * 更新 Agent 定义
     */
    @PutMapping("/definitions/{id}")
    public AgentDefinitionEntity updateDefinition(@PathVariable String id, @RequestBody AgentDefinitionEntity definition) {
        definition.setId(id);
        return dataManager.save(AgentDefinitionEntity.class, definition);
    }

    /**
     * 获取 Agent 定义
     */
    @GetMapping("/definitions/{id}")
    public ResponseEntity<AgentDefinitionEntity> getDefinition(@PathVariable String id) {
        return dataManager.findById(AgentDefinitionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 列出所有 Agent 定义
     */
    @GetMapping("/definitions")
    public List<AgentDefinitionEntity> listDefinitions() {
        return dataManager.findAll(AgentDefinitionEntity.class);
    }

    /**
     * 删除 Agent 定义（软删除）
     */
    @DeleteMapping("/definitions/{id}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable String id) {
        dataManager.deleteById(AgentDefinitionEntity.class, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Agent 会话 ====================

    /**
     * 创建会话
     */
    @PostMapping("/sessions")
    public AgentSessionEntity createSession(@RequestBody AgentSessionEntity session) {
        return dataManager.save(AgentSessionEntity.class, session);
    }

    /**
     * 获取会话
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<AgentSessionEntity> getSession(@PathVariable String id) {
        return dataManager.findById(AgentSessionEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 列出会话
     */
    @GetMapping("/sessions")
    public List<AgentSessionEntity> listSessions(@RequestParam String agentDefinitionId) {
        return dataManager.entity(AgentSessionEntity.class)
            .query()
            .where(Conditions.builder(AgentSessionEntity.class)
                .eq(AgentSessionEntity::getAgentDefinitionId, agentDefinitionId)
                .build())
            .list();
    }

    /**
     * 删除会话（软删除）
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable String id) {
        dataManager.deleteById(AgentSessionEntity.class, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Agent 执行 ====================

    /**
     * 执行 Agent（同步）
     * <p>
     * 不使用 @Transactional：AgentService.execute() 内部已将 AI 执行与数据库操作分离。
     */
    @PostMapping("/sessions/{id}/execute")
    public AgentResponse executeAgent(@PathVariable String id, @RequestBody Map<String, String> body) {
        String userInput = body.get("message");
        if (userInput == null || userInput.isBlank()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "Message is required");
        }
        return agentService.execute(id, userInput);
    }

    /**
     * 执行 Agent（SSE 流式）
     * <p>
     * 通过 Server-Sent Events 返回 Agent 执行进度和最终结果。
     * 使用 Flux.create() 包装同步的 AgentExecutor.execute()，
     * 在独立线程中执行 AI 调用，避免阻塞 Netty 事件循环。
     *
     * @param id   会话 ID
     * @param body 请求体，包含 message 字段
     * @return SSE 事件流
     */
    @GetMapping(value = "/sessions/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentResponse> executeAgentStream(@PathVariable String id, @RequestBody Map<String, String> body) {
        String userInput = body.get("message");
        if (userInput == null || userInput.isBlank()) {
            return Flux.error(new BusinessException(CommonErrorCode.PARAM_ERROR, "Message is required"));
        }

        return Flux.<AgentResponse>create(sink -> {
            try {
                AgentResponse response = agentService.execute(id, userInput);
                sink.next(response);
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}