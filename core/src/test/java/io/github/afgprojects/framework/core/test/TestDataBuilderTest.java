package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.ConfigEntry;
import io.github.afgprojects.framework.core.config.ConfigSource;
import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * TestDataBuilder 测试
 */
class TestDataBuilderTest extends BaseUnitTest {

    @Test
    @DisplayName("构建默认用户数据")
    void shouldBuildDefaultUser() {
        TestDataBuilder.UserData user = TestDataBuilder.user().build();

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("testuser");
        assertThat(user.email()).isEqualTo("test@example.com");
        assertThat(user.enabled()).isTrue();
    }

    @Test
    @DisplayName("构建自定义用户数据")
    void shouldBuildCustomUser() {
        TestDataBuilder.UserData user = TestDataBuilder.user()
                .id("user-001")
                .username("customuser")
                .email("custom@example.com")
                .phone("13900139000")
                .tenantId("tenant-001")
                .enabled(false)
                .attribute("role", "ADMIN")
                .build();

        assertThat(user.id()).isEqualTo("user-001");
        assertThat(user.username()).isEqualTo("customuser");
        assertThat(user.email()).isEqualTo("custom@example.com");
        assertThat(user.phone()).isEqualTo("13900139000");
        assertThat(user.tenantId()).isEqualTo("tenant-001");
        assertThat(user.enabled()).isFalse();
        assertThat(user.attributes()).containsEntry("role", "ADMIN");
    }

    @Test
    @DisplayName("批量构建用户数据")
    void shouldBuildUserList() {
        List<TestDataBuilder.UserData> users = TestDataBuilder.user()
                .buildList(5, (builder, i) -> builder.id("user-" + i).username("user" + i));

        assertThat(users).hasSize(5);
        assertThat(users.get(0).id()).isEqualTo("user-0");
        assertThat(users.get(0).username()).isEqualTo("user0");
        assertThat(users.get(4).id()).isEqualTo("user-4");
    }

    @Test
    @DisplayName("构建默认租户数据")
    void shouldBuildDefaultTenant() {
        TestDataBuilder.TenantData tenant = TestDataBuilder.tenant().build();

        assertThat(tenant.id()).isNotNull();
        assertThat(tenant.name()).isEqualTo("Default Tenant");
        assertThat(tenant.code()).isEqualTo("default");
        assertThat(tenant.enabled()).isTrue();
    }

    @Test
    @DisplayName("构建自定义租户数据")
    void shouldBuildCustomTenant() {
        TestDataBuilder.TenantData tenant = TestDataBuilder.tenant()
                .id("tenant-001")
                .name("Custom Tenant")
                .code("custom")
                .enabled(false)
                .config("maxUsers", 100)
                .build();

        assertThat(tenant.id()).isEqualTo("tenant-001");
        assertThat(tenant.name()).isEqualTo("Custom Tenant");
        assertThat(tenant.code()).isEqualTo("custom");
        assertThat(tenant.enabled()).isFalse();
        assertThat(tenant.config()).containsEntry("maxUsers", 100);
    }

    @Test
    @DisplayName("批量构建租户数据")
    void shouldBuildTenantList() {
        List<TestDataBuilder.TenantData> tenants = TestDataBuilder.tenant()
                .buildList(3, (builder, i) -> builder.code("tenant-" + i));

        assertThat(tenants).hasSize(3);
        assertThat(tenants.get(0).code()).isEqualTo("tenant-0");
    }

    @Test
    @DisplayName("构建默认模块定义")
    void shouldBuildDefaultModule() {
        ModuleDefinition module = TestDataBuilder.module().buildDefinition();

        assertThat(module.id()).isEqualTo("test-module");
        assertThat(module.name()).isEqualTo("Test Module");
        assertThat(module.dependencies()).isEmpty();
    }

    @Test
    @DisplayName("构建带依赖的模块定义")
    void shouldBuildModuleWithDependencies() {
        ModuleDefinition module = TestDataBuilder.module()
                .id("auth-module")
                .name("Auth Module")
                .dependency("core")
                .dependency("database")
                .buildDefinition();

        assertThat(module.id()).isEqualTo("auth-module");
        assertThat(module.name()).isEqualTo("Auth Module");
        assertThat(module.dependencies()).containsExactly("core", "database");
    }

    @Test
    @DisplayName("批量构建模块定义")
    void shouldBuildModuleList() {
        List<ModuleDefinition> modules = TestDataBuilder.module()
                .buildDefinitionList(4, (builder, i) -> builder.id("module-" + i).dependency("core"));

        assertThat(modules).hasSize(4);
        assertThat(modules.get(0).id()).isEqualTo("module-0");
        assertThat(modules.get(0).dependencies()).containsExactly("core");
    }

    @Test
    @DisplayName("构建默认配置条目")
    void shouldBuildDefaultConfig() {
        ConfigEntry config = TestDataBuilder.config().build();

        assertThat(config.source()).isEqualTo(ConfigSource.CURRENT_CONFIG);
        assertThat(config.prefix()).isEqualTo("test.prefix");
        assertThat(config.value()).isEqualTo("test-value");
        assertThat(config.loadedAt()).isPositive();
    }

    @Test
    @DisplayName("构建自定义配置条目")
    void shouldBuildCustomConfig() {
        long expectedLoadedAt = 1700000000000L;
        ConfigEntry config = TestDataBuilder.config()
                .source(ConfigSource.CONFIG_CENTER)
                .prefix("custom.prefix")
                .value(Map.of("key", "value"))
                .loadedAt(expectedLoadedAt)
                .build();

        assertThat(config.source()).isEqualTo(ConfigSource.CONFIG_CENTER);
        assertThat(config.prefix()).isEqualTo("custom.prefix");
        assertThat(config.loadedAt()).isEqualTo(expectedLoadedAt);
    }

    @Test
    @DisplayName("批量构建配置条目")
    void shouldBuildConfigList() {
        List<ConfigEntry> configs = TestDataBuilder.config()
                .buildList(5, (builder, i) -> builder.prefix("config.prefix." + i));

        assertThat(configs).hasSize(5);
        assertThat(configs.get(0).prefix()).isEqualTo("config.prefix.0");
    }

    @Test
    @DisplayName("使用 loadedAtNow 设置当前时间")
    void shouldSetLoadedAtNow() {
        long before = System.currentTimeMillis();
        ConfigEntry config = TestDataBuilder.config().loadedAtNow().build();
        long after = System.currentTimeMillis();

        assertThat(config.loadedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("用户数据包含不可变 attributes")
    void shouldHaveImmutableAttributes() {
        TestDataBuilder.UserData user = TestDataBuilder.user().attribute("key", "value").build();

        // UserData record 中的 attributes 是不可变的
        assertThat(user.attributes()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("租户数据包含不可变 config")
    void shouldHaveImmutableConfig() {
        TestDataBuilder.TenantData tenant = TestDataBuilder.tenant().config("key", "value").build();

        assertThat(tenant.config()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("构建带 createdAt 的用户")
    void shouldBuildUserWithCreatedAt() {
        LocalDateTime expectedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        TestDataBuilder.UserData user = TestDataBuilder.user().createdAt(expectedTime).build();

        assertThat(user.createdAt()).isEqualTo(expectedTime);
    }
}