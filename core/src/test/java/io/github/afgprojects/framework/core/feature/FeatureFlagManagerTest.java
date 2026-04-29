package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * FeatureFlagManager 测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagManager 测试")
class FeatureFlagManagerTest extends BaseUnitTest {

    private FeatureFlagProperties properties;

    @Mock
    private FeatureFlagManager.DistributedStorageClient storageClient;

    @BeforeEach
    void setUp() {
        properties = new FeatureFlagProperties();
    }

    @Test
    @DisplayName("内存模式 - 注册和获取功能开关")
    void memoryMode_registerAndGet() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        FeatureFlag flag = FeatureFlag.of("test-feature", true);
        manager.register(flag);

        assertThat(manager.getFeatureFlag("test-feature")).isEqualTo(flag);
    }

    @Test
    @DisplayName("内存模式 - 启用/禁用功能")
    void memoryMode_enableDisable() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        manager.register(FeatureFlag.of("toggle-feature", false));
        assertThat(manager.isEnabled("toggle-feature")).isFalse();

        manager.enable("toggle-feature");
        assertThat(manager.isEnabled("toggle-feature")).isTrue();

        manager.disable("toggle-feature");
        assertThat(manager.isEnabled("toggle-feature")).isFalse();
    }

    @Test
    @DisplayName("内存模式 - 设置灰度规则")
    void memoryMode_setGrayscaleRule() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        manager.register(FeatureFlag.of("rule-feature", true));
        GrayscaleRule rule = GrayscaleRule.ofUserWhitelist(Set.of(123L, 456L));
        manager.setGrayscaleRule("rule-feature", rule);

        FeatureFlag flag = manager.getFeatureFlag("rule-feature");
        assertThat(flag.grayscaleRule()).isEqualTo(rule);

        // 用户 123 应能访问
        assertThat(manager.isEnabled("rule-feature", GrayscaleContext.fromUserId(123L))).isTrue();

        // 用户 999 应不能访问
        assertThat(manager.isEnabled("rule-feature", GrayscaleContext.fromUserId(999L))).isFalse();
    }

    @Test
    @DisplayName("内存模式 - 删除功能开关")
    void memoryMode_remove() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        manager.register(FeatureFlag.of("remove-feature", true));
        assertThat(manager.getFeatureFlag("remove-feature")).isNotNull();

        FeatureFlag removed = manager.remove("remove-feature");
        assertThat(removed).isNotNull();
        assertThat(removed.name()).isEqualTo("remove-feature");
        assertThat(manager.getFeatureFlag("remove-feature")).isNull();
    }

    @Test
    @DisplayName("内存模式 - 批量注册")
    void memoryMode_registerAll() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        List<FeatureFlag> flags = List.of(
                FeatureFlag.of("feature-1", true),
                FeatureFlag.of("feature-2", false),
                FeatureFlag.of("feature-3", true));
        manager.registerAll(flags);

        assertThat(manager.getAllFeatureFlags()).hasSize(3);
        assertThat(manager.isEnabled("feature-1")).isTrue();
        assertThat(manager.isEnabled("feature-2")).isFalse();
        assertThat(manager.isEnabled("feature-3")).isTrue();
    }

    @Test
    @DisplayName("内存模式 - 未配置功能使用默认值")
    void memoryMode_unconfiguredFeature_defaultValue() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        // 未配置的功能，默认启用
        assertThat(manager.isEnabled("unconfigured", true)).isTrue();

        // 未配置的功能，默认禁用
        assertThat(manager.isEnabled("unconfigured", false)).isFalse();
    }

    @Test
    @DisplayName("分布式模式 - 注册时同步到存储")
    void distributedMode_register_shouldSyncToStorage() {
        properties.setStorageType(FeatureFlagProperties.StorageType.REDISSON);
        FeatureFlagManager manager = new FeatureFlagManager(properties, storageClient);

        FeatureFlag flag = FeatureFlag.of("distributed-feature", true);
        manager.register(flag);

        verify(storageClient).put("distributed-feature", flag);
    }

    @Test
    @DisplayName("分布式模式 - 获取时先从存储读取")
    void distributedMode_get_shouldReadFromStorageFirst() {
        properties.setStorageType(FeatureFlagProperties.StorageType.REDISSON);
        FeatureFlagManager manager = new FeatureFlagManager(properties, storageClient);

        FeatureFlag storedFlag = FeatureFlag.builder()
                .name("stored-feature")
                .enabled(true)
                .description("从存储获取")
                .build();
        when(storageClient.get("stored-feature")).thenReturn(storedFlag);

        FeatureFlag result = manager.getFeatureFlag("stored-feature");

        assertThat(result).isEqualTo(storedFlag);
        verify(storageClient).get("stored-feature");
    }

    @Test
    @DisplayName("分布式模式 - 删除时同步到存储")
    void distributedMode_remove_shouldSyncToStorage() {
        properties.setStorageType(FeatureFlagProperties.StorageType.REDISSON);
        FeatureFlagManager manager = new FeatureFlagManager(properties, storageClient);

        FeatureFlag flag = FeatureFlag.of("remove-distributed", true);
        manager.register(flag);
        manager.remove("remove-distributed");

        verify(storageClient).remove("remove-distributed");
    }

    @Test
    @DisplayName("分布式模式 - 刷新从存储重新加载")
    void distributedMode_refresh_shouldReloadFromStorage() {
        properties.setStorageType(FeatureFlagProperties.StorageType.REDISSON);
        FeatureFlagManager manager = new FeatureFlagManager(properties, storageClient);

        Map<String, FeatureFlag> storedFlags = Map.of(
                "refreshed-1", FeatureFlag.of("refreshed-1", true),
                "refreshed-2", FeatureFlag.of("refreshed-2", false));
        when(storageClient.getAll()).thenReturn(storedFlags);

        manager.refresh();

        verify(storageClient).getAll();
        assertThat(manager.getAllFeatureFlags()).hasSize(2);
    }

    @Test
    @DisplayName("内存模式 - 刷新不执行任何操作")
    void memoryMode_refresh_shouldDoNothing() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);
        manager.register(FeatureFlag.of("test", true));

        // 内存模式刷新不应抛异常
        manager.refresh();

        assertThat(manager.getAllFeatureFlags()).hasSize(1);
    }

    @Test
    @DisplayName("isEnabledFor - 灰度上下文正确传递")
    void isEnabledFor_grayscaleContext_shouldWork() {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        // 设置百分比灰度
        manager.register(FeatureFlag.of("percent-feature", GrayscaleRule.ofPercentage(0)));

        // 0% 灰度，任何用户都不应能访问
        assertThat(manager.isEnabled("percent-feature", GrayscaleContext.fromUserId(123L))).isFalse();

        // 设置 100% 灰度
        manager.setGrayscaleRule("percent-feature", GrayscaleRule.ofPercentage(100));
        assertThat(manager.isEnabled("percent-feature", GrayscaleContext.fromUserId(123L))).isTrue();
    }

    @Test
    @DisplayName("线程安全 - 并发操作")
    void threadSafe_concurrentOperations() throws InterruptedException {
        FeatureFlagManager manager = new FeatureFlagManager(properties);

        // 创建多个线程并发注册和查询
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String featureName = "concurrent-" + index;
                manager.register(FeatureFlag.of(featureName, true));
                manager.isEnabled(featureName);
                manager.disable(featureName);
                manager.isEnabled(featureName);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 所有功能应被禁用
        for (int i = 0; i < 10; i++) {
            assertThat(manager.isEnabled("concurrent-" + i)).isFalse();
        }
    }
}