package io.github.afgprojects.framework.governance.server.service.config;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigGroupService {

    private final DataManager dataManager;

    public List<ConfigGroup> findAll() {
        var condition = Conditions.eq(ConfigGroup.class, ConfigGroup::isDeleted, false);
        return dataManager.entity(ConfigGroup.class)
            .query()
            .where(condition)
            .orderBy(Sort.asc("sort"))
            .list();
    }

    public List<ConfigGroup> findActive() {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigGroup.class, ConfigGroup::getStatus, 1),
            Conditions.eq(ConfigGroup.class, ConfigGroup::isDeleted, false)
        );
        return dataManager.entity(ConfigGroup.class)
            .query()
            .where(condition)
            .orderBy(Sort.asc("sort"))
            .list();
    }

    public Optional<ConfigGroup> findById(Long id) {
        return dataManager.findById(ConfigGroup.class, id);
    }

    public Optional<ConfigGroup> findByCode(String code) {
        var condition = Conditions.allOf(
            Conditions.eq(ConfigGroup.class, ConfigGroup::getCode, code),
            Conditions.eq(ConfigGroup.class, ConfigGroup::isDeleted, false)
        );
        return dataManager.entity(ConfigGroup.class)
            .query()
            .where(condition)
            .one();
    }

    @Transactional
    public ConfigGroup create(ConfigGroup group) {
        // 检查编码是否已存在
        var condition = Conditions.allOf(
            Conditions.eq(ConfigGroup.class, ConfigGroup::getCode, group.getCode()),
            Conditions.eq(ConfigGroup.class, ConfigGroup::isDeleted, false)
        );
        Optional<ConfigGroup> existing = dataManager.entity(ConfigGroup.class)
            .query()
            .where(condition)
            .one();

        if (existing.isPresent()) {
            throw new IllegalArgumentException("配置分组编码已存在: " + group.getCode());
        }

        return dataManager.save(ConfigGroup.class, group);
    }

    @Transactional
    public ConfigGroup update(Long id, ConfigGroup group) {
        ConfigGroup existing = dataManager.findById(ConfigGroup.class, id)
            .orElseThrow(() -> new IllegalArgumentException("配置分组不存在: " + id));

        existing.setName(group.getName());
        existing.setDescription(group.getDescription());
        existing.setIcon(group.getIcon());
        existing.setSort(group.getSort());
        existing.setStatus(group.getStatus());

        return dataManager.save(ConfigGroup.class, existing);
    }

    @Transactional
    public void delete(Long id) {
        ConfigGroup group = dataManager.findById(ConfigGroup.class, id)
            .orElseThrow(() -> new IllegalArgumentException("配置分组不存在: " + id));
        group.markDeleted();
        dataManager.save(ConfigGroup.class, group);
    }
}
