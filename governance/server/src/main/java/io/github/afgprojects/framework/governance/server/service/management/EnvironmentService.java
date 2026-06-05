package io.github.afgprojects.framework.governance.server.service.management;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import io.github.afgprojects.framework.governance.server.entity.management.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 环境服务类
 *
 * @author afg-projects
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final DataManager dataManager;

    /**
     * 查询所有未删除的环境，按排序字段排序
     *
     * @return 环境列表
     */
    public List<Environment> findAll() {
        return dataManager.entity(Environment.class)
                .query()
                .where(Conditions.builder(Environment.class)
                        .eq(Environment::isDeleted, false)
                        .build())
                .orderBy(Sort.asc("sort"))
                .list();
    }

    /**
     * 根据编码查询环境
     *
     * @param code 环境编码
     * @return 环境对象
     */
    public Environment findByCode(String code) {
        return dataManager.entity(Environment.class)
                .query()
                .where(Conditions.builder(Environment.class)
                        .eq(Environment::getCode, code)
                        .eq(Environment::isDeleted, false)
                        .build())
                .one()
                .orElse(null);
    }

    /**
     * 创建环境
     *
     * @param environment 环境对象
     * @return 创建后的环境
     */
    @Transactional
    public Environment create(Environment environment) {
        log.info("Creating environment: {}", environment.getCode());
        return dataManager.save(Environment.class, environment);
    }

    /**
     * 更新环境
     *
     * @param id          环境ID
     * @param environment 环境对象
     * @return 更新后的环境
     */
    @Transactional
    public Environment update(Long id, Environment environment) {
        log.info("Updating environment: {}", id);
        Environment existing = dataManager.findById(Environment.class, id)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + id));

        existing.setCode(environment.getCode());
        existing.setName(environment.getName());
        existing.setDescription(environment.getDescription());
        existing.setSort(environment.getSort());
        existing.setStatus(environment.getStatus());

        return dataManager.save(Environment.class, existing);
    }

    /**
     * 克隆配置从源环境到目标环境
     * <p>
     * 克隆所有配置组、配置项和配置值
     *
     * @param sourceEnvId 源环境ID
     * @param targetEnvId 目标环境ID
     */
    @Transactional
    public void cloneConfig(Long sourceEnvId, Long targetEnvId) {
        log.info("Cloning config from environment {} to {}", sourceEnvId, targetEnvId);

        // 1. 查询源环境的所有配置组
        List<ConfigGroup> sourceGroups = dataManager.entity(ConfigGroup.class)
                .query()
                .where(Conditions.builder(ConfigGroup.class)
                        .eq(ConfigGroup::getEnvironmentId, sourceEnvId)
                        .eq(ConfigGroup::isDeleted, false)
                        .build())
                .list();

        // 2. 克隆配置组，并记录新旧ID映射
        Map<Long, ConfigGroup> oldToNewGroupMap = sourceGroups.stream()
                .collect(Collectors.toMap(
                        ConfigGroup::getId,
                        sourceGroup -> {
                            ConfigGroup newGroup = new ConfigGroup();
                            newGroup.setEnvironmentId(targetEnvId);
                            newGroup.setCode(sourceGroup.getCode());
                            newGroup.setName(sourceGroup.getName());
                            newGroup.setDescription(sourceGroup.getDescription());
                            newGroup.setIcon(sourceGroup.getIcon());
                            newGroup.setSort(sourceGroup.getSort());
                            newGroup.setStatus(sourceGroup.getStatus());
                            return dataManager.save(ConfigGroup.class, newGroup);
                        }
                ));

        // 3. 查询所有配置项
        List<Long> sourceGroupIds = sourceGroups.stream()
                .map(ConfigGroup::getId)
                .toList();

        List<ConfigItem> sourceItems = dataManager.entity(ConfigItem.class)
                .query()
                .where(Conditions.builder(ConfigItem.class)
                        .in(ConfigItem::getGroupId, sourceGroupIds)
                        .eq(ConfigItem::isDeleted, false)
                        .build())
                .list();

        // 4. 克隆配置项，并记录新旧ID映射
        Map<Long, ConfigItem> oldToNewItemMap = sourceItems.stream()
                .collect(Collectors.toMap(
                        ConfigItem::getId,
                        sourceItem -> {
                            ConfigGroup newGroup = oldToNewGroupMap.get(sourceItem.getGroupId());
                            ConfigItem newItem = new ConfigItem();
                            newItem.setGroupId(newGroup.getId());
                            newItem.setCode(sourceItem.getCode());
                            newItem.setName(sourceItem.getName());
                            newItem.setDescription(sourceItem.getDescription());
                            newItem.setType(sourceItem.getType());
                            newItem.setDefaultValue(sourceItem.getDefaultValue());
                            newItem.setOptions(sourceItem.getOptions());
                            newItem.setValidation(sourceItem.getValidation());
                            newItem.setPlaceholder(sourceItem.getPlaceholder());
                            newItem.setIsSecret(sourceItem.getIsSecret());
                            newItem.setIsRequired(sourceItem.getIsRequired());
                            newItem.setIsDynamic(sourceItem.getIsDynamic());
                            newItem.setIsDeprecated(sourceItem.getIsDeprecated());
                            newItem.setSort(sourceItem.getSort());
                            newItem.setStatus(sourceItem.getStatus());
                            return dataManager.save(ConfigItem.class, newItem);
                        }
                ));

        // 5. 查询所有配置值
        List<Long> sourceItemIds = sourceItems.stream()
                .map(ConfigItem::getId)
                .toList();

        List<ConfigValue> sourceValues = dataManager.entity(ConfigValue.class)
                .query()
                .where(Conditions.builder(ConfigValue.class)
                        .in(ConfigValue::getItemId, sourceItemIds)
                        .build())
                .list();

        // 6. 克隆配置值
        sourceValues.forEach(sourceValue -> {
            ConfigItem newItem = oldToNewItemMap.get(sourceValue.getItemId());
            ConfigValue newValue = new ConfigValue();
            newValue.setEnvironmentId(targetEnvId);
            newValue.setItemId(newItem.getId());
            newValue.setValue(sourceValue.getValue());
            newValue.setValueType(sourceValue.getValueType());
            newValue.setVersion(1);
            newValue.setTenantId(sourceValue.getTenantId());
            dataManager.save(ConfigValue.class, newValue);
        });

        log.info("Cloned {} config groups, {} config items, {} config values",
                oldToNewGroupMap.size(), oldToNewItemMap.size(), sourceValues.size());
    }
}
