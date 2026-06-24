package io.github.afgprojects.framework.governance.server.service.management;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.governance.server.entity.management.PushRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 推送记录服务
 *
 * <p>管理配置推送记录的查询和状态更新。
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushRecordService {

    private final DataManager dataManager;

    /**
     * 分页查询推送记录
     *
     * @param configItemId 可选的配置项ID过滤
     * @param instanceId   可选的实例ID过滤
     * @param pushStatus   可选的推送状态过滤
     * @param pageRequest  分页参数
     */
    public PageData<PushRecord> list(String configItemId, String instanceId, String pushStatus, PageRequest pageRequest) {
        var builder = Conditions.builder(PushRecord.class);
        if (configItemId != null) {
            builder.eq(PushRecord::getConfigItemId, configItemId);
        }
        if (instanceId != null) {
            builder.eq(PushRecord::getInstanceId, instanceId);
        }
        if (pushStatus != null && !pushStatus.isBlank()) {
            builder.eq(PushRecord::getPushStatus, pushStatus);
        }

        return dataManager.entity(PushRecord.class)
            .query()
            .where(builder.build())
            .page(pageRequest.withSort(Sort.Direction.DESC, "pushTime"));
    }

    /**
     * 根据ID查询推送记录
     */
    public PushRecord findById(String id) {
        return dataManager.findById(PushRecord.class, id).orElse(null);
    }

    /**
     * 创建推送记录
     */
    public PushRecord create(PushRecord record) {
        return dataManager.save(PushRecord.class, record);
    }

    /**
     * 标记推送成功
     */
    public void markSuccess(String id) {
        PushRecord record = dataManager.findById(PushRecord.class, id).orElse(null);
        if (record != null) {
            record.setPushStatus("SUCCESS");
            record.setAckTime(java.time.Instant.now());
            dataManager.save(PushRecord.class, record);
            log.info("Push record marked as SUCCESS: id={}", id);
        }
    }

    /**
     * 标记推送失败
     */
    public void markFailed(String id, String errorMessage) {
        PushRecord record = dataManager.findById(PushRecord.class, id).orElse(null);
        if (record != null) {
            record.setPushStatus("FAILED");
            record.setErrorMessage(errorMessage);
            dataManager.save(PushRecord.class, record);
            log.info("Push record marked as FAILED: id={}, error={}", id, errorMessage);
        }
    }
}
