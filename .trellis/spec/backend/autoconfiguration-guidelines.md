# 自动配置规范

> 来源：CLAUDE.md 自动配置规范、PRD 1.5 / 3.3 / 6.6、附录 A

---

## 核心原则

框架所有自动配置遵循统一的启用哲学：

```
引入依赖即生效 → 有 Bean 即增强 → 注解即启用 → 配置只覆盖默认值
```

这意味着：开发者只需添加 Maven/Gradle 依赖，框架功能即可自动可用；引入额外的实现依赖（如 Redis）后自动升级为生产级实现；无需编写任何配置即可运行。

### 启用优先级

| 优先级 | 启用方式 | 说明 | 示例 |
|--------|---------|------|------|
| 1（最高） | 自动装配 | `@ConditionalOnClass` / `@ConditionalOnBean` 自动检测 | 引入 `afg-framework-data-jdbc` → DataManager 自动注册 |
| 2 | 注解启用 | 框架注解自动触发功能装配 | `@AiChat` 触发 Chat 客户端装配 |
| 3（最低） | 配置属性 | 仅覆盖默认值时需要 | `afg.ai.rag.similarity-threshold: 0.8` |

---

## AutoConfiguration 编写铁律

### 1. 必须使用 @AutoConfiguration（非 @Configuration）

所有框架自动配置类必须使用 Spring Boot 3.x+ 的 `@AutoConfiguration` 注解，而非 `@Configuration`。`@AutoConfiguration` 是 Spring Boot 自动配置机制的正式入口，它确保类被正确注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

```java
// 正确
@AutoConfiguration(after = {DataSourceAutoConfiguration.class})
@ConditionalOnClass(DataSource.class)
public class DataManagerAutoConfiguration { }

// 错误 — 禁止使用 @Configuration 作为自动配置类
@Configuration
public class DataManagerAutoConfiguration { }
```

### 2. 必须声明 @AutoConfigureAfter / @AutoConfigureBefore

每个 AutoConfiguration 必须声明其依赖排序，确保 Bean 创建顺序正确。未声明排序会导致 Bean 创建时依赖尚未就绪，产生 `NullPointerException` 或 `NoSuchBeanDefinitionException`。

```java
// 正确 — 声明依赖排序
@AutoConfiguration(after = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class DataManagerAutoConfiguration { }

// 错误 — 不声明排序，Bean 创建顺序不确定
@AutoConfiguration
public class DataManagerAutoConfiguration { }
```

### 3. 跨模块引用使用 afterName 字符串引用

跨模块引用其他模块的 AutoConfiguration 时，必须使用 `@AutoConfigureAfter(afterName = "...")` 字符串引用，而非 Class 引用。Class 引用需要编译期依赖，导致模块间耦合。

```java
// 正确 — 字符串引用，不需要编译期依赖
@AutoConfiguration(afterName = {
    "io.github.afgprojects.framework.data.core.autoconfigure.TenantContextAutoConfiguration",
    "io.github.afgprojects.framework.data.core.autoconfigure.TransactionAutoConfiguration"
})
public class DataManagerAutoConfiguration { }

// 错误 — Class 引用需要额外的 compileOnly 依赖
@AutoConfiguration(after = {
    TenantContextAutoConfiguration.class,  // 需要 compileOnly 依赖
    TransactionAutoConfiguration.class     // 需要 compileOnly 依赖
})
public class DataManagerAutoConfiguration { }
```

### 4. 可替换组件必须使用 @ConditionalOnMissingBean

框架提供的默认实现必须标记 `@ConditionalOnMissingBean`，允许业务应用替换为自定义实现。这是"有 Bean 即增强"原则的实现基础。

```java
@Bean
@ConditionalOnMissingBean(CacheManager.class)
public CacheManager cacheManager(CacheProperties properties,
                                 @Nullable CacheStorageProvider storageProvider) {
    DefaultCacheManager manager = new DefaultCacheManager(properties);
    if (storageProvider != null) {
        manager.setCacheStorageProvider(storageProvider);
    }
    return manager;
}
```

### 5. 可注入协作组件使用 @Nullable 参数注入

AutoConfiguration 创建的 Bean 若依赖可替换的协作组件，必须在 AutoConfiguration 方法参数中使用 `@Nullable` 注入，而非在实现类内部 `new`。实现类保留默认实例用于向后兼容。

**AutoConfiguration 注入协作 Bean：**

```java
@Bean
@ConditionalOnMissingBean(JdbcDataManager.class)
public JdbcDataManager dataManager(DataSource ds,
                                   @Nullable TenantContextHolder tenantContextHolder,
                                   @Nullable TransactionAdapter transactionAdapter) {
    JdbcDataManager dm = new JdbcDataManager(ds);
    if (tenantContextHolder != null) dm.setTenantContextHolder(tenantContextHolder);
    if (transactionAdapter != null) dm.setTransactionAdapter(transactionAdapter);
    return dm;
}
```

