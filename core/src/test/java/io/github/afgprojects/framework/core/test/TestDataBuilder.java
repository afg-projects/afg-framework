package io.github.afgprojects.framework.core.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.config.ConfigEntry;
import io.github.afgprojects.framework.core.config.ConfigSource;
import io.github.afgprojects.framework.core.module.ModuleDefinition;

/**
 * 测试数据构建器
 * 提供流式 API 构建测试数据
 *
 * <p>使用示例:
 * <pre>{@code
 * // 构建用户
 * TestDataBuilder.UserData user = TestDataBuilder.user()
 *     .id("user-001")
 *     .username("testuser")
 *     .email("test@example.com")
 *     .build();
 *
 * // 构建模块
 * ModuleDefinition module = TestDataBuilder.module()
 *     .id("module-001")
 *     .name("TestModule")
 *     .dependency("core")
 *     .buildDefinition();
 *
 * // 批量构建
 * List<TestDataBuilder.UserData> users = TestDataBuilder.user()
 *     .buildList(10, (builder, i) -> builder.id("user-" + i).username("user" + i));
 * }</pre>
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 创建用户数据构建器
     */
    public static UserDataBuilder user() {
        return new UserDataBuilder();
    }

    /**
     * 创建租户数据构建器
     */
    public static TenantDataBuilder tenant() {
        return new TenantDataBuilder();
    }

    /**
     * 创建模块数据构建器
     */
    public static ModuleDataBuilder module() {
        return new ModuleDataBuilder();
    }

    /**
     * 创建配置数据构建器
     */
    public static ConfigDataBuilder config() {
        return new ConfigDataBuilder();
    }

    /**
     * 用户数据构建器
     */
    public static class UserDataBuilder {

        private String id = UUID.randomUUID().toString();
        private String username = "testuser";
        private String email = "test@example.com";
        private String phone = "13800138000";
        private String tenantId = "default";
        private boolean enabled = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        private Map<String, Object> attributes = new HashMap<>();

        public UserDataBuilder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        public UserDataBuilder username(@NonNull String username) {
            this.username = username;
            return this;
        }

        public UserDataBuilder email(@NonNull String email) {
            this.email = email;
            return this;
        }

        public UserDataBuilder phone(@NonNull String phone) {
            this.phone = phone;
            return this;
        }

        public UserDataBuilder tenantId(@NonNull String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public UserDataBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserDataBuilder createdAt(@NonNull LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDataBuilder attribute(@NonNull String key, @Nullable Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public UserDataBuilder attributes(@NonNull Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        /**
         * 构建 UserData 实例
         */
        public UserData build() {
            return new UserData(id, username, email, phone, tenantId, enabled, createdAt, attributes);
        }

        /**
         * 构建多个 UserData 实例
         *
         * @param count     数量
         * @param configurator 配置回调
         * @return UserData 列表
         */
        public List<UserData> buildList(int count, @NonNull BiConsumer<UserDataBuilder, Integer> configurator) {
            List<UserData> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                UserDataBuilder builder = new UserDataBuilder();
                configurator.accept(builder, i);
                list.add(builder.build());
            }
            return list;
        }
    }

    /**
     * 用户数据
     */
    public record UserData(
            String id,
            String username,
            String email,
            String phone,
            String tenantId,
            boolean enabled,
            LocalDateTime createdAt,
            Map<String, Object> attributes) {

        public UserData {
            attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        }
    }

    /**
     * 租户数据构建器
     */
    public static class TenantDataBuilder {

        private String id = UUID.randomUUID().toString();
        private String name = "Default Tenant";
        private String code = "default";
        private boolean enabled = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        private Map<String, Object> config = new HashMap<>();

        public TenantDataBuilder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        public TenantDataBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        public TenantDataBuilder code(@NonNull String code) {
            this.code = code;
            return this;
        }

        public TenantDataBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public TenantDataBuilder createdAt(@NonNull LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TenantDataBuilder config(@NonNull String key, @Nullable Object value) {
            this.config.put(key, value);
            return this;
        }

        public TenantDataBuilder configs(@NonNull Map<String, Object> config) {
            this.config.putAll(config);
            return this;
        }

        /**
         * 构建 TenantData 实例
         */
        public TenantData build() {
            return new TenantData(id, name, code, enabled, createdAt, config);
        }

        /**
         * 构建多个 TenantData 实例
         */
        public List<TenantData> buildList(int count, @NonNull BiConsumer<TenantDataBuilder, Integer> configurator) {
            List<TenantData> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                TenantDataBuilder builder = new TenantDataBuilder();
                configurator.accept(builder, i);
                list.add(builder.build());
            }
            return list;
        }
    }

    /**
     * 租户数据
     */
    public record TenantData(
            String id,
            String name,
            String code,
            boolean enabled,
            LocalDateTime createdAt,
            Map<String, Object> config) {

        public TenantData {
            config = config != null ? new HashMap<>(config) : new HashMap<>();
        }
    }

    /**
     * 模块数据构建器
     */
    public static class ModuleDataBuilder {

        private String id = "test-module";
        private String name = "Test Module";
        private List<String> dependencies = new ArrayList<>();
        private Object moduleInstance = null;

        public ModuleDataBuilder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        public ModuleDataBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        public ModuleDataBuilder dependency(@NonNull String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public ModuleDataBuilder dependencies(@NonNull List<String> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public ModuleDataBuilder moduleInstance(@Nullable Object moduleInstance) {
            this.moduleInstance = moduleInstance;
            return this;
        }

        /**
         * 构建 ModuleDefinition 实例
         */
        public ModuleDefinition buildDefinition() {
            return ModuleDefinition.builder()
                    .id(id)
                    .name(name)
                    .dependencies(dependencies)
                    .moduleInstance(null)
                    .build();
        }

        /**
         * 构建多个 ModuleDefinition 实例
         */
        public List<ModuleDefinition> buildDefinitionList(int count, @NonNull BiConsumer<ModuleDataBuilder, Integer> configurator) {
            List<ModuleDefinition> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ModuleDataBuilder builder = new ModuleDataBuilder();
                configurator.accept(builder, i);
                list.add(builder.buildDefinition());
            }
            return list;
        }
    }

    /**
     * 配置数据构建器
     */
    public static class ConfigDataBuilder {

        private ConfigSource source = ConfigSource.CURRENT_CONFIG;
        private String prefix = "test.prefix";
        private Object value = "test-value";
        private long loadedAt = System.currentTimeMillis();

        public ConfigDataBuilder source(@NonNull ConfigSource source) {
            this.source = source;
            return this;
        }

        public ConfigDataBuilder prefix(@NonNull String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ConfigDataBuilder value(@Nullable Object value) {
            this.value = value;
            return this;
        }

        public ConfigDataBuilder loadedAt(long loadedAt) {
            this.loadedAt = loadedAt;
            return this;
        }

        public ConfigDataBuilder loadedAtNow() {
            this.loadedAt = System.currentTimeMillis();
            return this;
        }

        /**
         * 构建 ConfigEntry 实例
         */
        public ConfigEntry build() {
            return ConfigEntry.builder()
                    .source(source)
                    .prefix(prefix)
                    .value(value)
                    .loadedAt(loadedAt)
                    .build();
        }

        /**
         * 构建多个 ConfigEntry 实例
         */
        public List<ConfigEntry> buildList(int count, @NonNull BiConsumer<ConfigDataBuilder, Integer> configurator) {
            List<ConfigEntry> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ConfigDataBuilder builder = new ConfigDataBuilder();
                configurator.accept(builder, i);
                list.add(builder.build());
            }
            return list;
        }
    }
}
