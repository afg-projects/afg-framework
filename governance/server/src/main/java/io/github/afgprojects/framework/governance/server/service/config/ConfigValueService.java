package io.github.afgprojects.framework.governance.server.service.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigHistory;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
public class ConfigValueService {

    private final DataManager dataManager;
    private final ConfigItemService itemService;
    private final ConfigHistoryService historyService;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<ConfigValue> findByItemId(String itemId) {
        var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, itemId);
        return dataManager.entity(ConfigValue.class)
            .query()
            .where(condition)
            .one();
    }

    public Optional<String> getValueByCode(String code) {
        return itemService.findByCode(code)
            .flatMap(item -> {
                var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, item.getId());
                return dataManager.entity(ConfigValue.class)
                    .query()
                    .where(condition)
                    .one();
            })
            .map(ConfigValue::getValue);
    }

    public Map<String, String> getValuesByPrefix(String prefix) {
        Map<String, String> result = new HashMap<>();
        List<ConfigItem> items = itemService.findByPrefix(prefix);

        for (ConfigItem item : items) {
            var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, item.getId());
            dataManager.entity(ConfigValue.class)
                .query()
                .where(condition)
                .one()
                .ifPresent(v -> result.put(item.getCode(), v.getValue()));
        }

        return result;
    }

    public Map<String, String> getAllValues() {
        Map<String, String> result = new HashMap<>();
        List<ConfigItem> items = itemService.findAll();

        for (ConfigItem item : items) {
            if (!item.isDeleted()) {
                var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, item.getId());
                dataManager.entity(ConfigValue.class)
                    .query()
                    .where(condition)
                    .one()
                    .ifPresent(v -> result.put(item.getCode(), v.getValue()));
            }
        }

        return result;
    }

    @Transactional
    public ConfigValue updateValue(String itemId, String newValue, String reason, String operatorName) {
        ConfigItem item = itemService.findById(itemId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "配置项不存在: " + itemId));

        var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, itemId);
        ConfigValue value = dataManager.entity(ConfigValue.class)
            .query()
            .where(condition)
            .one()
            .orElseGet(() -> {
                ConfigValue v = new ConfigValue();
                v.setItemId(itemId);
                v.setVersion(0);
                return v;
            });

        String oldValue = value.getValue();

        // 记录历史
        ConfigHistory history = new ConfigHistory();
        history.setItemId(itemId);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangeType(value.getId() == null ? "CREATE" : "UPDATE");
        history.setReason(reason);
        history.setOperatorName(operatorName);
        historyService.create(history);

        // 更新值
        value.setValue(newValue);
        value.setVersion(value.getVersion() + 1);
        value.setValueType(item.getType());

        ConfigValue saved = dataManager.save(ConfigValue.class, value);

        // 发布配置变更事件
        eventPublisher.publishEvent(new ConfigChangedEvent(item.getCode(), newValue));

        return saved;
    }

    @Transactional
    public void deleteByItemId(String itemId) {
        ConfigItem item = itemService.findById(itemId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "配置项不存在: " + itemId));

        var condition = Conditions.eq(ConfigValue.class, ConfigValue::getItemId, itemId);
        ConfigValue value = dataManager.entity(ConfigValue.class)
            .query()
            .where(condition)
            .one()
            .orElse(null);

        if (value != null) {
            // 记录历史
            ConfigHistory history = new ConfigHistory();
            history.setItemId(itemId);
            history.setOldValue(value.getValue());
            history.setNewValue(null);
            history.setChangeType("DELETE");
            historyService.create(history);

            dataManager.deleteById(ConfigValue.class, value.getId());
        }
    }

    /**
     * 配置变更事件
     */
    public record ConfigChangedEvent(String key, String value) {}
}
