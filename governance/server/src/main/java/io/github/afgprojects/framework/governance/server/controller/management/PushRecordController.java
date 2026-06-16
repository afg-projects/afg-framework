package io.github.afgprojects.framework.governance.server.controller.management;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.commons.model.Result;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.governance.server.entity.management.PushRecord;
import io.github.afgprojects.framework.governance.server.service.management.PushRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 推送记录控制器
 *
 * <p>提供推送记录的查询 REST API：
 * <ul>
 *   <li>GET /console/push/records — 推送记录列表（支持分页和过滤）</li>
 *   <li>GET /console/push/records/{id} — 推送记录详情</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/console/push/records")
@RequiredArgsConstructor
public class PushRecordController {

    private final PushRecordService pushRecordService;

    @GetMapping
    public Result<PageData<PushRecord>> list(
            @RequestParam(required = false) Long configItemId,
            @RequestParam(required = false) Long instanceId,
            @RequestParam(required = false) String pushStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(pushRecordService.list(configItemId, instanceId, pushStatus,
            PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public Result<PushRecord> get(@PathVariable Long id) {
        return Result.success(pushRecordService.findById(id));
    }
}
