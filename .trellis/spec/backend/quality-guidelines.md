# 质量规范

> AFG Framework 代码质量底线、禁止模式、强制模式、AutoConfiguration 规范、SPI 设计规范与命名约定。

**PRD 来源：** §1.5 质量底线、§5.3 Core 模块（AutoConfiguration）、§6.1 编码规范、§6.6 模块开发规范

---

## 质量底线（6 条铁律）

以下 6 条规则来自 PRD §1.5，是框架开发的最低质量标准，违反任何一条即视为不可发布：

| # | 规则 | 说明 |
|---|------|------|
| 1 | **每个功能必须有 NoOp 降级实现** | 不引入外部依赖时框架仍可运行 |
| 2 | **每个功能必须有开关注解或条件** | `@ConditionalOnXxx` + `enabled` 配置项 |
| 3 | **每个功能必须有测试覆盖** | 集成测试 + Testcontainers，Mockito 铁律禁止 |
| 4 | **每个 AutoConfiguration 必须声明依赖排序** | `@AutoConfigureAfter` / `@AutoConfigureBefore` |
| 5 | **每个配置项必须有合理默认值** | 零配置必须可运行 |
| 6 | **每个 SPI 必须有本地默认实现** | 不依赖外部实现即可工作 |

---

## 禁止模式

以下模式在 AFG Framework 中严格禁止，违反即视为代码缺陷：

### 框架级禁止

| 禁止模式 | 替代方案 | 原因 |
|----------|----------|------|
| `@Data` 用于实体类 | `@Getter` + `@Setter` | `@Data` 包含 `toString`/`equals`/`hashCode`，与 JPA 实体代理冲突，导致延迟加载异常和性能问题 |
| `@Value` 用于实体类 | `@Getter` + `@Setter` 或 record | 实体需要可变性（DataManager save/update），`@Value` 生成不可变类 |
| Mockito（`@Mock`/`Mockito.mock()`/`when`/`verify`） | 注入真实 Bean + 真实数据准备 | 铁律！Mock 掩盖真实行为，导致测试与生产行为不一致 |
| `@MockBean` | `@TestConfiguration` 注册真实实现 | Spring 上下文污染，测试不反映真实 Bean 交互 |
| H2 内存数据库 | Testcontainers（MySQL/PostgreSQL） | SQL 方言差异大，H2 通过的测试在生产可能失败 |
| `spring-boot-starter-data-jpa` | `afg-framework-data-jdbc`（DataManager） | JPA 的 EntityManagerFactory 与 DataManager 冲突 |
| `grpc-netty-shaded` | `grpc-netty` | shaded 后类名不匹配，与 Spring Cloud Gateway 冲突 |
| 手动创建 Logger | Lombok `@Slf4j` | 统一日志创建方式，减少模板代码 |
| `@Configuration` 用于 AutoConfiguration | `@AutoConfiguration` | `@AutoConfiguration` 支持 `after`/`before` 排序，`@Configuration` 不支持 |
| 注释掉的代码 | 删除，需要时从 git 历史恢复 | 注释代码造成混乱，git 已有完整历史 |
| TODO 无 Issue 编号 | `TODO(#123): ...` 格式 | 无法追踪的 TODO 等于没有 TODO |

### 测试级禁止

| 禁止模式 | 替代方案 |
|----------|----------|
| `@Mock` / `Mockito.mock()` | 注入真实 Bean |
| `when(...).thenReturn(...)` | 准备真实数据 |
| `verify(...)` | 断言业务结果 |
| `@MockBean` | `@TestConfiguration` 注册真实实现 |
| H2 内存数据库 | Testcontainers |
| 测试名 `testMethod1` | `shouldXxx_whenYyy` 命名 |
| `@Sql` 数据准备 | DataManager 真实数据操作 |

**唯一例外：** 外部 HTTP 服务（AI LLM API、OAuth2 远程调用）用 WireMock 模拟 HTTP 层。

---

## 强制模式

### 实体类

