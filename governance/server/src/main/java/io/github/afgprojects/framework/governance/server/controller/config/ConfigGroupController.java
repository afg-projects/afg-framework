package io.github.afgprojects.framework.governance.server.controller.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/console/config/groups")
@RequiredArgsConstructor
public class ConfigGroupController {

    private final DataManager dataManager;

    @GetMapping
    public List<ConfigGroup> list() {
        return dataManager.entity(ConfigGroup.class)
            .query()
            .where(Conditions.builder(ConfigGroup.class)
                .eq(ConfigGroup::getStatus, 1)
                .eq(ConfigGroup::isDeleted, false)
                .build())
            .list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfigGroup> get(@PathVariable Long id) {
        return dataManager.findById(ConfigGroup.class, id)
            .filter(g -> !g.isDeleted())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ConfigGroup> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ConfigGroup.class, ConfigGroup::getCode, code)
            .filter(g -> !g.isDeleted())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ConfigGroup create(@RequestBody ConfigGroup group) {
        // Check if code already exists
        if (dataManager.findOneByField(ConfigGroup.class, ConfigGroup::getCode, group.getCode()).isPresent()) {
            throw new BusinessException(CommonErrorCode.ENTITY_ALREADY_EXISTS, "Config group code already exists: " + group.getCode());
        }
                return dataManager.save(ConfigGroup.class, group);
    }

    @PutMapping("/{id}")
    @Transactional
    public ConfigGroup update(@PathVariable Long id, @RequestBody ConfigGroup group) {
        ConfigGroup existing = dataManager.findById(ConfigGroup.class, id)
            .filter(g -> !g.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config group not found: " + id));

        existing.setName(group.getName());
        existing.setDescription(group.getDescription());
        existing.setIcon(group.getIcon());
        existing.setSort(group.getSort());
        existing.setStatus(group.getStatus());

        return dataManager.save(ConfigGroup.class, existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ConfigGroup group = dataManager.findById(ConfigGroup.class, id)
            .filter(g -> !g.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config group not found: " + id));
        group.markDeleted();
        dataManager.save(ConfigGroup.class, group);
        return ResponseEntity.ok().build();
    }
}
