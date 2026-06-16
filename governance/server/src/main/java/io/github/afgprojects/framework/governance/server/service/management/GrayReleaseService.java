package io.github.afgprojects.framework.governance.server.service.management;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.governance.server.entity.management.GrayRelease;
import io.github.afgprojects.framework.governance.server.entity.management.PushRecord;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import io.github.afgprojects.framework.governance.proto.ChangeType;
import io.github.afgprojects.framework.governance.server.grpc.ConfigStreamManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 灰度发布服务
 *
 * <p>管理灰度发布的完整生命周期：
 * <ul>
 *   <li>DRAFT（草稿）→ PUBLISHED（已发布）</li>
 *   <li>PUBLISHED → FULL_ROLLOUT（全量发布）</li>
 *   <li>PUBLISHED → ROLLED_BACK（已回滚）</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrayReleaseService {

    /** 灰度发布状态常量 */
    public static final int STATUS_DRAFT = 1;
    public static final int STATUS_PUBLISHED = 2;
    public static final int STATUS_FULL_ROLLOUT = 3;
    public static final int STATUS_ROLLED_BACK = 4;

    private final DataManager dataManager;
    private final ConfigStreamManager streamManager;

    /**
     * 创建灰度发布规则（草稿状态）
     */
    @Transactional
    public GrayRelease create(GrayRelease release) {
        // 验证配置项存在
        ConfigItem item = dataManager.findById(ConfigItem.class, release.getConfigItemId())
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                "Config item not found: " + release.getConfigItemId()));

        release.setStatus(STATUS_DRAFT);
        GrayRelease saved = dataManager.save(GrayRelease.class, release);
        log.info("Created gray release: id={}, item={}, status=DRAFT", saved.getId(), item.getCode());
        return saved;
    }

    /**
     * 发布灰度规则（DRAFT → PUBLISHED）
     *
     * <p>将新值推送到灰度实例，创建推送记录。
     */
    @Transactional
    public GrayRelease publish(Long id) {
        GrayRelease release = findByIdOrThrow(id);

        if (release.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "Only DRAFT releases can be published, current status: " + release.getStatus());
        }

        release.setStatus(STATUS_PUBLISHED);
        GrayRelease saved = dataManager.save(GrayRelease.class, release);

        // 推送配置变更到灰度实例
        pushToGrayInstances(release);

        log.info("Published gray release: id={}", id);
        return saved;
    }

    /**
     * 全量发布（PUBLISHED → FULL_ROLLOUT）
     *
     * <p>将灰度值更新为配置项的正式值，推送到所有实例。
     */
    @Transactional
    public GrayRelease fullRollout(Long id) {
        GrayRelease release = findByIdOrThrow(id);

        if (release.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "Only PUBLISHED releases can be fully rolled out, current status: " + release.getStatus());
        }

        // 更新配置项的正式值为灰度值
        ConfigValue configValue = dataManager.findOneByField(ConfigValue.class,
                ConfigValue::getItemId, release.getConfigItemId())
            .orElseGet(() -> {
                ConfigValue v = new ConfigValue();
                v.setItemId(release.getConfigItemId());
                v.setVersion(0);
                return v;
            });

        String oldValue = configValue.getValue();
        configValue.setValue(release.getNewValue());
        configValue.setVersion(configValue.getVersion() + 1);
        dataManager.save(ConfigValue.class, configValue);

        // 推送全量更新
        ConfigItem item = dataManager.findById(ConfigItem.class, release.getConfigItemId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                "Config item not found: " + release.getConfigItemId()));

        streamManager.pushConfigChange(item.getCode(), release.getNewValue(), ChangeType.CHANGE_TYPE_UPDATE);

        release.setStatus(STATUS_FULL_ROLLOUT);
        GrayRelease saved = dataManager.save(GrayRelease.class, release);

        log.info("Full rollout for gray release: id={}, item={}", id, item.getCode());
        return saved;
    }

    /**
     * 回滚灰度发布（PUBLISHED → ROLLED_BACK）
     *
     * <p>恢复灰度实例的原始配置值。
     */
    @Transactional
    public GrayRelease rollback(Long id) {
        GrayRelease release = findByIdOrThrow(id);

        if (release.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "Only PUBLISHED releases can be rolled back, current status: " + release.getStatus());
        }

        // 恢复灰度实例的原始值
        rollbackGrayInstances(release);

        release.setStatus(STATUS_ROLLED_BACK);
        GrayRelease saved = dataManager.save(GrayRelease.class, release);

        log.info("Rolled back gray release: id={}", id);
        return saved;
    }

    /**
     * 查询灰度发布列表
     */
    public List<GrayRelease> list(Long environmentId) {
        var builder = Conditions.builder(GrayRelease.class);
        if (environmentId != null) {
            builder.eq(GrayRelease::getEnvironmentId, environmentId);
        }
        return dataManager.entity(GrayRelease.class)
            .query()
            .where(builder.build())
            .list();
    }

    /**
     * 根据ID查询灰度发布
     */
    public GrayRelease findById(Long id) {
        return dataManager.findById(GrayRelease.class, id).orElse(null);
    }

    // ========== 私有方法 ==========

    private GrayRelease findByIdOrThrow(Long id) {
        return dataManager.findById(GrayRelease.class, id)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND,
                "Gray release not found: " + id));
    }

    /**
     * 推送配置到灰度实例，创建推送记录
     */
    private void pushToGrayInstances(GrayRelease release) {
        ConfigItem item = dataManager.findById(ConfigItem.class, release.getConfigItemId())
            .orElse(null);
        if (item == null) return;

        // 推送 gRPC 配置变更
        streamManager.pushConfigChange(item.getCode(), release.getNewValue(), ChangeType.CHANGE_TYPE_UPDATE);

        // 创建推送记录（针对灰度实例）
        PushRecord record = new PushRecord();
        record.setConfigItemId(release.getConfigItemId());
        record.setPushStatus("SUCCESS");
        record.setPushTime(Instant.now());
        record.setCreatedAt(Instant.now());
        dataManager.save(PushRecord.class, record);
    }

    /**
     * 回滚灰度实例的配置
     */
    private void rollbackGrayInstances(GrayRelease release) {
        ConfigItem item = dataManager.findById(ConfigItem.class, release.getConfigItemId())
            .orElse(null);
        if (item == null) return;

        // 获取原始值
        String originalValue = dataManager.findOneByField(ConfigValue.class,
                ConfigValue::getItemId, release.getConfigItemId())
            .map(ConfigValue::getValue)
            .orElse(item.getDefaultValue());

        // 推送原始值
        streamManager.pushConfigChange(item.getCode(), originalValue, ChangeType.CHANGE_TYPE_UPDATE);
    }
}
