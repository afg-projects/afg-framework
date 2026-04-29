package io.github.afgprojects.framework.core.test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.github.afgprojects.framework.core.config.ConfigEntry;
import io.github.afgprojects.framework.core.config.ConfigSource;
import io.github.afgprojects.framework.core.module.ModuleDefinition;

/**
 * 测试固件
 * 提供常用的预定义测试数据
 *
 * <p>使用示例:
 * <pre>{@code
 * // 获取默认用户
 * TestDataBuilder.UserData user = TestFixtures.DEFAULT_USER;
 *
 * // 获取测试模块列表
 * List<ModuleDefinition> modules = TestFixtures.TEST_MODULES;
 *
 * // 获取测试配置
 * ConfigEntry config = TestFixtures.TEST_CONFIG;
 * }</pre>
 */
public final class TestFixtures {

    private TestFixtures() {
        // 工具类，禁止实例化
    }

    // ==================== 用户数据 ====================

    /**
     * 默认测试用户
     */
    public static final TestDataBuilder.UserData DEFAULT_USER = TestDataBuilder.user()
            .id("user-001")
            .username("testuser")
            .email("testuser@example.com")
            .phone("13800138001")
            .tenantId("tenant-001")
            .enabled(true)
            .build();

    /**
     * 管理员用户
     */
    public static final TestDataBuilder.UserData ADMIN_USER = TestDataBuilder.user()
            .id("user-admin")
            .username("admin")
            .email("admin@example.com")
            .phone("13800138002")
            .tenantId("tenant-001")
            .enabled(true)
            .attribute("role", "ADMIN")
            .build();

    /**
     * 已禁用用户
     */
    public static final TestDataBuilder.UserData DISABLED_USER = TestDataBuilder.user()
            .id("user-disabled")
            .username("disabled")
            .email("disabled@example.com")
            .phone("13800138003")
            .tenantId("tenant-001")
            .enabled(false)
            .build();

    /**
     * 系统用户列表
     */
    public static final List<TestDataBuilder.UserData> TEST_USERS = List.of(DEFAULT_USER, ADMIN_USER, DISABLED_USER);

    // ==================== 租户数据 ====================

    /**
     * 默认租户
     */
    public static final TestDataBuilder.TenantData DEFAULT_TENANT = TestDataBuilder.tenant()
            .id("tenant-001")
            .name("Default Tenant")
            .code("default")
            .enabled(true)
            .build();

    /**
     * 测试租户
     */
    public static final TestDataBuilder.TenantData TEST_TENANT = TestDataBuilder.tenant()
            .id("tenant-002")
            .name("Test Tenant")
            .code("test")
            .enabled(true)
            .config("maxUsers", 100)
            .config("features", Map.of("feature1", true, "feature2", false))
            .build();

    /**
     * 已禁用租户
     */
    public static final TestDataBuilder.TenantData DISABLED_TENANT = TestDataBuilder.tenant()
            .id("tenant-003")
            .name("Disabled Tenant")
            .code("disabled")
            .enabled(false)
            .build();

    /**
     * 租户列表
     */
    public static final List<TestDataBuilder.TenantData> TEST_TENANTS =
            List.of(DEFAULT_TENANT, TEST_TENANT, DISABLED_TENANT);

    // ==================== 模块数据 ====================

    /**
     * 核心模块
     */
    public static final ModuleDefinition CORE_MODULE = ModuleDefinition.builder()
            .id("core")
            .name("Core Module")
            .dependencies(List.of())
            .build();

    /**
     * 认证模块（依赖核心模块）
     */
    public static final ModuleDefinition AUTH_MODULE = ModuleDefinition.builder()
            .id("auth")
            .name("Auth Module")
            .dependencies(List.of("core"))
            .build();

    /**
     * 业务模块（依赖认证模块）
     */
    public static final ModuleDefinition BUSINESS_MODULE = ModuleDefinition.builder()
            .id("business")
            .name("Business Module")
            .dependencies(List.of("auth"))
            .build();

