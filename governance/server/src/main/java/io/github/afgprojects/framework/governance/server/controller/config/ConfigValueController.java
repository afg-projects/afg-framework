package io.github.afgprojects.framework.governance.server.controller.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.proto.ChangeType;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigHistory;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import io.github.afgprojects.framework.governance.server.grpc.ConfigStreamManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/console/config/values")
@RequiredArgsConstructor
public class ConfigValueController {

    private final DataManager dataManager;
    private final ConfigStreamManager streamManager;

    @GetMapping
    public Result<ConfigValue> getByItemId(@RequestParam String itemId) {
        return Result.success(dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, itemId).orElse(null));
    }

    @GetMapping("/code/{code}")
    public Result<String> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ConfigItem.class, ConfigItem::getCode, code)
            .flatMap(item -> dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId()))
            .map(ConfigValue::getValue)
            .map(Result::success)
            .orElse(Result.fail(404, "Config value not found for code: " + code));
    }

    @GetMapping("/prefix/{prefix}")
    public Result<Map<String, String>> getByPrefix(@PathVariable String prefix) {
        Map<String, String> result = new HashMap<>();
        List<ConfigItem> items = dataManager.entity(ConfigItem.class)
            .query()
            .where(Conditions.builder(ConfigItem.class)
                .eq(ConfigItem::isDeleted, false)
                .build())
            .list();

        for (ConfigItem item : items) {
            if (item.getCode().startsWith(prefix)) {
                dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, item.getId())
                    .ifPresent(v -> result.put(item.getCode(), v.getValue()));
            }
        }

        return Result.success(result);
    }

    @PutMapping("/{itemId}")
    @Transactional
    public Result<ConfigValue> update(@PathVariable String itemId, @RequestBody UpdateValueRequest request) {
        ConfigItem item = dataManager.findById(ConfigItem.class, itemId)
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config item not found: " + itemId));

        ConfigValue value = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, itemId)
            .orElseGet(() -> {
                ConfigValue v = new ConfigValue();
                v.setItemId(itemId);
                v.setVersion(0);
                return v;
            });

        String oldValue = value.getValue();

        // Record history
        ConfigHistory history = new ConfigHistory();
        history.setItemId(itemId);
        history.setOldValue(oldValue);
        history.setNewValue(request.getValue());
        history.setChangeType(value.getId() == null ? "CREATE" : "UPDATE");
        history.setReason(request.getReason());
        history.setOperatorName(request.getOperator());
        dataManager.save(ConfigHistory.class, history);

        // Update value
        value.setValue(request.getValue());
        value.setVersion(value.getVersion() + 1);
        value.setValueType(item.getType());

        ConfigValue saved = dataManager.save(ConfigValue.class, value);

        // Push config change via gRPC stream
        ChangeType changeType = value.getId() == null ? ChangeType.CHANGE_TYPE_CREATE : ChangeType.CHANGE_TYPE_UPDATE;
        streamManager.pushConfigChange(item.getCode(), request.getValue(), changeType);
        log.info("Config updated and pushed: key={}, value={}", item.getCode(), request.getValue());

        return Result.success(saved);
    }

    @PostMapping("/batch")
    @Transactional
    public Result<Void> batchUpdate(@RequestBody List<UpdateValueRequest> requests) {
        for (UpdateValueRequest request : requests) {
            update(request.getItemId(), request);
        }
        return Result.success(null);
    }

    @DeleteMapping("/{itemId}")
    @Transactional
    public Result<Void> delete(@PathVariable String itemId) {
        ConfigItem item = dataManager.findById(ConfigItem.class, itemId)
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config item not found: " + itemId));

        ConfigValue value = dataManager.findOneByField(ConfigValue.class, ConfigValue::getItemId, itemId)
            .orElse(null);

        if (value != null) {
            // Record history
            ConfigHistory history = new ConfigHistory();
            history.setItemId(itemId);
            history.setOldValue(value.getValue());
            history.setNewValue(null);
            history.setChangeType("DELETE");
            dataManager.save(ConfigHistory.class, history);

            dataManager.deleteById(ConfigValue.class, value.getId());
        }

        return Result.success(null);
    }

    @Data
    public static class UpdateValueRequest {
        private String itemId;
        private String value;
        private String reason;
        private String operator;
    }
}
