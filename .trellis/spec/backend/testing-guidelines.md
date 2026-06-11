# 测试规范

> 来源：CLAUDE.md 测试规范、PRD 1.5 质量底线

---

## 铁律

```
禁止 Mockito mock。违反 = 删除重写。
```

这是 AFG 框架测试的绝对红线。所有测试必须基于真实的 Spring 上下文和真实的数据操作，不得使用任何 mock 框架替代真实行为。

---

## 禁止与替代方案

| 禁止 | 替代方案 | 原因 |
|------|---------|------|
| `@Mock` / `Mockito.mock()` | 注入真实 Bean | mock 隐藏了真实的依赖关系和交互错误 |
| `when(...).thenReturn(...)` | 准备真实数据 | mock 返回值无法验证业务逻辑的正确性 |
| `verify(...)` | 断言业务结果 | 验证调用次数不等于验证业务正确性 |
| `@MockBean` | `@TestConfiguration` 注册真实实现 | `@MockBean` 破坏 Spring 上下文缓存，增加启动时间 |
| `@SpyBean` | 注入真实 Bean 并使用真实方法 | spy 是 mock 的变种，同样隐藏真实行为 |

### 唯一例外：外部 HTTP 服务

外部 HTTP 服务（AI LLM API、OAuth2 远程调用）可用 WireMock 模拟 HTTP 层。这是因为：

- 外部服务不在应用控制范围内，不可在测试环境启动
- WireMock 模拟的是 HTTP 协议层，而非 Java 对象层
- 必须使用 `com.github.tomakehurst:wiremock-jre12-standalone`

```java
// 正确 — WireMock 模拟外部 HTTP 服务
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiChatControllerTest {

    @Autowired
    WireMockServer wireMockServer;

    @BeforeEach
    void setup() {
        wireMockServer.stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"choices\":[{\"message\":{\"content\":\"Hello\"}}]}")));
    }
}
```

---

## 测试分层

### 单元测试（少量）

纯逻辑类，不需要 Spring 上下文：

- 工具类（`NamingUtils`、`DateUtils`、`CollectionUtils`）
- 条件构建器（`Conditions.builder()`）
- SQL 构建器（`SqlQueryBuilder`、`SqlInsertBuilder`）
- DSL 解析器
- 枚举/常量验证

```java
class NamingUtilsTest {

    @Test
    void shouldConvertToSnakeCase_whenInputIsCamelCase() {
        assertThat(NamingUtils.toSnakeCase("userName")).isEqualTo("user_name");
    }

    @Test
    void shouldConvertToCamelCase_whenInputIsSnakeCase() {
        assertThat(NamingUtils.toCamelCase("user_name")).isEqualTo("userName");
    }
}
```

### 集成测试（主体）

涉及 DataManager / Service / Controller / Security 的测试，必须使用 Spring 上下文 + Testcontainers 真实数据库：

- 实体 CRUD 生命周期
- 条件查询（多租户、数据权限、软删除自动过滤）
- API 端到端（RestClient + 真实 HTTP）
- 复杂业务场景（事务、乐观锁、级联）

---

## 目录结构

```
src/test/java/.../{module}/
├── entity/          # 实体 CRUD 生命周期测试
├── query/           # 条件查询测试（多租户/数据权限/软删除过滤）
├── api/             # API 端到端测试（Controller + RestClient）
├── scenario/        # 复杂业务场景测试（事务/乐观锁/级联）
└── unit/            # 纯逻辑单元测试（少量）
```

---

## 测试命名

统一使用 `shouldXxx_whenYyy` 模式：

```java
// 正确 — 清晰描述期望行为和触发条件
void shouldReturnToken_whenLoginWithValidCredentials() { }
void shouldLockAccount_whenLoginFailExceedsMaxAttempts() { }
void shouldThrowOptimisticLockException_whenConcurrentUpdate() { }
void shouldFilterByTenant_whenQueryWithTenantEntity() { }

// 错误 — 方法名无法表达测试意图
void testMethod1() { }
void testLogin() { }
void testUser() { }
```

---

## 数据准备

### 使用 DataManager（推荐）