```java
// 强制 — 实体类使用 @Getter + @Setter
@Getter
@Setter
@AfEntity
@Table(name = "sys_user")
public class User extends SoftDeleteEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    private Integer status = 1;
}
```

### 依赖注入

```java
// 强制 — 使用 @RequiredArgsConstructor 构造器注入
@Service
@RequiredArgsConstructor
public class UserService {

    private final DataManager dataManager;
    private final UserValidator userValidator;

    // 禁止 @Autowired 字段注入
    // @Autowired
    // private DataManager dataManager;
}
```

### 日志

```java
// 强制 — 使用 @Slf4j
@Slf4j
@Service
public class UserService {
    public void createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
    }
}
```

---

## AutoConfiguration 编写规范

### 强制使用 @AutoConfiguration

```java
// 正确
@AutoConfiguration(after = {DataSourceAutoConfiguration.class})
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration { ... }

// 禁止 — 不支持 after/before 排序
@Configuration
public class CacheAutoConfiguration { ... }
```

### 强制声明依赖排序

```java
// 同模块内 — 类引用
@AutoConfiguration(after = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class DataManagerAutoConfiguration { ... }

// 跨模块 — 字符串引用（避免编译期硬依赖）
@AutoConfiguration(afterName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
public class LiquibaseAutoConfiguration { ... }
```

### 可替换组件必须使用 @ConditionalOnMissingBean

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

### 可注入组件必须使用 @Nullable

```java
@Bean
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

### AutoConfiguration 依赖链

```
DataSourceAutoConfiguration (Spring Boot)
  └→ DataSourceTransactionManagerAutoConfiguration (Spring Boot)
       └→ TransactionAutoConfiguration (data-core) — TransactionAdapter
       └→ TenantContextAutoConfiguration (data-core) — TenantContextHolder
            └→ DataManagerAutoConfiguration (data-jdbc) — JdbcDataManager
                 └→ LiquibaseAutoConfiguration (data-liquibase) — SpringLiquibase
```

### 测试中 @ImportAutoConfiguration 不自动解析排序

`@ImportAutoConfiguration` 不会自动解析 `@AutoConfigureAfter`，需显式列出前置配置：

```java
@SpringBootConfiguration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    DataManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration { }
```

---

## SPI 设计规范

### 三层实现结构

每个 SPI 必须遵循三层实现结构：

| 层级 | 命名规则 | 说明 | 示例 |
|------|----------|------|------|
| **接口** | 名词或动词 | 定义在 `api` 或 `spi` 子包 | `DistributedCacheStorage`、`AuditLogStorage` |
| **默认实现** | `Default{Spi}` 或 `{Technology}{Spi}` | 功能完整，无外部依赖 | `LocalDistributedCacheStorage`、`DefaultCacheManager` |
| **NoOp 实现** | `NoOp{Spi}` | 空操作，功能完全关闭时使用 | `NoOpAuditLogStorage`、`NoOpVectorStore` |

### SPI 自动发现

通过 `@ConditionalOnBean` / `@ConditionalOnClass` 实现 SPI 实现的自动发现：

```java
@AutoConfiguration
public class AuditLogAutoConfiguration {

    // 默认实现 — 无外部依赖时使用
    @Bean
    @ConditionalOnMissingBean(AuditLogStorage.class)
    @ConditionalOnProperty(name = "afg.core.audit.storage-type",
                           havingValue = "log", matchIfMissing = true)
    public AuditLogStorage logAuditLogStorage() {
        return new LogAuditLogStorage();
    }

    // NoOp 实现 — 明确关闭时使用
    @Bean
    @ConditionalOnMissingBean(AuditLogStorage.class)
    @ConditionalOnProperty(name = "afg.core.audit.storage-type", havingValue = "none")
    public AuditLogStorage noOpAuditLogStorage() {
        return new NoOpAuditLogStorage();
    }

