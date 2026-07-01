package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.api.skill.SkillDispatcher;
import io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.api.skill.SkillExecutor;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRoutingResult;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateApplicationVersionRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.CreateToolRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.HitTestItem;
import io.github.afgprojects.framework.ai.core.dto.resource.HitTestRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.HitTestResponse;
import io.github.afgprojects.framework.ai.core.dto.resource.ToolExecuteRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateApplicationRequest;
import io.github.afgprojects.framework.ai.core.dto.resource.UpdateToolRequest;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationVersionEntity;
import io.github.afgprojects.framework.ai.core.entity.skill.UserSkillEntity;
import io.github.afgprojects.framework.ai.core.entity.chat.ChatLogEntity;
import io.github.afgprojects.framework.ai.core.entity.tool.ToolRegistryEntity;
import io.github.afgprojects.framework.ai.core.service.ApplicationPublishService;
import io.github.afgprojects.framework.ai.core.service.ToolManagementService;
import io.github.afgprojects.framework.ai.core.skill.UserSkillLoader;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;
    private final SkillRegistry skillRegistry;
    private final SkillExecutor skillExecutor;
    private final UserSkillLoader userSkillLoader;

    // ==================== 工具注册 CRUD ====================

    /**
     * 创建工具
     */
    @PostMapping("/tools")
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
    public ResponseEntity<ToolRegistryEntity> updateTool(@PathVariable String id,
                                         @Valid @RequestBody UpdateToolRequest request) {
        return dataManager.executeInTransaction(() -> {
            ToolRegistryEntity entity = dataManager.findById(ToolRegistryEntity.class, id)
                .orElse(null);
            if (entity == null) {
                return ResponseEntity.<ToolRegistryEntity>notFound().build();
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
        });
    }

    /**
     * 删除工具（软删除）
     */
    @DeleteMapping("/tools/{id}")
    public ResponseEntity<Void> deleteTool(@PathVariable String id) {
        return dataManager.executeInTransaction(() -> {
            ToolRegistryEntity entity = dataManager.findById(ToolRegistryEntity.class, id)
                .orElse(null);
            if (entity == null) {
                return ResponseEntity.<Void>notFound().build();
            }
            entity.markDeleted();
            dataManager.save(ToolRegistryEntity.class, entity);
            return ResponseEntity.noContent().build();
        });
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

    // ==================== 用户技能 CRUD ====================

    /**
     * 列出全部用户技能（含禁用）
     */
    @GetMapping("/skills")
    public List<UserSkillEntity> listSkills() {
        return dataManager.entity(UserSkillEntity.class)
            .query()
            .orderByDesc(UserSkillEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个用户技能
     */
    @GetMapping("/skills/{name}")
    public ResponseEntity<UserSkillEntity> getSkill(@PathVariable String name) {
        return dataManager.entity(UserSkillEntity.class)
            .query()
            .where(Conditions.builder(UserSkillEntity.class)
                .eq(UserSkillEntity::getName, name)
                .build())
            .list().stream().findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建用户技能（持久化 + 注册进 SkillRegistry）
     */
    @PostMapping("/skills")
    public ResponseEntity<UserSkillEntity> createSkill(@Valid @RequestBody UserSkillEntity request) {
        return dataManager.executeInTransaction(() -> {
            request.setId(null);
            if (request.getEnabled() == null) {
                request.setEnabled(true);
            }
            UserSkillEntity saved = dataManager.save(UserSkillEntity.class, request);
            if (Boolean.TRUE.equals(saved.getEnabled())) {
                skillRegistry.register(userSkillLoader.toDefinition(saved));
            }
            return ResponseEntity.ok(saved);
        });
    }

    /**
     * 更新用户技能（持久化 + 重新注册）
     */
    @PutMapping("/skills/{name}")
    public ResponseEntity<UserSkillEntity> updateSkill(@PathVariable String name,
                                                       @Valid @RequestBody UserSkillEntity request) {
        return dataManager.executeInTransaction(() -> {
            UserSkillEntity existing = dataManager.entity(UserSkillEntity.class)
                .query()
                .where(Conditions.builder(UserSkillEntity.class)
                    .eq(UserSkillEntity::getName, name)
                    .build())
                .list().stream().findFirst().orElse(null);
            if (existing == null) {
                return ResponseEntity.<UserSkillEntity>notFound().build();
            }
            // 更新字段（name 不改，作为业务键）
            existing.setDescription(request.getDescription());
            existing.setPrompt(request.getPrompt());
            existing.setInputs(request.getInputs());
            existing.setTools(request.getTools());
            existing.setDependsOn(request.getDependsOn());
            existing.setMetadata(request.getMetadata());
            if (request.getEnabled() != null) {
                existing.setEnabled(request.getEnabled());
            }
            UserSkillEntity saved = dataManager.save(UserSkillEntity.class, existing);
            // 重新注册：注销旧定义、注册新定义
            skillRegistry.unregister(name);
            if (Boolean.TRUE.equals(saved.getEnabled())) {
                skillRegistry.register(userSkillLoader.toDefinition(saved));
            }
            return ResponseEntity.ok(saved);
        });
    }

    /**
     * 删除用户技能（软删除 + 注销）
     */
    @DeleteMapping("/skills/{name}")
    public ResponseEntity<Void> deleteSkill(@PathVariable String name) {
        return dataManager.executeInTransaction(() -> {
            UserSkillEntity existing = dataManager.entity(UserSkillEntity.class)
                .query()
                .where(Conditions.builder(UserSkillEntity.class)
                    .eq(UserSkillEntity::getName, name)
                    .build())
                .list().stream().findFirst().orElse(null);
            if (existing == null) {
                return ResponseEntity.<Void>notFound().build();
            }
            existing.markDeleted();
            dataManager.save(UserSkillEntity.class, existing);
            skillRegistry.unregister(name);
            return ResponseEntity.noContent().build();
        });
    }

    /**
     * 启用用户技能（注册进 SkillRegistry）
     */
    @PostMapping("/skills/{name}/enable")
    public ResponseEntity<UserSkillEntity> enableSkill(@PathVariable String name) {
        return dataManager.executeInTransaction(() -> {
            UserSkillEntity existing = dataManager.entity(UserSkillEntity.class)
                .query()
                .where(Conditions.builder(UserSkillEntity.class)
                    .eq(UserSkillEntity::getName, name)
                    .build())
                .list().stream().findFirst().orElse(null);
            if (existing == null) {
                return ResponseEntity.<UserSkillEntity>notFound().build();
            }
            existing.setEnabled(true);
            UserSkillEntity saved = dataManager.save(UserSkillEntity.class, existing);
            skillRegistry.register(userSkillLoader.toDefinition(saved));
            return ResponseEntity.ok(saved);
        });
    }

    /**
     * 禁用用户技能（从 SkillRegistry 注销）
     */
    @PostMapping("/skills/{name}/disable")
    public ResponseEntity<UserSkillEntity> disableSkill(@PathVariable String name) {
        return dataManager.executeInTransaction(() -> {
            UserSkillEntity existing = dataManager.entity(UserSkillEntity.class)
                .query()
                .where(Conditions.builder(UserSkillEntity.class)
                    .eq(UserSkillEntity::getName, name)
                    .build())
                .list().stream().findFirst().orElse(null);
            if (existing == null) {
                return ResponseEntity.<UserSkillEntity>notFound().build();
            }
            existing.setEnabled(false);
            UserSkillEntity saved = dataManager.save(UserSkillEntity.class, existing);
            skillRegistry.unregister(name);
            return ResponseEntity.ok(saved);
        });
    }

    /**
     * 执行用户技能（复用 SkillExecutor）
     */
    @PostMapping("/skills/{name}/invoke")
    public ResponseEntity<Object> invokeSkill(@PathVariable String name,
                                              @RequestBody(required = false) Map<String, Object> inputs) {
        return skillRegistry.get(name)
            .map(def -> {
                Object result = skillExecutor.execute(name, inputs != null ? inputs : Map.of());
                return ResponseEntity.ok(result);
            })
            .orElse(ResponseEntity.<Object>notFound().build());
    }

    // ==================== 应用 CRUD ====================

    /**
     * 创建应用
     */
    @PostMapping("/applications")
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
    public ResponseEntity<ApplicationEntity> updateApplication(@PathVariable String id,
                                                @Valid @RequestBody UpdateApplicationRequest request) {
        return dataManager.executeInTransaction(() -> {
            ApplicationEntity entity = dataManager.findById(ApplicationEntity.class, id)
                .orElse(null);
            if (entity == null) {
                return ResponseEntity.<ApplicationEntity>notFound().build();
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
        });
    }

    /**
     * 删除应用（软删除）
     */
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable String id) {
        return dataManager.executeInTransaction(() -> {
            ApplicationEntity entity = dataManager.findById(ApplicationEntity.class, id)
                .orElse(null);
            if (entity == null) {
                return ResponseEntity.<Void>notFound().build();
            }
            entity.markDeleted();
            dataManager.save(ApplicationEntity.class, entity);
            return ResponseEntity.noContent().build();
        });
    }

    // ==================== 应用命中测试 ====================

    /**
     * 应用命中测试
     * <p>
     * 根据应用关联的知识库做检索 + 关联工具匹配，返回命中结果。
     * 关联配置（knowledgeBaseIds/toolIds）存于 ApplicationEntity.config JSON。
     *
     * @param id      应用 ID
     * @param request 测试请求（question + 可选 topN/similarityThreshold）
     */
    @PostMapping("/applications/{id}/hit-test")
    public HitTestResponse hitTest(@PathVariable String id, @Valid @RequestBody HitTestRequest request) {
        ApplicationEntity app = dataManager.findById(ApplicationEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        Map<String, Object> config = parseConfigJson(app.getConfig());
        @SuppressWarnings("unchecked")
        List<String> knowledgeBaseIds = (List<String>) config.getOrDefault("knowledgeBaseIds", List.of());
        @SuppressWarnings("unchecked")
        List<String> toolIds = (List<String>) config.getOrDefault("toolIds", List.of());

        int topK = request.getTopN() != null ? request.getTopN() : 5;
        double similarityThreshold = request.getSimilarityThreshold() != null ? request.getSimilarityThreshold() : 0.7;
        String question = request.getQuestion();

        List<HitTestItem> results = new ArrayList<>();

        // 知识库检索
        for (String kbId : knowledgeBaseIds) {
            try {
                List<Document> docs = knowledgeBaseService.search(kbId, question, topK, similarityThreshold);
                for (Document doc : docs) {
                    double score = extractScore(doc);
                    results.add(new HitTestItem("knowledge", kbId, score, truncate(doc.content(), 500)));
                }
            } catch (Exception e) {
                log.warn("hit-test: knowledge base {} search failed: {}", kbId, e.getMessage());
            }
        }

        // 工具匹配（按名称/描述包含 question 关键词）
        for (Tool<?, ?> tool : toolRegistry.getAllTools()) {
            if (!toolIds.isEmpty() && !toolIds.contains(tool.name())) {
                continue;
            }
            if (matchesQuestion(tool, question)) {
                results.add(new HitTestItem("tool", tool.name(), 1.0, tool.description()));
            }
        }

        return new HitTestResponse(results);
    }

    /** 解析 config JSON，失败返回空 Map */
    private Map<String, Object> parseConfigJson(String config) {
        if (config == null || config.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(config, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("parse config JSON failed: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 从 Document.metadata 提取 score，无则默认 1.0 */
    private double extractScore(Document doc) {
        Object score = doc.metadata() == null ? null : doc.metadata().get("score");
        if (score instanceof Number n) {
            return n.doubleValue();
        }
        return 1.0;
    }

    /** 工具名称/描述是否包含 question 的关键词（简单匹配，不区分大小写） */
    private boolean matchesQuestion(Tool<?, ?> tool, String question) {
        String q = question.toLowerCase();
        String name = tool.name() == null ? "" : tool.name().toLowerCase();
        String desc = tool.description() == null ? "" : tool.description().toLowerCase();
        return name.contains(q) || desc.contains(q)
            || java.util.Arrays.stream(q.split("\\s+")).anyMatch(name::contains);
    }

    /** 截断文本到指定长度 */
    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
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
    public ResponseEntity<ApplicationVersionEntity> createApplicationVersion(@PathVariable String id,
                                                              @Valid @RequestBody CreateApplicationVersionRequest request) {
        return dataManager.executeInTransaction(() -> {
            if (!dataManager.existsById(ApplicationEntity.class, id)) {
                return ResponseEntity.<ApplicationVersionEntity>notFound().build();
            }

            ApplicationVersionEntity entity = new ApplicationVersionEntity();
            entity.setApplicationId(id);
            entity.setVersion(request.getVersion());
            entity.setConfig(request.getConfig());
            entity.setDescription(request.getDescription());
            entity.setPublishedAt(request.getPublishedAt());
            entity.setUserId(request.getUserId());
            return ResponseEntity.ok(dataManager.save(ApplicationVersionEntity.class, entity));
        });
    }

    /**
     * 发布应用
     *
     * <p>将应用状态从 DRAFT 转换为 PUBLISHED。
     */
    @PostMapping("/applications/{id}/publish")
    public ResponseEntity<ApplicationEntity> publishApplication(@PathVariable String id) {
        return ResponseEntity.ok(applicationPublishService.publish(id));
    }

    /**
     * 取消发布应用
     *
     * <p>将应用状态从 PUBLISHED 转换为 DRAFT。
     */
    @PostMapping("/applications/{id}/unpublish")
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
