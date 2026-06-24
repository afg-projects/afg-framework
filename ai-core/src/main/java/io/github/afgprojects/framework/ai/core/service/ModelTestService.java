package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.dto.model.ConnectionTestResponse;
import io.github.afgprojects.framework.ai.core.entity.model.ModelProviderEntity;
import io.github.afgprojects.framework.ai.core.provider.ProviderTemplateRegistry;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 模型连接测试服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelTestService {

    private final DataManager dataManager;
    private final ProviderTemplateRegistry providerTemplateRegistry;

    /**
     * 验证供应商连接
     *
     * @param providerId 供应商 ID
     * @return 验证结果
     */
    public ConnectionTestResponse verifyConnection(String providerId) {
        ModelProviderEntity provider = dataManager.findById(ModelProviderEntity.class, providerId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "供应商不存在: " + providerId));

        String baseUrl = provider.getBaseUrl();
        String apiKey = provider.getApiKey();
        String providerType = provider.getProviderType();

        try {
            long startTime = System.currentTimeMillis();
            String modelName = verifyProviderConnection(baseUrl, apiKey, providerType);
            long responseTime = System.currentTimeMillis() - startTime;
            return ConnectionTestResponse.success(modelName, responseTime);
        } catch (Exception e) {
            log.warn("Connection test failed for provider {}: {}", providerId, e.getMessage());
            return ConnectionTestResponse.failure("连接失败: " + e.getMessage());
        }
    }

    private String verifyProviderConnection(String baseUrl, String apiKey, String providerType) {
        // 通过模板注册表解析 providerCategory，若未注册则使用 providerType 本身
        var template = providerTemplateRegistry.getByType(providerType);
        String category = template != null ? template.providerCategory() : providerType;

        if ("OLLAMA".equalsIgnoreCase(category)) {
            return verifyOllamaConnection(baseUrl);
        }
        return verifyOpenAiCompatibleConnection(baseUrl, apiKey);
    }

    private String verifyOpenAiCompatibleConnection(String baseUrl, String apiKey) {
        String url = normalizeBaseUrl(baseUrl) + "/v1/models";
        RestClient client = RestClient.builder().baseUrl(url).build();
        if (apiKey != null && !apiKey.isBlank()) {
            client.get()
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(String.class);
        } else {
            client.get()
                .retrieve()
                .body(String.class);
        }
        return "openai-compatible";
    }

    private String verifyOllamaConnection(String baseUrl) {
        String url = normalizeBaseUrl(baseUrl) + "/api/tags";
        RestClient client = RestClient.builder().baseUrl(url).build();
        client.get()
            .retrieve()
            .body(String.class);
        return "ollama";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String url = baseUrl.trim();
        if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 3);
        } else if (url.endsWith("/v1/")) {
            url = url.substring(0, url.length() - 4);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
