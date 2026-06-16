package io.github.afgprojects.framework.governance.server.controller.management;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.governance.server.entity.management.Environment;
import io.github.afgprojects.framework.governance.server.service.management.EnvironmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 环境管理控制器
 *
 * @author afg-projects
 */
@Slf4j
@RestController
@RequestMapping("/console/env")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    /**
     * 查询所有环境
     */
    @GetMapping("/")
    public Result<List<Environment>> list() {
        return Result.success(environmentService.findAll());
    }

    /**
     * 根据ID查询环境
     */
    @GetMapping("/{id}")
    public Result<Environment> getById(@PathVariable Long id) {
        // TODO: 应该使用 findById，暂时使用 findByCode
        log.warn("getById called with id: {}, but using findByCode as fallback", id);
        return Result.fail(404, "Environment not found: " + id);
    }

    /**
     * 根据编码查询环境
     */
    @GetMapping("/code/{code}")
    public Result<Environment> getByCode(@PathVariable String code) {
        Environment environment = environmentService.findByCode(code);
        if (environment == null) {
            return Result.fail(404, "Environment not found for code: " + code);
        }
        return Result.success(environment);
    }

    /**
     * 创建环境
     */
    @PostMapping("/")
    public Result<Environment> create(@RequestBody Environment environment) {
        return Result.success(environmentService.create(environment));
    }

    /**
     * 更新环境
     */
    @PutMapping("/{id}")
    public Result<Environment> update(@PathVariable Long id, @RequestBody Environment environment) {
        return Result.success(environmentService.update(id, environment));
    }

    /**
     * 克隆环境配置
     *
     * @param sourceEnvId 源环境ID
     * @param targetEnvId 目标环境ID
     */
    @PostMapping("/clone")
    public Result<Void> cloneConfig(
            @RequestParam Long sourceEnvId,
            @RequestParam Long targetEnvId) {
        environmentService.cloneConfig(sourceEnvId, targetEnvId);
        return Result.success(null);
    }
}
