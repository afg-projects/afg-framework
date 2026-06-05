package io.github.afgprojects.framework.governance.server.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigSnapshotService {

    private final DataManager dataManager;
    private final ConfigItemService itemService;
    private final ConfigValueService valueService;
    private final ObjectMapper objectMapper;

    public List<ConfigSnapshot> findAll() {
        return dataManager.findAll(ConfigSnapshot.class)
            .stream()
            .sorted(Comparator.comparing(ConfigSnapshot::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public List<ConfigSnapshot> findByGroupId(Long groupId) {
        var condition = Conditions.eq(ConfigSnapshot.class, ConfigSnapshot::getGroupId, groupId);
        return dataManager.entity(ConfigSnapshot.class)
            .query()
            .where(condition)
            .list()
            .stream()
            .sorted(Comparator.comparing(ConfigSnapshot::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public Optional<ConfigSnapshot> findById(Long id) {
        return dataManager.findById(ConfigSnapshot.class, id);
    }

    public Optional<ConfigSnapshot> findLatest() {
        return findAll().stream().findFirst();
    }

    public Optional<ConfigSnapshot> findLatestByGroupId(Long groupId) {
        return findByGroupId(groupId).stream().findFirst();
    }

    @Transactional
    public ConfigSnapshot createSnapshot(String name, String description, String tag, Long groupId,
                                         String creatorName) {
        // 收集配置数据
        Map<String, String> configData = new HashMap<>();

        List<ConfigItem> items;
        if (groupId != null) {
            items = itemService.findByGroupId(groupId);
        } else {
            items = itemService.findAll().stream()
                .filter(item -> !item.isDeleted())
                .collect(Collectors.toList());
        }

        for (ConfigItem item : items) {
            valueService.findByItemId(item.getId())
                .ifPresent(v -> configData.put(item.getCode(), v.getValue()));
        }

        String dataJson;
        try {
            dataJson = objectMapper.writeValueAsString(configData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化配置数据失败", e);
        }

        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.setName(name);
        snapshot.setDescription(description);
        snapshot.setTag(tag);
        snapshot.setData(dataJson);
        snapshot.setGroupId(groupId);
        snapshot.setCreatorName(creatorName);

        return dataManager.save(ConfigSnapshot.class, snapshot);
    }

    @Transactional
    public void rollbackToSnapshot(Long snapshotId, String operatorName) {
        ConfigSnapshot snapshot = dataManager.findById(ConfigSnapshot.class, snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId));

        Map<String, String> configData;
        try {
            configData = objectMapper.readValue(snapshot.getData(),
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化配置数据失败", e);
        }

        // 恢复配置值
        for (Map.Entry<String, String> entry : configData.entrySet()) {
            itemService.findByCode(entry.getKey()).ifPresent(item -> {
                valueService.updateValue(item.getId(), entry.getValue(),
                    "回滚到快照: " + snapshot.getName(), operatorName);
            });
        }
    }

    public Map<String, String> compareSnapshots(Long snapshotId1, Long snapshotId2) {
        ConfigSnapshot snapshot1 = dataManager.findById(ConfigSnapshot.class, snapshotId1)
            .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId1));
        ConfigSnapshot snapshot2 = dataManager.findById(ConfigSnapshot.class, snapshotId2)
            .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId2));

        Map<String, String> data1 = parseSnapshotData(snapshot1);
        Map<String, String> data2 = parseSnapshotData(snapshot2);

        Map<String, String> diff = new HashMap<>();

        // 找出新增的配置
        for (String key : data2.keySet()) {
            if (!data1.containsKey(key)) {
                diff.put(key + "_added", data2.get(key));
            }
        }

        // 找出删除的配置
        for (String key : data1.keySet()) {
            if (!data2.containsKey(key)) {
                diff.put(key + "_removed", data1.get(key));
            }
        }

        // 找出修改的配置
        for (String key : data1.keySet()) {
            if (data2.containsKey(key) && !data1.get(key).equals(data2.get(key))) {
                diff.put(key + "_changed", data1.get(key) + " -> " + data2.get(key));
            }
        }

        return diff;
    }

    private Map<String, String> parseSnapshotData(ConfigSnapshot snapshot) {
        try {
            return objectMapper.readValue(snapshot.getData(),
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化配置数据失败", e);
        }
    }
}
