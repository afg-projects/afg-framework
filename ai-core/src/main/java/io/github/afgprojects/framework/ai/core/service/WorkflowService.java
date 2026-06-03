package io.github.afgprojects.framework.ai.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.dsl.DslConverter;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEngine;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagEvent;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagResult;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.DagStatus;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowExecutionEntity;
import io.github.afgprojects.framework.ai.core.workflow.engine.DefaultExecutionContext;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;

/**
 * 工作流执行服务
 * <p>
 * 负责加载工作流定义、构建执行上下文、调用 DagEngine 执行，并保存执行记录。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final DataManager dataManager;
    private final DagEngine dagEngine;
    private final DslConverter dslConverter;
    private final ObjectMapper objectMapper;

    /**
     * 同步执行工作流
     *
     * @param definitionId 工作流定义 ID
     * @param inputs       输入变量
     * @param userId       执行用户 ID
     * @return 执行结果
     */
    @Transactional
    public DagResult execute(Long definitionId, Map<String, Object> inputs, String userId) {
        WorkflowDefinitionEntity entity = dataManager.findById(WorkflowDefinitionEntity.class, definitionId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + definitionId));

        WorkflowDefinition workflow = dslConverter.fromJson(entity.getDslContent());
        DefaultExecutionContext context = new DefaultExecutionContext(
            String.valueOf(entity.getId()),
            null,
            userId,
            inputs != null ? inputs : Collections.emptyMap()
        );

        DagResult result = dagEngine.execute(workflow, context);
        saveExecutionRecord(entity.getId(), result, inputs, userId);
        return result;
    }

    /**
     * 流式执行工作流
     *
     * @param definitionId 工作流定义 ID
     * @param inputs       输入变量
     * @param userId       执行用户 ID
     * @return 事件流
     */
    public Flux<DagEvent> executeStream(Long definitionId, Map<String, Object> inputs, String userId) {
        WorkflowDefinitionEntity entity = dataManager.findById(WorkflowDefinitionEntity.class, definitionId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow definition not found: " + definitionId));

        WorkflowDefinition workflow = dslConverter.fromJson(entity.getDslContent());
        DefaultExecutionContext context = new DefaultExecutionContext(
            String.valueOf(entity.getId()),
            null,
            userId,
            inputs != null ? inputs : Collections.emptyMap()
        );

        return dagEngine.executeStream(workflow, context)
            .doOnComplete(() -> saveExecutionRecordAsync(entity.getId(), DagStatus.SUCCESS, null, inputs, userId))
            .doOnError(err -> saveExecutionRecordAsync(entity.getId(), DagStatus.FAILED, err.getMessage(), inputs, userId));
    }

    private void saveExecutionRecord(Long definitionId, DagResult result, Map<String, Object> inputs, String userId) {
        WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
        execution.setWorkflowDefinitionId(definitionId);
        execution.setStatus(result.status().name());
        execution.setInput(toJsonOrNull(inputs));
        execution.setOutput(result.content());
        execution.setDurationMs(result.durationMs());
        execution.setUserId(userId);
        dataManager.save(WorkflowExecutionEntity.class, execution);
        log.info("Workflow execution saved: definitionId={}, status={}, durationMs={}",
            definitionId, result.status(), result.durationMs());
    }

    private void saveExecutionRecordAsync(Long definitionId, DagStatus status, String error,
                                           Map<String, Object> inputs, String userId) {
        try {
            WorkflowExecutionEntity execution = new WorkflowExecutionEntity();
            execution.setWorkflowDefinitionId(definitionId);
            execution.setStatus(status.name());
            execution.setInput(toJsonOrNull(inputs));
            execution.setError(error);
            execution.setUserId(userId);
            dataManager.save(WorkflowExecutionEntity.class, execution);
        } catch (Exception e) {
            log.warn("Failed to save workflow execution record: {}", e.getMessage());
        }
    }

    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value to JSON: {}", e.getMessage());
            return null;
        }
    }
}
