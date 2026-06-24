package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.skill.SkillDispatcher;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRoutingResult;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationVersionRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateToolRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.ToolExecuteRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateToolRequest;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationVersionEntity;
import io.github.afgprojects.framework.ai.core.entity.chat.ChatLogEntity;
import io.github.afgprojects.framework.ai.core.entity.tool.ToolRegistryEntity;
import io.github.afgprojects.framework.ai.core.service.ApplicationPublishService;
import io.github.afgprojects.framework.ai.core.service.ToolManagementService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 资源管理控制器
 * <p>
 * 提供工具注册、技能调度、应用管理的 CRUD 和运行时接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class AiResourceController {

    private final DataManager dataManager;
    private final ToolRegistry toolRegistry;
    private final SkillDispatcher skillDispatcher;
    private final ToolManagementService toolManagementService;
    private final ApplicationPublishService applicationPublishService;

    // ==================== 工具注册 CRUD ====================

    /**
     * 创建工具
     */
    @PostMapping("/tools")
    @Transactional
    public ToolRegistryEntity createTool(@Valid @RequestBody CreateToolRequest request) {
        ToolRegistryEntity entity = new ToolRegistryEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setEndpoint(request.getEndpoint());
        entity.setParameters(request.getParameters());
        entity.setConfig(request.getConfig());
        entity.setEnabled(request.getEnabled());
        return dataManager.save(ToolRegistryEntity.class, entity);
    }

    /**
     * 列出工具（支持按类型筛选）
     */
    @GetMapping("/tools")
    public List<ToolRegistryEntity> listTools(@RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return dataManager.entity(ToolRegistryEntity.class)
                .query()
                .where(Conditions.builder(ToolRegistryEntity.class)
                    .eq(ToolRegistryEntity::getType, type)
                    .build())
                .orderByDesc(ToolRegistryEntity::getCreatedAt)
                .list();
        }
        return dataManager.entity(ToolRegistryEntity.class)
            .query()
            .orderByDesc(ToolRegistryEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个工具
     */
    @GetMapping("/tools/{id}")
    public ResponseEntity<ToolRegistryEntity> getTool(@PathVariable String id) {
        return dataManager.findById(ToolRegistryEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新工具
     */
    @PutMapping("/tools/{id}")
    @Transactional
    public ResponseEntity<ToolRegistryEntity> updateTool(@PathVariable String id,
                                         @Valid @RequestBody UpdateToolRequest request) {
        ToolRegistryEntity entity = dataManager.findById(ToolRegistryEntity.class, id)
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
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getEndpoint() != null) {
            entity.setEndpoint(request.getEndpoint());
        }
        if (request.getParameters() != null) {
            entity.setParameters(request.getParameters());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        return ResponseEntity.ok(dataManager.save(ToolRegistryEntity.class, entity));
    }

    /**
     * 删除工具（软删除）
     */
    @DeleteMapping("/tools/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTool(@PathVariable String id) {
        ToolRegistryEntity entity = dataManager.findById(ToolRegistryEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.markDeleted();
        dataManager.save(ToolRegistryEntity.class, entity);
        return ResponseEntity.noContent().build();
    }

    // ==================== 运行时工具执行 ====================

    /**
     * 通过名称执行运行时工具
     */
    @PostMapping("/tools/{name}/execute")
    public ResponseEntity<Object> executeTool(@PathVariable String name,
                                              @RequestBody(required = false) ToolExecuteRequest request) {
        Tool<?, ?> tool = toolRegistry.getTool(name)
            .orElse(null);
        if (tool == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> params = (request != null && request.getParameters() != null)
            ? request.getParameters()
            : Map.of();

        @SuppressWarnings("unchecked")
        Object result = ((Tool<Map<String, Object>, ?>) tool).execute(params);
        return ResponseEntity.ok(result);
    }

    // ==================== 技能调度 ====================

    /**
     * 调度技能
     */
    @PostMapping("/skills/dispatch")
    public SkillRoutingResult dispatchSkill(@RequestBody String input) {
        return skillDispatcher.dispatch(input);
    }

    /**
     * 列出运行时已注册的工具
     */
    @GetMapping("/skills/runtime")
    public List<Tool<?, ?>> listRuntimeTools() {
        return List.copyOf(toolRegistry.getAllTools());
    }

    // ==================== 应用 CRUD ====================

    /**
     * 创建应用
     */
    @PostMapping("/applications")
    @Transactional
    public ApplicationEntity createApplication(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationEntity entity = new ApplicationEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setAccessToken(request.getAccessToken());
        entity.setStatus(request.getStatus());
        entity.setConfig(request.getConfig());
        entity.setIcon(request.getIcon());
        entity.setSort(request.getSort());
        return dataManager.save(ApplicationEntity.class, entity);
    }

    /**
     * 列出所有应用
     */
    @GetMapping("/applications")
    public List<ApplicationEntity> listApplications() {
        return dataManager.entity(ApplicationEntity.class)
            .query()
            .orderByAsc(ApplicationEntity::getSort)
            .orderByDesc(ApplicationEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个应用
     */
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationEntity> getApplication(@PathVariable String id) {
        return dataManager.findById(ApplicationEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新应用
     */
    @PutMapping("/applications/{id}")
    @Transactional
    public ResponseEntity<ApplicationEntity> updateApplication(@PathVariable String id,
                                                @Valid @RequestBody UpdateApplicationRequest request) {
        ApplicationEntity entity = dataManager.findById(ApplicationEntity.class, id)
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
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getAccessToken() != null) {
            entity.setAccessToken(request.getAccessToken());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }
        if (request.getIcon() != null) {
            entity.setIcon(request.getIcon());
        }
        if (request.getSort() != null) {
            entity.setSort(request.getSort());
        }

        return ResponseEntity.ok(dataManager.save(ApplicationEntity.class, entity));
    }

    /**
     * 删除应用（软删除）
     */
    @DeleteMapping("/applications/{id}")
    @Transactional
    public ResponseEntity<Void> deleteApplication(@PathVariable String id) {
        ApplicationEntity entity = dataManager.findById(ApplicationEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.markDeleted();
        dataManager.save(ApplicationEntity.class, entity);
        return ResponseEntity.noContent().build();
    }

    // ==================== 应用版本 ====================

    /**
     * 列出应用版本
     */
    @GetMapping("/applications/{id}/versions")
    public List<ApplicationVersionEntity> listApplicationVersions(@PathVariable String id) {
        return dataManager.entity(ApplicationVersionEntity.class)
            .query()
            .where(Conditions.builder(ApplicationVersionEntity.class)
                .eq(ApplicationVersionEntity::getApplicationId, id)
                .build())
            .orderByDesc(ApplicationVersionEntity::getCreatedAt)
            .list();
    }

    /**
     * 创建应用版本
     */
    @PostMapping("/applications/{id}/versions")
    @Transactional
    public ResponseEntity<ApplicationVersionEntity> createApplicationVersion(@PathVariable String id,
                                                              @Valid @RequestBody CreateApplicationVersionRequest request) {
        if (!dataManager.existsById(ApplicationEntity.class, id)) {
            return ResponseEntity.notFound().build();
        }

        ApplicationVersionEntity entity = new ApplicationVersionEntity();
        entity.setApplicationId(id);
        entity.setVersion(request.getVersion());
        entity.setConfig(request.getConfig());
        entity.setDescription(request.getDescription());
        entity.setPublishedAt(request.getPublishedAt());
        entity.setUserId(request.getUserId());
        return ResponseEntity.ok(dataManager.save(ApplicationVersionEntity.class, entity));
    }

    /**
     * 发布应用
     *
     * <p>将应用状态从 DRAFT 转换为 PUBLISHED。
     */
    @PostMapping("/applications/{id}/publish")
    @Transactional
    public ResponseEntity<ApplicationEntity> publishApplication(@PathVariable String id) {
        return ResponseEntity.ok(applicationPublishService.publish(id));
    }

    /**
     * 取消发布应用
     *
     * <p>将应用状态从 PUBLISHED 转换为 DRAFT。
     */
    @PostMapping("/applications/{id}/unpublish")
    @Transactional
    public ResponseEntity<ApplicationEntity> unpublishApplication(@PathVariable String id) {
        return ResponseEntity.ok(applicationPublishService.unpublish(id));
    }

    /**
     * 按应用查询对话日志
     *
     * <p>查询指定应用下的所有对话日志，按创建时间倒序排列。
     */
    @GetMapping("/applications/{id}/chat-logs")
    public List<ChatLogEntity> listChatLogsByApplication(@PathVariable String id) {
        return dataManager.entity(ChatLogEntity.class)
            .query()
            .where(Conditions.builder(ChatLogEntity.class)
                .eq(ChatLogEntity::getApplicationId, id)
                .build())
            .orderByDesc(ChatLogEntity::getCreatedAt)
            .list();
    }

    // ==================== 工具管理扩展 ====================

    /**
     * 启用工具
     */
    @PostMapping("/tools/{id}/enable")
    public ResponseEntity<ToolRegistryEntity> enableTool(@PathVariable String id) {
        return ResponseEntity.ok(toolManagementService.enableTool(id));
    }

    /**
     * 禁用工具
     */
    @PostMapping("/tools/{id}/disable")
    public ResponseEntity<ToolRegistryEntity> disableTool(@PathVariable String id) {
        return ResponseEntity.ok(toolManagementService.disableTool(id));
    }

    /**
     * 同步运行时注册表到数据库
     */
    @PostMapping("/tools/sync-registry")
    public ResponseEntity<Integer> syncToolRegistry() {
        return ResponseEntity.ok(toolManagementService.syncRegistry());
    }
}
