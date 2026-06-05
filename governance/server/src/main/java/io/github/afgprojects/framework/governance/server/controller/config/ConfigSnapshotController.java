package io.github.afgprojects.framework.governance.server.controller.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.governance.server.dto.config.ConfigDiffDTO;
import io.github.afgprojects.framework.governance.server.dto.config.ConfigSnapshotDTO;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigSnapshot;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/console/config/snapshots")
@RequiredArgsConstructor
public class ConfigSnapshotController {

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;

    @GetMapping
    public List<ConfigSnapshotDTO> list(@RequestParam(required = false) Long groupId) {
        List<ConfigSnapshot> snapshots;
        if (groupId != null) {
            snapshots = dataManager.findAllByField(ConfigSnapshot.class, ConfigSnapshot::getGroupId, groupId);
        } else {
            snapshots = dataManager.findAll(ConfigSnapshot.class);
        }

        return snapshots.stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfigSnapshotDTO> get(@PathVariable Long id) {
        return dataManager.findById(ConfigSnapshot.class, id)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ConfigSnapshotDTO create(@RequestBody CreateSnapshotRequest request) {
        // 收集配置数据
        Map<String, Object> configData = collectConfigData(request.getGroupId());

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(configData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config data", e);
        }

        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setName(request.getName());
        snapshot.setDescription(request.getDescription());
        snapshot.setTag(request.getTag());
        snapshot.setData(jsonData);
        snapshot.setGroupId(request.getGroupId());
        snapshot.setCreatorId(request.getCreatorId());
        snapshot.setCreatorName(request.getCreatorName());

        ConfigSnapshot saved = dataManager.save(ConfigSnapshot.class, snapshot);
        log.info("Created config snapshot: id={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<Void> rollback(@PathVariable Long id, @RequestBody(required = false) RollbackRequest request) {
        ConfigSnapshot snapshot = dataManager.findById(ConfigSnapshot.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + id));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> configData = objectMapper.readValue(snapshot.getData(), Map.class);

            // 应用配置
            applyConfigData(configData, request != null ? request.getReason() : "Rollback to snapshot: " + snapshot.getName());

            log.info("Rolled back to snapshot: id={}, name={}", id, snapshot.getName());
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse snapshot data", e);
        }
    }

    @GetMapping("/compare/{id1}/{id2}")
    public ConfigDiffDTO compare(@PathVariable Long id1, @PathVariable Long id2) {
        ConfigSnapshot snapshot1 = dataManager.findById(ConfigSnapshot.class, id1)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + id1));
        ConfigSnapshot snapshot2 = dataManager.findById(ConfigSnapshot.class, id2)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + id2));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data1 = objectMapper.readValue(snapshot1.getData(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> data2 = objectMapper.readValue(snapshot2.getData(), Map.class);

            return calculateDiff(snapshot1, snapshot2, data1, data2);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse snapshot data", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dataManager.deleteById(ConfigSnapshot.class, id);
        log.info("Deleted config snapshot: id={}", id);
        return ResponseEntity.ok().build();
    }

    // === 私有方法 ===

    private Map<String, Object> collectConfigData(Long groupId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<Long, String> groupNames = new HashMap<>();

        // 获取所有分组名称
        List<ConfigGroup> groups = dataManager.findAll(ConfigGroup.class);
        for (ConfigGroup group : groups) {
            groupNames.put(group.getId(), group.getName());
        }

        // 获取配置项和值
        List<ConfigItem> items;
        if (groupId != null) {
            items = dataManager.findAllByField(ConfigItem.class, ConfigItem::getGroupId, groupId);
        } else {
            items = dataManager.findAll(ConfigItem.class);
        }

        for (ConfigItem item : items) {
            if (item.getStatus() != 1 || item.isDeleted()) {
                continue;
            }

            Map<String, Object> itemData = new LinkedHashMap<>();
            itemData.put("name", item.getName());
            itemData.put("type", item.getType());
            itemData.put("defaultValue", item.getDefaultValue());
            itemData.put("groupId", item.getGroupId());
            itemData.put("groupName", groupNames.get(item.getGroupId()));

            // 获取配置值
            Optional<ConfigValue> valueOpt = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId());
            itemData.put("value", valueOpt.map(ConfigValue::getValue).orElse(item.getDefaultValue()));

            result.put(item.getCode(), itemData);
        }

        return result;
    }

    private void applyConfigData(Map<String, Object> configData, String reason) {
        for (Map.Entry<String, Object> entry : configData.entrySet()) {
            String code = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> itemData = (Map<String, Object>) entry.getValue();
            String value = (String) itemData.get("value");

            // 查找配置项
            Optional<ConfigItem> itemOpt = dataManager.findOneByField(ConfigItem.class, ConfigItem::getCode, code);
            if (itemOpt.isPresent()) {
                ConfigItem item = itemOpt.get();

                // 更新或创建配置值
                Optional<ConfigValue> valueOpt = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId());
                ConfigValue configValue = valueOpt.orElseGet(() -> {
                    ConfigValue v = new ConfigValue();
                    v.setItemId(item.getId());
                    v.setVersion(0);
                    return v;
                });

                String oldValue = configValue.getValue();
                configValue.setValue(value);
                configValue.setVersion(configValue.getVersion() + 1);
                dataManager.save(ConfigValue.class, configValue);

                // 记录历史
                ConfigHistory history = new ConfigHistory();
                history.setItemId(item.getId());
                history.setOldValue(oldValue);
                history.setNewValue(value);
                history.setChangeType("UPDATE");
                history.setReason(reason);
                dataManager.save(ConfigHistory.class, history);
            }
        }
    }

    private ConfigDiffDTO calculateDiff(ConfigSnapshot s1, ConfigSnapshot s2,
                                         Map<String, Object> data1, Map<String, Object> data2) {
        ConfigDiffDTO diff = new ConfigDiffDTO();
        diff.setSnapshot1Id(s1.getId());
        diff.setSnapshot2Id(s2.getId());
        diff.setSnapshot1Name(s1.getName());
        diff.setSnapshot2Name(s2.getName());

        List<ConfigDiffDTO.ConfigDiffItem> added = new ArrayList<>();
        List<ConfigDiffDTO.ConfigDiffItem> modified = new ArrayList<>();
        List<ConfigDiffDTO.ConfigDiffItem> deleted = new ArrayList<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(data1.keySet());
        allKeys.addAll(data2.keySet());

        for (String key : allKeys) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item1 = (Map<String, Object>) data1.get(key);
            @SuppressWarnings("unchecked")
            Map<String, Object> item2 = (Map<String, Object>) data2.get(key);

            ConfigDiffDTO.ConfigDiffItem diffItem = new ConfigDiffDTO.ConfigDiffItem();
            diffItem.setKey(key);

            if (item1 == null && item2 != null) {
                // 新增
                diffItem.setItemName((String) item2.get("name"));
                diffItem.setNewValue((String) item2.get("value"));
                added.add(diffItem);
            } else if (item1 != null && item2 == null) {
                // 删除
                diffItem.setItemName((String) item1.get("name"));
                diffItem.setOldValue((String) item1.get("value"));
                deleted.add(diffItem);
            } else if (item1 != null && item2 != null) {
                String value1 = (String) item1.get("value");
                String value2 = (String) item2.get("value");
                if (!Objects.equals(value1, value2)) {
                    // 修改
                    diffItem.setItemName((String) item1.get("name"));
                    diffItem.setOldValue(value1);
                    diffItem.setNewValue(value2);
                    modified.add(diffItem);
                }
            }
        }

        diff.setAdded(added);
        diff.setModified(modified);
        diff.setDeleted(deleted);

        return diff;
    }

    private ConfigSnapshotDTO toDTO(ConfigSnapshot snapshot) {
        ConfigSnapshotDTO dto = new ConfigSnapshotDTO();
        dto.setId(snapshot.getId());
        dto.setName(snapshot.getName());
        dto.setDescription(snapshot.getDescription());
        dto.setTag(snapshot.getTag());
        dto.setData(snapshot.getData());
        dto.setGroupId(snapshot.getGroupId());
        dto.setCreatorId(snapshot.getCreatorId());
        dto.setCreatorName(snapshot.getCreatorName());
        dto.setCreatedAt(snapshot.getCreatedAt());

        // 获取分组名称
        if (snapshot.getGroupId() != null) {
            dataManager.findById(ConfigGroup.class, snapshot.getGroupId())
                .ifPresent(group -> dto.setGroupName(group.getName()));
        }

        return dto;
    }

    // === 请求对象 ===

    @lombok.Data
    public static class CreateSnapshotRequest {
        private String name;
        private String description;
        private String tag;
        private Long groupId;
        private Long creatorId;
        private String creatorName;
    }

    @lombok.Data
    public static class RollbackRequest {
        private String reason;
    }
}