    // 外部实现（如 Redis）由集成模块提供
    // afg-redis 模块的 RedisAuditLogStorage 通过 @ConditionalOnClass 自动注册
}
```

### SPI 接口包路径规范

```
{module}/src/main/java/.../{module}/
├── api/                    # SPI 接口定义
│   ├── cache/
│   │   └── DistributedCacheStorage.java
│   └── audit/
│       └── AuditLogStorage.java
├── cache/                  # 默认实现
│   └── LocalDistributedCacheStorage.java
└── autoconfigure/
    └── CacheAutoConfiguration.java
```

### 框架 SPI 示例

| SPI 接口 | 默认实现 | 外部实现 | NoOp 实现 |
|----------|----------|----------|-----------|
| `DistributedCacheStorage` | `LocalDistributedCacheStorage` | `RedisDistributedCacheStorage`（afg-redis） | -- |
| `AuditLogStorage` | `LogAuditLogStorage` | `RedisAuditLogStorage`（afg-redis）/ `DatabaseAuditLogStorage`（afg-jdbc） | `NoOpAuditLogStorage` |
| `VectorStore` | `NoOpVectorStore` | Spring AI / LangChain4J 适配 | -- |
| `EventPublisher` | `LocalEventPublisher` | `RabbitMQEventPublisher`（afg-rabbitmq） | -- |
| `PiiDetector` | `DefaultPiiDetector` | -- | -- |
| `RedisHealthChecker` | `NoOpRedisHealthChecker` | afg-redis 模块提供真实实现 | `NoOpRedisHealthChecker` |

---

## 模块间通信规范

### 优先使用事件，避免直接 Bean 调用

```java
// 正确 — 通过事件通信
@Autowired
private DomainEventPublisher eventPublisher;

public void createUser(User user) {
    dataManager.save(User.class, user);
    eventPublisher.publish(new UserCreatedEvent(user.getId()));
}

// 消费方
@EventHandler(topic = "user-created")
public void onUserCreated(UserCreatedEvent event) {
    // 初始化用户配置等
}

// 禁止 — 直接 Bean 调用产生循环依赖
@Autowired
private ConfigService configService;  // 如果 ConfigService 也依赖 UserService → 循环依赖

public void createUser(User user) {
    dataManager.save(User.class, user);
    configService.initUserConfig(user.getId());  // 禁止！
}
```

### 事件定义规范

| 规范 | 说明 |
|------|------|
| 事件接口 | 实现 `DomainEvent<T>` 接口 |
| 事件定义位置 | `api.event` 子包 |
| 事件发布 | `DomainEventPublisher`（默认 `LocalEventPublisher`，分布式场景用 `RabbitMQEventPublisher`） |
| 事件消费 | `@EventHandler` 注解，支持 `topic`/`groupId`/`concurrency`/`retryCount`/`deadLetterTopic` |
| 循环依赖 | 模块 A 依赖模块 B 时，B 不能依赖 A |

### DomainEvent 接口

```java
public interface DomainEvent<T> {
    String getEventId();
    String getEventType();
    Instant getTimestamp();
    String getAggregateId();
    T getPayload();
    int getVersion();
    String getSource();
}
```

### 事件重试与死信

- `EventRetryHandler` 提供指数退避重试
- 重试耗尽后自动转发到死信主题（`DeadLetterEvent`）
- `@EventHandler` 注解支持配置 `retryCount`、`retryInterval`、`exponentialBackoff`、`deadLetterTopic`

---

## 依赖管理规范

### 版本集中管理

所有依赖版本必须集中在 `gradle/libs.versions.toml`：

```toml
[versions]
spring-boot = "4.0.6"
spring-ai = "2.0.0-M7"
langchain4j = "1.15.1"
redisson = "4.3.1"
grpc = "1.81.0"
jcasbin = "1.9.2"
spring-grpc = "1.0.3"

[libraries]
# Spring Boot BOM 管理的依赖 — 不指定版本
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-redis = { module = "org.springframework.boot:spring-boot-starter-data-redis" }

