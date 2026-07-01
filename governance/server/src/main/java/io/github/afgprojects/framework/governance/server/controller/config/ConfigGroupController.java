package io.github.afgprojects.framework.governance.server.controller.config;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/console/config/groups")
@RequiredArgsConstructor
public class ConfigGroupController {

    private final DataManager dataManager;

    @GetMapping
    public Result<List<ConfigGroup>> list() {
        return Result.success(dataManager.entity(ConfigGroup.class)
            .query()
            .where(Conditions.builder(ConfigGroup.class)
                .eq(ConfigGroup::getStatus, 1)
                .eq(ConfigGroup::isDeleted, false)
                .build())
            .list());
    }

    @GetMapping("/{id}")
    public Result<ConfigGroup> get(@PathVariable String id) {
        return dataManager.findById(ConfigGroup.class, id)
            .filter(g -> !g.isDeleted())
            .map(Result::success)
            .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
    }

    @GetMapping("/code/{code}")
    public Result<ConfigGroup> getByCode(@PathVariable String code) {
        return dataManager.findOneByField(ConfigGroup.class, ConfigGroup::getCode, code)
            .filter(g -> !g.isDeleted())
            .map(Result::success)
            .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
    }

    @PostMapping
    public Result<ConfigGroup> create(@RequestBody ConfigGroup group) {
        return dataManager.executeInTransaction(() -> {
            // Check if code already exists
            if (dataManager.findOneByField(ConfigGroup.class, ConfigGroup::getCode, group.getCode()).isPresent()) {
                throw new BusinessException(CommonErrorCode.ENTITY_ALREADY_EXISTS, "Config group code already exists: " + group.getCode());
            }
            return Result.success(dataManager.save(ConfigGroup.class, group));
        });
    }

    @PutMapping("/{id}")
    public Result<ConfigGroup> update(@PathVariable String id, @RequestBody ConfigGroup group) {
        return dataManager.executeInTransaction(() -> {
            ConfigGroup existing = dataManager.findById(ConfigGroup.class, id)
                .filter(g -> !g.isDeleted())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config group not found: " + id));

            existing.setName(group.getName());
            existing.setDescription(group.getDescription());
            existing.setIcon(group.getIcon());
            existing.setSort(group.getSort());
            existing.setStatus(group.getStatus());

            return Result.success(dataManager.save(ConfigGroup.class, existing));
        });
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        return dataManager.executeInTransaction(() -> {
            ConfigGroup group = dataManager.findById(ConfigGroup.class, id)
                .filter(g -> !g.isDeleted())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Config group not found: " + id));
            group.markDeleted();
            dataManager.save(ConfigGroup.class, group);
            return Result.<Void>success();
        });
    }
}
