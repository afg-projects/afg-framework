package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.ConfigSource;
import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * TestFixtures 测试
 */
class TestFixturesTest extends BaseUnitTest {

    @Test
    @DisplayName("默认用户固件")
    void shouldHaveDefaultUser() {
        assertThat(TestFixtures.DEFAULT_USER).isNotNull();
        assertThat(TestFixtures.DEFAULT_USER.id()).isEqualTo("user-001");
        assertThat(TestFixtures.DEFAULT_USER.username()).isEqualTo("testuser");
        assertThat(TestFixtures.DEFAULT_USER.enabled()).isTrue();
    }

    @Test
    @DisplayName("管理员用户固件")
    void shouldHaveAdminUser() {
        assertThat(TestFixtures.ADMIN_USER).isNotNull();
        assertThat(TestFixtures.ADMIN_USER.username()).isEqualTo("admin");
        assertThat(TestFixtures.ADMIN_USER.attributes()).containsEntry("role", "ADMIN");
    }

    @Test
    @DisplayName("已禁用用户固件")
    void shouldHaveDisabledUser() {
        assertThat(TestFixtures.DISABLED_USER).isNotNull();
        assertThat(TestFixtures.DISABLED_USER.enabled()).isFalse();
    }

    @Test
    @DisplayName("测试用户列表")
    void shouldHaveTestUsersList() {
        assertThat(TestFixtures.TEST_USERS).hasSize(3);
        assertThat(TestFixtures.TEST_USERS).contains(TestFixtures.DEFAULT_USER);
        assertThat(TestFixtures.TEST_USERS).contains(TestFixtures.ADMIN_USER);
        assertThat(TestFixtures.TEST_USERS).contains(TestFixtures.DISABLED_USER);
    }

    @Test
    @DisplayName("默认租户固件")
    void shouldHaveDefaultTenant() {
        assertThat(TestFixtures.DEFAULT_TENANT).isNotNull();
        assertThat(TestFixtures.DEFAULT_TENANT.code()).isEqualTo("default");
        assertThat(TestFixtures.DEFAULT_TENANT.enabled()).isTrue();
    }

    @Test
    @DisplayName("测试租户固件")
    void shouldHaveTestTenant() {
        assertThat(TestFixtures.TEST_TENANT).isNotNull();
        assertThat(TestFixtures.TEST_TENANT.code()).isEqualTo("test");
        assertThat(TestFixtures.TEST_TENANT.config()).containsKey("maxUsers");
    }

    @Test
    @DisplayName("已禁用租户固件")
    void shouldHaveDisabledTenant() {
        assertThat(TestFixtures.DISABLED_TENANT).isNotNull();
        assertThat(TestFixtures.DISABLED_TENANT.enabled()).isFalse();
    }

    @Test
    @DisplayName("测试租户列表")
    void shouldHaveTestTenantsList() {
        assertThat(TestFixtures.TEST_TENANTS).hasSize(3);
    }

    @Test
    @DisplayName("核心模块固件")
    void shouldHaveCoreModule() {
        assertThat(TestFixtures.CORE_MODULE).isNotNull();
        assertThat(TestFixtures.CORE_MODULE.id()).isEqualTo("core");
        assertThat(TestFixtures.CORE_MODULE.dependencies()).isEmpty();
    }

    @Test
    @DisplayName("认证模块固件")
    void shouldHaveAuthModule() {
        assertThat(TestFixtures.AUTH_MODULE).isNotNull();
        assertThat(TestFixtures.AUTH_MODULE.id()).isEqualTo("auth");
        assertThat(TestFixtures.AUTH_MODULE.dependencies()).containsExactly("core");
    }

    @Test
    @DisplayName("业务模块固件")
    void shouldHaveBusinessModule() {
        assertThat(TestFixtures.BUSINESS_MODULE).isNotNull();
        assertThat(TestFixtures.BUSINESS_MODULE.id()).isEqualTo("business");
        assertThat(TestFixtures.BUSINESS_MODULE.dependencies()).containsExactly("auth");
    }

    @Test
    @DisplayName("测试模块列表")
    void shouldHaveTestModulesList() {
        assertThat(TestFixtures.TEST_MODULES).hasSize(3);
        // 验证依赖顺序
        assertThat(TestFixtures.TEST_MODULES.get(0)).isEqualTo(TestFixtures.CORE_MODULE);
        assertThat(TestFixtures.TEST_MODULES.get(1)).isEqualTo(TestFixtures.AUTH_MODULE);
        assertThat(TestFixtures.TEST_MODULES.get(2)).isEqualTo(TestFixtures.BUSINESS_MODULE);
    }

