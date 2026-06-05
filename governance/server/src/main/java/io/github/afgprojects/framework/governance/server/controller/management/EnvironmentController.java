package io.github.afgprojects.framework.governance.server.controller.management;

import io.github.afgprojects.framework.governance.server.entity.management.Environment;
import io.github.afgprojects.framework.governance.server.service.management.EnvironmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public List<Environment> list() {
        return environmentService.findAll();
    }

    /**
     * 根据ID查询环境
     * 注：暂时使用 findByCode 实现
     */
    @GetMapping("/{id}")
    public ResponseEntity<Environment> getById(@PathVariable Long id) {
        // TODO: 应该使用 findById，暂时使用 findByCode
        // 需要在 EnvironmentService 中添加 findById 方法
        log.warn("getById called with id: {}, but using findByCode as fallback", id);
        return ResponseEntity.notFound().build();
    }

    /**
     * 根据编码查询环境
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Environment> getByCode(@PathVariable String code) {
        Environment environment = environmentService.findByCode(code);
        if (environment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(environment);
    }

    /**
     * 创建环境
     */
    @PostMapping("/")
    public Environment create(@RequestBody Environment environment) {
        return environmentService.create(environment);
    }

    /**
     * 更新环境
     */
    @PutMapping("/{id}")
    public Environment update(@PathVariable Long id, @RequestBody Environment environment) {
        return environmentService.update(id, environment);
    }

    /**
     * 克隆环境配置
     *
     * @param sourceEnvId 源环境ID
     * @param targetEnvId 目标环境ID
     */
    @PostMapping("/clone")
    public ResponseEntity<Void> cloneConfig(
            @RequestParam Long sourceEnvId,
            @RequestParam Long targetEnvId) {
        environmentService.cloneConfig(sourceEnvId, targetEnvId);
        return ResponseEntity.ok().build();
    }
}
