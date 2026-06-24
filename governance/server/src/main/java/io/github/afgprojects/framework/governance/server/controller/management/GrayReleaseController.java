package io.github.afgprojects.framework.governance.server.controller.management;

import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.governance.server.entity.management.GrayRelease;
import io.github.afgprojects.framework.governance.server.service.management.GrayReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 灰度发布控制器
 *
 * <p>提供灰度发布的 REST API：
 * <ul>
 *   <li>GET /console/gray/releases — 灰度发布列表</li>
 *   <li>GET /console/gray/releases/{id} — 灰度发布详情</li>
 *   <li>POST /console/gray/releases — 创建灰度规则</li>
 *   <li>POST /console/gray/releases/{id}/publish — 发布灰度规则</li>
 *   <li>POST /console/gray/releases/{id}/full-rollout — 全量发布</li>
 *   <li>POST /console/gray/releases/{id}/rollback — 回滚</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/console/gray/releases")
@RequiredArgsConstructor
public class GrayReleaseController {

    private final GrayReleaseService grayReleaseService;

    @GetMapping
    public Result<List<GrayRelease>> list(@RequestParam(required = false) String environmentId) {
        return Result.success(grayReleaseService.list(environmentId));
    }

    @GetMapping("/{id}")
    public Result<GrayRelease> get(@PathVariable String id) {
        return Result.success(grayReleaseService.findById(id));
    }

    @PostMapping
    public Result<GrayRelease> create(@RequestBody GrayRelease release) {
        return Result.success(grayReleaseService.create(release));
    }

    @PostMapping("/{id}/publish")
    public Result<GrayRelease> publish(@PathVariable String id) {
        return Result.success(grayReleaseService.publish(id));
    }

    @PostMapping("/{id}/full-rollout")
    public Result<GrayRelease> fullRollout(@PathVariable String id) {
        return Result.success(grayReleaseService.fullRollout(id));
    }

    @PostMapping("/{id}/rollback")
    public Result<GrayRelease> rollback(@PathVariable String id) {
        return Result.success(grayReleaseService.rollback(id));
    }
}
