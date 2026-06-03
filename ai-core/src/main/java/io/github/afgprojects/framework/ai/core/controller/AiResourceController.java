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
import io.github.afgprojects.framework.ai.core.entity.tool.ToolRegistryEntity;
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
    public ResponseEntity<ToolRegistryEntity> getTool(@PathVariable Long id) {
        return dataManager.findById(ToolRegistryEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新工具
     */
    @PutMapping("/tools/{id}")
    @Transactional
    public ToolRegistryEntity updateTool(@PathVariable Long id,
                                         @Valid @RequestBody UpdateToolRequest request) {
        ToolRegistryEntity entity = dataManager.findById(ToolRegistryEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + id));

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

        return dataManager.save(ToolRegistryEntity.class, entity);
    }

    /**
     * 删除工具（软删除）
     */
    @DeleteMapping("/tools/{id}")
    @Transactional
    public ResponseEntity<Void> deleteTool(@PathVariable Long id) {
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
            .orElseThrow(() -> new IllegalArgumentException("Runtime tool not found: " + name));

        Map<String, Object> params = (request != null && request.getParameters() != null)
            ? request.getParameters()
            : Map.of();

        Object result = tool.execute(params);
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
    public ResponseEntity<ApplicationEntity> getApplication(@PathVariable Long id) {
        return dataManager.findById(ApplicationEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新应用
     */
    @PutMapping("/applications/{id}")
    @Transactional
    public ApplicationEntity updateApplication(@PathVariable Long id,
                                                @Valid @RequestBody UpdateApplicationRequest request) {
        ApplicationEntity entity = dataManager.findById(ApplicationEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

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

        return dataManager.save(ApplicationEntity.class, entity);
    }

    /**
     * 删除应用（软删除）
     */
    @DeleteMapping("/applications/{id}")
    @Transactional
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
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
    public List<ApplicationVersionEntity> listApplicationVersions(@PathVariable Long id) {
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
    public ApplicationVersionEntity createApplicationVersion(@PathVariable Long id,
                                                              @Valid @RequestBody CreateApplicationVersionRequest request) {
        if (!dataManager.existsById(ApplicationEntity.class, id)) {
            throw new IllegalArgumentException("Application not found: " + id);
        }

        ApplicationVersionEntity entity = new ApplicationVersionEntity();
        entity.setApplicationId(id);
        entity.setVersion(request.getVersion());
        entity.setConfig(request.getConfig());
        entity.setDescription(request.getDescription());
        entity.setPublishedAt(request.getPublishedAt());
        entity.setUserId(request.getUserId());
        return dataManager.save(ApplicationVersionEntity.class, entity);
    }
}
