package io.github.afgprojects.framework.governance.server.service.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigItemService {

    private final DataManager dataManager;

    public List<ConfigItem> findByGroupId(Long groupId) {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigItem.class, ConfigItem::getGroupId, groupId),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        return dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .orderBy(Sort.asc("sort"))
            .list();
    }

    public List<ConfigItem> findActiveByGroupId(Long groupId) {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigItem.class, ConfigItem::getGroupId, groupId),
            Conditions.eq(ConfigItem.class, ConfigItem::getStatus, 1),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        return dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .orderBy(Sort.asc("sort"))
            .list();
    }

    public List<ConfigItem> findAll() {
        return dataManager.findAll(ConfigItem.class);
    }

    public List<ConfigItem> findDynamicItems() {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigItem.class, ConfigItem::getIsDynamic, true),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        return dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .list();
    }

    public Optional<ConfigItem> findById(Long id) {
        return dataManager.findById(ConfigItem.class, id);
    }

    public Optional<ConfigItem> findByCode(String code) {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigItem.class, ConfigItem::getCode, code),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        return dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .one();
    }

    public List<ConfigItem> findByPrefix(String prefix) {
        var condition = Conditions.allOf(
            Conditions.like(ConfigItem.class, ConfigItem::getCode, prefix + "%"),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        return dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .list();
    }

    @Transactional
    public ConfigItem create(ConfigItem item) {
        // 检查编码是否已存在
        var condition = Conditions.allOf(
            Conditions.eq(ConfigItem.class, ConfigItem::getGroupId, item.getGroupId()),
            Conditions.eq(ConfigItem.class, ConfigItem::getCode, item.getCode()),
            Conditions.eq(ConfigItem.class, ConfigItem::isDeleted, false)
        );
        Optional<ConfigItem> existing = dataManager.entity(ConfigItem.class)
            .query()
            .where(condition)
            .one();

        if (existing.isPresent()) {
            throw new BusinessException(CommonErrorCode.ENTITY_ALREADY_EXISTS, "配置项编码在此分组中已存在: " + item.getCode());
        }

        return dataManager.save(ConfigItem.class, item);
    }

    @Transactional
    public ConfigItem update(Long id, ConfigItem item) {
        ConfigItem existing = dataManager.findById(ConfigItem.class, id)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "配置项不存在: " + id));

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

        return dataManager.save(ConfigItem.class, existing);
    }

    @Transactional
    public void delete(Long id) {
        ConfigItem item = dataManager.findById(ConfigItem.class, id)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "配置项不存在: " + id));
        item.markDeleted();
        dataManager.save(ConfigItem.class, item);
    }
}
