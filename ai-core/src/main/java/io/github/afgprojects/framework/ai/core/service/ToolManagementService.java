package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.entity.tool.ToolRegistryEntity;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 工具管理服务
 *
 * <p>提供工具启用/禁用、同步运行时注册表到数据库的功能。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolManagementService {

    private final DataManager dataManager;
    private final ToolRegistry toolRegistry;

    /**
     * 启用工具
     *
     * @param toolId 工具ID
     * @return 更新后的工具实体
     */
    @Transactional
    public ToolRegistryEntity enableTool(Long toolId) {
        ToolRegistryEntity tool = dataManager.findById(ToolRegistryEntity.class, toolId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "工具不存在: " + toolId));
        tool.setEnabled(true);
        log.info("Tool enabled: id={}, name={}", toolId, tool.getName());
        return dataManager.save(ToolRegistryEntity.class, tool);
    }

    /**
     * 禁用工具
     *
     * @param toolId 工具ID
     * @return 更新后的工具实体
     */
    @Transactional
    public ToolRegistryEntity disableTool(Long toolId) {
        ToolRegistryEntity tool = dataManager.findById(ToolRegistryEntity.class, toolId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "工具不存在: " + toolId));
        tool.setEnabled(false);
        log.info("Tool disabled: id={}, name={}", toolId, tool.getName());
        return dataManager.save(ToolRegistryEntity.class, tool);
    }

    /**
     * 同步运行时注册表到数据库
     *
     * <p>将 ToolRegistry 中运行时注册的工具信息同步到 ToolRegistryEntity 数据库表。
     * 已存在的工具（按名称匹配）跳过，新增的工具写入数据库。
     *
     * @return 新同步的工具数量
     */
    @Transactional
    public int syncRegistry() {
        Collection<io.github.afgprojects.framework.ai.core.api.tool.Tool<?, ?>> runtimeTools = toolRegistry.getAllTools();
        int synced = 0;

        for (io.github.afgprojects.framework.ai.core.api.tool.Tool<?, ?> runtimeTool : runtimeTools) {
            String toolName = runtimeTool.name();

            // 检查数据库中是否已存在同名工具
            List<ToolRegistryEntity> existing = dataManager.entity(ToolRegistryEntity.class)
                .query()
                .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder(ToolRegistryEntity.class)
                    .eq(ToolRegistryEntity::getName, toolName)
                    .build())
                .list();

            if (existing.isEmpty()) {
                ToolRegistryEntity entity = new ToolRegistryEntity();
                entity.setName(toolName);
                entity.setDescription(runtimeTool.description());
                entity.setType("RUNTIME");
                entity.setEnabled(true);
                dataManager.save(ToolRegistryEntity.class, entity);
                synced++;
                log.info("Synced runtime tool to database: name={}", toolName);
            }
        }

        log.info("Registry sync completed: synced={}, total={}", synced, runtimeTools.size());
        return synced;
    }
}
