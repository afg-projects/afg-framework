package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.ai.core.api.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import io.github.afgprojects.framework.ai.core.dto.model.CreateModelConfigRequest;
import io.github.afgprojects.framework.ai.core.dto.model.CreateProviderRequest;
import io.github.afgprojects.framework.ai.core.dto.model.DiscoveredModel;
import io.github.afgprojects.framework.ai.core.dto.model.ModelUsageQuery;
import io.github.afgprojects.framework.ai.core.dto.model.ConnectionTestResponse;
import io.github.afgprojects.framework.ai.core.dto.model.UpdateModelConfigRequest;
import io.github.afgprojects.framework.ai.core.dto.model.UpdateProviderRequest;
import io.github.afgprojects.framework.ai.core.entity.model.ModelConfigEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelProviderEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelUsageEntity;
import io.github.afgprojects.framework.ai.core.provider.ProviderTemplate;
import io.github.afgprojects.framework.ai.core.provider.ProviderTemplateRegistry;
import io.github.afgprojects.framework.ai.core.service.CredentialService;
import io.github.afgprojects.framework.ai.core.service.ModelDiscoveryService;
import io.github.afgprojects.framework.ai.core.service.ModelTestService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 模型管理控制器
 * <p>
 * 提供模型提供商、模型配置、模型用量、运行时模型注册表的 CRUD 和查询接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class AiModelController {

    private final DataManager dataManager;
    private final ModelRegistry modelRegistry;
    private final ProviderTemplateRegistry providerTemplateRegistry;
    private final ModelDiscoveryService modelDiscoveryService;
    private final ModelTestService modelTestService;
    private final CredentialService credentialService;

    // ==================== 模型提供商 CRUD ====================

    /**
     * 创建模型提供商
     */
    @PostMapping("/providers")
    @Transactional
    public ModelProviderEntity createProvider(@Valid @RequestBody CreateProviderRequest request) {
        ModelProviderEntity entity = new ModelProviderEntity();
        entity.setProviderName(request.getProviderName());
        entity.setProviderType(request.getProviderType());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setApiKey(request.getApiKey());
        entity.setEnabled(request.getEnabled());
        entity.setConfig(request.getConfig());
        return dataManager.save(ModelProviderEntity.class, entity);
    }

    /**
     * 列出所有模型提供商
     */
    @GetMapping("/providers")
    public List<ModelProviderEntity> listProviders() {
        return dataManager.entity(ModelProviderEntity.class)
            .query()
            .orderByDesc(ModelProviderEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个模型提供商
     */
    @GetMapping("/providers/{id}")
    public ResponseEntity<ModelProviderEntity> getProvider(@PathVariable String id) {
        return dataManager.findById(ModelProviderEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新模型提供商
     */
    @PutMapping("/providers/{id}")
    @Transactional
    public ResponseEntity<ModelProviderEntity> updateProvider(@PathVariable String id,
                                               @Valid @RequestBody UpdateProviderRequest request) {
        ModelProviderEntity entity = dataManager.findById(ModelProviderEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getProviderName() != null) {
            entity.setProviderName(request.getProviderName());
        }
        if (request.getProviderType() != null) {
            entity.setProviderType(request.getProviderType());
        }
        if (request.getBaseUrl() != null) {
            entity.setBaseUrl(request.getBaseUrl());
        }
        if (request.getApiKey() != null) {
            entity.setApiKey(request.getApiKey());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }

        return ResponseEntity.ok(dataManager.save(ModelProviderEntity.class, entity));
    }

    /**
     * 删除模型提供商
     */
    @DeleteMapping("/providers/{id}")
    @Transactional
    public ResponseEntity<Void> deleteProvider(@PathVariable String id) {
        if (!dataManager.existsById(ModelProviderEntity.class, id)) {
            return ResponseEntity.notFound().build();
        }
        dataManager.deleteById(ModelProviderEntity.class, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 模型配置 CRUD ====================

    /**
     * 创建模型配置
     */
    @PostMapping("/configs")
    @Transactional
    public ModelConfigEntity createModelConfig(@Valid @RequestBody CreateModelConfigRequest request) {
        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setProviderId(request.getProviderId());
        entity.setModelName(request.getModelName());
        entity.setDisplayName(request.getDisplayName());
        entity.setModelType(request.getModelType());
        entity.setCapabilities(request.getCapabilities());
        entity.setConfig(request.getConfig());
        entity.setEnabled(request.getEnabled());
        return dataManager.save(ModelConfigEntity.class, entity);
    }

    /**
     * 列出模型配置（支持按 providerId 筛选）
     */
    @GetMapping("/configs")
    public List<ModelConfigEntity> listModelConfigs(@RequestParam(required = false) String providerId) {
        if (providerId != null) {
            return dataManager.entity(ModelConfigEntity.class)
                .query()
                .where(Conditions.builder(ModelConfigEntity.class)
                    .eq(ModelConfigEntity::getProviderId, providerId)
                    .build())
                .orderByDesc(ModelConfigEntity::getCreatedAt)
                .list();
        }
        return dataManager.entity(ModelConfigEntity.class)
            .query()
            .orderByDesc(ModelConfigEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个模型配置
     */
    @GetMapping("/configs/{id}")
    public ResponseEntity<ModelConfigEntity> getModelConfig(@PathVariable String id) {
        return dataManager.findById(ModelConfigEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新模型配置
     */
    @PutMapping("/configs/{id}")
    @Transactional
    public ResponseEntity<ModelConfigEntity> updateModelConfig(@PathVariable String id,
                                                @Valid @RequestBody UpdateModelConfigRequest request) {
        ModelConfigEntity entity = dataManager.findById(ModelConfigEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getModelName() != null) {
            entity.setModelName(request.getModelName());
        }
        if (request.getDisplayName() != null) {
            entity.setDisplayName(request.getDisplayName());
        }
        if (request.getModelType() != null) {
            entity.setModelType(request.getModelType());
        }
        if (request.getCapabilities() != null) {
            entity.setCapabilities(request.getCapabilities());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        return ResponseEntity.ok(dataManager.save(ModelConfigEntity.class, entity));
    }

    /**
     * 删除模型配置
     */
    @DeleteMapping("/configs/{id}")
    @Transactional
    public ResponseEntity<Void> deleteModelConfig(@PathVariable String id) {
        if (!dataManager.existsById(ModelConfigEntity.class, id)) {
            return ResponseEntity.notFound().build();
        }
        dataManager.deleteById(ModelConfigEntity.class, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 模型用量查询 ====================

    /**
     * 查询模型用量（分页 + 筛选）
     */
    @GetMapping("/usage")
    public PageData<ModelUsageEntity> queryUsage(ModelUsageQuery query) {
        var builder = Conditions.builder(ModelUsageEntity.class);

        if (query.getModelConfigId() != null) {
            builder.eq(ModelUsageEntity::getModelConfigId, query.getModelConfigId());
        }
        if (query.getApplicationId() != null) {
            builder.eq(ModelUsageEntity::getApplicationId, query.getApplicationId());
        }
        if (query.getUserId() != null) {
            builder.eq(ModelUsageEntity::getUserId, query.getUserId());
        }
        if (query.getStatus() != null) {
            builder.eq(ModelUsageEntity::getStatus, query.getStatus());
        }

        Sort sort = Sort.desc(ModelUsageEntity::getCreatedAt);
        PageRequest pageRequest = PageRequest.of(
            query.getPage() != null ? query.getPage() : 1,
            query.getSize() != null ? query.getSize() : 10,
            sort
        );

        return dataManager.entity(ModelUsageEntity.class)
            .query()
            .where(builder.build())
            .page(pageRequest);
    }

    // ==================== 运行时模型注册表 ====================

    /**
     * 列出 ModelRegistry 中已注册的运行时模型
     */
    @GetMapping("/registry")
    public List<ModelInfo> listRegistryModels(@RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            try {
                ModelType modelType = ModelType.valueOf(type.toUpperCase());
                return modelRegistry.listModels(modelType);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid model type: {}", type);
                return modelRegistry.listModels();
            }
        }
        return modelRegistry.listModels();
    }

    /**
     * 获取运行时模型详情
     */
    @GetMapping("/registry/{modelId}")
    public ResponseEntity<ModelInfo> getRegistryModel(@PathVariable String modelId) {
        return modelRegistry.getModel(modelId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ==================== 供应商模板 ====================

    /**
     * 获取所有供应商模板
     */
    @GetMapping("/templates")
    public List<ProviderTemplate> listTemplates() {
        return providerTemplateRegistry.getTemplates();
    }

    /**
     * 获取单个供应商模板
     */
    @GetMapping("/templates/{type}")
    public ResponseEntity<ProviderTemplate> getTemplate(@PathVariable String type) {
        ProviderTemplate template = providerTemplateRegistry.getByType(type);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    // ==================== 模型发现 ====================

    /**
     * 发现供应商可用的模型列表
     *
     * @param id 供应商 ID
     * @return 发现的模型列表
     */
    @PostMapping("/providers/{id}/discover-models")
    public ResponseEntity<List<DiscoveredModel>> discoverModels(@PathVariable String id) {
        List<DiscoveredModel> models = modelDiscoveryService.discoverModels(id);
        return ResponseEntity.ok(models);
    }

    /**
     * 预览供应商可用模型（创建供应商前）
     *
     * @param baseUrl          供应商 API 基础 URL
     * @param apiKey           API Key
     * @param providerCategory 供应商类别
     * @return 发现的模型列表
     */
    @PostMapping("/discover-models")
    public ResponseEntity<List<DiscoveredModel>> previewModels(
            @RequestParam String baseUrl,
            @RequestParam(required = false) String apiKey,
            @RequestParam String providerCategory) {
        List<DiscoveredModel> models = modelDiscoveryService.discoverModels(baseUrl, apiKey, providerCategory);
        return ResponseEntity.ok(models);
    }

    // ==================== 模型连接测试 ====================

    /**
     * 测试供应商连接
     *
     * @param id 供应商 ID
     * @return 测试结果
     */
    @PostMapping("/providers/{id}/test")
    public ResponseEntity<ConnectionTestResponse> verifyConnection(@PathVariable String id) {
        ConnectionTestResponse response = modelTestService.verifyConnection(id);
        return ResponseEntity.ok(response);
    }

    // ==================== 凭证管理 ====================

    /**
     * 获取供应商的脱敏 API Key
     *
     * @param id 供应商 ID
     * @return 脱敏后的 API Key
     */
    @GetMapping("/providers/{id}/masked-api-key")
    public ResponseEntity<Map<String, String>> getMaskedApiKey(@PathVariable String id) {
        return dataManager.findById(ModelProviderEntity.class, id)
            .map(provider -> {
                String maskedKey = credentialService.mask(provider.getApiKey());
                return ResponseEntity.ok(Map.of("maskedApiKey", maskedKey));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 加密 API Key
     *
     * @param request 包含 plaintext 的请求体
     * @return 加密后的值
     */
    @PostMapping("/credentials/encrypt")
    public Map<String, String> encrypt(@RequestBody Map<String, String> request) {
        String plaintext = request.get("plaintext");
        String encrypted = credentialService.encrypt(plaintext);
        return Map.of("encrypted", encrypted);
    }

    /**
     * 解密 API Key
     *
     * @param request 包含 ciphertext 的请求体
     * @return 解密后的值
     */
    @PostMapping("/credentials/decrypt")
    public Map<String, String> decrypt(@RequestBody Map<String, String> request) {
        String ciphertext = request.get("ciphertext");
        String decrypted = credentialService.decrypt(ciphertext);
        return Map.of("decrypted", decrypted);
    }
}