    /**
     * 测试模块列表（按依赖顺序）
     */
    public static final List<ModuleDefinition> TEST_MODULES = List.of(CORE_MODULE, AUTH_MODULE, BUSINESS_MODULE);

    /**
     * 带循环依赖的模块列表（用于测试异常情况）
     */
    public static final List<ModuleDefinition> CIRCULAR_DEPENDENCY_MODULES = List.of(
            ModuleDefinition.builder().id("module-a").name("Module A").dependencies(List.of("module-b")).build(),
            ModuleDefinition.builder().id("module-b").name("Module B").dependencies(List.of("module-a")).build());

    // ==================== 配置数据 ====================

    /**
     * 测试配置
     */
    public static final ConfigEntry TEST_CONFIG = ConfigEntry.builder()
            .source(ConfigSource.CURRENT_CONFIG)
            .prefix("test.key")
            .value("test-value")
            .loadedAt(System.currentTimeMillis())
            .build();

    /**
     * 数据库配置
     */
    public static final ConfigEntry DB_CONFIG = ConfigEntry.builder()
            .source(ConfigSource.CURRENT_CONFIG)
            .prefix("database.url")
            .value("jdbc:h2:mem:testdb")
            .loadedAt(System.currentTimeMillis())
            .build();

    /**
     * 环境变量配置
     */
    public static final ConfigEntry ENV_CONFIG = ConfigEntry.builder()
            .source(ConfigSource.ENVIRONMENT)
            .prefix("app.env")
            .value("test")
            .loadedAt(System.currentTimeMillis())
            .build();

    /**
     * 配置中心配置
     */
    public static final ConfigEntry CONFIG_CENTER_ENTRY = ConfigEntry.builder()
            .source(ConfigSource.CONFIG_CENTER)
            .prefix("dynamic.feature.enabled")
            .value(true)
            .loadedAt(System.currentTimeMillis())
            .build();

    /**
     * 测试配置列表
     */
    public static final List<ConfigEntry> TEST_CONFIGS = List.of(TEST_CONFIG, DB_CONFIG, ENV_CONFIG, CONFIG_CENTER_ENTRY);

    // ==================== 时间数据 ====================

    /**
     * 固定的测试时间点
     */
    public static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

    /**
     * 过去时间
     */
    public static final LocalDateTime PAST_TIME = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

    /**
     * 未来时间
     */
    public static final LocalDateTime FUTURE_TIME = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    // ==================== 字符串数据 ====================

    /**
     * 空 JSON 对象
     */
    public static final String EMPTY_JSON = "{}";

    /**
     * 空数组
     */
    public static final String EMPTY_ARRAY = "[]";

    /**
     * 测试用户 JSON
     */
    public static final String USER_JSON = """
            {
              "id": "user-001",
              "username": "testuser",
              "email": "testuser@example.com",
              "phone": "13800138001",
              "tenantId": "tenant-001",
              "enabled": true
            }
            """;

    /**
     * 测试结果 JSON
     */
    public static final String RESULT_SUCCESS_JSON = """
            {
              "code": 0,
              "message": "success",
              "data": null
            }
            """;

    /**
     * 测试失败结果 JSON
     */
    public static final String RESULT_FAIL_JSON = """
            {
              "code": 10001,
              "message": "参数错误"
            }
            """;

    // ==================== 分页数据 ====================

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页数量
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 大页码
     */
    public static final int LARGE_PAGE_SIZE = 100;

    // ==================== 工具方法 ====================

    /**
     * 创建带随机 ID 的用户
     */
    public static TestDataBuilder.UserData randomUser() {
        return TestDataBuilder.user().build();
    }

    /**
     * 创建带随机 ID 的租户
     */
    public static TestDataBuilder.TenantData randomTenant() {
        return TestDataBuilder.tenant().build();
    }

    /**
     * 创建带随机 ID 的模块
     */
    public static ModuleDefinition randomModule() {
        return TestDataBuilder.module().buildDefinition();
    }

    /**
     * 创建带随机前缀的配置
     */
    public static ConfigEntry randomConfig() {
        return TestDataBuilder.config().build();
    }
}