# 需要显式版本的依赖
spring-ai-client-chat = { module = "org.springframework.ai:spring-ai-client-chat", version.ref = "spring-ai" }
langchain4j = { module = "dev.langchain4j:langchain4j", version.ref = "langchain4j" }
```

### Spring Boot BOM

框架使用 Spring Boot BOM 进行依赖管理，BOM 管理的依赖不需要指定版本：

```kotlin
dependencies {
    // BOM 管理的依赖 — 无版本号
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.redis)

    // 非 BOM 管理的依赖 — 显式版本
    implementation(libs.spring.ai.client.chat)
    implementation(libs.langchain4j)
}
```

### gRPC 传输层依赖

- 禁止使用 `grpc-netty-shaded`，统一使用 `grpc-netty`
- gRPC 传输层依赖必须使用 `implementation`（非 `api`）声明，不传递给下游
- 引入 governance-client 的项目需显式添加 `implementation(libs.grpc.netty)`

### OWASP 安全扫描

框架通过 Gradle 依赖安全扫描检查已知漏洞依赖，发布前必须通过安全扫描。

---

## 命名约定

### 通用命名

| 类型 | 规则 | 正确示例 | 错误示例 |
|------|------|----------|----------|
| 类 | UpperCamelCase | `UserService`、`OrderController` | `user_service`、`userService` |
| 方法 | lowerCamelCase，动词开头 | `findById`、`createOrder`、`isExpired` | `FindById`、`order_create` |
| 字段 | lowerCamelCase | `userName`、`createdAt` | `UserName`、`user_name` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`、`DEFAULT_PAGE_SIZE` | `maxRetryCount`、`MaxRetryCount` |
| 包 | 全小写，点分隔 | `io.github.afgprojects.framework.core.cache` | `io.github.afgprojects.framework.Core.Cache` |

### 数据库命名

| 类型 | 规则 | 正确示例 |
|------|------|----------|
| 表名 | lower_snake_case | `sys_user`、`sec_role` |
| 列名 | lower_snake_case | `user_name`、`created_at` |
| 主键约束 | `pk_{表名}` | `pk_sys_user` |
| 唯一约束 | `uk_{表名}_{字段名}` | `uk_user_username` |
| 普通索引 | `idx_{表名}_{字段名}` | `idx_user_tenant` |
| 外键约束 | `fk_{表名}_{关联表}` | `fk_user_dept` |

### 框架特殊命名

| 类型 | 规则 | 正确示例 |
|------|------|----------|
| AutoConfiguration | `{Feature}AutoConfiguration` | `CacheAutoConfiguration`、`DataManagerAutoConfiguration` |
| SPI 接口 | 名词或动词 | `DistributedLock`、`EventPublisher` |
| 默认实现 | `Default{Spi}` 或 `{Technology}{Spi}` | `DefaultCacheManager`、`RedisDistributedCacheStorage` |
| NoOp 实现 | `NoOp{Spi}` | `NoOpVectorStore`、`NoOpAuditLogStorage` |
| 异常类 | `{Name}Exception` | `BusinessException`、`OptimisticLockException` |
| 错误码枚举 | `{Domain}ErrorCode` | `CommonErrorCode`、`AiErrorCode` |
| 实体类 | 名词，无 Entity 后缀 | `User`、`Order` | `UserEntity`（禁止） |
| 属性配置 | `{Module}{Feature}Properties` | `AfgCoreLoggingProperties`、`CacheProperties` |
| 测试方法 | `shouldXxx_whenYyy` | `shouldReturnToken_whenLoginWithValidCredentials` |

### 注释规范

| 规范 | 说明 |
|------|------|
| 类级 Javadoc | 所有 public 类必须有 Javadoc 说明职责和用法 |
| 方法级 Javadoc | Public API 方法必须有 `@param`/`@return`/`@throws` |
| 行内注释 | 只注释 "为什么"，不注释 "做什么"，代码应自解释 |
| 禁止 | 注释掉的代码、无 Issue 编号的 TODO、过度注释 |

---

## 测试规范

### 测试分层

| 层级 | 使用场景 | 占比 |
|------|----------|------|
| 单元测试 | 纯逻辑类（工具类、条件构建器、SQL 构建器、DSL 解析器），不需要 Spring 上下文 | 少量 |
| 集成测试 | 涉及 DataManager/Service/Controller/Security，必须 Spring 上下文 + Testcontainers | 主体 |

