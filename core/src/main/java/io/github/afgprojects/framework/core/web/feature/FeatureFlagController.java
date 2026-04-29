package io.github.afgprojects.framework.core.web.feature;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.afgprojects.framework.core.feature.FeatureFlag;
import io.github.afgprojects.framework.core.feature.FeatureFlagManager;
import io.github.afgprojects.framework.core.feature.FeatureFlagRequest;
import io.github.afgprojects.framework.core.feature.FeatureFlagResponse;
import io.github.afgprojects.framework.core.feature.FeatureFlagUpdateRequest;
import io.github.afgprojects.framework.core.model.result.Result;
import io.github.afgprojects.framework.core.model.result.Results;

import jakarta.validation.Valid;

/**
 * 功能开关管理 REST API
 * <p>
 * 提供功能开关的 CRUD 操作和状态切换接口
 * </p>
 */
@RestController
@RequestMapping("/api/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagManager featureFlagManager;

    public FeatureFlagController(@NonNull FeatureFlagManager featureFlagManager) {
        this.featureFlagManager = featureFlagManager;
    }

    /**
     * 获取所有功能开关
     *
     * @return 功能开关列表
     */
    @GetMapping
    public Result<List<FeatureFlagResponse>> getAll() {
        List<FeatureFlagResponse> flags = featureFlagManager.getAllFeatureFlags().stream()
                .map(FeatureFlagResponse::from)
                .toList();
        return Results.success(flags);
    }

    /**
     * 获取单个功能开关
     *
     * @param name 功能名称
     * @return 功能开关
     */
    @GetMapping("/{name}")
    public ResponseEntity<Result<FeatureFlagResponse>> getByName(@PathVariable String name) {
        FeatureFlag flag = featureFlagManager.getFeatureFlag(name);
        if (flag == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Results.fail(17002, "功能开关不存在: " + name));
        }
        return ResponseEntity.ok(Results.success(FeatureFlagResponse.from(flag)));
    }

    /**
     * 创建功能开关
     *
     * @param request 请求
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Result<FeatureFlagResponse>> create(@Valid @RequestBody FeatureFlagRequest request) {
        // 检查是否已存在
        FeatureFlag existing = featureFlagManager.getFeatureFlag(request.name());
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Results.fail(17003, "功能开关已存在: " + request.name()));
        }

        // 创建功能开关
        FeatureFlag flag = FeatureFlag.builder()
                .name(request.name())
                .enabled(request.enabled())
                .grayscaleRule(request.toGrayscaleRule())
                .description(request.description())
                .updatedBy(request.updatedBy())
                .build();

        featureFlagManager.register(flag);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Results.success("功能开关创建成功", FeatureFlagResponse.from(flag)));
    }

    /**
     * 更新功能开关
     *
     * @param name    功能名称
     * @param request 请求
     * @return 更新结果
     */
    @PutMapping("/{name}")
    public ResponseEntity<Result<FeatureFlagResponse>> update(
            @PathVariable String name, @Valid @RequestBody FeatureFlagUpdateRequest request) {
        FeatureFlag existing = featureFlagManager.getFeatureFlag(name);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Results.fail(17002, "功能开关不存在: " + name));
        }

        // 构建更新后的功能开关
        FeatureFlag flag = FeatureFlag.builder()
                .name(name)
                .enabled(request.enabled() != null ? request.enabled() : existing.enabled())
                .grayscaleRule(request.toGrayscaleRule(existing.grayscaleRule()))
                .description(request.description() != null ? request.description() : existing.description())
                .createdAt(existing.createdAt())
                .updatedBy(request.updatedBy())
                .build();

        featureFlagManager.register(flag);

        return ResponseEntity.ok(Results.success("功能开关更新成功", FeatureFlagResponse.from(flag)));
    }

    /**
     * 删除功能开关
     *
     * @param name 功能名称
     * @return 删除结果
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Result<Void>> delete(@PathVariable String name) {
        FeatureFlag removed = featureFlagManager.remove(name);
        if (removed == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Results.fail(17002, "功能开关不存在: " + name));
        }
        return ResponseEntity.ok(Results.success("功能开关删除成功", null));
    }

    /**
     * 启用功能开关
     *
     * @param name 功能名称
     * @return 操作结果
     */
    @PostMapping("/{name}/enable")
    public ResponseEntity<Result<FeatureFlagResponse>> enable(@PathVariable String name) {
        FeatureFlag existing = featureFlagManager.getFeatureFlag(name);
        if (existing == null) {
            // 如果不存在，创建一个新的启用的功能开关
            FeatureFlag flag = FeatureFlag.of(name, true);
            featureFlagManager.register(flag);
            return ResponseEntity.ok(Results.success("功能开关已启用", FeatureFlagResponse.from(flag)));
        }

        featureFlagManager.enable(name);
        FeatureFlag updated = featureFlagManager.getFeatureFlag(name);
        return ResponseEntity.ok(Results.success("功能开关已启用", FeatureFlagResponse.from(updated)));
    }

    /**
     * 禁用功能开关
     *
     * @param name 功能名称
     * @return 操作结果
     */
    @PostMapping("/{name}/disable")
    public ResponseEntity<Result<FeatureFlagResponse>> disable(@PathVariable String name) {
        FeatureFlag existing = featureFlagManager.getFeatureFlag(name);
        if (existing == null) {
            // 如果不存在，创建一个新的禁用的功能开关
            FeatureFlag flag = FeatureFlag.of(name, false);
            featureFlagManager.register(flag);
            return ResponseEntity.ok(Results.success("功能开关已禁用", FeatureFlagResponse.from(flag)));
        }

        featureFlagManager.disable(name);
        FeatureFlag updated = featureFlagManager.getFeatureFlag(name);
        return ResponseEntity.ok(Results.success("功能开关已禁用", FeatureFlagResponse.from(updated)));
    }
}