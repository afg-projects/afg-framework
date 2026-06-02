package io.github.afgprojects.framework.ai.agent.tool.execution;

import io.github.afgprojects.framework.ai.agent.tool.entity.ToolExecutionEntity;
import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 DataManager 的工具执行记录器
 *
 * <p>将工具执行记录持久化到数据库，通过 DataManager 进行 CRUD 操作。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class JdbcToolExecutionRecorder implements ToolExecutionRecorder {

    private final DataManager dataManager;

    /**
     * 创建 JDBC 工具执行记录器
     *
     * @param dataManager 数据操作管理器
     */
    public JdbcToolExecutionRecorder(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public @NonNull String recordStart(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {

        String executionId = generateExecutionId();
        Instant startTime = Instant.now();

        try {
            ToolExecutionEntity entity = new ToolExecutionEntity();
            entity.setExecutionId(executionId);
            entity.setToolName(toolName);
            entity.setUserId(context.getUserId());
            entity.setTenantId(context.getTenantId());
            entity.setSessionId(context.getSessionId());
            entity.setArguments(serializeArguments(arguments));
            entity.setStatus("STARTED");
            entity.setStartTime(startTime);

            dataManager.save(ToolExecutionEntity.class, entity);

            log.debug("Tool execution start recorded: executionId={}, toolName={}, userId={}",
                executionId, toolName, context.getUserId());
        } catch (Exception e) {
            log.error("Failed to record tool execution start: {}", e.getMessage());
            // 不抛出异常，避免影响工具执行
        }

        return executionId;
    }

    @Override
    public void recordSuccess(
            @NonNull String executionId,
            @Nullable Object output,
            @NonNull Duration duration) {

        Instant endTime = Instant.now();

        try {
            Optional<ToolExecutionEntity> entityOpt = findEntityByExecutionId(executionId);
            if (entityOpt.isEmpty()) {
                log.warn("Tool execution record not found: executionId={}", executionId);
                return;
            }

            ToolExecutionEntity entity = entityOpt.get();
            entity.setOutput(serializeOutput(output));
            entity.setStatus("SUCCESS");
            entity.setEndTime(endTime);
            entity.setDurationMs(duration.toMillis());

            dataManager.save(ToolExecutionEntity.class, entity);

            log.debug("Tool execution success recorded: executionId={}, durationMs={}",
                executionId, duration.toMillis());
        } catch (Exception e) {
            log.error("Failed to record tool execution success: {}", e.getMessage());
        }
    }

    @Override
    public void recordFailure(
            @NonNull String executionId,
            @NonNull String error,
            @NonNull Duration duration) {

        Instant endTime = Instant.now();

        try {
            Optional<ToolExecutionEntity> entityOpt = findEntityByExecutionId(executionId);
            if (entityOpt.isEmpty()) {
                log.warn("Tool execution record not found: executionId={}", executionId);
                return;
            }

            ToolExecutionEntity entity = entityOpt.get();
            entity.setError(error);
            entity.setStatus("FAILURE");
            entity.setEndTime(endTime);
            entity.setDurationMs(duration.toMillis());

            dataManager.save(ToolExecutionEntity.class, entity);

            log.debug("Tool execution failure recorded: executionId={}, error={}", executionId, error);
        } catch (Exception e) {
            log.error("Failed to record tool execution failure: {}", e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 根据执行 ID 查找实体
     */
    private Optional<ToolExecutionEntity> findEntityByExecutionId(@NonNull String executionId) {
        return dataManager.entity(ToolExecutionEntity.class)
            .query()
            .where(Conditions.builder(ToolExecutionEntity.class)
                .eq(ToolExecutionEntity::getExecutionId, executionId)
                .build())
            .one();
    }

    /**
     * 生成执行 ID
     */
    private String generateExecutionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 序列化参数
     */
    private @Nullable String serializeArguments(@NonNull Map<String, Object> arguments) {
        try {
            Map<String, Object> filtered = new HashMap<>(arguments);
            filtered.remove("__tool__");
            return filtered.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 序列化输出
     */
    private @Nullable String serializeOutput(@Nullable Object output) {
        if (output == null) {
            return null;
        }
        return output.toString();
    }
}
