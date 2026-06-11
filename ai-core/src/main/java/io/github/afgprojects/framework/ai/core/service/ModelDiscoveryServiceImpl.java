package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.dto.model.DiscoveredModel;
import io.github.afgprojects.framework.ai.core.entity.model.ModelProviderEntity;
import io.github.afgprojects.framework.ai.core.provider.ProviderTemplate;
import io.github.afgprojects.framework.ai.core.provider.ProviderTemplateRegistry;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 模型发现服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryServiceImpl implements ModelDiscoveryService {

    private final DataManager dataManager;
    private final ProviderTemplateRegistry providerTemplateRegistry;

    private static final Pattern EMBEDDING_PATTERN = Pattern.compile(
        "embed|e5|bge|text-embedding|ada-002",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RERANK_PATTERN = Pattern.compile(
        "rerank|re-rank|cross-encoder",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IMAGE_PATTERN = Pattern.compile(
        "dall-e|stable-diffusion|flux|midjourney|cogview",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AUDIO_PATTERN = Pattern.compile(
        "tts|whisper|speech|audio",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<DiscoveredModel> discoverModels(Long providerId) {
        ModelProviderEntity provider = dataManager.findById(ModelProviderEntity.class, providerId)
            .orElseThrow(() -> new IllegalArgumentException("供应商不存在: " + providerId));

        // providerType 存储的是模板类型（如 "openai"、"ollama"），需要映射到 ProviderCategory
        String providerCategory = resolveProviderCategory(provider.getProviderType());
        return discoverModels(provider.getBaseUrl(), provider.getApiKey(), providerCategory);
    }

    /**
     * 将供应商类型（如 "openai"、"ollama"）解析为供应商类别（如 "OPENAI_COMPATIBLE"、"OLLAMA"）
     */
    private String resolveProviderCategory(String providerType) {
        if (providerType == null || providerType.isBlank()) {
            return providerType;
        }
        ProviderTemplate template = providerTemplateRegistry.getByType(providerType);
        if (template != null) {
            return template.providerCategory();
        }
        // 降级：如果已经是类别名则直接使用
        return providerType;
    }

    @Override
    public List<DiscoveredModel> discoverModels(String baseUrl, String apiKey, String providerCategory) {
        if (providerCategory == null || providerCategory.isBlank()) {
            return List.of();
        }
        return switch (providerCategory) {
            case "OPENAI_COMPATIBLE" -> discoverFromOpenAiCompatible(baseUrl, apiKey);
            case "OLLAMA" -> discoverFromOllama(baseUrl);
            default -> List.of();
        };
    }

    private List<DiscoveredModel> discoverFromOpenAiCompatible(String baseUrl, String apiKey) {
        String url = normalizeBaseUrl(baseUrl) + "/v1/models";
        try {
            RestClient client = RestClient.builder().baseUrl(url).build();
            String response;
            if (apiKey != null && !apiKey.isBlank()) {
                response = client.get()
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);
            } else {
                response = client.get()
                    .retrieve()
                    .body(String.class);
            }
            return parseOpenAiModelsResponse(response);
        } catch (Exception e) {
            log.warn("Failed to discover models from OpenAI-compatible API: {} - {}", url, e.getMessage());
            return List.of();
        }
    }

    private List<DiscoveredModel> discoverFromOllama(String baseUrl) {
        String url = normalizeBaseUrl(baseUrl) + "/api/tags";
        try {
            RestClient client = RestClient.builder().baseUrl(url).build();
            String response = client.get()
                .retrieve()
                .body(String.class);
            return parseOllamaModelsResponse(response);
        } catch (Exception e) {
            log.warn("Failed to discover models from Ollama API: {} - {}", url, e.getMessage());
            return List.of();
        }
    }

    private List<DiscoveredModel> parseOpenAiModelsResponse(String response) {
        List<DiscoveredModel> models = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return models;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode modelNode : data) {
                    String modelId = modelNode.has("id") ? modelNode.get("id").asText() : null;
                    String ownedBy = modelNode.has("owned_by") ? modelNode.get("owned_by").asText() : null;
                    if (modelId == null) continue;

                    models.add(DiscoveredModel.builder()
                        .modelName(modelId)
                        .modelType(inferModelType(modelId))
                        .displayName(modelId)
                        .ownedBy(ownedBy)
                        .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI models response: {}", e.getMessage());
        }
        return models;
    }

    private List<DiscoveredModel> parseOllamaModelsResponse(String response) {
        List<DiscoveredModel> models = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return models;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode modelNode : modelsNode) {
                    String name = modelNode.has("name") ? modelNode.get("name").asText() : null;
                    if (name == null) continue;

                    models.add(DiscoveredModel.builder()
                        .modelName(name)
                        .modelType(inferModelType(name))
                        .displayName(name)
                        .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Ollama models response: {}", e.getMessage());
        }
        return models;
    }

    private String inferModelType(String modelName) {
        if (EMBEDDING_PATTERN.matcher(modelName).find()) return "EMBEDDING";
        if (RERANK_PATTERN.matcher(modelName).find()) return "RERANK";
        if (IMAGE_PATTERN.matcher(modelName).find()) return "IMAGE";
        if (AUDIO_PATTERN.matcher(modelName).find()) return "AUDIO";
        return "CHAT";
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
