package io.github.afgprojects.framework.ai.core.workflow;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.workflow.checkpoint.CheckpointManager;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.entity.workflow.WorkflowCheckpointEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * JDBC 检查点管理器实现（基于 DataManager）
 *
 * <p>使用 DataManager 进行数据库操作，将检查点持久化到数据库，适用于生产环境。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class JdbcCheckpointManager implements CheckpointManager {

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;

    public JdbcCheckpointManager(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
        this.objectMapper = new ObjectMapper();
        log.info("JdbcCheckpointManager initialized");
    }

    @Override
    public void save(@NonNull String executionId, @NonNull ExecutionContext context, @NonNull String currentNodeId) {
        String contextJson = serializeContext(context);

        // Check if checkpoint already exists for this executionId
        WorkflowCheckpointEntity existing = findByExecutionId(executionId);
        if (existing != null) {
            existing.setWorkflowId(context.getWorkflowId());
            existing.setCurrentNodeId(currentNodeId);
            existing.setContextData(contextJson);
            dataManager.save(WorkflowCheckpointEntity.class, existing);
            log.debug("Updated checkpoint for executionId={}, currentNodeId={}", executionId, currentNodeId);
        } else {
            WorkflowCheckpointEntity entity = new WorkflowCheckpointEntity();
            entity.setExecutionId(executionId);
            entity.setWorkflowId(context.getWorkflowId());
            entity.setCurrentNodeId(currentNodeId);
            entity.setContextData(contextJson);
            dataManager.save(WorkflowCheckpointEntity.class, entity);
            log.debug("Saved checkpoint for executionId={}, currentNodeId={}", executionId, currentNodeId);
        }
    }

    @Override
    public @Nullable Checkpoint load(@NonNull String executionId) {
        WorkflowCheckpointEntity entity = findByExecutionId(executionId);
        if (entity == null) {
            log.debug("No checkpoint found for executionId={}", executionId);
            return null;
        }
        return new Checkpoint(
            entity.getExecutionId(),
            entity.getWorkflowId(),
            entity.getCurrentNodeId(),
            entity.getCreatedAt().toEpochMilli()
        );
    }

    @Override
    public void complete(@NonNull String executionId) {
        WorkflowCheckpointEntity entity = findByExecutionId(executionId);
        if (entity != null) {
            dataManager.deleteById(WorkflowCheckpointEntity.class, entity.getId());
            log.debug("Completed and removed checkpoint for executionId={}", executionId);
        } else {
            log.debug("No checkpoint to complete for executionId={}", executionId);
        }
    }

    private @Nullable WorkflowCheckpointEntity findByExecutionId(@NonNull String executionId) {
        List<WorkflowCheckpointEntity> results = dataManager.entity(WorkflowCheckpointEntity.class)
            .query()
            .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(WorkflowCheckpointEntity.class)
                .eq(WorkflowCheckpointEntity::getExecutionId, executionId)
                .build())
            .list();
        return results.isEmpty() ? null : results.get(0);
    }

    private String serializeContext(@NonNull ExecutionContext context) {
        try {
            var data = new java.util.LinkedHashMap<String, Object>();
            data.put("workflowId", context.getWorkflowId());
            data.put("conversationId", context.getConversationId());
            data.put("userId", context.getUserId());
            data.put("variables", context.getVariables());
            data.put("nodeOutputs", context.getNodeOutputs());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to serialize execution context", e);
            return "{}";
        }
    }
}
