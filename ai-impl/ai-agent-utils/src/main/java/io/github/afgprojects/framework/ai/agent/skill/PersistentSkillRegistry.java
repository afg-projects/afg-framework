package io.github.afgprojects.framework.ai.agent.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.agent.skill.entity.SkillEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.builder;

/**
 * 基于数据库持久化的 Skill 注册表实现
 *
 * <p>直接基于 {@link DataManager} 做数据库 CRUD，无需内存缓存双写。
 * DataManager 自带缓存机制，无需额外缓存层。
 *
 * <p>将 {@link SkillDefinition} 持久化到 {@code ai_skill} 表，
 * register/unregister/getAll 等操作直接操作数据库。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentSkillRegistry implements SkillRegistry {

    private final DataManager dataManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void register(@NonNull SkillDefinition definition) {
        String name = definition.name();

        // 查找是否已存在同名的未删除记录
        Optional<SkillEntity> existingOpt = dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getName, name)
                        .eq(SkillEntity::getDeleted, false)
                        .build())
                .one();

        SkillEntity entity;
        if (existingOpt.isPresent()) {
            // 已存在则更新
            entity = existingOpt.get();
            updateEntityFromDefinition(entity, definition);
            entity.setUpdatedAt(LocalDateTime.now());
            log.info("Updating existing skill in DB: {}", name);
        } else {
            // 不存在则新增
            entity = new SkillEntity();
            updateEntityFromDefinition(entity, definition);
            entity.setStatus("ENABLED");
            entity.setDeleted(false);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            log.info("Registering new skill in DB: {}", name);
        }

        dataManager.save(SkillEntity.class, entity);
    }

    @Override
    public boolean unregister(@NonNull String name) {
        Optional<SkillEntity> existingOpt = dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getName, name)
                        .eq(SkillEntity::getDeleted, false)
                        .build())
                .one();

        if (existingOpt.isEmpty()) {
            log.warn("Skill not found for unregister: {}", name);
            return false;
        }

        SkillEntity entity = existingOpt.get();
        entity.setDeleted(true);
        entity.setUpdatedAt(LocalDateTime.now());
        dataManager.save(SkillEntity.class, entity);

        log.info("Unregistered skill (soft delete): {}", name);
        return true;
    }

    @Override
    @NonNull
    public Optional<SkillDefinition> get(@NonNull String name) {
        Optional<SkillEntity> entityOpt = dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getName, name)
                        .eq(SkillEntity::getDeleted, false)
                        .build())
                .one();

        return entityOpt.map(this::toDefinition);
    }

    @Override
    public boolean exists(@NonNull String name) {
        return dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getName, name)
                        .eq(SkillEntity::getDeleted, false)
                        .build())
                .exists();
    }

    @Override
    @NonNull
    public List<SkillDefinition> getAll() {
        List<SkillEntity> entities = dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getDeleted, false)
                        .eq(SkillEntity::getStatus, "ENABLED")
                        .build())
                .list();

        List<SkillDefinition> definitions = new ArrayList<>(entities.size());
        for (SkillEntity entity : entities) {
            definitions.add(toDefinition(entity));
        }
        return Collections.unmodifiableList(definitions);
    }

    @Override
    public int size() {
        return (int) dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getDeleted, false)
                        .eq(SkillEntity::getStatus, "ENABLED")
                        .build())
                .count();
    }

    @Override
    public void clear() {
        List<SkillEntity> entities = dataManager.entity(SkillEntity.class)
                .query()
                .where(builder(SkillEntity.class)
                        .eq(SkillEntity::getDeleted, false)
                        .build())
                .list();

        for (SkillEntity entity : entities) {
            entity.setDeleted(true);
            entity.setUpdatedAt(LocalDateTime.now());
            dataManager.save(SkillEntity.class, entity);
        }

        log.info("Cleared all skills (soft delete): {} skills", entities.size());
    }

    // ========== Entity <-> Definition 转换 ==========

    /**
     * 将 SkillDefinition 的数据写入 SkillEntity
     */
    private void updateEntityFromDefinition(@NonNull SkillEntity entity, @NonNull SkillDefinition definition) {
        entity.setName(definition.name());
        entity.setDescription(definition.description());
        entity.setPromptTemplate(definition.prompt());
        entity.setInputParameters(toJson(definition.inputs()));
        entity.setTools(toJson(definition.tools()));
        entity.setDependsOn(toJson(definition.dependsOn()));
        entity.setMetadata(toJson(definition.metadata()));

        // 从 metadata 中提取 type 分类
        if (definition.metadata() != null && definition.metadata().containsKey("category")) {
            Object category = definition.metadata().get("category");
            entity.setType(category != null ? category.toString() : null);
        }
    }

    /**
     * 将 SkillEntity 转换为 SkillDefinition
     */
    @NonNull
    SkillDefinition toDefinition(@NonNull SkillEntity entity) {
        List<SkillDefinition.InputParameter> inputs = parseInputParameters(entity.getInputParameters());
        List<String> tools = parseJsonToStringList(entity.getTools());
        List<String> dependsOn = parseJsonToStringList(entity.getDependsOn());
        Map<String, Object> metadata = parseJsonToMap(entity.getMetadata());

        return new SkillDefinition(
                entity.getName(),
                entity.getDescription() != null ? entity.getDescription() : "",
                entity.getPromptTemplate() != null ? entity.getPromptTemplate() : "",
                inputs,
                tools,
                dependsOn,
                metadata
        );
    }

    /**
     * 解析输入参数 JSON 为 InputParameter 列表
     */
    private @Nullable List<SkillDefinition.InputParameter> parseInputParameters(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            List<SkillDefinition.InputParameter> result = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String paramName = (String) item.get("name");
                String paramDesc = item.get("description") != null ? item.get("description").toString() : null;
                SkillDefinition.ParameterType type = parseParameterType(
                        item.get("type") != null ? item.get("type").toString() : "STRING");
                boolean required = item.get("required") != null && Boolean.parseBoolean(item.get("required").toString());
                String defaultValue = item.get("defaultValue") != null ? item.get("defaultValue").toString() : null;
                List<String> enumValues = item.get("enumValues") != null
                        ? parseJsonToStringList(toJsonString(item.get("enumValues")))
                        : null;

                result.add(new SkillDefinition.InputParameter(paramName, paramDesc, type, required, defaultValue, enumValues));
            }
            return result.isEmpty() ? null : result;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse input parameters JSON: {}", json, e);
            return null;
        }
    }

    private SkillDefinition.ParameterType parseParameterType(@Nullable String type) {
        if (type == null) return SkillDefinition.ParameterType.STRING;
        return switch (type.toUpperCase()) {
            case "INTEGER" -> SkillDefinition.ParameterType.INTEGER;
            case "NUMBER" -> SkillDefinition.ParameterType.NUMBER;
            case "BOOLEAN" -> SkillDefinition.ParameterType.BOOLEAN;
            case "ENUM" -> SkillDefinition.ParameterType.ENUM;
            case "ARRAY" -> SkillDefinition.ParameterType.ARRAY;
            case "OBJECT" -> SkillDefinition.ParameterType.OBJECT;
            default -> SkillDefinition.ParameterType.STRING;
        };
    }

    // ========== JSON 工具方法 ==========

    private @Nullable String toJson(@Nullable Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }

    private @Nullable String toJsonString(@Nullable Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private @Nullable List<String> parseJsonToStringList(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON list: {}", json, e);
            return null;
        }
    }

    private @Nullable Map<String, Object> parseJsonToMap(@Nullable String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON map: {}", json, e);
            return null;
        }
    }
}
