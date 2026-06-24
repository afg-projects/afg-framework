package io.github.afgprojects.framework.governance.server.controller.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/console/config")
@RequiredArgsConstructor
public class ConfigImportExportController {

    private final DataManager dataManager;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @GetMapping("/export")
    public ResponseEntity<String> exportConfig(
            @RequestParam(required = false) String groupId,
            @RequestParam(defaultValue = "json") String format) throws JsonProcessingException {

        // 收集配置数据
        Map<String, Object> exportData = collectExportData(groupId);

        String content;
        String filename;
        MediaType mediaType;

        if ("yaml".equalsIgnoreCase(format)) {
            content = yamlMapper.writeValueAsString(exportData);
            filename = "config-export-" + LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".yaml";
            mediaType = MediaType.parseMediaType("application/x-yaml");
        } else {
            content = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            filename = "config-export-" + LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
            mediaType = MediaType.APPLICATION_JSON;
        }

        log.info("Exported config: groupId={}, format={}, items={}", groupId, format, exportData.size());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(mediaType)
            .body(content);
    }

    @PostMapping("/import")
    public Result<ImportResult> importConfig(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String mode) throws IOException {

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename();

        Map<String, Object> importData;
        if (filename != null && filename.endsWith(".yaml")) {
            importData = yamlMapper.readValue(content, new TypeReference<>() {
            });
        } else {
            importData = jsonMapper.readValue(content, new TypeReference<>() {
            });
        }

        ImportResult result = applyImportData(importData, "merge".equalsIgnoreCase(mode));

        log.info("Imported config: file={}, created={}, updated={}, skipped={}",
            filename, result.getCreated(), result.getUpdated(), result.getSkipped());

        return Result.success(result);
    }

    // === 私有方法 ===

    private Map<String, Object> collectExportData(String groupId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exportTime", Instant.now().toString());
        result.put("version", "1.0");

        // 导出分组信息
        List<Map<String, Object>> groupsData = new ArrayList<>();
        List<ConfigGroup> groups;
        if (groupId != null) {
            groups = dataManager.findAllByField(ConfigGroup.class, ConfigGroup::getId, groupId)
                .stream().toList();
        } else {
            groups = dataManager.findAll(ConfigGroup.class);
        }

        Map<String, String> groupNames = new HashMap<>();
        for (ConfigGroup group : groups) {
            if (group.getStatus() != 1 || group.isDeleted()) {
                continue;
            }
            groupNames.put(group.getId(), group.getCode());

            Map<String, Object> groupData = new LinkedHashMap<>();
            groupData.put("code", group.getCode());
            groupData.put("name", group.getName());
            groupData.put("description", group.getDescription());
            groupData.put("icon", group.getIcon());
            groupData.put("sort", group.getSort());
            groupsData.add(groupData);
        }
        result.put("groups", groupsData);

        // 导出配置项和值
        List<Map<String, Object>> itemsData = new ArrayList<>();
        List<ConfigItem> items = dataManager.findAll(ConfigItem.class);

        for (ConfigItem item : items) {
            if (item.getStatus() != 1 || item.isDeleted()) {
                continue;
            }
            if (groupId != null && !item.getGroupId().equals(groupId)) {
                continue;
            }

            Map<String, Object> itemData = new LinkedHashMap<>();
            itemData.put("code", item.getCode());
            itemData.put("name", item.getName());
            itemData.put("description", item.getDescription());
            itemData.put("type", item.getType());
            itemData.put("defaultValue", item.getDefaultValue());
            itemData.put("options", item.getOptions());
            itemData.put("validation", item.getValidation());
            itemData.put("placeholder", item.getPlaceholder());
            itemData.put("isSecret", item.getIsSecret());
            itemData.put("isRequired", item.getIsRequired());
            itemData.put("isDynamic", item.getIsDynamic());
            itemData.put("sort", item.getSort());
            itemData.put("groupCode", groupNames.get(item.getGroupId()));

            // 获取配置值
            Optional<ConfigValue> valueOpt = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId());
            itemData.put("value", valueOpt.map(ConfigValue::getValue).orElse(null));

            itemsData.add(itemData);
        }
        result.put("items", itemsData);

