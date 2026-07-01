package io.github.afgprojects.framework.governance.server.controller.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.dto.config.ConfigItemResponse;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/console/config/items")
@RequiredArgsConstructor
public class ConfigItemController {

    private final DataManager dataManager;

    @GetMapping
    public Result<List<ConfigItemResponse>> list(@RequestParam(required = false) String groupId) {
        List<ConfigItem> items;
        if (groupId != null) {
            items = dataManager.entity(ConfigItem.class)
                .query()
                .where(Conditions.builder(ConfigItem.class)
                    .eq(ConfigItem::getGroupId, groupId)
                    .eq(ConfigItem::getStatus, 1)
                    .eq(ConfigItem::isDeleted, false)
                    .build())
                .list();
        } else {
            items = dataManager.entity(ConfigItem.class)
                .query()
                .where(Conditions.builder(ConfigItem.class)
                    .eq(ConfigItem::isDeleted, false)
                    .build())
                .list();
        }

        // 批量获取当前值
        Map<String, String> valueMap = fetchCurrentValues(items);

        return Result.success(items.stream()
            .map(item -> ConfigItemResponse.fromEntity(item, valueMap.get(item.getId())))
            .collect(Collectors.toList()));
    }

    /**
     * 批量获取配置项的当前值
     */
    private Map<String, String> fetchCurrentValues(List<ConfigItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }

        List<String> itemIds = items.stream()
            .map(ConfigItem::getId)
            .toList();

        // 逐个查询，避免 IN 条件问题
        Map<String, String> result = new java.util.HashMap<>();
        for (String itemId : itemIds) {
            dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, itemId)
                .ifPresent(v -> result.put(itemId, v.getValue()));
        }

        return result;
    }

    @GetMapping("/{id}")
    public Result<ConfigItemResponse> get(@PathVariable String id) {
        return dataManager.findById(ConfigItem.class, id)
            .filter(i -> !i.isDeleted())
            .map(item -> {
                String currentValue = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId())
                    .map(ConfigValue::getValue)
                    .orElse(null);
                return Result.success(ConfigItemResponse.fromEntity(item, currentValue));
            })
            .orElse(Result.fail(404, "Config item not found: " + id));
    }

    @GetMapping("/code/{code}")
    public Result<ConfigItemResponse> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ConfigItem.class, ConfigItem::getCode, code)
            .filter(i -> !i.isDeleted())
            .map(item -> {
                String currentValue = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId())
                    .map(ConfigValue::getValue)
                    .orElse(null);
                return Result.success(ConfigItemResponse.fromEntity(item, currentValue));
            })
            .orElse(Result.fail(404, "Config item not found by code: " + code));
    }

    @PostMapping
    public Result<ConfigItem> create(@RequestBody ConfigItem item) {
        return dataManager.executeInTransaction(() -> {
            // Check if code already exists in the group
            var existing = dataManager.entity(ConfigItem.class)
                .query()
                .where(Conditions.builder(ConfigItem.class)
                    .eq(ConfigItem::getGroupId, item.getGroupId())
                    .eq(ConfigItem::getCode, item.getCode())
                    .build())
                .one();
            if (existing.isPresent()) {
                throw new BusinessException(CommonErrorCode.ENTITY_ALREADY_EXISTS, "Config item code already exists in this group: " + item.getCode());
            }
            return Result.success(dataManager.save(ConfigItem.class, item));
        });
    }

    @PutMapping("/{id}")
    public Result<ConfigItem> update(@PathVariable String id, @RequestBody ConfigItem item) {
        return dataManager.executeInTransaction(() -> {
            ConfigItem existing = dataManager.findById(ConfigItem.class, id)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config item not found: " + id));

            existing.setName(item.getName());
            existing.setDescription(item.getDescription());
            existing.setType(item.getType());
            existing.setDefaultValue(item.getDefaultValue());
            existing.setOptions(item.getOptions());
            existing.setValidation(item.getValidation());
            existing.setPlaceholder(item.getPlaceholder());
            existing.setIsSecret(item.getIsSecret());
            existing.setIsRequired(item.getIsRequired());
            existing.setIsDynamic(item.getIsDynamic());
            existing.setIsDeprecated(item.getIsDeprecated());
            existing.setSort(item.getSort());
            existing.setStatus(item.getStatus());

            return Result.success(dataManager.save(ConfigItem.class, existing));
        });
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        return dataManager.executeInTransaction(() -> {
            ConfigItem item = dataManager.findById(ConfigItem.class, id)
                .filter(i -> !i.isDeleted())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config item not found: " + id));
            item.markDeleted();
            dataManager.save(ConfigItem.class, item);
            return Result.<Void>success();
        });
    }
}