```java
@Test
void shouldNotDeleteUser_whenUserHasActiveOrders() {
    // 使用 DataManager 准备真实数据
    User user = dataManager.save(User.class, createUser("test"));
    Order order = dataManager.save(Order.class, createOrder(user.getId()));

    assertThatThrownBy(() -> dataManager.deleteById(User.class, user.getId()))
        .isInstanceOf(ReferentialIntegrityException.class);
}
```

### 禁止使用 @Sql

`@Sql` 脚本与实体元数据脱节，维护成本高。DataManager 基于 APT 元数据操作，天然与实体一致。

```java
// 禁止 — SQL 脚本与实体脱节
@Sql(scripts = "/test-data.sql")
void testFindAll() { }

// 正确 — DataManager 准备数据
@Test
void shouldReturnAllActiveUsers_whenFindAll() {
    dataManager.save(User.class, createUser("active", 1));
    dataManager.save(User.class, createUser("inactive", 0));

    List<User> active = dataManager.findList(User.class,
        builder(User.class).eq(User::getStatus, 1).build());

    assertThat(active).hasSize(1);
}
```

---

## 基类

### BaseDataTest — 数据层测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
abstract class BaseDataTest {

    @Autowired
    protected DataManager dataManager;

    protected <T> T saveEntity(Class<T> entityClass, T entity) {
        return dataManager.save(entityClass, entity);
    }
}
```

**适用场景**：实体 CRUD 测试、条件查询测试、DataManager API 测试

**关键**：`@Transactional` 使每个测试方法在独立事务中执行，测试结束后自动回滚，不污染数据库。

### BaseWebTest — Web 层测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseWebTest {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder restClientBuilder;

    protected RestClient restClient() {
        return restClientBuilder.baseUrl("http://localhost:" + port).build();
    }
}
```

**适用场景**：API 端到端测试、Controller 测试、Security 过滤器测试

**关键**：HTTP 请求在不同线程处理，`@Transactional` 无效，需要手动清理测试数据。

---

## @Transactional 使用规则

| 场景 | 使用 @Transactional | 原因 |
|------|--------------------|----|
| DataManager 数据操作测试 | 是 | 自动回滚，不污染数据库 |
| API 端到端测试 | 否 | HTTP 请求在不同线程，事务不生效 |
| 并发/锁测试 | 否 | 需要多事务环境验证锁行为 |
| 乐观锁测试 | 否（或部分是） | 需要多个事务验证冲突检测 |

```java
// 正确 — 数据层测试使用 @Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class UserRepositoryTest {
    @Test
    void shouldSaveAndFindUser() {
        User saved = dataManager.save(User.class, newUser("test"));
        User found = dataManager.findById(User.class, saved.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("test");
    }
}

// 正确 — API 测试不使用 @Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiControllerTest {
    @Test
    void shouldReturnCreatedUser_whenPostUser() {
        // 手动清理
        try {
            restClient().post().uri("/users")
                .body(new CreateUserRequest("test"))
                .retrieve()
                .toEntity(User.class);
        } finally {
            dataManager.deleteByCondition(User.class,
                builder(User.class).eq(User::getUsername, "test").build());
        }
    }
}
```

---

## 红旗清单

在代码审查中，以下任何一项出现即为违规：

| 红旗 | 问题 | 修复方式 |
|------|------|---------|
| `@Mock` / `@MockBean` | 使用了 mock 框架 | 删除，注入真实 Bean |
| `when(...).thenReturn(...)` | mock 行为定义 | 准备真实数据 |
| `verify(...)` | 验证调用次数 | 断言业务结果 |
| `@SpyBean` | 部分 mock | 使用真实实现 |
| `testMethod1` 命名 | 命名无法表达意图 | 改为 `shouldXxx_whenYyy` |
| H2 内存数据库 | 与生产数据库行为不一致 | 换 Testcontainers（MySQL/PostgreSQL） |
| `@Sql` 脚本 | 与实体元数据脱节 | 使用 DataManager 准备数据 |
| 无 `@DisplayName` | 测试意图不明确 | 添加中文 `@DisplayName` |
| 平铺测试类 | 测试组织混乱 | 使用 `@Nested` 内部类分组 |

---

## 依赖配置

```kotlin
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.github.tomakehurst:wiremock-jre12-standalone")
    // 禁止引入 mockito
    // 禁止引入 H2（除非专门测试 H2 方言兼容性）
}
```