        return result;
    }

    @SuppressWarnings("unchecked")
    private ImportResult applyImportData(Map<String, Object> importData, boolean mergeMode) {
        ImportResult result = new ImportResult();

        // 构建分组 code -> id 映射
        Map<String, String> groupCodeToId = new HashMap<>();
        List<ConfigGroup> existingGroups = dataManager.findAll(ConfigGroup.class);
        for (ConfigGroup group : existingGroups) {
            groupCodeToId.put(group.getCode(), group.getId());
        }

        // 导入分组
        List<Map<String, Object>> groupsData = (List<Map<String, Object>>) importData.get("groups");
        if (groupsData != null) {
            for (Map<String, Object> groupData : groupsData) {
                String code = (String) groupData.get("code");
                if (!groupCodeToId.containsKey(code)) {
                    ConfigGroup group = new ConfigGroup();
                    group.setCode(code);
                    group.setName((String) groupData.get("name"));
                    group.setDescription((String) groupData.get("description"));
                    group.setIcon((String) groupData.get("icon"));
                    group.setSort(((Number) groupData.getOrDefault("sort", 0)).intValue());
                    group.setStatus(1);

                    ConfigGroup saved = dataManager.save(ConfigGroup.class, group);
                    groupCodeToId.put(code, saved.getId());
                    result.incrementCreated();
                } else if (!mergeMode) {
                    // 更新现有分组
                    String id = groupCodeToId.get(code);
                    dataManager.findById(ConfigGroup.class, id).ifPresent(group -> {
                        group.setName((String) groupData.get("name"));
                        group.setDescription((String) groupData.get("description"));
                        dataManager.save(ConfigGroup.class, group);
                        result.incrementUpdated();
                    });
                }
            }
        }

        // 导入配置项
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) importData.get("items");
        if (itemsData != null) {
            for (Map<String, Object> itemData : itemsData) {
                String code = (String) itemData.get("code");
                String groupCode = (String) itemData.get("groupCode");
                String groupId = groupCodeToId.get(groupCode);

                if (groupId == null) {
                    result.incrementSkipped();
                    continue;
                }

                Optional<ConfigItem> existingItem = dataManager.findOneByField(ConfigItem.class, ConfigItem::getCode, code);

                if (existingItem.isEmpty()) {
                    // 创建新配置项
                    ConfigItem item = new ConfigItem();
                    item.setCode(code);
                    item.setName((String) itemData.get("name"));
                    item.setDescription((String) itemData.get("description"));
                    item.setType((String) itemData.getOrDefault("type", "STRING"));
                    item.setDefaultValue((String) itemData.get("defaultValue"));
                    item.setOptions((String) itemData.get("options"));
                    item.setValidation((String) itemData.get("validation"));
                    item.setPlaceholder((String) itemData.get("placeholder"));
                    item.setIsSecret((Boolean) itemData.getOrDefault("isSecret", false));
                    item.setIsRequired((Boolean) itemData.getOrDefault("isRequired", false));
                    item.setIsDynamic((Boolean) itemData.getOrDefault("isDynamic", true));
                    item.setSort(((Number) itemData.getOrDefault("sort", 0)).intValue());
                    item.setGroupId(groupId);
                    item.setStatus(1);

                    ConfigItem savedItem = dataManager.save(ConfigItem.class, item);

                    // 设置值
                    String value = (String) itemData.get("value");
                    if (value != null) {
                        ConfigValue configValue = new ConfigValue();
                        configValue.setItemId(savedItem.getId());
                        configValue.setValue(value);
                        configValue.setVersion(1);
                        dataManager.save(ConfigValue.class, configValue);
                    }

                    result.incrementCreated();
                } else if (!mergeMode) {
                    // 更新现有配置项
                    ConfigItem item = existingItem.get();
                    item.setName((String) itemData.get("name"));
                    item.setDescription((String) itemData.get("description"));
                    dataManager.save(ConfigItem.class, item);

                    // 更新值
                    String value = (String) itemData.get("value");
                    if (value != null) {
                        Optional<ConfigValue> valueOpt = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId());
                        ConfigValue configValue = valueOpt.orElseGet(() -> {
                            ConfigValue v = new ConfigValue();
                            v.setItemId(item.getId());
                            v.setVersion(0);
                            return v;
                        });
                        configValue.setValue(value);
                        configValue.setVersion(configValue.getVersion() + 1);
                        dataManager.save(ConfigValue.class, configValue);
                    }

                    result.incrementUpdated();
                } else {
                    result.incrementSkipped();
                }
            }
        }

        return result;
    }

    @Data
    public static class ImportResult {
        private int created = 0;
        private int updated = 0;
        private int skipped = 0;

        public void incrementCreated() { created++; }
        public void incrementUpdated() { updated++; }
        public void incrementSkipped() { skipped++; }
    }
}
