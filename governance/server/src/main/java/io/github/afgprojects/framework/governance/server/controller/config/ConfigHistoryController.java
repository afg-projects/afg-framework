package io.github.afgprojects.framework.governance.server.controller.config;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/console/config/history")
@RequiredArgsConstructor
public class ConfigHistoryController {

    private final DataManager dataManager;

    @GetMapping
    public PageData<ConfigHistory> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return dataManager.entity(ConfigHistory.class)
            .query()
            .page(PageRequest.of(page, size)
                .withSort(Sort.Direction.DESC, "createTime"));
    }

    @GetMapping("/item/{itemId}")
    public List<ConfigHistory> listByItem(@PathVariable Long itemId) {
        return dataManager.findAllByField(ConfigHistory.class, ConfigHistory::getItemId, itemId);
    }

    @GetMapping("/item/{itemId}/paged")
    public PageData<ConfigHistory> listByItemPaged(@PathVariable Long itemId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return dataManager.entity(ConfigHistory.class)
            .query()
            .where(Conditions.eq(ConfigHistory.class, ConfigHistory::getItemId, itemId))
            .page(PageRequest.of(page, size)
                .withSort(Sort.Direction.DESC, "createTime"));
    }
}
