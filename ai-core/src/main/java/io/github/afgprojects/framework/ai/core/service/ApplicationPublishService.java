package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用发布服务
 *
 * <p>提供应用发布/取消发布功能，管理应用状态转换。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationPublishService {

    private final DataManager dataManager;

    /**
     * 发布应用
     *
     * <p>将应用状态从 DRAFT 转换为 PUBLISHED。
     *
     * @param applicationId 应用 ID
     * @return 更新后的应用实体
     * @throws BusinessException   应用不存在
     * @throws IllegalStateException    应用配置不完整
     */
    @Transactional
    public ApplicationEntity publish(String applicationId) {
        ApplicationEntity app = dataManager.findById(ApplicationEntity.class, applicationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "应用不存在: " + applicationId));

        // 验证应用配置完整性
        if (app.getName() == null || app.getName().isBlank()) {
            throw new IllegalStateException("应用名称不能为空");
        }
        if (app.getType() == null || app.getType().isBlank()) {
            throw new IllegalStateException("应用类型不能为空");
        }

        // 更新状态为 PUBLISHED
        app.setStatus("PUBLISHED");
        log.info("发布应用: id={}, name={}", applicationId, app.getName());
        return dataManager.save(ApplicationEntity.class, app);
    }

    /**
     * 取消发布应用
     *
     * <p>将应用状态从 PUBLISHED 转换为 DRAFT。
     *
     * @param applicationId 应用 ID
     * @return 更新后的应用实体
     * @throws BusinessException 应用不存在
     */
    @Transactional
    public ApplicationEntity unpublish(String applicationId) {
        ApplicationEntity app = dataManager.findById(ApplicationEntity.class, applicationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "应用不存在: " + applicationId));

        // 更新状态为 DRAFT
        app.setStatus("DRAFT");
        log.info("取消发布应用: id={}, name={}", applicationId, app.getName());
        return dataManager.save(ApplicationEntity.class, app);
    }
}