---

## ImportAutoConfiguration 模式

集成测试中使用 `@ImportAutoConfiguration` 时，必须显式列出所有前置 AutoConfiguration。`@ImportAutoConfiguration` 不会自动解析 `@AutoConfigureAfter` 语义。

### 数据层测试配置

```java
@SpringBootConfiguration
@ImportAutoConfiguration({
    // 前置配置（必须按顺序列出，不可省略）
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    // data-core
    TransactionAutoConfiguration.class,
    TenantContextAutoConfiguration.class,
    // data-jdbc
    DataManagerAutoConfiguration.class,
    // data-liquibase
    LiquibaseAutoConfiguration.class
})
public class JdbcDataTestConfiguration { }
```

**常见错误**：只导入目标 AutoConfiguration 但遗漏前置配置，导致 Bean 创建失败。

### Web 层测试配置（Spring Boot 4）

Spring Boot 4 将自动配置拆分为独立模块（`spring-boot-webmvc`、`spring-boot-servlet`、`spring-boot-tomcat`、`spring-boot-restclient` 等），使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 的 Web 测试必须显式导入 Web 服务器基础设施。

```java
@SpringBootConfiguration
@ImportAutoConfiguration({
    // JDBC 基础设施
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    // AFG Core 基础设施（ModuleRegistry、AfgCoreProperties 等）
    AfgAutoConfiguration.class,
    AfgCoreAutoConfiguration.class,
    // 模块配置（触发 @ComponentScan 扫描 controller/service 等组件）
    AiCoreModuleConfig.class,           // 替换为你模块的 @AfgModuleAnnotation 配置类
    // AFG 数据层
    DataManagerAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    // Web 服务器基础设施（Spring Boot 4 拆分后必须显式导入）
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    RestClientAutoConfiguration.class,
    HttpEncodingAutoConfiguration.class,
    TomcatServletWebServerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ModuleWebAutoConfiguration.class,
    // AFG 异常处理（注册 GlobalExceptionHandler）
    WebAutoConfiguration.class,
    // 目标模块 AutoConfiguration
    AiCoreAutoConfiguration.class,
    AiChatAutoConfiguration.class,
    // ... 其他模块 AutoConfiguration
})
public class AiTestConfiguration { }
```

> **Warning**: Spring Boot 4 的 `@ImportAutoConfiguration` 不会自动解析 `@AutoConfigureAfter` 引用的配置类。遗漏 Web 服务器配置会导致 `MissingWebServerFactoryBeanException: No qualifying bean of type 'ServletWebServerFactory'`。遗漏 `WebAutoConfiguration` 会导致 Controller 异常返回 500 HTML 错误页面而非 `Result<T>` JSON。

> **Warning**: 遗漏模块的 `@AfgModuleAnnotation` 配置类（如 `AiCoreModuleConfig.class`）会导致 `@ComponentScan` 不触发，Controller 不被发现，所有请求返回 404。

### PMD 命名陷阱

PMD 的 `TestClassWithoutTestCases` 和 `UnitTestShouldUseTestAnnotation` 规则会标记以 "Test" 开头的非测试类和以 "test" 开头的非测试方法。框架中常见的陷阱：

| 场景 | 错误命名 | 正确命名 |
|------|---------|---------|
| 连接测试 DTO | `TestConnectionResponse` | `ConnectionTestResponse` |
| 验证连接服务方法 | `testConnection()` | `verifyConnection()` / `checkConnection()` |

---

## @DisplayName 与 @Nested

使用中文 `@DisplayName` 注解说明测试意图，使用 `@Nested` 内部类对测试方法分组：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@DisplayName("DataManager 用户实体测试")
class UserDataManagerTest {

    @Autowired
    DataManager dataManager;

    @Nested
    @DisplayName("保存操作")
    class Save {

