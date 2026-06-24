package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.dto.model.DiscoveredModel;

import java.util.List;

/**
 * 模型发现服务 — 从供应商 API 拉取可用模型列表
 */
public interface ModelDiscoveryService {

    /**
     * 从已有供应商拉取可用模型列表
     *
     * @param providerId 供应商 ID
     * @return 发现的模型列表
     */
    List<DiscoveredModel> discoverModels(String providerId);

    /**
     * 根据 base URL + credential 拉取（创建供应商前预览）
     *
     * @param baseUrl          供应商 API 基础 URL
     * @param apiKey           API Key
     * @param providerCategory 供应商类别
     * @return 发现的模型列表
     */
    List<DiscoveredModel> discoverModels(String baseUrl, String apiKey, String providerCategory);
}
