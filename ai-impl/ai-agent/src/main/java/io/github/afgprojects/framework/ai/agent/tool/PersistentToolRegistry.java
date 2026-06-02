package io.github.afgprojects.framework.ai.agent.tool;

import io.github.afgprojects.framework.ai.agent.tool.entity.ToolRegistryEntity;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 持久化工具注册表
 *
 * <p>基于 DataManager 实现的工具注册表，将工具注册信息持久化到数据库。
 * 同时维护内存中的 Tool 实例缓存，确保工具执行时能快速获取。
 *
 * <p>工作流程：
 * <ul>
 *   <li>register - 将工具元数据保存到数据库，并在内存中缓存 Tool 实例</li>
 *   <li>unregister - 从数据库软删除工具记录，并移除内存缓存</li>
 *   <li>getTool - 优先从内存缓存获取 Tool 实例</li>
 *   <li>getAllTools - 从内存缓存获取所有已注册的 Tool 实例</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class PersistentToolRegistry implements ToolRegistry {

    private final DataManager dataManager;

    /**
     * 内存中的 Tool 实例缓存
     *
     * <p>数据库只存储工具元数据，实际的 Tool 实例（包含执行逻辑）保存在内存中。
     */
    private final Map<String, Tool<?, ?>> toolCache = new ConcurrentHashMap<>();

    /**
     * 创建持久化工具注册表
     *
     * @param dataManager 数据操作管理器
     */
    public PersistentToolRegistry(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public <I, O> void register(@NonNull Tool<I, O> tool) {
        String name = tool.name();

        // 检查是否已存在
        if (exists(name)) {
            throw new IllegalArgumentException("Tool already registered: " + name);
        }

        // 保存到数据库
        saveToDatabase(tool);

        // 缓存 Tool 实例
        toolCache.put(name, tool);

        log.info("Tool registered: {}", name);
    }

    @Override
    public <I, O> void registerOrReplace(@NonNull Tool<I, O> tool) {
        String name = tool.name();

        // 检查数据库中是否已存在
        Optional<ToolRegistryEntity> existing = findEntityByName(name);

        if (existing.isPresent()) {
            // 更新数据库记录
            ToolRegistryEntity entity = existing.get();
            updateEntity(entity, tool);
            dataManager.save(ToolRegistryEntity.class, entity);
            log.info("Tool replaced in database: {}", name);
        } else {
            // 新增数据库记录
            saveToDatabase(tool);
            log.info("Tool registered to database: {}", name);
        }

        // 更新内存缓存
        toolCache.put(name, tool);
    }

    @Override
    public @NonNull Optional<Tool<?, ?>> getTool(@NonNull String name) {
        return Optional.ofNullable(toolCache.get(name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> @NonNull Optional<Tool<I, O>> getTool(
            @NonNull String name,
            @NonNull Class<I> inputType,
            @NonNull Class<O> outputType) {
        Tool<?, ?> tool = toolCache.get(name);
        if (tool == null) {
            return Optional.empty();
        }
        return Optional.of((Tool<I, O>) tool);
    }

    @Override
    public @NonNull Collection<Tool<?, ?>> getAllTools() {
        return Collections.unmodifiableCollection(toolCache.values());
    }

    @Override
    public boolean exists(@NonNull String name) {
        // 先检查内存缓存
        if (toolCache.containsKey(name)) {
            return true;
        }
        // 再检查数据库
        return findEntityByName(name).isPresent();
    }

    @Override
    public boolean unregister(@NonNull String name) {
        // 从数据库软删除
        Optional<ToolRegistryEntity> entity = findEntityByName(name);
        if (entity.isPresent()) {
            ToolRegistryEntity e = entity.get();
            e.setDeleted(true);
            e.setStatus("DELETED");
            dataManager.save(ToolRegistryEntity.class, e);
            log.info("Tool unregistered from database: {}", name);
        }

        // 从内存缓存移除
        Tool<?, ?> removed = toolCache.remove(name);
        if (removed == null && entity.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void clear() {
        // 软删除数据库中所有记录
        List<ToolRegistryEntity> allEntities = dataManager.entity(ToolRegistryEntity.class)
            .query()
            .where(Conditions.builder(ToolRegistryEntity.class)
                .eq(ToolRegistryEntity::getDeleted, false)
                .build())
            .list();

        for (ToolRegistryEntity entity : allEntities) {
            entity.setDeleted(true);
            entity.setStatus("DELETED");
            dataManager.save(ToolRegistryEntity.class, entity);
        }

        // 清除内存缓存
        toolCache.clear();

        log.info("All tools cleared from registry");
    }

    @Override
    public int size() {
        return toolCache.size();
    }

    // ==================== 内部方法 ====================

    /**
     * 根据名称查找数据库实体
     */
    private Optional<ToolRegistryEntity> findEntityByName(@NonNull String name) {
        try {
            return dataManager.entity(ToolRegistryEntity.class)
                .query()
                .where(Conditions.builder(ToolRegistryEntity.class)
                    .eq(ToolRegistryEntity::getName, name)
                    .eq(ToolRegistryEntity::getDeleted, false)
                    .build())
                .one();
        } catch (Exception e) {
            log.error("Failed to query tool registry by name: {} - {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 将 Tool 信息保存到数据库
     */
    private <I, O> void saveToDatabase(@NonNull Tool<I, O> tool) {
        ToolRegistryEntity entity = new ToolRegistryEntity();
        entity.setName(tool.name());
        entity.setDescription(tool.description());
        entity.setInputSchema(tool.inputSchema());
        entity.setStatus("ENABLED");

        dataManager.save(ToolRegistryEntity.class, entity);
    }

    /**
     * 更新数据库实体信息
     */
    private <I, O> void updateEntity(@NonNull ToolRegistryEntity entity, @NonNull Tool<I, O> tool) {
        entity.setDescription(tool.description());
        entity.setInputSchema(tool.inputSchema());
        entity.setStatus("ENABLED");
        entity.setDeleted(false);
    }
}