        @Test
        @DisplayName("新增用户时自动填充 createdAt 和 updatedAt")
        void shouldFillTimestamps_whenSaveNewUser() {
            User user = new User();
            user.setUsername("test");

            User saved = dataManager.save(User.class, user);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("更新用户时 updatedAt 自动刷新")
        void shouldRefreshUpdatedAt_whenUpdateUser() {
            User saved = dataManager.save(User.class, createUser("test"));
            Instant originalUpdatedAt = saved.getUpdatedAt();

            saved.setEmail("new@email.com");
            User updated = dataManager.save(User.class, saved);

            assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("软删除操作")
    class SoftDelete {

        @Test
        @DisplayName("软删除实体执行逻辑删除而非物理删除")
        void shouldPerformSoftDelete_whenDeleteSoftDeleteEntity() {
            User saved = dataManager.save(User.class, createUser("test"));

            dataManager.deleteById(User.class, saved.getId());

            // 查询不到（自动过滤 deleted=true）
            assertThat(dataManager.findById(User.class, saved.getId())).isEmpty();
            // 包含已删除记录时可以查到
            User deleted = dataManager.entity(User.class)
                .includeDeleted()
                .query()
                .where(builder(User.class).eq(User::getId, saved.getId()).build())
                .one()
                .orElseThrow();
            assertThat(deleted.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("恢复软删除记录后可正常查询")
        void shouldRestoreSoftDeletedRecord() {
            User saved = dataManager.save(User.class, createUser("test"));
            dataManager.deleteById(User.class, saved.getId());

            dataManager.restoreById(User.class, saved.getId());

            assertThat(dataManager.findById(User.class, saved.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("条件查询")
    class ConditionQuery {

        @Test
        @DisplayName("动态条件 null 值自动跳过")
        void shouldSkipNullValues_whenUsingIfPresentOperators() {
            dataManager.save(User.class, createUser("active", 1));
            dataManager.save(User.class, createUser("inactive", 0));

            Condition condition = builder(User.class)
                .eqIfPresent(User::getStatus, null)   // null -> 跳过
                .likeIfPresent(User::getUsername, null) // null -> 跳过
                .build();

            List<User> result = dataManager.findList(User.class, condition);
            assertThat(result).hasSize(2); // 无条件过滤，返回全部
        }
    }
}
```

---

## Testcontainers 配置

使用 Testcontainers 启动真实数据库实例，确保测试与生产环境一致。

### Singleton 容器模式（推荐）

多个测试类共享同一个 Testcontainers 容器时，**必须使用 singleton 模式**（`static final` + `static { start(); }`），而非 `@Container` 注解模式。`@Container` 模式由 Testcontainers JUnit5 扩展管理，在 Spring Context 重建时容器可能被销毁，导致后续测试无法连接数据库。

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseWebTest {

    // Singleton 模式 — JVM 生命周期内只启动一次，所有测试类共享
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("afg_test")
        .withUsername("test")
        .withPassword("test");

    static {
        MYSQL.start();  // 在类加载时启动，确保容器在 Spring Context 初始化前就绪
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
```

> **Warning**: `@Container` 注解的容器由 JUnit5 扩展按测试类生命周期管理。当 Spring Context 因配置差异被重建时，旧容器可能被销毁而新容器未及时启动，导致 `HikariPool - Connection is not available, request timed out after 30000ms` 错误。Singleton 模式避免了这个问题。

### @Container 模式（仅限单测试类）

如果只有一个测试类使用 Testcontainers（无 Context 共享风险），可以使用 `@Container` 模式：

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class SingleClassDataTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("afg_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

**禁止使用 H2 内存数据库**（除非专门测试 H2 方言兼容性），原因：

- H2 与 MySQL/PostgreSQL 语法差异大，测试通过不等于生产可用
- H2 不支持部分高级特性（JSON 函数、窗口函数、CTE 等）
- H2 的 NULL 语义与 MySQL 不同
- Testcontainers 启动速度已足够快（< 5s），无理由使用 H2

---

## 质量底线（来自 PRD 1.5）

1. 每个功能必须有测试覆盖 — 集成测试 + Testcontainers，禁止 Mockito
2. 每个功能必须有 NoOp 降级实现 — 不引入外部依赖即可运行
3. 每个功能必须有开/关注解或条件 — `@ConditionalOnXxx` + `enabled` 开关
4. 每个 AutoConfiguration 必须声明依赖排序 — `@AutoConfigureAfter` / `@AutoConfigureBefore`
5. 每个配置项必须有合理默认值 — 零配置即可运行
6. 每个 SPI 必须有本地默认实现 — 不依赖外部实现即可工作