### 测试目录结构

```
src/test/java/.../{module}/
├── entity/          # 实体 CRUD 生命周期测试
├── query/           # 条件查询测试
├── api/             # API 端到端测试
├── scenario/        # 复杂业务场景测试
└── unit/            # 纯逻辑单元测试（少量）
```

### 测试基类

```java
// 数据层测试
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
abstract class BaseDataTest { }

// Web 层测试
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseWebTest {
    @LocalServerPort int port;
    @Autowired RestClient.Builder restClientBuilder;
    protected RestClient restClient() {
        return restClientBuilder.baseUrl("http://localhost:" + port).build();
    }
}
```

### 数据准备

```java
// 正确 — 使用 DataManager 准备真实数据
@Test
void shouldNotDeleteUser_whenUserHasActiveOrders() {
    User user = dataManager.save(User.class, createUser("test"));
    Order order = dataManager.save(Order.class, createOrder(user.getId()));

    assertThatThrownBy(() -> dataManager.deleteById(User.class, user.getId()))
        .isInstanceOf(ReferentialIntegrityException.class);
}
```

### @Transactional 使用

| 场景 | 使用 @Transactional |
|------|---------------------|
| DataManager 数据操作测试 | 使用 — 自动回滚 |
| API 端到端测试 | 不使用 — HTTP 不同线程 |
| 并发/锁测试 | 不使用 — 需要多事务 |

### 测试依赖

```kotlin
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.github.tomakehurst:wiremock-jre12-standalone")
    // 禁止引入 mockito
}
```

---

## 代码审查清单

审查代码时，按以下清单逐项检查：

### 功能性

- [ ] 每个功能是否有 `NoOp` 降级实现？
- [ ] 每个功能是否有 `@ConditionalOnXxx` + `enabled` 开关？
- [ ] 每个配置项是否有合理默认值？
- [ ] 每个 SPI 是否有本地默认实现？

### AutoConfiguration

- [ ] 是否使用 `@AutoConfiguration`（非 `@Configuration`）？
- [ ] 是否声明了 `@AutoConfigureAfter` / `@AutoConfigureBefore`？
- [ ] 跨模块引用是否使用 `afterName` 字符串形式？
- [ ] 可替换组件是否使用 `@ConditionalOnMissingBean`？
- [ ] 可注入组件是否使用 `@Nullable`？

### 实体类

- [ ] 是否使用 `@Getter` + `@Setter`（非 `@Data`）？
- [ ] 是否有 `@AfEntity` 注解？
- [ ] 实体类名是否无 Entity 后缀？

### 依赖注入

- [ ] 是否使用 `@RequiredArgsConstructor` 构造器注入？
- [ ] 是否避免了 `@Autowired` 字段注入？

### 日志

- [ ] 是否使用 `@Slf4j`？
- [ ] 是否避免了敏感数据明文打印？
- [ ] 异常对象是否放在日志方法最后一个参数？

### 异常处理

- [ ] 是否使用 `BusinessException`（非原始 `RuntimeException`）？
- [ ] 错误码是否在正确范围（框架 10000-19999，业务 20000+）？
- [ ] 是否需要 i18n 支持？如需要，是否使用了模板参数构造方式？

### 测试

- [ ] 是否无 Mockito 使用？
- [ ] 测试名是否 `shouldXxx_whenYyy` 格式？
- [ ] 数据准备是否使用 DataManager（非 `@Sql`）？
- [ ] 是否使用 Testcontainers（非 H2）？

### 命名

- [ ] 类名 UpperCamelCase？
- [ ] 方法名 lowerCamelCase 动词开头？
- [ ] 常量 UPPER_SNAKE_CASE？
- [ ] 数据库对象 lower_snake_case？

### 依赖

- [ ] 版本是否在 `libs.versions.toml` 中管理？
- [ ] 是否无 `grpc-netty-shaded`？
- [ ] 是否无 `spring-boot-starter-data-jpa`？
- [ ] gRPC 传输层依赖是否使用 `implementation`？