    @Test
    @DisplayName("循环依赖模块列表")
    void shouldHaveCircularDependencyModules() {
        List<ModuleDefinition> modules = TestFixtures.CIRCULAR_DEPENDENCY_MODULES;
        assertThat(modules).hasSize(2);

        // 验证循环依赖：module-a -> module-b -> module-a
        ModuleDefinition moduleA = modules.get(0);
        ModuleDefinition moduleB = modules.get(1);
        assertThat(moduleA.dependencies()).contains("module-b");
        assertThat(moduleB.dependencies()).contains("module-a");
    }

    @Test
    @DisplayName("测试配置固件")
    void shouldHaveTestConfig() {
        assertThat(TestFixtures.TEST_CONFIG).isNotNull();
        assertThat(TestFixtures.TEST_CONFIG.source()).isEqualTo(ConfigSource.CURRENT_CONFIG);
        assertThat(TestFixtures.TEST_CONFIG.prefix()).isEqualTo("test.key");
    }

    @Test
    @DisplayName("数据库配置固件")
    void shouldHaveDbConfig() {
        assertThat(TestFixtures.DB_CONFIG).isNotNull();
        assertThat(TestFixtures.DB_CONFIG.prefix()).isEqualTo("database.url");
        assertThat(TestFixtures.DB_CONFIG.value()).isEqualTo("jdbc:h2:mem:testdb");
    }

    @Test
    @DisplayName("环境变量配置固件")
    void shouldHaveEnvConfig() {
        assertThat(TestFixtures.ENV_CONFIG).isNotNull();
        assertThat(TestFixtures.ENV_CONFIG.source()).isEqualTo(ConfigSource.ENVIRONMENT);
    }

    @Test
    @DisplayName("配置中心配置固件")
    void shouldHaveConfigCenterEntry() {
        assertThat(TestFixtures.CONFIG_CENTER_ENTRY).isNotNull();
        assertThat(TestFixtures.CONFIG_CENTER_ENTRY.source()).isEqualTo(ConfigSource.CONFIG_CENTER);
    }

    @Test
    @DisplayName("测试配置列表")
    void shouldHaveTestConfigsList() {
        assertThat(TestFixtures.TEST_CONFIGS).hasSize(4);
    }

    @Test
    @DisplayName("时间固件")
    void shouldHaveTimeFixtures() {
        assertThat(TestFixtures.FIXED_TIME).isNotNull();
        assertThat(TestFixtures.PAST_TIME).isBefore(TestFixtures.FIXED_TIME);
        assertThat(TestFixtures.FUTURE_TIME).isAfter(TestFixtures.FIXED_TIME);
    }

    @Test
    @DisplayName("JSON 字符串固件")
    void shouldHaveJsonFixtures() {
        assertThat(TestFixtures.EMPTY_JSON).isEqualTo("{}");
        assertThat(TestFixtures.EMPTY_ARRAY).isEqualTo("[]");
        assertThat(TestFixtures.USER_JSON).contains("testuser");
        assertThat(TestFixtures.RESULT_SUCCESS_JSON).contains("\"code\": 0");
        assertThat(TestFixtures.RESULT_FAIL_JSON).contains("\"code\": 10001");
    }

    @Test
    @DisplayName("分页参数固件")
    void shouldHavePaginationFixtures() {
        assertThat(TestFixtures.DEFAULT_PAGE).isEqualTo(1);
        assertThat(TestFixtures.DEFAULT_PAGE_SIZE).isEqualTo(10);
        assertThat(TestFixtures.LARGE_PAGE_SIZE).isEqualTo(100);
    }

    @Test
    @DisplayName("创建随机用户")
    void shouldCreateRandomUser() {
        TestDataBuilder.UserData user1 = TestFixtures.randomUser();
        TestDataBuilder.UserData user2 = TestFixtures.randomUser();

        assertThat(user1).isNotNull();
        assertThat(user2).isNotNull();
        // 随机 ID 应该不同
        assertThat(user1.id()).isNotEqualTo(user2.id());
    }

    @Test
    @DisplayName("创建随机租户")
    void shouldCreateRandomTenant() {
        TestDataBuilder.TenantData tenant = TestFixtures.randomTenant();
        assertThat(tenant).isNotNull();
        assertThat(tenant.id()).isNotNull();
    }

    @Test
    @DisplayName("创建随机模块")
    void shouldCreateRandomModule() {
        ModuleDefinition module = TestFixtures.randomModule();
        assertThat(module).isNotNull();
        assertThat(module.id()).isNotNull();
    }

    @Test
    @DisplayName("创建随机配置")
    void shouldCreateRandomConfig() {
        var config = TestFixtures.randomConfig();
        assertThat(config).isNotNull();
        assertThat(config.prefix()).isNotNull();
    }
}