package io.github.afgprojects.framework.ai.core.api;

import io.github.afgprojects.framework.commons.model.PageData;
import io.github.afgprojects.framework.ai.core.AbstractAiWebTest;
import io.github.afgprojects.framework.ai.core.dto.model.CreateModelConfigRequest;
import io.github.afgprojects.framework.ai.core.dto.model.CreateProviderRequest;
import io.github.afgprojects.framework.ai.core.dto.model.UpdateModelConfigRequest;
import io.github.afgprojects.framework.ai.core.dto.model.UpdateProviderRequest;
import io.github.afgprojects.framework.ai.core.entity.model.ModelConfigEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelProviderEntity;
import io.github.afgprojects.framework.ai.core.entity.model.ModelUsageEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiModelController 集成测试
 *
 * <p>测试模型提供商、模型配置、模型用量、运行时模型注册表的 CRUD 和查询接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiModelControllerTest extends AbstractAiWebTest {

    @Autowired
    DataManager dataManager;

    // ==================== Provider CRUD ====================

    @Test
    void shouldCreateProvider_whenPostValidRequest() {
        // Arrange
        CreateProviderRequest request = new CreateProviderRequest();
        request.setProviderName("test-provider-" + UUID.randomUUID());
        request.setProviderType("OPENAI");
        request.setBaseUrl("https://api.openai.com/v1");
        request.setApiKey("sk-test-key");
        request.setEnabled(true);

        // Act
        ModelProviderEntity created = restClient().post()
            .uri("/models/providers")
            .body(request)
            .retrieve()
            .body(ModelProviderEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getProviderName()).isEqualTo(request.getProviderName());
        assertThat(created.getProviderType()).isEqualTo("OPENAI");
        assertThat(created.getEnabled()).isTrue();

        // Cleanup
        dataManager.deleteById(ModelProviderEntity.class, created.getId());
    }

    @Test
    void shouldListProviders_whenGetAll() {
        // Arrange - create 2 providers via DataManager
        String prefix = "list-test-" + UUID.randomUUID();

        ModelProviderEntity provider1 = new ModelProviderEntity();
        provider1.setProviderName(prefix + "-provider-1");
        provider1.setProviderType("OPENAI");
        provider1.setEnabled(true);
        provider1 = dataManager.save(ModelProviderEntity.class, provider1);

        ModelProviderEntity provider2 = new ModelProviderEntity();
        provider2.setProviderName(prefix + "-provider-2");
        provider2.setProviderType("ANTHROPIC");
        provider2.setEnabled(true);
        provider2 = dataManager.save(ModelProviderEntity.class, provider2);

        // Act
        List<ModelProviderEntity> providers = restClient().get()
            .uri("/models/providers")
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(providers).isNotNull();
        assertThat(providers.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(ModelProviderEntity.class, provider1.getId());
        dataManager.deleteById(ModelProviderEntity.class, provider2.getId());
    }

    @Test
    void shouldReturnProvider_whenGetById() {
        // Arrange - create provider via DataManager
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("get-test-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setBaseUrl("https://api.openai.com/v1");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        // Act
        ModelProviderEntity found = restClient().get()
            .uri("/models/providers/{id}", provider.getId())
            .retrieve()
            .body(ModelProviderEntity.class);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(provider.getId());
        assertThat(found.getProviderName()).isEqualTo(provider.getProviderName());
        assertThat(found.getProviderType()).isEqualTo("OPENAI");

        // Cleanup
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    @Test
    void shouldReturn404_whenGetNonExistentProvider() {
        // Act & Assert - RestClient throws HttpClientErrorException for 4xx responses
        assertThatThrownBy(() -> restClient().get()
                .uri("/models/providers/{id}", 999999L)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    @Test
    void shouldUpdateProvider_whenPutValidRequest() {
        // Arrange - create provider
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("update-test-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        UpdateProviderRequest request = new UpdateProviderRequest();
        request.setProviderName("updated-provider-" + UUID.randomUUID());
        request.setProviderType("ANTHROPIC");

        // Act
        ModelProviderEntity updated = restClient().put()
            .uri("/models/providers/{id}", provider.getId())
            .body(request)
            .retrieve()
            .body(ModelProviderEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(provider.getId());
        assertThat(updated.getProviderName()).isEqualTo(request.getProviderName());
        assertThat(updated.getProviderType()).isEqualTo("ANTHROPIC");

        // Cleanup
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    @Test
    void shouldSoftDeleteProvider_whenDeleteById() {
        // Arrange - create provider
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("delete-test-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/models/providers/{id}", provider.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedId = provider.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/models/providers/{id}", deletedId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);
    }

    // ==================== Model Config CRUD ====================

    @Test
    void shouldCreateConfig_whenPostValidRequest() {
        // Arrange - create provider first
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("config-provider-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        CreateModelConfigRequest request = new CreateModelConfigRequest();
        request.setProviderId(provider.getId());
        request.setModelName("gpt-4");
        request.setDisplayName("GPT-4");
        request.setModelType("CHAT");
        request.setEnabled(true);

        // Act
        ModelConfigEntity created = restClient().post()
            .uri("/models/configs")
            .body(request)
            .retrieve()
            .body(ModelConfigEntity.class);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getModelName()).isEqualTo("gpt-4");
        assertThat(created.getProviderId()).isEqualTo(provider.getId());

        // Cleanup
        dataManager.deleteById(ModelConfigEntity.class, created.getId());
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    @Test
    void shouldListConfigsByProvider_whenGetWithFilter() {
        // Arrange - create provider + 2 configs
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("filter-provider-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        ModelConfigEntity config1 = new ModelConfigEntity();
        config1.setProviderId(provider.getId());
        config1.setModelName("gpt-4-" + UUID.randomUUID());
        config1.setModelType("CHAT");
        config1.setEnabled(true);
        config1 = dataManager.save(ModelConfigEntity.class, config1);

        ModelConfigEntity config2 = new ModelConfigEntity();
        config2.setProviderId(provider.getId());
        config2.setModelName("gpt-3.5-" + UUID.randomUUID());
        config2.setModelType("CHAT");
        config2.setEnabled(true);
        config2 = dataManager.save(ModelConfigEntity.class, config2);

        // Act
        List<ModelConfigEntity> configs = restClient().get()
            .uri("/models/configs?providerId={providerId}", provider.getId())
            .retrieve()
            .body(List.class);

        // Assert
        assertThat(configs).isNotNull();
        assertThat(configs.size()).isGreaterThanOrEqualTo(2);

        // Cleanup
        dataManager.deleteById(ModelConfigEntity.class, config1.getId());
        dataManager.deleteById(ModelConfigEntity.class, config2.getId());
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    @Test
    void shouldUpdateConfig_whenPutValidRequest() {
        // Arrange - create provider + config
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("update-config-provider-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        ModelConfigEntity config = new ModelConfigEntity();
        config.setProviderId(provider.getId());
        config.setModelName("old-model-" + UUID.randomUUID());
        config.setModelType("CHAT");
        config.setEnabled(true);
        config = dataManager.save(ModelConfigEntity.class, config);

        UpdateModelConfigRequest request = new UpdateModelConfigRequest();
        request.setModelName("new-model-" + UUID.randomUUID());
        request.setDisplayName("New Model");

        // Act
        ModelConfigEntity updated = restClient().put()
            .uri("/models/configs/{id}", config.getId())
            .body(request)
            .retrieve()
            .body(ModelConfigEntity.class);

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(config.getId());
        assertThat(updated.getModelName()).isEqualTo(request.getModelName());
        assertThat(updated.getDisplayName()).isEqualTo("New Model");

        // Cleanup
        dataManager.deleteById(ModelConfigEntity.class, config.getId());
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    @Test
    void shouldSoftDeleteConfig_whenDeleteById() {
        // Arrange - create provider + config
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("delete-config-provider-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        ModelConfigEntity config = new ModelConfigEntity();
        config.setProviderId(provider.getId());
        config.setModelName("delete-model-" + UUID.randomUUID());
        config.setModelType("CHAT");
        config.setEnabled(true);
        config = dataManager.save(ModelConfigEntity.class, config);

        // Act - delete
        ResponseEntity<Void> deleteResponse = restClient().delete()
            .uri("/models/configs/{id}", config.getId())
            .retrieve()
            .toBodilessEntity();

        // Assert - delete succeeded
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Assert - GET returns 404 (soft deleted)
        Long deletedConfigId = config.getId();
        assertThatThrownBy(() -> restClient().get()
                .uri("/models/configs/{id}", deletedConfigId)
                .retrieve()
                .toEntity(Map.class))
            .isInstanceOf(org.springframework.web.client.HttpClientErrorException.NotFound.class);

        // Cleanup
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    // ==================== Model Usage ====================

    @Test
    void shouldListUsage_whenGetWithParams() {
        // Arrange - create provider + config + usage
        ModelProviderEntity provider = new ModelProviderEntity();
        provider.setProviderName("usage-provider-" + UUID.randomUUID());
        provider.setProviderType("OPENAI");
        provider.setEnabled(true);
        provider = dataManager.save(ModelProviderEntity.class, provider);

        ModelConfigEntity config = new ModelConfigEntity();
        config.setProviderId(provider.getId());
        config.setModelName("usage-model-" + UUID.randomUUID());
        config.setModelType("CHAT");
        config.setEnabled(true);
        config = dataManager.save(ModelConfigEntity.class, config);

        ModelUsageEntity usage = new ModelUsageEntity();
        usage.setModelConfigId(config.getId());
        usage.setInputTokens(100L);
        usage.setOutputTokens(50L);
        usage.setTotalTokens(150L);
        usage.setCost(new BigDecimal("0.001"));
        usage.setDurationMs(500L);
        usage.setStatus("SUCCESS");
        usage = dataManager.save(ModelUsageEntity.class, usage);

        // Act
        PageData<Map> page = restClient().get()
            .uri("/models/usage?modelConfigId={configId}", config.getId())
            .retrieve()
            .body(PageData.class);

        // Assert
        assertThat(page).isNotNull();
        assertThat(page.records()).isNotNull();
        assertThat(page.records().size()).isGreaterThanOrEqualTo(1);

        // Cleanup
        dataManager.deleteById(ModelUsageEntity.class, usage.getId());
        dataManager.deleteById(ModelConfigEntity.class, config.getId());
        dataManager.deleteById(ModelProviderEntity.class, provider.getId());
    }

    // ==================== Registry ====================

    @Test
    void shouldListRuntimeModels_whenGetRegistry() {
        // Act
        List<Map> models = restClient().get()
            .uri("/models/registry")
            .retrieve()
            .body(List.class);

        // Assert - may be empty if no models registered, but should not error
        assertThat(models).isNotNull();
    }
}
