package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.feature.FeatureFlagController;

/**
 * FeatureFlagController 单元测试
 * <p>
 * 测试 Controller 的业务逻辑和 DTO 转换
 */
@DisplayName("FeatureFlagController 测试")
class FeatureFlagControllerTest {

    private FeatureFlagManager featureFlagManager;
    private FeatureFlagController controller;

    @BeforeEach
    void setUp() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        featureFlagManager = new FeatureFlagManager(properties);
        controller = new FeatureFlagController(featureFlagManager);
    }

    @Test
    @DisplayName("获取所有功能开关（空列表）")
    void getAll_empty() {
        var result = controller.getAll();

        assertThat(result.code()).isEqualTo(0);
        assertThat(result.data()).isEmpty();
    }

    @Test
    @DisplayName("获取所有功能开关")
    void getAll_withData() {
        featureFlagManager.register(FeatureFlag.of("feature-1", true));
        featureFlagManager.register(FeatureFlag.of("feature-2", false));

        var result = controller.getAll();

        assertThat(result.code()).isEqualTo(0);
        assertThat(result.data()).hasSize(2);
    }

    @Test
    @DisplayName("获取存在的功能开关")
    void getByName_exists() {
        featureFlagManager.register(FeatureFlag.builder()
                .name("test-feature")
                .enabled(true)
                .description("测试功能")
                .build());

        var response = controller.getByName("test-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);

        FeatureFlagResponse data = response.getBody().data();
        assertThat(data.name()).isEqualTo("test-feature");
        assertThat(data.enabled()).isTrue();
        assertThat(data.description()).isEqualTo("测试功能");
    }

    @Test
    @DisplayName("获取不存在的功能开关")
    void getByName_notFound() {
        var response = controller.getByName("non-existent");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(17002);
    }

    @Test
    @DisplayName("创建功能开关")
    void create_success() {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "new-feature", true, "ALL", null, null, null, "新功能", "admin");

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关创建成功");

        FeatureFlagResponse data = response.getBody().data();
        assertThat(data.name()).isEqualTo("new-feature");
        assertThat(data.enabled()).isTrue();
        assertThat(data.description()).isEqualTo("新功能");

        assertThat(featureFlagManager.getFeatureFlag("new-feature")).isNotNull();
    }

    @Test
    @DisplayName("创建带灰度规则的功能开关")
    void create_withGrayscaleRule() {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "grayscale-feature", true, "USER_WHITELIST", null, Set.of(1L, 2L, 3L), null, "灰度功能", "admin");

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);

        FeatureFlagResponse data = response.getBody().data();
        assertThat(data.name()).isEqualTo("grayscale-feature");
        assertThat(data.strategy()).isEqualTo("USER_WHITELIST");

        FeatureFlag flag = featureFlagManager.getFeatureFlag("grayscale-feature");
        assertThat(flag).isNotNull();
        assertThat(flag.grayscaleRule()).isNotNull();
        assertThat(flag.grayscaleRule().userIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("创建已存在的功能开关")
    void create_alreadyExists() {
        featureFlagManager.register(FeatureFlag.of("existing-feature", true));

        FeatureFlagRequest request = new FeatureFlagRequest("existing-feature", false, null, null, null, null, null, null);

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(17003);
    }

    @Test
    @DisplayName("更新功能开关")
    void update_success() {
        featureFlagManager.register(FeatureFlag.builder()
                .name("update-feature")
                .enabled(true)
                .description("原始描述")
                .build());

        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest(
                false, null, null, null, null, "更新后的描述", "admin");

        var response = controller.update("update-feature", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关更新成功");

        FeatureFlagResponse data = response.getBody().data();
        assertThat(data.enabled()).isFalse();
        assertThat(data.description()).isEqualTo("更新后的描述");

        FeatureFlag updated = featureFlagManager.getFeatureFlag("update-feature");
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.description()).isEqualTo("更新后的描述");
    }

    @Test
    @DisplayName("更新不存在的功能开关")
    void update_notFound() {
        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest(true, null, null, null, null, null, null);

        var response = controller.update("non-existent", request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(17002);
    }

    @Test
    @DisplayName("删除功能开关")
    void delete_success() {
        featureFlagManager.register(FeatureFlag.of("delete-feature", true));

        var response = controller.delete("delete-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关删除成功");

        assertThat(featureFlagManager.getFeatureFlag("delete-feature")).isNull();
    }

    @Test
    @DisplayName("删除不存在的功能开关")
    void delete_notFound() {
        var response = controller.delete("non-existent");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(17002);
    }

    @Test
    @DisplayName("启用功能开关")
    void enable_success() {
        featureFlagManager.register(FeatureFlag.of("toggle-feature", false));

        var response = controller.enable("toggle-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关已启用");

        assertThat(featureFlagManager.isEnabled("toggle-feature")).isTrue();
    }

    @Test
    @DisplayName("启用不存在的功能开关（自动创建）")
    void enable_notExists_create() {
        var response = controller.enable("auto-create-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关已启用");

        assertThat(featureFlagManager.isEnabled("auto-create-feature")).isTrue();
    }

    @Test
    @DisplayName("禁用功能开关")
    void disable_success() {
        featureFlagManager.register(FeatureFlag.of("toggle-feature", true));

        var response = controller.disable("toggle-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关已禁用");

        assertThat(featureFlagManager.isEnabled("toggle-feature")).isFalse();
    }

    @Test
    @DisplayName("禁用不存在的功能开关（自动创建）")
    void disable_notExists_create() {
        var response = controller.disable("auto-disable-feature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().message()).isEqualTo("功能开关已禁用");

        assertThat(featureFlagManager.isEnabled("auto-disable-feature")).isFalse();
    }

    @Test
    @DisplayName("更新灰度规则")
    void update_grayscaleRule() {
        featureFlagManager.register(FeatureFlag.builder()
                .name("rule-feature")
                .enabled(true)
                .grayscaleRule(GrayscaleRule.ofPercentage(50))
                .build());

        FeatureFlagUpdateRequest request =
                new FeatureFlagUpdateRequest(null, "USER_WHITELIST", null, Set.of(1L, 2L), null, null, null);

        var response = controller.update("rule-feature", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);

        FeatureFlag updated = featureFlagManager.getFeatureFlag("rule-feature");
        assertThat(updated.grayscaleRule().strategy()).isEqualTo(GrayscaleStrategy.USER_WHITELIST);
        assertThat(updated.grayscaleRule().userIds()).containsExactlyInAnyOrder(1L, 2L);
    }
}