**实现类保留默认实例向后兼容：**

```java
public class JdbcDataManager {
    private TenantContextHolder tenantContextHolder;  // 非 final，可注入

    public JdbcDataManager(DataSource ds) {
        this.tenantContextHolder = new TenantContextHolder();  // 默认实例
    }

    public void setTenantContextHolder(TenantContextHolder holder) {
        this.tenantContextHolder = holder;  // AutoConfiguration 注入时覆盖
    }
}
```

---

## AutoConfiguration 依赖链

框架的 AutoConfiguration 遵循严格的依赖顺序，确保每个 Bean 创建时其前置依赖已就绪：

```
DataSourceAutoConfiguration (Spring Boot)
  └→ DataSourceTransactionManagerAutoConfiguration (Spring Boot)
       ├→ TransactionAutoConfiguration (data-core) — TransactionAdapter
       └→ TenantContextAutoConfiguration (data-core) — TenantContextHolder
            └→ DataManagerAutoConfiguration (data-jdbc) — JdbcDataManager
                 └→ LiquibaseAutoConfiguration (data-liquibase) — SpringLiquibase
```

**关键规则：**
- 依赖方向从底层到上层，禁止反向依赖和循环依赖
- 每层只依赖其直接前驱，不跨层依赖
- 跨模块使用 `afterName` 字符串引用

---

## AutoConfiguration 完整清单

框架共包含 80+ 个 AutoConfiguration，按模块分布如下：

| 模块 | 数量 | 关键 AutoConfiguration |
|------|------|----------------------|
| core | 31+ | `AfgCoreAutoConfiguration`, `CacheAutoConfiguration`, `LockAutoConfiguration`, `EventAutoConfiguration`, `SchedulerAutoConfiguration`, `AuditLogAutoConfiguration`, `ModuleWebAutoConfiguration` 等 |
| data-core | 2 | `TenantContextAutoConfiguration`, `TransactionAutoConfiguration` |
| data-jdbc | 4 | `DataManagerAutoConfiguration`, `EntityCacheAutoConfiguration`, `SqlMetricsAutoConfiguration` |
| data-liquibase | 1 | `LiquibaseAutoConfiguration` |
| ai-core | 16 | `AiCoreAutoConfiguration`, `AiChatAutoConfiguration`, `AiAgentAutoConfiguration`, `AiWorkflowAutoConfiguration`, `AiRagAutoConfiguration` 等 |
| ai-langchain4j | 7 | `Lc4jChatAutoConfiguration`, `Lc4jEmbeddingAutoConfiguration`, `Lc4jModelAutoConfiguration` 等 |
| ai-spring-ai | 7 | `SpringAiChatAutoConfiguration`, `SpringAiEmbeddingAutoConfiguration`, `SpringAiModelAutoConfiguration` 等 |
| auth-server | 9 | `AuthorizationServerAutoConfiguration`, `LoginAutoConfiguration`, `CasbinAutoConfiguration`, `TenantAutoConfiguration`, `PermissionAutoConfiguration` 等 |
| resource-server | 2 | `ResourceServerAutoConfiguration`, `DefaultSecurityAutoConfiguration` |
| afg-redis | 1+ | `RedisAutoConfiguration`（应拆分为子 Configuration） |
| governance-client | 1 | `GovernanceClientAutoConfiguration` |
| governance-server | 1 | `GovernanceServerAutoConfiguration` |

---

## 模块 Context-Path

框架通过 `ModuleWebAutoConfiguration` 为模块 Controller 自动添加路径前缀。模块通过 `@AfgModuleAnnotation` 声明自己的 context-path：

```java
@AfgModuleAnnotation(
    name = "认证授权模块",
    contextPath = "/auth-api"
)
public class AuthModuleConfig {}
```

**完整路径计算**：`应用 Context Path + 模块 Context Path + Controller 路径`

示例：应用 context-path `/auth` + 模块 context-path `/auth-api` + Controller `/auth/login` = `/auth/auth-api/auth/login`

**注意事项：**
- `PathMatchConfigurer.addPathPrefix()` 会创建两个可访问路径：原始路径 + 带前缀路径
- Spring Security 需匹配带前缀路径：`.requestMatchers("/auth-api/auth/login").permitAll()`
- `@IgnoreModuleContextPath` 可排除特定 Controller 不加前缀
- 测试时 RestClient baseUrl 需包含应用 context path，URI 需包含模块 context path

---

## SPI 接口设计规则

### 默认实现与降级

- 每个 SPI 必须有 `Default{Spi}` 本地实现（内存/Caffeine/NoOp）
- 每个 SPI 必须有 `NoOp{Spi}` 降级实现（当功能完全不需要时）
- SPI 实现通过 `@ConditionalOnBean` / `@ConditionalOnClass` 自动发现
- SPI 接口放在 `api` 子包，实现放在功能子包

