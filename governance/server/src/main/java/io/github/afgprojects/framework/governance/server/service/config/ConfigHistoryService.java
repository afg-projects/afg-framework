package io.github.afgprojects.framework.governance.server.service.config;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigHistoryService {

    private final DataManager dataManager;

    public List<ConfigHistory> findByItemId(Long itemId) {
        var condition = Conditions.eq(ConfigHistory.class, ConfigHistory::getItemId, itemId);
        return dataManager.entity(ConfigHistory.class)
            .query()
            .where(condition)
            .list()
            .stream()
            .sorted(Comparator.comparing(ConfigHistory::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public List<ConfigHistory> findRecentByItemId(Long itemId) {
        return findByItemId(itemId)
            .stream()
            .limit(10)
            .collect(Collectors.toList());
    }

    public Page<ConfigHistory> findByItemId(Long itemId, Pageable pageable) {
        List<ConfigHistory> all = findByItemId(itemId);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<ConfigHistory> content = all.subList(start, end);
        return new PageImpl<>(content, pageable, all.size());
    }

    public Page<ConfigHistory> findAll(Pageable pageable) {
        List<ConfigHistory> all = dataManager.findAll(ConfigHistory.class)
            .stream()
            .sorted(Comparator.comparing(ConfigHistory::getCreatedAt).reversed())
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<ConfigHistory> content = all.subList(start, end);
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional
    public ConfigHistory create(ConfigHistory history) {
        return dataManager.save(ConfigHistory.class, history);
    }
}