### 自动升级机制

| SPI 接口 | 默认实现 | 升级实现（条件） |
|----------|---------|---------------|
| `CacheManager` | `DefaultCacheManager` (Caffeine) | `RedisCacheManager` (有 afg-redis) |
| `DistributedLock` | 内存锁 | `RedisDistributedLock` (有 afg-redis) |
| `EventPublisher` | `LocalEventPublisher` | RabbitMQ/Kafka 实现 |
| `DistributedTaskScheduler` | `LocalTaskScheduler` | `RedissonTaskScheduler` (有 afg-redis) |
| `AuditLogStorage` | 日志输出 | JDBC/Redis 实现 |
| `AfgChatClient` | `NoOpChatClient` | Spring AI / LangChain4J 实现 |
| `VectorStore` | `NoOpVectorStore` | 向量数据库实现 |

---

## 测试配置

### @ImportAutoConfiguration 必须显式列出前置配置

`@ImportAutoConfiguration` 不会自动解析 `@AutoConfigureAfter` 语义，必须显式列出所有前置 AutoConfiguration：

```java
@SpringBootConfiguration
@ImportAutoConfiguration({
    // 前置配置（必须按顺序列出）
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    // data-core 前置
    TransactionAutoConfiguration.class,
    TenantContextAutoConfiguration.class,
    // data-jdbc
    DataManagerAutoConfiguration.class,
    // data-liquibase
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration { }
```

**常见错误**：只导入 `DataManagerAutoConfiguration` 但遗漏 `TransactionAutoConfiguration` 和 `TenantContextAutoConfiguration`，导致 `DataManager` 创建时依赖的 Bean 不存在。

### Spring Boot 4 Web 测试配置

Spring Boot 4 将自动配置拆分为独立模块，Web 测试（`@SpringBootTest(webEnvironment = RANDOM_PORT)`）必须额外导入以下配置：

| 配置类 | 来源模块 | 提供 |
|--------|---------|------|
| `JacksonAutoConfiguration` | `spring-boot-jackson` | JSON 序列化 |
| `HttpMessageConvertersAutoConfiguration` | `spring-boot-http-converter` | HTTP 消息转换器 |
| `RestClientAutoConfiguration` | `spring-boot-restclient` | `RestClient.Builder` |
| `HttpEncodingAutoConfiguration` | `spring-boot-servlet` | 字符编码过滤器 |
| `TomcatServletWebServerAutoConfiguration` | `spring-boot-tomcat` | 嵌入式 Tomcat |
| `DispatcherServletAutoConfiguration` | `spring-boot-webmvc` | DispatcherServlet |
| `WebMvcAutoConfiguration` | `spring-boot-webmvc` | Spring MVC 基础设施 |

此外还需导入 AFG Core 基础设施和模块配置类：

| 配置类 | 说明 |
|--------|------|
| `AfgAutoConfiguration` | AFG 核心属性 |
| `AfgCoreAutoConfiguration` | ModuleRegistry 等 |
| `ModuleWebAutoConfiguration` | 模块 context-path 前缀 |
| `WebAutoConfiguration` | GlobalExceptionHandler |
| `{Module}ModuleConfig.class` | 模块 `@ComponentScan` 配置 |

> **Warning**: 遗漏 `TomcatServletWebServerAutoConfiguration` 导致 `MissingWebServerFactoryBeanException`。遗漏 `{Module}ModuleConfig.class` 导致 Controller 不被发现（404）。遗漏 `WebAutoConfiguration` 导致异常返回 500 HTML 而非 `Result<T>` JSON。

---

## 模块间通信约定

- 模块间通信优先使用事件（`EventPublisher`），而非直接 Bean 调用
- 事件定义放在 `api.event` 子包
- 禁止循环依赖：模块 A 依赖模块 B 时，B 不能依赖 A
- 每个功能必须有 `enabled` 开关 + `@ConditionalOnProperty` 控制
- 每个配置项必须有合理默认值，零配置即可运行

---

## 配置属性设计

### 命名约定

- 前缀：`afg.{module}.{feature}`
- 风格：kebab-case
- 层级：不超过 4 层

### 禁止配置爆炸

- 每个功能最多 1 个 `enabled` 开关 + 必要的行为参数
- 不暴露框架内部实现细节的配置项
- 基础设施配置（数据库/Redis/MQ 地址）由 Spring Boot 原生管理
- 框架只配置"行为参数"（如超时、重试次数、策略选择），不配置"连接参数"

```yaml
afg:
  ai:
    rag:
      enabled: true               # 功能开关
      embedding-dimensions: 1536  # 行为参数
      # LLM 连接配置由 Spring AI / LangChain4J 原生管理
```
