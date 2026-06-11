# AFG Framework — 产品需求文档 (PRD)

> **版本：** 1.0.0-SNAPSHOT
> **日期：** 2026-06-11
> **状态：** 产品需求蓝图
> **定位：** Spring Boot 的企业级增强框架

---

## 1. 产品概述

### 1.1 产品定位

AFG Framework 是一款面向企业级 Java 应用开发的**全栈增强框架**，基于 **Java 25 + Spring Boot 4 + Gradle** 构建。

**核心定位：Spring Boot 的企业级增强框架。**

不是替代 Spring Boot，而是在其之上补齐企业级开发缺失的能力——多租户、数据权限、统一审计、AI 全链路、声明式韧性安全——让开发者从"组装轮子"变为"聚焦业务"。

**一句话定义**：在 Spring Boot 之上，提供企业级应用开发所需的全部增强能力，引入即生效，不引入零侵入。

### 1.2 设计哲学

框架的每一条设计决策都遵循以下 5 条哲学：

#### 1. 约定优于配置

合理的默认值优于显式配置。框架的每个功能都有开箱即用的默认行为，配置只用于覆盖。

```
引入 afg-framework-auth-server → OAuth2 授权服务器自动可用
引入 afg-framework-afg-redis → 分布式缓存/锁/调度自动可用
无需任何配置，即可运行
```

#### 2. 增强而非替代

所有功能基于 Spring Boot 增强，不替换原生能力。开发者可以同时使用框架增强和 Spring Boot 原生 API。

```
框架提供 DataManager → 增强 Spring JDBC，不替代 JdbcTemplate
框架提供 @AiChat → 增强 Spring AI，不替代 ChatClient
框架提供 AfgCache → 增强 Spring Cache，不替代 @Cacheable
```

#### 3. 编译时安全

APT 编译时生成元数据，运行时零反射开销。类型安全优先，错误在编译期暴露而非运行时。

```
@AfEntity → 编译时生成 {Entity}Metadata.java → 运行时零反射
Conditions.builder(User.class).eq(User::getStatus, 1) → Lambda 类型安全
APT 编译期校验 → 缺少 @AfEntity、字段类型不支持等错误即时报告
```

#### 4. 开箱即用

每个功能都有本地降级实现，不引入外部依赖即可运行。引入外部依赖后自动升级为生产级实现。

```
无 Redis → Caffeine 本地缓存 + 内存锁 + 本地调度
有 Redis → Redisson 分布式缓存/锁/调度，自动升级
无 AI 引擎 → NoOp 默认实现，框架正常运行
有 Spring AI / LangChain4J → 自动发现并装配
```

#### 5. 模块化按需加载

引入即生效，不引入零侵入。每个模块通过 `@ConditionalOnBean` / `@ConditionalOnClass` 自动发现和装配。

```
引入 afg-framework-ai-core → AI 能力自动可用
不引入 → 零 AI 相关 Bean 注册，零性能开销
```

### 1.3 框架价值观

框架的文化决定了 API 的设计风格和开发者的使用体验：

#### 1. 简洁胜于复杂

一个 DataManager 替代 JPA Repository + Service + Query 三层。声明式优于编程式，链式优于嵌套，Lambda 优于字符串。

```java
// 期望的开发者体验：一行代码完成查询
dataManager.entity(User.class)
    .query()
    .where(builder(User.class).eq(User::getStatus, 1).build())
    .page(PageRequest.of(1, 20));
```

#### 2. 安全是基础设施

多租户、数据权限、审计不是可选插件，而是默认行为。框架自动为查询注入租户过滤和数据权限条件，开发者无需手动处理。

```java
// 框架自动注入租户和数据权限条件，开发者无感知
dataManager.findAll(User.class);  // 自动过滤当前租户 + 数据权限范围
```

#### 3. AI 是一等公民

AI 能力与数据访问、安全同等重要。Chat、Agent、Workflow、RAG、Tool 全链路 AI 能力，双引擎适配（Spring AI / LangChain4J），声明式注解驱动。

```java
@AiChat(client = "default", systemPrompt = "你是助手")
public String chat(String message) { ... }
```

#### 4. 企业级是底线

多租户、数据权限、审计、国际化、软删除、乐观锁——这些不是"高级功能"，是"基本要求"。框架默认提供，开发者无需手动拼装。

#### 5. 优雅降级

分布式不可用时本地兜底，外部依赖不可用时 NoOp 兜底。框架在任何环境下都能正常运行，不会因为缺少某个依赖而启动失败。

### 1.4 开发者体验理念

框架的 API 设计遵循以下优先级：

1. **声明式 > 编程式** — `@AiChat` 优于手动调用 ChatClient
2. **链式 > 嵌套** — `dataManager.entity(User.class).query().where(condition).list()` 优于 `query.setParameter(...)`
3. **Lambda > 字符串** — `User::getStatus` 优于 `"status"`
4. **注解 > 配置** — `@DistributedTask` 优于 `afg.scheduler.task.xxx.enabled=true`
5. **自动发现 > 手动注册** — `@ConditionalOnBean` 优于 `@Bean` 手动声明
6. **默认安全 > 默认开放** — 新增 API 默认需要认证，显式 `permitAll()` 才开放

### 1.5 质量底线

框架的每个功能必须满足以下底线：

1. **每个功能必须有 NoOp 降级实现** — 不引入外部依赖即可运行
2. **每个功能必须有开/关注解或条件** — `@ConditionalOnXxx` + `enabled` 开关
3. **每个功能必须有测试覆盖** — 集成测试 + Testcontainers，禁止 Mockito
4. **每个 AutoConfiguration 必须声明依赖排序** — `@AutoConfigureAfter` / `@AutoConfigureBefore`
5. **每个配置项必须有合理默认值** — 零配置即可运行
6. **每个 SPI 必须有本地默认实现** — 不依赖外部实现即可工作

### 1.6 核心价值（差异化优势）

AFG Framework 在以下 5 个领域是**全球唯一的开源框架**：

| 差异化能力 | 说明 | 竞品现状 |
|-----------|------|---------|
| **APT 零反射 DataManager** | 编译时生成实体元数据，运行时零反射，类型安全条件查询 | 所有竞品依赖运行时反射 |
| **三种多租户隔离模式** | 共享数据库 / 独立数据库 / 混合模式，开源提供 | 仅 BladeX 商业版有（付费） |
| **行级数据权限自动注入** | DataScopeType 自动为查询注入条件，开发者无感知 | 所有竞品需手动实现 |
| **AI 韧性 + 安全 + 审计** | @AiResilient / @ContentSafety / @AiAudited 声明式注解 | 所有竞品无此能力 |
| **AI 工作流 DAG 引擎** | 37 种内置节点，DAG 执行，Checkpoint，人机交互 | 所有竞品无内置 AI 工作流 |

### 1.7 目标用户

- 企业级 Java 后端开发团队（从 3 人初创到 300 人企业）
- 需要微服务/单体灵活切换的架构团队
- 需要集成 AI 能力的业务系统
- 国产化信创环境下的 Java 开发者
- 从 MyBatis/JPA 迁移的开发者

### 1.8 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.0.6 |
| 构建 | Gradle | Kotlin DSL |
| 安全 | Spring Security + jCasbin | 1.9.2 |
| AI - Spring AI | Spring AI | 2.0.0-M7 |
| AI - LangChain4J | LangChain4J | 1.15.1 |
| 数据库迁移 | Liquibase | BOM 管理 |
| 缓存 | Caffeine + Redis (Redisson 4.3.1) | — |
| 消息队列 | Spring AMQP (RabbitMQ) / Spring Kafka | BOM 管理 |
| 文件存储 | Local（框架内置） | — |
| gRPC | spring-grpc | 1.0.3 |
| Protobuf | protobuf-java | 3.25.8 |
| 代码质量 | PMD 7.23.0 + JaCoCo 0.8.14 | — |
| 安全扫描 | OWASP Dependency Check 12.2.2 | CVSS ≥ 7.0 阻断构建 |
| 序列化 | Jackson 2.x（Jackson 3 迁移待第三方库适配） | BOM 管理 |
| 校验 | Jakarta Validation + Hibernate Validator | BOM 管理 |
| API 文档 | SpringDoc OpenAPI | 3.0.3 |
| DTO 转换 | MapStruct | BOM 管理 |

---

## 2. 快速开始

### 2.1 环境要求

- JDK 25+
- Gradle 8.x（通过 Wrapper）
- 数据库（MySQL 8.0+ / PostgreSQL 15+ / Oracle 19c+ / H2 2.x / 达梦 / 金仓 / GaussDB / OceanBase / openGauss / SQL Server）

### 2.2 创建项目

**build.gradle.kts：**

```kotlin
plugins {
    id("io.github.afg-projects.afg-framework") version "1.0.0-SNAPSHOT"
}

afg {
    springBootVersion.set("4.0.6")
    frameworkVersion.set("1.0.0-SNAPSHOT")
    standalone.set(true)
    useLombok.set(true)
    enableCodegen.set(true)
    basePackage.set("com.example.demo")
    securityMode.set("MONOLITH")
    databaseType.set("mysql")
}

dependencies {
    // 核心模块（自动引入 commons + apt-api + apt-impl + core + data-core + data-jdbc）
    // 已由 Gradle 插件自动添加

    // 安全模块（MONOLITH 模式自动引入 auth-server）
    // 已由 Gradle 插件根据 securityMode 自动添加

    // 可选：AI 能力
    implementation("io.github.afg-projects:afg-framework-ai-core")
    implementation("io.github.afg-projects:afg-framework-ai-langchain4j")

    // 可选：Redis 分布式能力
    implementation("io.github.afg-projects:afg-framework-afg-redis")
}
```

### 2.3 最小配置

**application.yml：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo
    username: root
    password: root

afg:
  security:
    auth-server:
      enabled: true
      token:
        signing-key: your-secret-key-at-least-256-bits-long
```

> 仅需数据库连接和安全密钥。框架其余功能零配置即可运行。

### 2.4 创建第一个实体

```java
@Getter @Setter
@AfEntity
@Table(name = "sys_user")
public class User extends SoftDeleteEntity {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "status")
    private Integer status = 1;
}
```

### 2.5 创建第一个 Controller

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final DataManager dataManager;

    @PostMapping
    @Transactional
    public Result<User> create(@RequestBody User user) {
        return Result.success(dataManager.save(User.class, user));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return dataManager.findById(User.class, id)
            .map(Result::success)
            .orElse(Result.fail(CommonErrorCode.NOT_FOUND));
    }

    @GetMapping
    public Result<PageData<User>> list(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Condition condition = builder(User.class)
            .likeIfPresent(User::getUsername, username)  // null 时自动跳过
            .eq(User::getStatus, 1)
            .build();

        PageData<User> result = dataManager.entity(User.class)
            .query()
            .where(condition)
            .page(PageRequest.of(page, size));

        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        dataManager.deleteById(User.class, id);  // 软删除自动执行
        return Result.success();
    }
}
```

### 2.6 启动并验证

```bash
./gradlew bootRun

# 创建用户
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com"}'

# 查询用户列表
curl http://localhost:8080/users

# 登录获取 Token
curl -X POST http://localhost:8080/auth-api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 2.7 添加 AI 对话

```java
@Service
public class ChatService {

    @AiChat(client = "default", systemPrompt = "你是一个智能助手")
    public String chat(String message) {
        return message;  // 由框架自动调用 AI 模型
    }
}
```

```yaml
afg:
  ai:
    chat:
      enabled: true
    # LLM 配置由 Spring AI 或 LangChain4J 的原生配置管理
```

---

## 3. 核心概念

### 3.1 DataManager vs JPA Repository

| 维度 | JPA Repository | DataManager |
|------|---------------|-------------|
| **设计理念** | 每个实体一个 Repository | 一个 DataManager 操作所有实体 |
| **元数据加载** | 运行时反射 | APT 编译时生成，运行时零反射 |
| **查询方式** | 方法名派生 / @Query / Specification | Lambda 条件构建器 + 链式查询 |
| **多租户** | Hibernate Filter（需手动配置） | 自动注入租户条件（零配置） |
| **数据权限** | 无 | 自动注入数据权限条件（零配置） |
| **软删除** | @Where 注解（需手动） | 自动过滤（零配置） |
| **聚合查询** | 无内置 | 内置 GROUP BY + HAVING + 聚合函数 |
| **关联加载** | N+1 问题 / FETCH JOIN | 预加载 / 批量加载 / 延迟加载 |
| **DTO 投影** | Interface Projection / Class Projection | ProjectedQuery + Lambda 字段选择 |
| **缓存** | 二级缓存（复杂） | EntityCache + Caffeine/Redis（简单） |
| **依赖** | Hibernate EntityManager | 纯 JDBC |

**从 JPA 迁移**：

| JPA | AFG |
|-----|-----|
| `UserRepository extends JpaRepository` | `DataManager` |
| `repository.findById(id)` | `dataManager.findById(User.class, id)` |
| `repository.save(user)` | `dataManager.save(User.class, user)` |
| `@Query("WHERE status = ?1")` | `Conditions.builder(User.class).eq(User::getStatus, 1)` |

**从 MyBatis-Plus 迁移**：

| MyBatis-Plus | AFG |
|-------------|-----|
| `BaseMapper<User>` | `DataManager` |
| `mapper.selectById(id)` | `dataManager.findById(User.class, id)` |
| `LambdaQueryWrapper<User>` | `Conditions.builder(User.class)` |
| `Page<User>` | `PageData<User>` |
| `@TableLogic` | 继承 `SoftDeleteEntity` |
| `TenantLineInnerInterceptor` | 自动注入（零配置） |

### 3.2 APT 编译时元数据

**为什么用 APT？**

传统 ORM 框架在运行时通过反射读取实体类的注解（`@Table`、`@Column`），生成 SQL。这有两个问题：
1. **运行时性能损耗** — 每次启动都要反射扫描
2. **错误发现太晚** — 字段名拼错、类型不匹配等错误在运行时才暴露

APT（Annotation Processing Tool）在**编译时**处理注解，生成元数据类，运行时直接使用，零反射开销。

**工作流程**：

```
编译时：
  实体类 (@AfEntity + @Table + @Column)
    → APT 处理器 (EntityMetadataProcessor)
    → {Entity}Metadata.java（编译时生成的元数据类）

运行时：
  DataManager → EntityMetadataCache
    → AptMetadataLoader（优先，加载 APT 生成的类）
    → ReflectiveMetadataLoader（降级，运行时反射）
```

**对开发者的影响**：

- 编译期即可发现：缺少 `@AfEntity`、字段类型不支持、表名冲突等错误
- IDE 中可直接跳转到 `UserMetadata.TABLE_NAME`、`UserMetadata.USERNAME` 等常量
- Lambda 条件查询 `User::getUsername` 由 APT 生成的元数据提供类型安全保证

### 3.3 自动配置约定

框架基于 Spring Boot AutoConfiguration 机制，遵循以下约定：

**启用优先级**：

```
1. 自动装配（优先）— @ConditionalOnClass / @ConditionalOnBean 自动检测
2. 注解启用（次选）— @AiChat / @DistributedTask 等注解自动触发
3. 配置属性（最后）— 仅覆盖默认值时需要配置
```

**引入即生效**：

| 操作 | 框架行为 |
|------|---------|
| 引入 `afg-framework-data-jdbc` | DataManager 自动注册，JDBC 增强可用 |
| 引入 `afg-framework-afg-redis` | 分布式缓存/锁/调度自动升级，替换本地实现 |
| 引入 `afg-framework-ai-core` | AI SPI 接口 + NoOp 默认实现自动注册 |
| 引入 `afg-framework-ai-langchain4j` | LangChain4J 实现自动装配，替换 NoOp |
| 引入 `afg-framework-auth-server` | OAuth2 授权服务器自动可用 |

### 3.4 实体基类选择决策树

```
你的实体需要什么？
│
├─ 只需要 id + 时间戳
│   └─ BaseEntity
│
├─ 需要多租户隔离？
│   ├─ 是 → TenantEntity（+ 根据下方需求继续叠加）
│   └─ 否 → 继续往下看
│
├─ 需要软删除？
│   ├─ 只需 deleted 标记 → SoftDeleteEntity
│   └─ 需要删除时间 → TimestampSoftDeleteEntity
│
├─ 需要乐观锁？
│   └─ VersionedEntity
│
├─ 需要审计（谁创建/谁修改）？
│   └─ 继续看 FullEntity
│
└─ 需要全部功能（软删除 + 乐观锁 + 审计）？
    └─ FullEntity
```

**基类组合规则**：

| 需求 | 基类 | 特征接口 |
|------|------|---------|
| 基础 | `BaseEntity` | — |
| 多租户 | `TenantEntity` | — |
| 软删除 | `SoftDeleteEntity` | `SoftDeletable` |
| 软删除+时间戳 | `TimestampSoftDeleteEntity` | `TimestampSoftDeletable` |
| 乐观锁 | `VersionedEntity` | `Versioned` |
| 全功能 | `FullEntity` | `SoftDeletable` + `Versioned` + `Auditable` |

> **注意**：`FullEntity` 不继承 `TenantEntity`。需要多租户 + 全功能时，继承 `FullEntity` 并手动添加 `tenantId` 字段。

### 3.5 模块化架构理念

框架通过 `@AfgModuleAnnotation` 实现模块注册和自动发现：

```java
@AfgModuleAnnotation(
    name = "认证授权模块",
    contextPath = "/auth-api",
    dependencies = {"core", "data-jdbc"}
)
public class AuthModuleConfig {}
```

**设计思想**：
- 每个模块声明自己的 Context-Path，框架自动为该模块的 Controller 添加路径前缀
- 模块声明依赖关系，框架按依赖顺序加载 AutoConfiguration
- 模块是 Spring Boot 应用的"功能单元"，而非独立的微服务

### 3.6 增强而非替代原则

框架的核心边界原则：

| 原则 | 说明 |
|------|------|
| **框架不重新实现 Spring Boot 已有的能力** | 数据库连接池用 HikariCP、Web 容器用 Tomcat/Undertow、JSON 用 Jackson |
| **框架在 Spring Boot 之上做增强** | DataManager 增强 JdbcTemplate、AfgCache 增强 Spring Cache、@AiChat 增强 Spring AI |
| **开发者可以同时使用框架和 Spring Boot 原生** | 可以用 DataManager，也可以直接注入 JdbcTemplate；可以用 @AiChat，也可以直接用 ChatClient |
| **框架独创的能力无 Spring Boot 对应物** | APT 元数据、数据权限自动注入、AI 韧性注解、DAG 工作流 |

---

## 4. 模块架构

### 4.1 模块清单

框架由 **20 个 Gradle 子模块** 组成，按职责分为 7 大类：

```
afg-framework/
├── 通用层
│   ├── commons/                    # 通用工具（最底层，零框架依赖）
│   ├── apt-api/                    # APT 注解定义
│   └── apt-impl/                   # APT 处理器实现
│
├── 核心层
│   └── core/                       # 框架核心（31+ AutoConfiguration）
│
├── 数据层
│   ├── data-core/                  # 数据访问抽象（DataManager 接口、实体基类、条件构建器）
│   └── data-impl/
│       ├── data-sql/               # SQL 解析与构建器
│       ├── data-jdbc/              # JDBC 增强实现（JdbcDataManager + 审计日志存储）
│       └── data-liquibase/         # Liquibase 数据库迁移基础设施
│
├── 安全层
│   ├── security-core/              # 安全 SPI 接口
│   └── security-impl/
│       ├── auth-server/            # 认证服务器（9 AutoConfiguration）
│       └── resource-server/        # 资源服务器（2 AutoConfiguration）
│
├── AI 层
│   ├── ai-core/                    # AI 核心（16 AutoConfiguration）
│   └── ai-impl/
│       ├── ai-spring-ai/           # Spring AI 适配
│       └── ai-langchain4j/         # LangChain4J 适配
│
├── 集成层
│   └── afg-redis/                  # Redis/Redisson（缓存、锁、限流、调度、审计存储）
│
├── 治理层
│   └── governance/
│       ├── proto/                  # gRPC Proto 定义
│       ├── client/                 # 客户端 SDK
│       └── server/                 # 服务端模块
│
└── 工具层
    └── gradle-plugin/              # 自定义 Gradle 插件
```

### 4.2 依赖关系

```
commons（零依赖）
  └→ data-core
  │    └→ data-sql（SQL 构建器实现）
  │    └→ data-jdbc ←── core（JdbcDataManager + 审计日志存储）
  │         └→ data-liquibase（数据库迁移）
  └→ core
  │    └→ ai-core → ai-spring-ai / ai-langchain4j（AI 适配）
  └→ security-core
       └→ auth-server ←── data-jdbc + core
       └→ resource-server ←── data-jdbc + core
governance/proto（零框架依赖）
  └→ governance/client ←── proto + core
       └→ governance/server ←── client + core + data-jdbc + auth-server + data-liquibase
afg-redis ←── core（Redis/Redisson 实现 core 定义的分布式 SPI）
```

**依赖规则**：
- 依赖方向：从底层到上层，禁止反向依赖和循环依赖
- `commons` 和 `governance/proto` 是零依赖模块
- 集成模块（afg-redis）依赖 core 定义的 SPI 接口，不依赖其他集成模块

### 4.3 Maven 坐标

- **Group ID：** `io.github.afg-projects`
- **Artifact ID：** `afg-framework-{module-name}`
- **版本：** `1.0.0-SNAPSHOT`
- **发布目标：** Maven Central（通过 vanniktech.maven.publish 插件自动签名 + 发布）

| 模块 | Artifact ID |
|------|------------|
| 通用工具 | `afg-framework-commons` |
| APT 注解 | `afg-framework-apt-api` |
| APT 实现 | `afg-framework-apt-impl` |
| 核心 | `afg-framework-core` |
| 数据抽象 | `afg-framework-data-core` |
| SQL 构建 | `afg-framework-data-sql` |
| JDBC 实现 | `afg-framework-data-jdbc` |
| 数据库迁移 | `afg-framework-data-liquibase` |
| 安全 SPI | `afg-framework-security-core` |
| 认证服务器 | `afg-framework-auth-server` |
| 资源服务器 | `afg-framework-resource-server` |
| AI 核心 | `afg-framework-ai-core` |
| Spring AI 适配 | `afg-framework-ai-spring-ai` |
| LangChain4J 适配 | `afg-framework-ai-langchain4j` |
| Redis 集成 | `afg-framework-afg-redis` |
| 治理 Proto | `afg-framework-governance-proto` |
| 治理客户端 | `afg-framework-governance-client` |
| 治理服务端 | `afg-framework-governance-server` |
| Gradle 插件 | `afg-framework-gradle-plugin` |

### 4.4 模块选择指南

| 场景 | 需要的模块 | 最小依赖 |
|------|-----------|---------|
| 基础 CRUD 应用 | core + data-jdbc | `afg-framework-core` + `afg-framework-data-jdbc` |
| 需要多租户 | 上述 + auth-server | + `afg-framework-auth-server` |
| 需要资源服务器 | core + resource-server | `afg-framework-core` + `afg-framework-resource-server` |
| 需要 AI 对话 | 上述 + ai-core + ai-langchain4j | + `afg-framework-ai-core` + `afg-framework-ai-langchain4j` |
| 需要 RAG 知识库 | 上述 + 向量数据库 | + AI 模块 + 向量数据库驱动 |
| 需要分布式缓存/锁 | 上述 + afg-redis | + `afg-framework-afg-redis` |
| 需要配置中心 | 上述 + governance-client | + `afg-framework-governance-client` |
| 完整单体应用 | 全部 | 所有模块 |

> Gradle 插件根据 `securityMode` 和 `basePackage` 自动添加核心依赖，开发者只需添加可选模块。

### 4.5 模块与 Spring Boot 原生功能对比

| 功能 | Spring Boot 原生 | AFG 框架增强 |
|------|-----------------|-------------|
| 数据访问 | JPA / JdbcTemplate | DataManager（APT 零反射 + 类型安全 + 多租户/数据权限自动注入） |
| 安全 | Spring Security | OAuth2 授权服务器 + Casbin RBAC + 多租户 + 数据权限 + 登录策略 |
| 缓存 | @Cacheable + CacheManager | AfgCache + 三级缓存（本地/分布式/多级）+ 实体缓存 |
| 事件 | ApplicationEventPublisher | EventPublisher + 分布式事件 + 重试 + 死信 |
| 调度 | @Scheduled | @ScheduledTask + @DistributedTask + 动态任务 + 延迟队列 |
| 校验 | Bean Validation | 统一校验异常处理 → Result.fail |
| AI | 无 | Chat/Agent/Workflow/RAG/Tool 全链路 |
| 多租户 | 无 | 三种隔离模式 + 自动注入 |
| 数据权限 | 无 | 行级自动注入 + 5 种 DataScopeType |
| 审计 | Actuator Events | 统一审计框架（数据/登录/AI/工作流） |
| 导入/导出 | 无 | 注解驱动 + Excel/CSV + 流式处理 |
| 状态机 | 无 | 轻量级 @StateMachine |

---

## 5. 逐模块功能需求

> 每个模块按 6 段式模板描述：一句话定位 → 使用场景 → 期望 API 体验 → 完整功能清单 → 配置属性 → 注意事项/限制
> 每个模块包含"与 Spring Boot 原生对比"说明框

### 5.1 Commons 通用模块

**一句话定位**：框架最底层的通用工具集，零框架依赖，提供统一响应、异常体系、命名工具和通用工具类。

**使用场景**：
- 所有 API 响应统一包装为 `Result<T>`
- 业务异常抛出 `BusinessException`，框架自动转换为错误响应
- 字段命名转换（Java camelCase ↔ 数据库 snake_case）
- 日期、集合、字符串等日常工具操作

#### 期望 API 体验

```java
// 统一响应
Result.success(user)                          // → {"code":0,"message":"success","data":{...}}
Result.fail(CommonErrorCode.NOT_FOUND)         // → {"code":10100,"message":"资源不存在"}
Result.fail(10001, "用户名已存在")               // → {"code":10001,"message":"用户名已存在"}

// 分页
PageData.of(records, 100, 1, 20)              // → PageData(records, total=100, page=1, size=20, pages=5)
page.hasNext()                                 // → true
page.records()                                 // → List<T>

// 异常
throw BusinessException.of(CommonErrorCode.PARAM_ERROR, "参数不能为空");
throw BusinessException.of(CommonErrorCode.ENTITY_NOT_FOUND, "用户 %s 不存在", username);
exception.getMessage(Locale.CHINA)              // → i18n 消息

// 命名
NamingUtils.toSnakeCase("userName")            // → "user_name"
NamingUtils.toCamelCase("user_name")           // → "userName"
```

#### 完整功能清单

| 类 | 功能 |
|----|------|
| `Result<T>` | 统一 API 响应封装。成功 `code=0`，失败携带错误码和消息。支持 `success(data)` / `fail(code, message)` / `fail(ErrorCode)` |
| `PageData<T>` | 分页数据封装。`records` / `total` / `page` / `size` / `pages` / `hasNext` / `hasPrevious`。工厂方法 `of(records, total, page, size)` |
| `AfgException` | 抽象基类，继承 `RuntimeException` |
| `BusinessException` | 业务异常，携带 `ErrorCode` + `businessMessage` + `arg[]`（消息模板参数）+ `getMessage(Locale)`（i18n） |
| `ErrorCode` | 错误码接口（`getCode()` / `getMessage()` / `getCategory()`） |
| `ErrorCategory` | 错误分类枚举：`BUSINESS("B")` / `SYSTEM("S")` / `NETWORK("N")` / `SECURITY("A")` |
| `CommonErrorCode` | 94 个标准错误码枚举（范围 10000-19999） |
| `NamingUtils` | 命名转换：`toSnakeCase()` / `toCamelCase()` / `capitalize()` / `uncapitalize()` |
| `ArgumentAssert` | 参数断言工具：`notNull()` / `notEmpty()` / `isTrue()` / `state()` — 断言失败抛 `BusinessException` |
| `DateUtils` | 日期工具：`format()` / `parse()` / `between()` / `isExpired()` |
| `CollectionUtils` | 集合工具：`isEmpty()` / `isNotEmpty()` / `first()` / `last()` / `partition()` |
| `StringUtils` | 字符串工具：`isBlank()` / `truncate()` / `join()` / `splitAndTrim()` |
| `IoUtils` | IO 工具：`readAsString()` / `copy()` / `closeQuietly()` |

#### 配置属性

commons 模块无配置属性，纯工具类。

#### 注意事项/限制

- `CommonErrorCode` 的错误码区间分配：通用(10001)、资源(10100)、请求(10200)、限流(10300)、认证(10400)、数据(11000)、存储(12000)、任务(13000)、HTTP(14000)、模块(15000)、配置(16000)、功能开关(17000)、系统(19000)
- 业务应用的自定义错误码应从 20000 开始，避免与框架错误码冲突
- `BusinessException.getMessage(Locale)` 需要 `messages.properties` 资源文件配合，key 为错误码数字

> **与 Spring Boot 原生对比**：
> - Boot 原生无统一响应格式 — 框架提供 `Result<T>` + `PageData<T>`
> - Boot 原生异常处理需手动 `@ControllerAdvice` — 框架提供 `GlobalExceptionHandler` + `CommonErrorCode`
> - Boot 原生无参数断言工具 — 框架提供 `ArgumentAssert`

---

### 5.2 APT 注解处理模块（apt-api + apt-impl）

**一句话定位**：编译时注解处理器，生成实体元数据和服务元数据，实现零反射运行时和编译期类型安全。

**使用场景**：
- 实体类标注 `@AfEntity`，APT 自动生成 `{Entity}Metadata.java`
- 服务类标注 `@AfService`，APT 自动生成 `{Service}Metadata.java`
- 模块类标注 `@AfgModuleAnnotation`，APT 自动生成模块索引文件

#### 期望 API 体验

```java
// 定义实体 — APT 编译时自动生成 UserMetadata.java
@Getter @Setter
@AfEntity
@Table(name = "sys_user")
public class User extends SoftDeleteEntity {
    @Column(name = "username", nullable = false, length = 50)
    private String username;
}

// APT 生成的元数据（编译时可用，运行时零反射）
public class UserMetadata {
    public static final String TABLE_NAME = "sys_user";
    public static final SFunction<User, ?> USERNAME = User::getUsername;
    public static final FieldMetadata USERNAME_FIELD = FieldMetadata.builder()
        .name("username").columnName("username").type(String.class)
        .nullable(false).length(50).build();
    // ... 其他字段
}

// 编译期错误提示
// 如果实体类缺少 @AfEntity，编译时报告：
// "类 User 使用了 @Table 注解但缺少 @AfEntity，DataManager 将无法识别此实体"
```

#### 完整功能清单

**注解定义（apt-api）**：

| 注解 | 目标 | 功能 | 关键属性 |
|------|------|------|---------|
| `@AfEntity` | TYPE | 触发 APT 元数据生成 | `tableName` / `generateRelations` |
| `@CommonFieldDefinition` | FIELD, TYPE | 可复用字段元数据 | `name` / `propertyName` / `columnName` / `fieldType` / `isId` / `isGenerated` |
| `@CommonFieldDefinitions` | TYPE | `@CommonFieldDefinition` 容器 | — |
| `@AfService` | TYPE | 标记为动态可调用服务 | `name` / `description` / `category` / `tags` / `deprecated` |
| `@AfOperation` | METHOD | 标记可调用操作 | `name` / `description` / `async` / `permission` / `requiredRoles` / `audit` / `tenantScope` / `dataScope` |
| `@AfParam` | PARAMETER | 参数元数据 | `name` / `description` / `required` / `defaultValue` / `enumValues` |
| `@AfResult` | METHOD | 返回值元数据 | `description` / `paged` / `streaming` |
| `@AfgModuleAnnotation` | TYPE | 模块注册 | `id` / `name` / `basePackage` / `contextPath` / `dependencies` / `version` / `description` / `configFile` |
| `@AfgEnum` | TYPE | 枚举元数据 | `valueField` / `labelField` / `i18nPrefix` |
| `@EncryptedField` | FIELD | 字段级加密标记 | `algorithm` / `keyRef` |

**处理器实现（apt-impl）**：

| 处理器 | 输出 | 功能 |
|--------|------|------|
| `EntityMetadataProcessor` | `{Entity}Metadata.java` | 实体字段元数据、表名映射、关联关系、特征检测 |
| `ServiceMetadataProcessor` | `{Service}Metadata.java` | 服务操作元数据、参数描述、返回值信息 |
| `AfgModuleAnnotationProcessor` | `META-INF/afg/modules/{module}` | 模块索引文件，自动收集 `@AfgModuleAnnotation` |
| `EnumMetadataProcessor` | `{Enum}Metadata.java` | 枚举值映射、i18n 标签、数据库值转换 |

**编译期校验（新增）**：

| 校验规则 | 错误级别 | 说明 |
|---------|---------|------|
| `@Table` 存在但缺少 `@AfEntity` | ERROR | DataManager 无法识别此实体 |
| `@AfEntity` 实体字段类型不支持 | ERROR | 列出支持的类型清单 |
| 表名冲突（多个实体映射同一表） | ERROR | 编译期检测，避免运行时混淆 |
| `@Column(name)` 重复 | WARNING | 同一实体内列名重复 |
| `@ManyToOne`/`@OneToMany` 目标实体未标注 `@AfEntity` | ERROR | 关联目标必须也是 APT 实体 |

#### 配置属性

APT 模块无运行时配置属性。编译行为通过 Gradle 插件的 `afg { enableCodegen.set(true) }` 控制。

#### 注意事项/限制

- APT 生成的类在 `build/generated/sources/annotationProcessor` 目录下，IDE 需要标记为源码目录
- 增量编译支持：APT 处理器需声明 `@SupportedAnnotationTypes` 和增量编译能力
- 元数据缓存：编译时生成的类会被 JVM 缓存，全量编译时自动刷新

> **与 Spring Boot 原生对比**：
> - Boot 原生 JPA 使用运行时反射读取实体注解 — AFG 使用 APT 编译时生成元数据
> - Boot 原生无编译期实体校验 — AFG 在编译期检测缺少注解、类型不支持等错误
> - Boot 原生无枚举元数据生成 — AFG 生成枚举映射和 i18n 标签

---

### 5.3 Core 核心模块

**一句话定位**：框架的"大管家"，提供缓存、锁、事件、调度、审计、国际化、功能开关、校验、异常处理等横切面能力。

**使用场景**：
- 方法级缓存 `@Cacheable`、声明式锁 `@Lock`
- 业务事件发布/订阅、分布式事件
- 定时任务 `@ScheduledTask`、分布式调度 `@DistributedTask`
- 操作审计 `@Audited`、访问日志
- Bean Validation 校验异常统一处理
- 功能开关 `@FeatureToggle`、重复提交防护 `@DuplicateSubmit`
- 国际化 `LocaleFilter` + 错误码 i18n

#### 期望 API 体验

```java
// ===== 缓存 =====
@Cacheable(cacheName = "users", key = "#id")
public User getUser(Long id) { ... }

AfgCache<User> cache = cacheManager.getCache("users");
cache.put("key", user, Duration.ofMinutes(30));

// ===== 分布式锁 =====
@Lock(key = "'order:' + #orderId", lockType = LockType.REENTRANT, waitTime = 5000)
public Order processOrder(Long orderId) { ... }

DistributedLock lock = applicationContext.getBean(DistributedLock.class);
lock.lock("order:123", () -> doSomething());

// ===== 事件 =====
domainEventPublisher.publish(new UserCreatedEvent(user));

@DomainEventListener
public void onUserCreated(UserCreatedEvent event) { ... }

// ===== 调度 =====
@ScheduledTask(cron = "0 0 2 * * ?")
public void cleanupExpiredSessions() { ... }

@DistributedTask(cron = "0 */5 * * * ?", sharded = true)
public void syncData() { ... }

// ===== 审计 =====
@Audited(action = "DELETE_USER", recordArgs = true, sensitiveFields = {"password"})
public void deleteUser(Long id) { ... }

// ===== 访问日志 =====
// 自动记录每个请求：method, path, status, duration, userId, tenantId, clientIp
// 无需注解，自动生效，可配置排除路径

// ===== 重复提交防护 =====
@DuplicateSubmit(interval = 3000)
@PostMapping("/orders")
public Result<Order> createOrder(@RequestBody OrderRequest request) { ... }

// ===== 功能开关 =====
@FeatureToggle("new-search-algorithm")
@GetMapping("/search")
public Result<List<Item>> search(String keyword) { ... }

// ===== 校验 =====
// Bean Validation 校验失败自动转为 Result.fail(CommonErrorCode.PARAM_ERROR, details)
// 无需手动捕获 MethodArgumentNotValidException

// ===== 导入/导出 =====
@ExcelSheet(name = "用户列表")
public class UserExportVO {
    @ExcelColumn(name = "用户名", order = 1)
    private String username;

    @ExcelColumn(name = "状态", order = 2, enumConverter = UserStatus.class)
    private Integer status;
}

// 导出
ExcelExporter.export(users, UserExportVO.class, response.getOutputStream());

// 导入
ImportResult<UserImportVO> result = ExcelImporter.importAs(file.getInputStream(), UserImportVO.class);

// ===== 状态机 =====
@StateMachine(entity = Order.class)
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    @Transition(from = PENDING, to = CONFIRMED)
    public void confirm(Order order) { ... }

    @Transition(from = CONFIRMED, to = CANCELLED)
    public void cancel(Order order) { ... }
}

// ===== 枚举管理 =====
@AfgEnum(valueField = "code", labelField = "label", i18nPrefix = "enum.user-status")
public enum UserStatus {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");

    private final int code;
    private final String label;
}

// ===== 通知 =====
notificationService.send(Notification.builder()
    .to(userId)
    .channel(NotificationChannel.EMAIL)
    .template("welcome")
    .variable("username", user.getUsername())
    .build());

// ===== Webhook =====
webhookService.dispatch("order.created", OrderCreatedPayload.from(order));

// ===== SSE =====
@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter events() {
    return sseService.createConnection(userId);
}
```

#### 完整功能清单

**31+ AutoConfiguration**：

| AutoConfiguration | 功能 | 启用方式 |
|-------------------|------|---------|
| `AfgCoreAutoConfiguration` | 框架核心 Bean 初始化 | 自动 |
| `AfgAutoConfiguration` | 框架自动配置入口 | 自动 |
| `ModuleAutoConfiguration` | 模块注册与发现 | 自动 |
| `ModuleWebAutoConfiguration` | 模块 Controller 路径前缀 | 自动 |
| `WebAutoConfiguration` | Web 层通用配置 | 自动 |
| `HttpClientAutoConfiguration` | HTTP 客户端（重试+熔断+追踪） | 自动 |
| `LoggingAutoConfiguration` | 结构化日志 + MDC 增强 | 自动 |
| `MetricsAutoConfiguration` | Micrometer 指标采集 | 自动 |
| `HealthAutoConfiguration` | 健康检查端点 | 自动 |
| `ShutdownAutoConfiguration` | 优雅停机 | 自动 |
| `EncryptionAutoConfiguration` | 配置加密（ENC() 前缀） | 自动 |
| `RemoteConfigAutoConfiguration` | 远程配置拉取 | Governance 可用时自动 |
| `CacheAutoConfiguration` | 缓存管理（本地/分布式/多级） | 自动 |
| `LockAutoConfiguration` | 分布式锁 | 自动（有 DistributedLock Bean 时升级） |
| `MultiDataSourceAutoConfiguration` | 多数据源 + 读写分离 | `afg.core.multi-datasource.enabled=true` |
| `VirtualThreadAutoConfiguration` | 虚拟线程 | Java 21+ 自动 |
| `AfgSecurityAutoConfiguration` | 安全基础设施 | 自动 |
| `SignatureAutoConfiguration` | API 签名验证 | `@SignatureRequired` 注解触发 |
| `RateLimitAutoConfiguration` | 接口限流 | `@RateLimited` 注解触发 |
| `DataScopeAutoConfiguration` | 数据权限基础设施 | 自动 |
| `AuditLogAutoConfiguration` | 审计日志 | `@Audited` 注解触发 |
| `EventAutoConfiguration` | 事件发布/订阅 | 自动 |
| `FeatureFlagAutoConfiguration` | 功能开关 | `@FeatureToggle` 注解触发 |
| `FeatureFlagWebAutoConfiguration` | 功能开关 Web 端点 | 自动 |
| `CloudNativeAutoConfiguration` | 云原生支持 | K8s 环境自动 |
| `KubernetesProbeAutoConfiguration` | K8s 探针 | K8s 环境自动 |
| `LocaleAutoConfiguration` | 国际化 | 自动 |
| `BeanInvocationAutoConfiguration` | Bean 动态调用（@AfService） | `@AfService` 注解触发 |
| `ContextAutoConfiguration` | 请求上下文管理 | 自动 |
| `AfgOpenApiAutoConfiguration` | OpenAPI/Swagger | SpringDoc 可用时自动 |
| `SchedulerAutoConfiguration` | 定时任务 + 分布式调度 | `@ScheduledTask` / `@DistributedTask` 注解触发 |
| `AccessLogAutoConfiguration` | 访问日志过滤器 | 自动（可配置排除路径） |
| `ValidationAutoConfiguration` | Bean Validation + 统一异常处理 | 自动 |
| `SseAutoConfiguration` | SSE 基础设施 | 自动 |
| `StateMachineAutoConfiguration` | 轻量级状态机 | `@StateMachine` 注解触发 |
| `EnumManagementAutoConfiguration` | 枚举元数据管理 | `@AfgEnum` 注解触发 |
| `ImportExportAutoConfiguration` | 导入/导出（Excel/CSV） | `@ExcelSheet` / `@CsvSheet` 注解触发 |
| `NotificationAutoConfiguration` | 通知服务 SPI | 自动 |
| `WebhookAutoConfiguration` | Webhook 分发 | 自动 |
| `DuplicateSubmitAutoConfiguration` | 重复提交防护 | `@DuplicateSubmit` 注解触发 |
| `ApiVersionAutoConfiguration` | API 版本路由 | `@ApiVersion` 注解触发 |
| `IdGeneratorAutoConfiguration` | 分布式 ID 生成 | 自动（替代数据库自增） |

**核心 SPI 接口**：

| 子包 | 核心接口 | 默认实现 |
|------|---------|---------|
| `api.cache` | `AfgCache<V>` / `CacheManager` | Caffeine 本地缓存 |
| `api.lock` | `DistributedLock` | 内存锁（Redis 升级） |
| `api.event` | `EventPublisher<T>` / `EventSubscriber<T>` | `LocalEventPublisher`（RabbitMQ/Kafka 升级） |
| `api.scheduler` | `DistributedTaskScheduler` / `DelayQueue` | `LocalTaskScheduler`（Redis 升级） |
| `api.ratelimit` | `RateLimitStorage` | 内存存储（Redis 升级） |
| `api.audit` | `AuditLogStorage` | 日志输出（JDBC/Redis 升级） |
| `api.feature` | `FeatureFlagManager` | 内存存储（Redis 升级） |
| `api.notification` | `NotificationService` / `NotificationChannel` | LogNotificationService（邮件/短信 SPI） |
| `api.webhook` | `WebhookService` / `WebhookRepository` | 内存注册 + HTTP 分发 |
| `api.statemachine` | `StateMachine<T, S>` / `StateMachineFactory` | 内存状态机 |
| `api.id` | `IdGenerator` | SnowflakeIdGenerator |
| `api.sse` | `SseConnectionManager` | 内存连接管理 |
| `api.importexport` | `DataExporter` / `DataImporter` / `FormatHandler` | Excel (EasyExcel) + CSV |
| `api.encryption` | `FieldEncryptor` | AES 字段加密 |

#### 配置属性

core 模块遵循"引入即生效"原则，大部分功能无需配置。仅以下场景需要覆盖默认值：

```yaml
afg:
  core:
    # 缓存（通常无需配置，默认 Caffeine 本地缓存）
    cache:
      type: LOCAL                          # LOCAL / DISTRIBUTED / MULTI_LEVEL
      local:
        max-size: 1000
        ttl-seconds: 300

    # 事件
    event:
      type: LOCAL                          # LOCAL / RABBITMQ / KAFKA

    # 调度
    scheduler:
      dynamic-task:
        enabled: false                     # 动态任务管理

    # 审计
    audit:
      storage-type: log                    # log / database / redis / none

    # 访问日志
    access-log:
      enabled: true
      exclude-paths: /health,/actuator/**  # 排除路径

    # 多数据源
    multi-datasource:
      enabled: false

    # 分布式 ID
    id-generator:
      type: SNOWFLAKE                      # SNOWFLAKE / SEGMENT / UUID
      snowflake:
        worker-id: 1
        datacenter-id: 1
```

> 基础设施配置（数据库/Redis/MQ 地址）由 Spring Boot 原生管理（`spring.datasource.*` / `spring.data.redis.*`），框架不重复定义。

#### 注意事项/限制

- `CacheAutoConfiguration` 创建 `DefaultCacheManager`（Caffeine），引入 afg-redis 后自动升级为 `RedisCacheManager`
- `LockAutoConfiguration` 创建内存锁，引入 afg-redis 后自动升级为 `RedisDistributedLock`
- `EventAutoConfiguration` 默认使用本地事件，引入 RabbitMQ/Kafka 依赖后自动升级为分布式事件
- 审计日志的存储实现：`log`（默认）/ `database`（data-jdbc 提供）/ `redis`（afg-redis 提供）
- `@DuplicateSubmit` 基于 Redis 实现分布式去重，无 Redis 时降级为内存去重（仅单实例有效）
- 访问日志默认记录所有请求，生产环境建议排除健康检查等高频低价值路径

> **与 Spring Boot 原生对比**：
> - Boot 原生 `@Cacheable` + `ConcurrentMapCacheManager` — AFG 增强：AfgCache 统一接口 + Caffeine 默认 + Redis 自动升级 + 实体缓存
> - Boot 原生无分布式锁 — AFG 提供 `@Lock` 注解式 + 编程式，Redis 自动升级
> - Boot 原生 `ApplicationEventPublisher` — AFG 增强：分布式事件 + 重试 + 死信 + 领域事件
> - Boot 原生 `@Scheduled` — AFG 增强：`@DistributedTask` + 动态任务 + 延迟队列 + 分片
> - Boot 原生 Bean Validation 异常需手动 `@ControllerAdvice` — AFG 自动处理为 `Result.fail`
> - Boot 原生无访问日志 — AFG 提供 `AccessLogFilter`，结构化记录每个请求
> - Boot 原生无重复提交防护 — AFG 提供 `@DuplicateSubmit`
> - Boot 原生无状态机 — AFG 提供轻量级 `@StateMachine`
> - Boot 原生无导入导出 — AFG 提供注解驱动 Excel/CSV
> - Boot 原生无通知服务 — AFG 提供 `NotificationService` SPI


---

### 5.4 DataManager 数据访问模块（data-core + data-sql + data-jdbc + data-liquibase）

**一句话定位**：框架的核心差异化能力——统一数据操作门面，APT 零反射，类型安全条件查询，自动注入多租户/数据权限/软删除过滤。

**使用场景**：
- 所有数据 CRUD 操作通过 DataManager 完成，无需定义 Repository
- 条件查询使用 Lambda 类型安全构建器
- 分页、聚合、关联加载、DTO 投影
- 软删除/乐观锁/多租户/数据权限自动处理
- 数据库迁移通过 Liquibase + Gradle 任务

#### 期望 API 体验

```java
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final DataManager dataManager;

    // ===== 基础 CRUD =====
    @PostMapping
    @Transactional
    public Result<Order> create(@RequestBody Order order) {
        return Result.success(dataManager.save(Order.class, order));
    }

    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        return dataManager.findById(Order.class, id)
            .map(Result::success)
            .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
    }

    // ===== 条件查询（Lambda 类型安全） =====
    @GetMapping("/search")
    public Result<List<Order>> search(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant startTime) {

        // 动态条件：null 值自动跳过
        Condition condition = builder(Order.class)
            .likeIfPresent(Order::getCustomerName, customerName)  // null → 跳过
            .eqIfPresent(Order::getStatus, status)                 // null → 跳过
            .geIfPresent(Order::getCreatedAt, startTime)           // null → 跳过
            .eq(Order::getDeleted, false)                          // 软删除自动过滤，也可显式
            .build();

        return Result.success(dataManager.findList(Order.class, condition));
    }

    // ===== 链式查询 + 分页 =====
    @GetMapping
    public Result<PageData<Order>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageData<Order> result = dataManager.entity(Order.class)
            .query()
            .where(builder(Order.class).eq(Order::getStatus, OrderStatus.ACTIVE).build())
            .orderByDesc(Order::getCreatedAt)
            .page(PageRequest.of(page, size));

        return Result.success(result);
    }

    // ===== DTO 投影查询 =====
    @GetMapping("/summary")
    public Result<List<OrderSummaryDTO>> summary() {
        return Result.success(
            dataManager.entity(Order.class)
                .project()
                .select(Order::getId, Order::getOrderNo, Order::getTotalAmount)
                .where(builder(Order.class).eq(Order::getStatus, OrderStatus.ACTIVE).build())
                .list()
        );
    }

    // ===== 聚合查询 =====
    @GetMapping("/stats")
    public Result<List<AggregateResult>> stats() {
        return Result.success(
            dataManager.entity(Order.class)
                .query()
                .aggregate()
                .groupBy(Order::getStatus)
                .count("id", "orderCount")
                .sum("totalAmount", "totalRevenue")
                .avg("totalAmount", "avgAmount")
                .list()
        );
    }

    // ===== 关联加载 =====
    @GetMapping("/{id}/with-items")
    public Result<Order> getWithItems(@PathVariable Long id) {
        return dataManager.entity(Order.class)
            .query()
            .withAssociation("items")      // 预加载关联
            .where(builder(Order.class).eq(Order::getId, id).build())
            .one()
            .map(Result::success)
            .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
    }

    // ===== 数据权限 =====
    @GetMapping("/my-dept")
    public Result<List<Order>> myDeptOrders() {
        // 自动注入当前用户的数据权限范围
        return Result.success(
            dataManager.entity(Order.class)
                .query()
                .withDataScope()            // 自动注入 DEPT_AND_CHILD 条件
                .list()
        );
    }

    // ===== 多租户 =====
    @GetMapping("/tenant/{tenantId}")
    public Result<List<Order>> tenantOrders(@PathVariable String tenantId) {
        // 显式指定租户（通常由框架自动处理）
        try (var scope = dataManager.tenantScope(tenantId)) {
            return Result.success(dataManager.findAll(Order.class));
        }
    }

    // ===== 软删除 + 恢复 =====
    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        dataManager.deleteById(Order.class, id);  // 软删除实体自动执行软删除
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    @Transactional
    public Result<Void> restore(@PathVariable Long id) {
        dataManager.restoreById(Order.class, id);  // 恢复软删除记录
        return Result.success();
    }

    // ===== 乐观锁 =====
    @PutMapping("/{id}")
    @Transactional
    public Result<Order> update(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        try {
            return Result.success(dataManager.save(Order.class, order));  // version 自动 +1
        } catch (OptimisticLockException e) {
            return Result.fail(CommonErrorCode.OPTIMISTIC_LOCK_ERROR, "数据已被修改，请刷新后重试");
        }
    }

    // ===== 批量操作 =====
    @PostMapping("/batch")
    @Transactional
    public Result<List<Order>> batchCreate(@RequestBody List<Order> orders) {
        return Result.success(dataManager.saveAll(Order.class, orders));
    }

    // ===== 条件更新 =====
    @PatchMapping("/cancel-expired")
    @Transactional
    public Result<Integer> cancelExpired() {
        int affected = dataManager.entity(Order.class)
            .updateAll(
                builder(Order.class)
                    .eq(Order::getStatus, OrderStatus.PENDING)
                    .lt(Order::getCreatedAt, Instant.now().minus(7, ChronoUnit.DAYS))
                    .build(),
                Map.of(Order::getStatus, OrderStatus.CANCELLED)  // Lambda → 字段映射
            );
        return Result.success(affected);
    }

    // ===== 实体变更事件 =====
    // 保存/更新/删除后自动发布 EntityChangedEvent，包含 old/new value diff
    // 无需手动发布，框架自动处理
}
```

#### 完整功能清单

**DataManager — 统一数据操作门面**：

| 操作类别 | 方法 | 说明 |
|----------|------|------|
| **实体代理** | `entity(Class)` | 获取 `EntityProxy<T>` 进行实体操作 |
| **元数据** | `getEntityMetadata(Class)` | 获取实体元数据 |
| **SQL 构建** | `query()` / `update()` / `insert()` / `delete()` | SQL 构建器 |
| **事务** | `executeInTransaction(Supplier)` / `executeInReadOnly(Supplier)` | 编程式事务 |
| **租户** | `tenantScope(tenantId)` | 创建租户作用域（try-with-resources） |
| **原始 SQL** | `executeUpdate()` / `queryForList()` / `queryForObject()` / `queryForCount()` | 原生 SQL 操作 |
| **快捷查询** | `findById()` / `findAll()` / `findOne()` / `findList()` / `existsById()` / `count()` | 实体快捷方法 |
| **快捷写入** | `save()` / `saveAll()` / `insertAll()` | 保存快捷方法 |
| **快捷删除** | `deleteById()` / `deleteAllById()` / `deleteByCondition()` | 删除快捷方法 |
| **快捷恢复** | `restoreById()` | 软删除恢复 |
| **数据权限** | `findListWithDataScope()` | 带数据权限的查询 |
| **字段查询** | `findOneByField()` / `findAllByField()` / `existsByField()` / `countByField()` | Lambda 字段查询 |

**EntityProxy — 实体操作代理**：

| 操作 | 方法 | 说明 |
|------|------|------|
| 查询 | `query()` → `EntityQuery<T>` | 链式查询 |
| 投影 | `project()` → `ProjectedQuery<T, R>` | DTO 投影查询 |
| 保存 | `save(entity)` | 新增或更新（id 为 null 则 insert） |
| 强制插入 | `insert(entity)` | 强制 INSERT |
| 强制更新 | `update(entity)` | 强制 UPDATE |
| 删除 | `deleteById(id)` | 删除（软删除实体执行软删除） |
| 恢复 | `restoreById(id)` | 恢复软删除记录 |
| 条件更新 | `updateAll(condition, updates)` | 批量更新 → 影响行数 |
| 条件删除 | `deleteByCondition(condition)` | 批量删除 → 影响行数 |
| 关联加载 | `fetch(entity, name)` / `fetchAll(entities, name)` | 关联关系加载 |
| 数据权限 | `withDataScope()` / `withDataScopes(...)` | 启用数据权限 |
| 多租户 | `withTenant(tenantId)` | 指定租户 |
| 软删除 | `includeDeleted()` | 包含已软删除记录 |
| 关联预加载 | `withAssociation(name)` / `withAssociations(...)` | 预加载关联 |
| 数据源 | `withDataSource(name)` | 切换数据源 |
| 只读 | `withReadOnly()` | 只读事务 |

**条件查询构建器**：

```java
import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

// Lambda 方式（类型安全，推荐）
Condition condition = builder(User.class)
    .eq(User::getStatus, 1)
    .ne(User::getDeleted, true)
    .like(User::getUsername, "张")
    .likeStartsWith(User::getEmail, "admin")     // email LIKE 'admin%'
    .likeEndsWith(User::getEmail, "@example.com") // email LIKE '%@example.com'
    .notLike(User::getUsername, "test")
    .gt(User::getAge, 18)
    .ge(User::getCreatedAt, startTime)
    .lt(User::getAge, 60)
    .le(User::getCreatedAt, endTime)
    .between(User::getAge, 18, 60)
    .notBetween(User::getSalary, 10000, 20000)
    .in(User::getDeptId, deptIds)
    .notIn(User::getStatus, List.of(0, -1))
    .isNull(User::getDeletedAt)
    .isNotNull(User::getEmail)
    .jsonContains(User::getTags, "vip")
    .jsonPath(User::getMetadata, "$.level", 3)
    .build();

// 动态条件（null 值自动跳过）
Condition condition = builder(User.class)
    .eqIfPresent(User::getStatus, status)         // status != null 时添加
    .likeIfPresent(User::getUsername, name)        // name != null 时添加
    .geIfPresent(User::getCreatedAt, startTime)    // startTime != null 时添加
    .inIfPresent(User::getDeptId, deptIds)         // deptIds 非空时添加
    .betweenIfPresent(User::getAge, minAge, maxAge) // 两者都非 null 时添加
    .build();

// 组合条件
Condition condition = allOf(cond1, cond2);  // AND
Condition condition = anyOf(cond1, cond2);   // OR

// 嵌套条件
Condition condition = builder(User.class)
    .eq(User::getTenantId, tenantId)
    .and(builder(User.class)
        .like(User::getUsername, keyword)
        .or(builder(User.class)
            .like(User::getEmail, keyword)
            .like(User::getPhone, keyword)
            .build())
        .build())
    .build();
```

**完整操作符**：`eq`, `ne`, `like`, `likeStartsWith`, `likeEndsWith`, `notLike`, `in`, `notIn`, `isNull`, `isNotNull`, `gt`, `ge`, `lt`, `le`, `between`, `notBetween`, `jsonContains`, `jsonContained`, `jsonPath`

**空值语义**：
- `eq(field, null)` → `IS NULL`
- `ne(field, null)` → `IS NOT NULL`
- `in(field, emptyList)` → `1=0`（不匹配任何记录）
- `eqIfPresent(field, null)` → **跳过该条件**（不参与查询）

**实体基类**：

| 类 | 字段 | 特征接口 |
|----|------|---------|
| `BaseEntity` (abstract) | `id`(Long), `createdAt`(Instant), `updatedAt`(Instant) | — |
| `TenantEntity` | + `tenantId`(String) | — |
| `SoftDeleteEntity` | + `deleted`(Boolean=false) | `SoftDeletable` |
| `TimestampSoftDeleteEntity` | + `deletedAt`(Instant) | `TimestampSoftDeletable` |
| `VersionedEntity` | + `version`(Integer=0) | `Versioned` |
| `FullEntity` | + `deleted` + `version` + `createBy` + `updateBy` | `SoftDeletable` + `Versioned` + `Auditable` |
| `TreeEntity<T>` | + `parentId`(Long) + `level`(Integer) + `path`(String) + `sortOrder`(Integer) + `children`(List) | `Treeable<T>` |

**关联关系注解**：

| 注解 | 默认 Fetch | 特有属性 |
|------|-----------|---------|
| `@ManyToOne` | EAGER | `foreignKey` / `optional` |
| `@OneToMany` | LAZY | `mappedBy` / `foreignKey` / `orphanRemoval` |
| `@OneToOne` | LAZY | `mappedBy` / `foreignKey` |
| `@ManyToMany` | LAZY | `mappedBy` / `joinTable` / `joinColumn` / `inverseJoinColumn` |

所有注解共有：`targetEntity` / `cascade`（PERSIST/MERGE/REMOVE/REFRESH/DETACH/ALL） / `fetch`

**数据库方言（10 种）**：

| 数据库 | 方言类 | 系列 |
|--------|--------|------|
| MySQL | `MySQLDialect` | MySQL 家族 |
| PostgreSQL | `PostgreSQLDialect` | PostgreSQL 家族 |
| Oracle | `OracleDialect` | — |
| SQL Server | `SQLServerDialect` | — |
| H2 | `H2Dialect` | — |
| OceanBase | `OceanBaseDialect` | MySQL 家族 |
| openGauss | `OpenGaussDialect` | PostgreSQL 家族 |
| GaussDB | `GaussDBDialect` | PostgreSQL 家族 |
| 达梦 | `DmDialect` | 国产数据库 |
| 金仓 | `KingbaseDialect` | 国产数据库 |

**类型处理器（16+ 内置）**：

`BigDecimalTypeHandler`, `BlobTypeHandler`, `BooleanNumberTypeHandler`, `DateTimeTypeHandler`, `EnumTypeHandler`, `InstantTypeHandler`, `JsonTypeHandler`, `LocalDateTypeHandler`, `LocalTimeTypeHandler`, `NumberTypeHandler`, `OffsetDateTimeTypeHandler`, `StringTypeHandler`, `UUIDTypeHandler`, `YearMonthTypeHandler`, `YearTypeHandler`, `ZonedDateTimeTypeHandler`, `EncryptedTypeHandler`（字段级加密）

**SQL 构建器（data-sql）**：

| 构建器 | 说明 |
|--------|------|
| `SqlQueryBuilder` | SELECT 查询构建（CTE / JOIN / 子查询 / 窗口函数） |
| `SqlInsertBuilder` | INSERT 构建（批量 / ON DUPLICATE KEY UPDATE） |
| `SqlUpdateBuilder` | UPDATE 构建 |
| `SqlDeleteBuilder` | DELETE 构建 |
| `WindowFunctionBuilder` | 窗口函数构建（ROW_NUMBER / RANK / SUM OVER 等） |
| `SqlRewriteContext` | SQL 重写上下文（租户隔离 / 数据权限注入） |

**数据库迁移（data-liquibase）**：

| 功能 | 说明 |
|------|------|
| Liquibase 集成 | 框架提供 `LiquibaseAutoConfiguration`，自动执行迁移 |
| 内置迁移 | auth-server（22 个 changeSet）、ai-core（18 个 changeSet） |
| Gradle 任务 | `generateMigration` / `dbMigrate` / `generateEntity` / `generateEntityFromDb` |
| Schema 对比 | `SchemaComparator`（实体 vs 数据库 vs 基线三方差异） |

**新增功能**：

| 功能 | 说明 |
|------|------|
| 分布式 ID 生成 | `IdGenerator` SPI — Snowflake / Segment / UUID 三种策略，`@GeneratedValue(generator=SNOWFLAKE)` |
| 实体变更事件 | 保存/更新/删除后自动发布 `EntityChangedEvent<T>`（含 old/new value diff） |
| 字段级加密 | `@EncryptedField(algorithm=AES, keyRef="user-key")` — PII 数据加密存储 |
| 悲观锁 | `dataManager.entity(Order.class).withPessimisticLock().findById(id)` — `SELECT ... FOR UPDATE` |
| 树形结构 | `TreeEntity<T>` 基类 + `TreeQuery`（递归查询、路径查询、子树查询、闭包表支持） |
| ProjectedQuery | `dataManager.entity(User.class).project().select(User::getId, User::getUsername).list()` |
| 动态条件 | `eqIfPresent` / `likeIfPresent` / `inIfPresent` / `betweenIfPresent` 等 — null 值自动跳过 |

#### 配置属性

```yaml
afg:
  data:
    # 实体缓存
    entity-cache:
      enabled: true
      max-size: 1000
      ttl-seconds: 300

    # SQL 监控
    sql-metrics:
      enabled: true
      slow-query-threshold-ms: 500

    # 分布式 ID
    id-generator:
      type: SNOWFLAKE                    # SNOWFLAKE / SEGMENT / UUID
      snowflake:
        worker-id: 1
        datacenter-id: 1

    # 字段加密
    field-encryption:
      enabled: true
      default-algorithm: AES
```

#### 注意事项/限制

- `@Transactional` 与 `dataManager.executeInTransaction()` 可以共存，框架正确处理事务传播
- API 端到端测试中 `@Transactional` 无效（HTTP 不同线程），应使用 `RestClient` + 真实数据
- 软删除实体的唯一约束需注意：已删除记录的 username 可能与新建记录冲突。建议唯一约束包含 `deleted` 字段
- `FullEntity` 不继承 `TenantEntity`，需要多租户 + 全功能时需手动添加 `tenantId` 字段
- `TreeEntity` 的 `path` 字段使用 `/` 分隔的祖先 ID 路径（如 `/1/5/12/`），用于快速查询子树
- 关联关系 `@ManyToOne` 默认 EAGER 加载，`@OneToMany` 默认 LAZY，注意 N+1 问题
- `SchemaComparator` 需要数据库连接才能执行三方对比

> **与 Spring Boot 原生对比**：
> - Boot 原生 JPA Repository — AFG DataManager：统一门面、零反射、类型安全、自动注入多租户/数据权限/软删除
> - Boot 原生 JPA @Entity — AFG @AfEntity + @Table + @Column：APT 编译时元数据
> - Boot 原生 JPA Specification — AFG Conditions.builder()：Lambda 类型安全条件构建
> - Boot 原生 JPA 无内置软删除 — AFG SoftDeleteEntity：自动过滤 + 恢复
> - Boot 原生 JPA 无内置乐观锁 — AFG VersionedEntity：version 自动 +1 + 冲突检测
> - Boot 原生 JPA 无多租户 — AFG TenantEntity：自动注入租户条件
> - Boot 原生 JPA 无数据权限 — AFG DataScope：自动注入数据权限条件
> - Boot 原生 JPA 无聚合查询 — AFG 内置 GROUP BY + HAVING + 聚合函数
> - Boot 原生 JPA 无 DTO 投影 — AFG ProjectedQuery + Lambda 字段选择
> - Boot 原生 JPA 无树形结构 — AFG TreeEntity + TreeQuery
> - Boot 原生 JPA 无字段加密 — AFG @EncryptedField
> - Boot 原生 JPA 无分布式 ID — AFG IdGenerator SPI


---

### 5.5 Security 安全模块（security-core + auth-server + resource-server）

**一句话定位**：企业级安全基础设施——OAuth2 授权服务器 + Casbin RBAC + 多租户 + 数据权限 + 登录策略 + 审计，开箱即用。

**使用场景**：
- 用户登录（用户名密码/手机号/邮箱/社交登录）
- OAuth2 授权码流程（第三方应用接入）
- API 权限控制（@RequirePermission / @RequireRole / 动态 API 权限）
- 多租户隔离（TOKEN/HEADER/DOMAIN 三种解析策略）
- 数据权限（ALL/SELF/DEPT/DEPT_AND_CHILD/CUSTOM 五种范围）
- 登录安全（失败锁定 + 设备限制 + IP 限制 + 密码策略）

#### 期望 API 体验

```java
// ===== 实现 AfgUserDetailsService（接入框架安全的唯一必须实现） =====
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements AfgUserDetailsService {

    private final DataManager dataManager;

    @Override
    public AfgUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = dataManager.findOne(User.class,
            builder(User.class).eq(User::getUsername, username).build())
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return AfgUserDetails.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .tenantId(user.getTenantId())
            .roles(user.getRoleCodes())           // 角色编码列表
            .permissions(user.getPermissionCodes()) // 权限编码列表
            .enabled(user.getStatus() == 1)
            .build();
    }
}

// ===== 权限控制 =====
@RequirePermission("order:delete")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable Long id) { ... }

@RequireRole(value = {"ADMIN", "MANAGER"}, logical = Logical.OR)
@GetMapping("/dashboard")
public Result<DashboardData> dashboard() { ... }

// ===== 动态 API 权限（通过治理中心配置，无需改代码） =====
// 在治理中心配置：POST /orders → requireAuth=true, requirePermission="order:create"

// ===== 多租户 =====
// 自动生效，无需代码。框架从 Token/Header/Domain 解析租户 ID
// 查询自动注入 tenant_id 条件
dataManager.findAll(Order.class);  // → WHERE tenant_id = 'current-tenant'

// ===== 数据权限 =====
// 自动生效，根据用户的数据权限范围注入条件
dataManager.entity(Order.class).query().withDataScope().list();
// ALL → 无额外条件
// SELF → WHERE create_by = 'current-user'
// DEPT → WHERE dept_id = 'current-dept'
// DEPT_AND_CHILD → WHERE dept_id IN (current-dept + child-depts)
// CUSTOM → WHERE custom_condition

// ===== OAuth2 授权码流程 =====
// 1. 客户端重定向到 /oauth2/authorize?response_type=code&client_id=xxx
// 2. 用户授权后重定向回 redirect_uri?code=xxx
// 3. 客户端用 code 换 token：POST /oauth2/token

// ===== 社交登录 =====
// 框架提供微信/钉钉/飞书/企微的 LoginStrategy 实现
// 配置 appId/appSecret 即可启用

// ===== 2FA =====
// 登录时如用户启用了 TOTP，需额外输入验证码
// 框架提供 QR 码生成 + TOTP 验证

// ===== 密码重置 =====
// POST /auth-api/auth/password/reset-request  → 发送重置令牌到邮箱/手机
// POST /auth-api/auth/password/reset           → 验证令牌 + 重置密码
```

#### 完整功能清单

**AutoConfiguration（9 + 2 = 11 个）**：

| AutoConfiguration | 功能 |
|-------------------|------|
| `AuthorizationServerAutoConfiguration` | OAuth2 授权服务器核心 |
| `LoginAutoConfiguration` | 登录流程（验证码、策略工厂、社交登录、2FA） |
| `OAuth2AutoConfiguration` | OAuth2 客户端、授权码存储 |
| `CasbinAutoConfiguration` | jCasbin RBAC 策略引擎 |
| `PermissionAutoConfiguration` | 权限管理（角色、资源、RBAC、@RequirePermission/@RequireRole） |
| `DataScopeAutoConfiguration` | 数据权限（5 种 DataScopeType + 自动条件注入） |
| `TenantAutoConfiguration` | 多租户（3 种隔离模式 + 解析器链 + 验证器 + 过滤器） |
| `SecurityStrategyAutoConfiguration` | 安全策略（登录锁定 + 设备限制 + 密码校验 + IP 限制） |
| `AuditAutoConfiguration` | 安全审计（登录日志 + 安全事件 + 告警） |
| `ResourceServerAutoConfiguration` | 资源服务器（JWT 验证 + 远程权限校验） |
| `DefaultSecurityAutoConfiguration` | 默认安全配置（开发阶段放行所有请求） |

**登录策略**：

| 策略 | 说明 |
|------|------|
| `UsernamePasswordLoginStrategy` | 用户名密码 + 验证码 |
| `MobileCaptchaLoginStrategy` | 手机号 + 短信验证码 |
| `EmailCaptchaLoginStrategy` | 邮箱 + 验证码 |
| `WechatLoginStrategy` | 微信 OAuth2 登录 |
| `DingTalkLoginStrategy` | 钉钉 OAuth2 登录 |
| `FeishuLoginStrategy` | 飞书 OAuth2 登录 |
| `WeComLoginStrategy` | 企业微信 OAuth2 登录 |

**多租户**：

| 隔离模式 | 说明 | 适用场景 |
|---------|------|---------|
| 共享数据库 | tenant_id 列过滤 | 大多数 SaaS，成本最低 |
| 独立数据库 | 每个租户独立数据源 | 强隔离需求，合规要求 |
| 混合模式 | 按租户等级路由（大客户独立库，小客户共享库） | 混合场景 |

**数据权限**：

| DataScopeType | 自动注入条件 | 说明 |
|--------------|-------------|------|
| `ALL` | 无额外条件 | 看到所有数据 |
| `SELF` | `create_by = currentUserId` | 仅看自己创建的 |
| `DEPT` | `dept_id = currentDeptId` | 仅看本部门 |
| `DEPT_AND_CHILD` | `dept_id IN (currentDept + children)` | 本部门及子部门 |
| `CUSTOM` | 自定义策略 | 业务自定义条件 |

**部署模式**：

| 模式 | 配置 | 说明 |
|------|------|------|
| AUTH_SERVER | `afg.security.auth-server.enabled: true` | 独立认证服务 |
| RESOURCE_SERVER | `afg.security.resource-server.enabled: true` | 只验证 Token |
| MONOLITH | 两者同时启用 | 聚合部署 |

#### 配置属性

```yaml
afg:
  security:
    auth-server:
      enabled: true
      token:
        issuer: https://auth.example.com
        signing-key: your-secret-key-at-least-256-bits
        access-token-ttl: 2h
        refresh-token-ttl: 7d
      oauth2:
        enabled: true
        authorization-code-ttl: 5m
        clients:
          - client-id: my-client
            client-secret: my-secret
            redirect-uris: https://app.example.com/callback
            scopes: read,write
            grant-types: authorization_code,refresh_token
            require-pkce: true
      login:
        enabled: true
        captcha-ttl: 5m
      casbin:
        enabled: true
        model-type: rbac-domain
        policy-adapter-type: jdbc
      permission:
        enabled: true
        default-data-scope: ALL
      tenant:
        enabled: true
        strategies: TOKEN,HEADER,DOMAIN
        default-tenant: default
        header-name: X-Tenant-Id
      security:
        max-login-failures: 5
        lock-duration: 30m
        max-devices: 5
      audit:
        enabled: true
    resource-server:
      enabled: true
      jwt:
        jwk-set-uri: https://auth.example.com/.well-known/jwks.json
      permission:
        auth-server-url: http://auth-server:8080/auth-api/internal
```

#### 注意事项/限制

- 实现 `AfgUserDetailsService` 是接入框架安全的**唯一必须实现**
- Spring Security 需匹配带模块前缀路径：`.requestMatchers("/auth-api/auth/login").permitAll()`
- 多租户的独立数据库和混合模式需要 `MultiDataSourceAutoConfiguration` 配合
- Casbin 策略的 model 文件格式参考 [jCasbin 文档](https://github.com/casbin/jcasbin)
- `@RequirePermission` 和 `@RequireRole` 支持 `Logical.AND`（全部满足）和 `Logical.OR`（任一满足）

> **与 Spring Boot 原生对比**：
> - Boot 原生 Spring Security — AFG 增强：OAuth2 授权服务器 + Casbin RBAC + 多租户 + 数据权限 + 登录策略 + 审计
> - Boot 原生无 OAuth2 授权服务器 — AFG 完整实现（/authorize, /token, /introspect, /revoke）
> - Boot 原生无 RBAC 策略引擎 — AFG 集成 jCasbin
> - Boot 原生无多租户 — AFG 三种隔离模式
> - Boot 原生无数据权限 — AFG 行级自动注入
> - Boot 原生无社交登录 — AFG 微信/钉钉/飞书/企微
> - Boot 原生无 2FA — AFG TOTP

---

### 5.6 AI 核心模块（ai-core + ai-spring-ai + ai-langchain4j）

**一句话定位**：AI 是一等公民——Chat/Agent/Workflow/RAG/Tool 全链路 AI 能力，双引擎适配，声明式注解驱动，韧性+安全+审计内置。

**使用场景**：
- 给应用添加 AI 对话（@AiChat）
- 创建 AI Agent（@AiAgent + ReAct/PlanExecute/Reflection）
- 构建知识库问答（RAG + 向量存储 + ETL）
- 创建多步 AI 工作流（DAG 引擎 + 37 种节点 + Checkpoint）
- 多 Agent 协作（Coordinator + CommunicationBus）
- AI 调用韧性保护（@AiResilient 熔断/重试/降级）
- AI 内容安全（@ContentSafety 输入/输出检查 + PII 检测）

#### 期望 API 体验

```java
// ===== AI 对话 =====
@Service
public class ChatService {
    @AiChat(client = "default", systemPrompt = "你是客服助手", temperature = 0.7)
    public String chat(String message) { return message; }

    // 流式响应
    @AiChat(client = "default", streaming = true)
    public Flux<String> chatStream(String message) { return Flux.just(message); }
}

// ===== AI Agent =====
@AiAgent(value = "code-reviewer", maxIterations = 5, timeoutMs = 30000)
public AgentResponse reviewCode(String code) { ... }

// ===== RAG 知识库 =====
// 上传文档
knowledgeBaseService.upload(knowledgeBaseId, document);

// 查询
List<SearchResult> results = knowledgeBaseService.search(knowledgeBaseId, "如何退款", 5);

// ===== AI 工作流 =====
WorkflowDefinition workflow = WorkflowDefinition.builder()
    .name("customer-support")
    .node("input", InputNode.class)
    .node("classify", AiServiceNode.class, Map.of("prompt", "分类用户问题"))
    .node("search", ToolNode.class, Map.of("tool", "knowledgeSearch"))
    .node("answer", AiServiceNode.class, Map.of("prompt", "根据搜索结果回答"))
    .edge("input", "classify")
    .edge("classify", "search")
    .edge("search", "answer")
    .build();

DagResult result = dagEngine.execute(workflow, inputContext);

// ===== AI 韧性 =====
@AiResilient(retry = 3, retryIntervalMs = 1000, circuitBreaker = "ai-cb", fallbackMethod = "fallback")
@AiChat(client = "default")
public String chat(String message) { return message; }

public String fallback(String message) { return "AI 服务暂时不可用，请稍后重试"; }

// ===== AI 安全 =====
@ContentSafety(checkInput = true, checkOutput = true, block = true)
@AiChat(client = "default")
public String chat(String message) { return message; }

// ===== AI 审计 =====
@AiAudited
@AiChat(client = "default")
public String chat(String message) { return message; }

// ===== 工具注册 =====
@Tool(name = "weather", description = "查询天气")
public String getWeather(@ToolParam("城市") String city) { ... }

// ===== 技能路由 =====
@Skill(name = "refund", description = "退款处理", intentKeywords = {"退款", "退钱", "退费"})
public SkillResult handleRefund(SkillContext context) { ... }
```

#### AI 引擎选择指南

| 维度 | Spring AI | LangChain4J |
|------|-----------|-------------|
| **生态** | Spring 官方，与 Spring Boot 深度集成 | 独立生态，模型支持更广 |
| **模型支持** | OpenAI / Anthropic / Ollama / Vertex AI | 30+ 模型提供商（含国内通义/智谱/百川等） |
| **RAG** | VectorStore + Advisor | EmbeddingStore + ContentRetriever |
| **Agent** | ChatClient + Advisor 链 | AiServices + @Tool |
| **流式** | Flux<String> 原生支持 | Token 流支持 |
| **国内模型** | 有限 | 丰富（通义千问、智谱、百川、月之暗面等） |
| **推荐场景** | 已深度使用 Spring 生态 | 需要国内模型 / 更多模型选择 |

> 两个引擎可以同时使用，通过 `@AiChat(client = "spring-ai")` / `@AiChat(client = "langchain4j")` 指定。

#### 完整功能清单

**AutoConfiguration（16 + 7 + 7 = 30 个）**：

| 模块 | AutoConfiguration | 功能 |
|------|-------------------|------|
| ai-core | `AiCoreAutoConfiguration` | AI 核心初始化 |
| ai-core | `AiChatAutoConfiguration` | Chat 客户端注册 |
| ai-core | `AiAgentAutoConfiguration` | Agent 执行器 |
| ai-core | `AiModelAutoConfiguration` | 模型注册管理 |
| ai-core | `AiWorkflowAutoConfiguration` | 工作流 DAG 引擎 |
| ai-core | `AiPipelineAutoConfiguration` | 对话管线 |
| ai-core | `AiPersistenceAutoConfiguration` | 持久化（会话、消息历史） |
| ai-core | `AiResilienceAutoConfiguration` | 韧性（熔断、重试、降级） |
| ai-core | `AiPerformanceAutoConfiguration` | 性能（缓存、限流） |
| ai-core | `AiSecurityAutoConfiguration` | 安全（API Key、内容安全、PII） |
| ai-core | `AiObservabilityAutoConfiguration` | 可观测性（审计、指标、链路追踪） |
| ai-core | `AiRagAutoConfiguration` | RAG（向量存储、知识库、ETL） |
| ai-core | `AiEtlAutoConfiguration` | ETL 管线 |
| ai-core | `AiToolAutoConfiguration` | 工具注册与执行 |
| ai-core | `AiSkillAutoConfiguration` | 技能路由 |
| ai-core | `AiEntityAutoConfiguration` | AI 实体自动配置 |
| ai-langchain4j | `Lc4jChatAutoConfiguration` 等 7 个 | LangChain4J 适配 |
| ai-spring-ai | `SpringAiChatAutoConfiguration` 等 7 个 | Spring AI 适配 |

**AI 工作流 37 种节点**：

| 分类 | 节点类型 | 说明 |
|------|---------|------|
| **INPUT** | `InputNode` / `FileInputNode` / `HttpRequestNode` / `DatabaseQueryNode` | 数据输入 |
| **AI** | `AiServiceNode` / `AiAgentNode` / `AiChatNode` / `AiEmbeddingNode` | AI 调用 |
| **LOGIC** | `ConditionNode` / `LoopNode` / `ParallelNode` / `SwitchNode` / `MergeNode` / `DelayNode` / `SubWorkflowNode` | 流程控制 |
| **TOOL** | `ToolNode` / `HttpCallNode` / `DatabaseWriteNode` / `CodeExecuteNode` / `McpToolNode` | 工具调用 |
| **OUTPUT** | `OutputNode` / `FileOutputNode` / `NotificationNode` / `WebhookNode` / `LogOutputNode` | 数据输出 |
| **HUMAN** | `HumanApprovalNode` / `HumanInputNode` / `HumanChoiceNode` | 人机交互 |
| **TRANSFORM** | `JsonTransformNode` / `TextTransformNode` / `MappingNode` / `FilterNode` / `AggregateNode` | 数据转换 |
| **RAG** | `RetrievalNode` / `EmbeddingNode` / `ReRankNode` | 知识检索 |
| **CHECKPOINT** | `CheckpointNode` / `RecoveryNode` | 断点与恢复 |

**注解**：

| 注解 | 功能 |
|------|------|
| `@AiChat` | 声明式 AI 对话（client / systemPrompt / temperature / maxTokens / streaming） |
| `@AiAgent` | 声明式 Agent 执行（value / maxIterations / timeoutMs / chatClient） |
| `@ModelRoute` | 模型路由选择 |
| `@Workflow` | 声明式工作流执行（value / async） |
| `@AiResilient` | AI 调用韧性（retry / retryIntervalMs / circuitBreaker / fallbackMethod） |
| `@AiRateLimited` | AI 接口限流 |
| `@ContentSafety` | 内容安全检查（checkInput / checkOutput / block） |
| `@AiAudited` | AI 操作审计 |
| `@ToolExecution` | 工具执行审计 |

#### 配置属性

```yaml
afg:
  ai:
    enabled: true
    chat:
      enabled: true
      default-name: default
    agent:
      enabled: true
      max-iterations: 10
    workflow:
      enabled: true
    rag:
      enabled: true
      embedding-dimensions: 1536
      search-mode: BLEND
      similarity-threshold: 0.7
      top-n: 5
    resilience:
      enabled: true
      retry:
        max-retries: 3
      circuit-breaker:
        window-size: 100
        failure-rate-threshold: 0.5
    security:
      enabled: true
      content-safety:
        enabled: true
      pii:
        enabled: true
    observability:
      enabled: true
      audit:
        enabled: true
    # LLM 配置由 Spring AI 或 LangChain4J 的原生配置管理
```

#### 注意事项/限制

- AI 模块的每个功能都有 NoOp 默认实现，不引入 AI 引擎依赖即可运行
- `@AiResilient` 的 `fallbackMethod` 必须与原方法同签名
- `@ContentSafety` 与 Security 模块是正交关系：ContentSafety 处理 AI 内容安全，Security 处理访问控制
- AI 审计（`@AiAudited`）应与 core 审计（`@Audited`）统一格式和存储
- 工作流节点的输入/输出 Schema 需要匹配，框架在执行前校验

> **与 Spring Boot 原生对比**：
> - Boot 原生无 AI 能力 — AFG 完整 AI 全链路（Chat/Agent/Workflow/RAG/Tool/Skill）
> - Boot 原生 + Spring AI — AFG 增强：韧性注解 + 安全注解 + 审计注解 + 工作流引擎
> - Boot 原生无 AI 工作流 — AFG DAG 引擎 + 37 种节点
> - Boot 原生无 AI 韧性 — AFG @AiResilient（熔断/重试/降级）
> - Boot 原生无 AI 安全 — AFG @ContentSafety + PiiDetector

---

### 5.7 Integration 集成模块 — afg-redis

**一句话定位**：框架分布式能力的唯一实现模块——为 core 层定义的 10 个分布式 SPI 提供 Redis/Redisson 实现。

**使用场景**：
- 分布式缓存（替代 Caffeine 本地缓存）
- 分布式锁（@Lock 注解式 + 编程式）
- 分布式调度（@DistributedTask + 动态任务）
- 延迟队列
- 审计日志存储（Redis）
- 限流存储（令牌桶 + 滑动窗口）
- 功能开关存储（Redisson）

#### 期望 API 体验

```java
// 引入 afg-redis 依赖后，所有 core SPI 自动升级为 Redis 实现
// 无需额外配置，spring.data.redis.* 由 Spring Boot 原生管理

// ===== 分布式缓存（自动升级） =====
@Cacheable(cacheName = "users", key = "#id")
public User getUser(Long id) { ... }
// 无 Redis → Caffeine 本地缓存
// 有 Redis → Redis RMapCache 分布式缓存

// ===== 分布式锁（自动升级） =====
@Lock(key = "'order:' + #orderId", lockType = LockType.REENTRANT, waitTime = 5000)
public Order processOrder(Long orderId) { ... }
// 无 Redis → 内存锁（仅单实例有效）
// 有 Redis → Redisson 分布式锁（Watchdog 自动续期）

// ===== 分布式调度（自动升级） =====
@DistributedTask(cron = "0 */5 * * * ?")
public void syncData() { ... }
// 无 Redis → 本地调度
// 有 Redis → Redisson 分布式调度（集群唯一执行）

// ===== 延迟队列 =====
@Autowired
DelayQueue delayQueue;

delayQueue.offer("task-id", Duration.ofMinutes(30));  // 30 分钟后执行
```

#### 完整功能清单

| 实现的 SPI | 实现类 | 说明 |
|-----------|--------|------|
| `CacheManager` | `RedisCacheManager` | Redis 分布式缓存 |
| `CacheStorageProvider` | `RedisCacheStorageProvider` | 多级缓存的 Redis 存储层 |
| `DistributedLock` | `RedisDistributedLock` | 4 种锁类型（Reentrant/Fair/ReadWrite/Spin） |
| `DistributedTaskScheduler` | `RedissonTaskScheduler` | Cron/固定速率/一次性/分片任务 |
| `DelayQueue` | `RedissonDelayQueue` | 分布式延迟队列 |
| `AuditLogStorage` | `RedisAuditLogStorage` | Redis 审计日志存储 |
| `RateLimitStorage` | `RedisRateLimitStorage` | 令牌桶 + 滑动窗口限流 |
| `FeatureFlagManager.DistributedStorageClient` | `RedissonStorageClient` | 功能开关 Redis 存储 |
| `HealthIndicator` | `RedisHealthIndicator` | Redis 健康检查 |
| `RedisHealthChecker` | `RedissonHealthChecker` | Redisson 连接检查 |

#### 配置属性

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # Spring Boot 原生配置，框架不重复定义

afg:
  redis:
    cache:
      enabled: true                    # 是否启用 Redis 缓存
    lock:
      enabled: true                    # 是否启用 Redis 锁
      annotations:
        enabled: true                   # 是否启用 @Lock 注解
    scheduler:
      redisson:
        enabled: true                   # 是否启用 Redisson 调度
        delay-queue:
          enabled: true                 # 是否启用延迟队列
```

#### 注意事项/限制

- `RedisAutoConfiguration` 应拆分为多个子 Configuration 类（Cache/Lock/Scheduler/Audit/RateLimit/Feature/Health），当前 12 个 @Bean 集中在一个类中
- `RedissonTaskScheduler.resume()` 仅更新状态，不恢复实际调度，需重新调用 `schedule`
- Redis 连接配置由 Spring Boot 原生 `spring.data.redis.*` 管理，框架不重复定义

> **与 Spring Boot 原生对比**：
> - Boot 原生 `RedisTemplate` — AFG 不封装 RedisTemplate，直接可用
> - Boot 原生 `RedisCacheManager` — AFG 增强：AfgCache 统一接口 + 按缓存名配置 TTL + 多级缓存
> - Boot 原生无分布式锁 — AFG 提供 @Lock + 4 种锁类型 + Watchdog
> - Boot 原生无分布式调度 — AFG 提供 @DistributedTask + 延迟队列
> - Boot 原生无限流存储 — AFG 提供令牌桶 + 滑动窗口

---

### 5.8 Governance 治理模块（proto + client + server）

**一句话定位**：基于 gRPC 的服务治理——配置中心 + 服务注册发现 + 灰度发布，框架自建轻量级方案。

**使用场景**：
- 配置中心：应用启动时从治理中心拉取配置，运行时实时推送变更
- 服务注册发现：微服务注册到治理中心，客户端发现服务实例
- 灰度发布：按规则路由流量到新版本

#### 完整功能清单

| 子模块 | 功能 |
|--------|------|
| proto | gRPC 服务定义（SubscribeConfig/ConfigAckStream/GetConfigs/PublishConfig/RegisterService/HeartbeatStream/DiscoverServices） |
| client | `GovernanceConfigClient` + `GovernanceRegistryClient` + `ConfigAutoRegistrar` + gRPC Channel 管理 + 签名拦截器 |
| server | gRPC 服务实现 + REST API（配置组/配置项/配置值/配置历史/配置快照/配置导入导出/服务注册/服务实例/环境管理）+ 实体层 + Liquibase 迁移 |

#### 配置属性

```yaml
afg:
  governance:
    client:
      server-address: localhost:9090
      enabled: true
    server:
      enabled: true
```

#### 注意事项/限制

- 服务注册发现（Phase 3）部分 RPC 已定义但实现可能不完整
- gRPC 传输层依赖 `grpc-netty`（非 shaded），引入 governance-client 的项目需显式添加 `implementation(libs.grpc.netty)`
- 禁止在框架模块中使用 `grpc-netty-shaded`

> **与 Spring Boot 原生对比**：
> - Boot 原生无配置中心 — AFG 自建 gRPC 配置中心（vs Nacos/Apollo/Consul，可扩展）
> - Boot 原生无服务注册 — AFG 自建 gRPC 注册中心
> - Boot 原生 + Spring Cloud Nacos — AFG 治理模块是轻量级替代，适合不想引入 Spring Cloud 全家桶的场景

---

### 5.9 Gradle 插件

**一句话定位**：项目脚手架和代码生成工具——afgInit 生成项目骨架，generateMigration 生成迁移脚本。

#### 期望 API 体验

```kotlin
// build.gradle.kts
plugins {
    id("io.github.afg-projects.afg-framework") version "1.0.0-SNAPSHOT"
}

afg {
    springBootVersion.set("4.0.6")
    frameworkVersion.set("1.0.0-SNAPSHOT")
    standalone.set(true)
    useLombok.set(true)
    enableCodegen.set(true)
    basePackage.set("com.example")
    securityMode.set("MONOLITH")
    databaseType.set("mysql")
}
```

```bash
./gradlew afgInfo              # 显示框架配置信息
./gradlew afgInit              # 生成项目脚手架代码
./gradlew generateMigration    # 从实体生成迁移脚本
./gradlew generateEntity       # 生成实体类
./gradlew generateEntityFromDb # 从数据库反向生成实体
./gradlew dbMigrate            # 执行数据库迁移
./gradlew generateDbDoc        # 生成数据库文档
```

#### 完整功能清单

| 任务 | 说明 |
|------|------|
| `afgInfo` | 显示框架配置信息 |
| `afgInit` | 生成项目骨架（Application.java + application.yml + AfgUserDetailsService + 目录结构 + Liquibase 迁移 + README.md + Dockerfile + .gitignore） |
| `generateMigration` | 从实体生成 Liquibase changeSet |
| `generateEntity` | 生成实体类 |
| `generateEntityFromDb` | 从数据库反向生成实体 |
| `dbMigrate` | 执行数据库迁移 |
| `generateDbDoc` | 从实体生成 Markdown/HTML 数据库文档 |

#### 配置属性

Gradle 插件通过 `afg {}` 扩展配置，无运行时配置属性。

#### 注意事项/限制

- `afgInit` 根据 `securityMode` 生成不同的安全配置代码
- `afgInit` 生成的 `AfgUserDetailsService` 包含 DataManager 使用示例
- 代码生成器支持 SPI 扩展，可注册自定义 Generator

> **与 Spring Boot 原生对比**：
> - Boot 原生 Spring Initializr — AFG afgInit：生成 AFG 框架项目骨架，包含安全配置 + DataManager 示例
> - Boot 原生无迁移生成 — AFG generateMigration：从实体自动生成 Liquibase changeSet
> - Boot 原生无数据库文档生成 — AFG generateDbDoc


---

## 6. 框架开发规范体系

### 6.1 编码规范

#### 命名约定

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `UserService` / `OrderController` |
| 方法名 | lowerCamelCase，动词开头 | `findById` / `createOrder` / `isExpired` |
| 字段名 | lowerCamelCase | `userName` / `createdAt` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` / `DEFAULT_PAGE_SIZE` |
| 包名 | 全小写，点分隔 | `io.github.afgprojects.framework.core.cache` |
| 数据库表名 | lower_snake_case | `sys_user` / `sec_role` |
| 数据库列名 | lower_snake_case | `user_name` / `created_at` |
| AutoConfiguration | `{Feature}AutoConfiguration` | `CacheAutoConfiguration` |
| SPI 接口 | 名词或动词 | `DistributedLock` / `EventPublisher` |
| 默认实现 | `Default{Spi}` 或 `{Technology}{Spi}` | `DefaultCacheManager` / `RedisDistributedLock` |
| NoOp 实现 | `NoOp{Spi}` | `NoOpVectorStore` |
| 实体类 | 名词，无后缀 | `User` / `Order` / `SecRole` |
| Controller | `{Entity}Controller` | `UserController` |
| Service | `{Entity}Service` | `UserService` |
| 异常类 | `{Name}Exception` | `BusinessException` / `OptimisticLockException` |
| 错误码枚举 | `{Domain}ErrorCode` | `CommonErrorCode` / `AiErrorCode` |

#### 注释规范

- **类级 Javadoc**：所有 public 类必须有 Javadoc，说明职责和使用方式
- **方法级 Javadoc**：public API 方法必须有 Javadoc，包含 `@param` / `@return` / `@throws`
- **行内注释**：仅注释"为什么"而非"是什么"，代码本身应自解释
- **禁止**：注释掉的代码、TODO 无 issue 编号、过度注释

#### 日志规范

- 统一使用 Lombok `@Slf4j`，**禁止**手动创建 Logger
- 日志级别：`ERROR`（异常） > `WARN`（潜在问题） > `INFO`（关键业务） > `DEBUG`（调试信息） > `TRACE`（详细追踪）
- MDC 字段：`traceId` / `userId` / `tenantId` / `requestId` / `clientIp`（框架自动注入）
- 敏感信息脱敏：密码、Token、身份证号等在日志中必须脱敏（`***`）
- 日志格式：结构化 JSON（生产环境）/ 纯文本（开发环境）

#### Lombok 使用规范

- `@Getter` / `@Setter`：实体类使用
- `@RequiredArgsConstructor`：依赖注入使用（替代 `@Autowired`）
- `@Builder`：Builder 模式使用
- `@Slf4j`：日志使用
- **禁止**：`@Data`（包含 `toString`/`equals`/`hashCode`，JPA 实体会出问题）、`@Value`（不可变实体不适用）

### 6.2 API 设计规范

#### RESTful 约定

| 操作 | HTTP 方法 | URL | 说明 |
|------|----------|-----|------|
| 查询列表 | GET | `/users` | 支持分页/排序/筛选参数 |
| 查询详情 | GET | `/users/{id}` | — |
| 创建 | POST | `/users` | 请求体为 JSON |
| 全量更新 | PUT | `/users/{id}` | 请求体为完整 JSON |
| 部分更新 | PATCH | `/users/{id}` | 请求体为部分 JSON |
| 删除 | DELETE | `/users/{id}` | 软删除优先 |
| 批量删除 | DELETE | `/users` | 请求体为 ID 列表 |

#### 统一响应格式

```json
// 成功
{"code": 0, "message": "success", "data": {...}}

// 失败
{"code": 10400, "message": "未认证", "data": null}

// 分页
{"code": 0, "message": "success", "data": {"records": [...], "total": 100, "page": 1, "size": 20, "pages": 5}}

// 校验失败
{"code": 10002, "message": "参数错误", "data": {"username": "不能为空", "email": "格式不正确"}}
```

#### 版本管理策略

- **URL 版本**（推荐）：`/v1/users` / `/v2/users`
- **Header 版本**：`Api-Version: 2`
- 框架通过 `@ApiVersion("v2")` 注解在 Controller 或方法上声明版本

#### 错误响应格式

```json
{
  "code": 11000,
  "message": "实体不存在",
  "data": null,
  "timestamp": "2026-06-11T10:30:00Z",
  "traceId": "abc-123-def"
}
```

### 6.3 异常处理规范

#### 异常分类

| 异常类型 | 基类 | 场景 | HTTP 状态码 |
|---------|------|------|------------|
| 业务异常 | `BusinessException` | 参数错误、业务规则违反 | 400 / 404 / 409 |
| 认证异常 | `AuthenticationException` | 未登录、Token 过期 | 401 |
| 权限异常 | `AccessDeniedException` | 无权限访问 | 403 |
| 系统异常 | `RuntimeException` | 框架内部错误 | 500 |

#### 全局异常处理行为

框架 `GlobalExceptionHandler` 统一处理：

| 异常类型 | 处理方式 | 响应 |
|---------|---------|------|
| `BusinessException` | 转为 `Result.fail(errorCode, message)` | 200 + code != 0 |
| `MethodArgumentNotValidException` | 收集所有字段错误 | 200 + code=10002 + details |
| `OptimisticLockException` | 乐观锁冲突 | 200 + code=11007 |
| `AuthenticationException` | 重定向到登录或返回 401 | 401 |
| `AccessDeniedException` | 返回 403 | 403 |
| 其他 `Exception` | 记录 ERROR 日志 + 返回 500 | 200 + code=19000 |

#### 错误码使用规则

- 框架错误码范围：10000-19999（`CommonErrorCode`）
- 业务应用错误码范围：20000+（自定义 `ErrorCode` 枚举）
- 新增错误码必须分配到正确的区间
- 错误码必须关联 `ErrorCategory`
- 错误消息支持 i18n（通过 `messages.properties`）

### 6.4 配置规范

#### 核心原则

```
引入依赖即生效 → 有 Bean 即增强 → 注解即启用 → 配置只覆盖默认值
```

#### 启用方式优先级

1. **自动装配**（优先）：`@ConditionalOnClass` / `@ConditionalOnBean` 自动检测
2. **注解启用**（次选）：`@AiChat` / `@DistributedTask` / `@Lock` 等注解
3. **配置属性**（最后）：仅覆盖默认值时需要

#### 禁止配置爆炸

- 每个功能最多 1 个 `enabled` 开关 + 必要的行为参数
- 不暴露框架内部实现细节的配置项
- 基础设施配置（数据库/Redis/MQ 地址）由 Spring Boot 原生管理
- 框架只配置"行为参数"（如超时、重试次数、策略选择），不配置"连接参数"

#### 属性命名约定

- 前缀：`afg.{module}.{feature}`
- 风格：kebab-case
- 层级：不超过 4 层

```yaml
afg:
  ai:
    rag:
      enabled: true           # ✓ 功能开关
      embedding-dimensions: 1536  # ✓ 行为参数
      # spring-ai 相关配置由 Spring AI 原生管理
```

### 6.5 实体设计规范

#### 基类选择

参考 3.4 实体基类选择决策树。

#### 字段命名约定

| Java 字段 | 数据库列 | 类型约定 |
|----------|---------|---------|
| `createdAt` | `created_at` | `Instant`（非 LocalDateTime） |
| `updatedAt` | `updated_at` | `Instant` |
| `deletedAt` | `deleted_at` | `Instant` |
| `deleted` | `deleted` | `Boolean`（默认 false） |
| `version` | `version` | `Integer`（默认 0） |
| `tenantId` | `tenant_id` | `String` |
| `createBy` | `create_by` | `Long`（用户 ID） |
| `updateBy` | `update_by` | `Long` |

#### 字段类型约定

| 场景 | 推荐类型 | 说明 |
|------|---------|------|
| 时间戳 | `Instant` | UTC 时间，无时区 |
| 金额 | `BigDecimal` | 精确计算 |
| 枚举 | `String`（数据库）/ Java enum | 避免用 ordinal |
| JSON | `String` + `@JsonTypeHandler` | 自动序列化/反序列化 |
| UUID | `UUID` | 标准格式 |
| 大文本 | `String` + `@Column(columnDefinition="TEXT")` | — |

#### 关联关系设计规则

- `@ManyToOne` 默认 EAGER（避免 N+1，可改为 LAZY + 预加载）
- `@OneToMany` 默认 LAZY，使用 `withAssociation()` 显式加载
- 避免 `@ManyToMany`（优先用中间实体 + 两个 `@OneToMany`）
- `orphanRemoval = true` 谨慎使用（级联删除不可恢复）

#### 软删除/乐观锁/多租户/审计组合约定

- 软删除实体的唯一约束应包含 `deleted` 字段
- 乐观锁更新时 `WHERE id = ? AND version = ?`，冲突抛 `OptimisticLockException`
- 多租户字段自动注入，无需手动设置
- 审计字段（`createBy`/`updateBy`）自动从 Security 上下文获取当前用户 ID

### 6.6 模块开发规范

#### AutoConfiguration 编写规则

```java
@AutoConfiguration(after = {DataSourceAutoConfiguration.class})
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

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
}
```

规则：
- 必须使用 `@AutoConfiguration`（非 `@Configuration`）
- 必须声明 `@AutoConfigureAfter` / `@AutoConfigureBefore`
- 跨模块用 `afterName` 字符串引用
- 可替换组件必须 `@ConditionalOnMissingBean`
- 可注入组件用 `@Nullable` 参数注入

#### SPI 接口设计规则

- 每个 SPI 必须有 `Default{Spi}` 本地实现
- 每个 SPI 必须有 `NoOp{Spi}` 降级实现（当功能完全不需要时）
- SPI 实现通过 `@ConditionalOnBean` / `@ConditionalOnClass` 自动发现
- SPI 接口放在 `api` 子包，实现放在功能子包

#### 模块间通信约定

- 模块间通信优先使用**事件**（`EventPublisher`），而非直接 Bean 调用
- 事件定义放在 `api.event` 子包
- 禁止循环依赖：模块 A 依赖模块 B 时，B 不能依赖 A

### 6.7 依赖管理规范

- 版本统一在 `gradle/libs.versions.toml` 管理
- Spring Boot BOM 管理所有 Spring 依赖版本
- 第三方依赖必须经过 OWASP 安全扫描
- 依赖范围：`api`（传递暴露）/ `implementation`（内部使用）/ `compileOnly`（编译时）
- **禁止**引入 `spring-boot-starter-data-jpa`
- **禁止**引入 `mockito`（测试依赖）
- **禁止**在框架模块中使用 `grpc-netty-shaded`

### 6.8 安全规范

- 所有敏感操作必须审计（`@Audited`）
- SQL 查询必须参数化（`Conditions.builder` 自动参数化，`RawSqlSecurityGuard` 拦截原生 SQL 注入）
- XSS 防护：`XssFilter` 自动过滤输入
- 密码必须 BCrypt 哈希存储，**禁止**明文
- Token 必须使用 RS256/HS256 签名
- API 签名：`@SignatureRequired` 防止请求篡改和重放
- 敏感字段日志脱敏：`SensitiveFieldProcessor`
- 生产环境必须修改的配置项：`signing-key` / `client-secret` / 数据库密码

---

## 7. 版本策略与演进

### 7.1 版本号规则

- 语义化版本：`MAJOR.MINOR.PATCH`
- SNAPSHOT：开发版本，`1.0.0-SNAPSHOT`
- 正式发布：`1.0.0` / `1.1.0` / `2.0.0`

### 7.2 功能分级

| 级别 | 定义 | 测试要求 | 文档要求 | 迁移脚本 |
|------|------|---------|---------|---------|
| **Alpha** | 接口已定义，基本实现可用 | 单元测试 | API 文档 | 可选 |
| **Beta** | 功能完整，有测试覆盖 | 集成测试 + Testcontainers | 使用指南 + 配置参考 | 必须 |
| **GA** | 生产级质量 | 完整测试 + 性能基准 | 完整文档 + 迁移指南 + 示例 | 必须 |

### 7.3 版本兼容性

- **MINOR 版本**：向后兼容，可新增 API，不删除 API
- **MAJOR 版本**：允许破坏性变更，提前 1 个 MINOR 版本标记 `@Deprecated`
- 弃用 API 生命周期：标记 `@Deprecated` → 保留 1 个大版本 → 移除

### 7.4 模块版本策略

- 所有模块统一版本号（`1.0.0-SNAPSHOT`）
- 模块间版本必须一致，不允许混用不同版本的框架模块

---

## 8. 框架边界定义

### 8.1 功能边界矩阵

| 功能 | 框架增强 🔴 | Boot 原生 🟢 | 框架独创 🟣 |
|------|:-----------:|:-----------:|:-----------:|
| 数据访问 | DataManager | JdbcTemplate | APT 零反射 |
| 安全 | OAuth2 Server + RBAC + 多租户 | Spring Security | 数据权限自动注入 |
| 缓存 | AfgCache + 多级缓存 | @Cacheable + Caffeine | 实体缓存 |
| 事件 | 分布式事件 + 重试 | ApplicationEvent | — |
| 调度 | 分布式调度 + 延迟队列 | @Scheduled | — |
| 校验 | 统一异常处理 | Bean Validation | — |
| 日志 | MDC 增强 + 结构化日志 | Logback | — |
| AI | 全链路 AI | — | DAG 工作流 + 韧性注解 |
| 多租户 | 三种隔离模式 | — | 全部独创 |
| 数据权限 | 行级自动注入 | — | 全部独创 |
| 导入/导出 | 注解驱动 Excel/CSV | — | — |
| 状态机 | 轻量级 @StateMachine | — | — |
| HTTP 客户端 | 重试 + 熔断 | RestClient | — |
| 配置中心 | gRPC 配置中心 | — | — |

### 8.2 "不造轮子"清单

框架**不重新实现**以下能力，直接使用 Spring Boot 原生：

- 数据库连接池（HikariCP）
- Web 容器（Tomcat/Undertow）
- JSON 序列化（Jackson）
- HTTP 客户端（RestClient）
- 邮件发送（JavaMailSender）
- 文件上传（MultipartResolver）
- Bean Validation（Hibernate Validator）
- 缓存接口（Spring Cache 抽象）
- AOP（Spring AOP）
- 事务管理（Spring Transaction）

---

## 9. 方言完整度矩阵

### 9.1 数据库功能支持矩阵

| 功能 | MySQL | PostgreSQL | Oracle | SQL Server | H2 | OceanBase | openGauss | GaussDB | 达梦 | 金仓 |
|------|:-----:|:---------:|:------:|:----------:|:---:|:---------:|:---------:|:-------:|:---:|:---:|
| 基础 CRUD | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 分页 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 聚合查询 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| JSON 操作 | ✅ | ✅ | ⚠️ | ⚠️ | ⚠️ | ✅ | ✅ | ✅ | ❌ | ❌ |
| CTE | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| 窗口函数 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| 批量操作 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 关联加载 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Schema 对比 | ✅ | ✅ | ✅ | ⚠️ | ✅ | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| 读写分离 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 字段加密 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

✅ 完整 | ⚠️ 部分 | ❌ 不支持

### 9.2 方言扩展 SPI

添加新数据库方言的步骤：
1. 实现 `DatabaseDialect` 接口
2. 注册到 `DialectRegistry`
3. 添加 Liquibase 迁移脚本兼容性测试
4. 添加方言特定的集成测试

---

## 10. 脚手架生成规范

### 10.1 afgInit 生成文件清单

| securityMode | 生成文件 |
|-------------|---------|
| MONOLITH | `Application.java` + `application.yml` + `UserDetailsServiceImpl.java` + `entity/` + `controller/` + `security/` + `db/changelog.xml` + `db/changelog/init.xml` + `Dockerfile` + `docker-compose.yml` + `README.md` + `.gitignore` |
| AUTH_SERVER | 同上，但 `application.yml` 仅包含 auth-server 配置 |
| RESOURCE_SERVER | 同上，但无 `UserDetailsServiceImpl.java`，`application.yml` 仅包含 resource-server 配置 |
| null（无安全） | 同上，但无 security 目录和相关配置 |

### 10.2 脚手架代码生成规范

| 生成类型 | 说明 |
|---------|------|
| Entity | 继承基类 + `@AfEntity` + `@Table` + `@Column` + Lombok 注解 + Validation 注解 |
| Controller | RESTful 端点 + `DataManager` 注入 + `Result<T>` 响应 + 分页查询 + 条件搜索 |
| Service | 可选，复杂业务逻辑时生成 |
| Migration | Liquibase XML + 命名规范 |
| Test | 集成测试 + Testcontainers + DataManager 数据准备 |

### 10.3 新增生成需求

- `generateDbDoc`：从实体生成 Markdown/HTML 数据库文档
- `generateDocker`：生成 Dockerfile + docker-compose.yml
- `generateReadme`：生成 README.md（包含框架使用说明）

---

## 11. 配置属性完整参考

### 11.1 配置设计原则

- 引入依赖即生效，配置只覆盖默认值
- 基础设施配置由 Spring Boot 原生管理（`spring.datasource.*` / `spring.data.redis.*` / `spring.rabbitmq.*`）
- 框架只配置"行为参数"

### 11.2 最小配置集

一个典型的 AFG 应用最少只需要：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo
    username: root
    password: root

afg:
  security:
    auth-server:
      enabled: true
      token:
        signing-key: your-secret-key-at-least-256-bits
```

### 11.3 各模块配置属性

> 以下仅列出"需要覆盖默认值时"的配置项。大部分功能零配置即可运行。

```yaml
afg:
  # ===== Core =====
  core:
    cache:
      type: LOCAL                          # LOCAL / DISTRIBUTED / MULTI_LEVEL
    event:
      type: LOCAL                          # LOCAL / RABBITMQ / KAFKA
    scheduler:
      dynamic-task:
        enabled: false
    audit:
      storage-type: log                    # log / database / redis / none
    access-log:
      enabled: true
      exclude-paths: /health,/actuator/**
    multi-datasource:
      enabled: false
    id-generator:
      type: SNOWFLAKE                      # SNOWFLAKE / SEGMENT / UUID
    field-encryption:
      enabled: true

  # ===== Data =====
  data:
    entity-cache:
      enabled: true
    sql-metrics:
      enabled: true
      slow-query-threshold-ms: 500

  # ===== Security =====
  security:
    auth-server:
      enabled: true
      token:
        signing-key: (必填)
        access-token-ttl: 2h
        refresh-token-ttl: 7d
      oauth2:
        enabled: true
      tenant:
        enabled: true
        strategies: TOKEN,HEADER,DOMAIN
    resource-server:
      enabled: true
      jwt:
        jwk-set-uri: (资源服务器模式必填)

  # ===== AI =====
  ai:
    enabled: true
    chat:
      enabled: true
    agent:
      enabled: true
    rag:
      enabled: true
      embedding-dimensions: 1536
    resilience:
      enabled: true
    security:
      enabled: true

  # ===== Redis =====
  redis:
    cache:
      enabled: true
    lock:
      enabled: true
    scheduler:
      redisson:
        enabled: true

  # ===== Governance =====
  governance:
    client:
      server-address: localhost:9090
      enabled: true
```

---

## 12. 附录

### 附录 A：AutoConfiguration 完整清单

| 模块 | 数量 | 清单 |
|------|------|------|
| core | 31+ | `AfgCoreAutoConfiguration`, `AfgAutoConfiguration`, `ModuleAutoConfiguration`, `ModuleWebAutoConfiguration`, `WebAutoConfiguration`, `HttpClientAutoConfiguration`, `LoggingAutoConfiguration`, `MetricsAutoConfiguration`, `HealthAutoConfiguration`, `ShutdownAutoConfiguration`, `EncryptionAutoConfiguration`, `RemoteConfigAutoConfiguration`, `CacheAutoConfiguration`, `LockAutoConfiguration`, `MultiDataSourceAutoConfiguration`, `VirtualThreadAutoConfiguration`, `AfgSecurityAutoConfiguration`, `SignatureAutoConfiguration`, `RateLimitAutoConfiguration`, `DataScopeAutoConfiguration`, `AuditLogAutoConfiguration`, `EventAutoConfiguration`, `FeatureFlagAutoConfiguration`, `FeatureFlagWebAutoConfiguration`, `CloudNativeAutoConfiguration`, `KubernetesProbeAutoConfiguration`, `LocaleAutoConfiguration`, `BeanInvocationAutoConfiguration`, `ContextAutoConfiguration`, `AfgOpenApiAutoConfiguration`, `SchedulerAutoConfiguration`, `AccessLogAutoConfiguration`, `ValidationAutoConfiguration`, `SseAutoConfiguration`, `StateMachineAutoConfiguration`, `EnumManagementAutoConfiguration`, `ImportExportAutoConfiguration`, `NotificationAutoConfiguration`, `WebhookAutoConfiguration`, `DuplicateSubmitAutoConfiguration`, `ApiVersionAutoConfiguration`, `IdGeneratorAutoConfiguration` |
| data-core | 2 | `TenantContextAutoConfiguration`, `TransactionAutoConfiguration` |
| data-jdbc | 4 | `DataManagerAutoConfiguration`, `EntityCacheAutoConfiguration`, `SqlMetricsAutoConfiguration` |
| data-liquibase | 1 | `LiquibaseAutoConfiguration` |
| ai-core | 16 | `AiCoreAutoConfiguration`, `AiChatAutoConfiguration`, `AiAgentAutoConfiguration`, `AiModelAutoConfiguration`, `AiWorkflowAutoConfiguration`, `AiPipelineAutoConfiguration`, `AiPersistenceAutoConfiguration`, `AiResilienceAutoConfiguration`, `AiPerformanceAutoConfiguration`, `AiSecurityAutoConfiguration`, `AiObservabilityAutoConfiguration`, `AiRagAutoConfiguration`, `AiEtlAutoConfiguration`, `AiToolAutoConfiguration`, `AiSkillAutoConfiguration`, `AiEntityAutoConfiguration` |
| ai-langchain4j | 7 | `Lc4jChatAutoConfiguration`, `Lc4jEmbeddingAutoConfiguration`, `Lc4jModelAutoConfiguration`, `Lc4jMemoryAutoConfiguration`, `Lc4jToolAutoConfiguration`, `Lc4jObservationAutoConfiguration`, `Lc4jAdvisorAutoConfiguration` |
| ai-spring-ai | 7 | `SpringAiChatAutoConfiguration`, `SpringAiEmbeddingAutoConfiguration`, `SpringAiModelAutoConfiguration`, `SpringAiMemoryAutoConfiguration`, `SpringAiToolAutoConfiguration`, `SpringAiObservationAutoConfiguration`, `SpringAiAdvisorAutoConfiguration` |
| auth-server | 9 | `AuthorizationServerAutoConfiguration`, `LoginAutoConfiguration`, `OAuth2AutoConfiguration`, `CasbinAutoConfiguration`, `PermissionAutoConfiguration`, `DataScopeAutoConfiguration`, `TenantAutoConfiguration`, `SecurityStrategyAutoConfiguration`, `AuditAutoConfiguration` |
| resource-server | 2 | `ResourceServerAutoConfiguration`, `DefaultSecurityAutoConfiguration` |
| afg-redis | 1+ | `RedisAutoConfiguration`（应拆分为 Cache/Lock/Scheduler/Audit/RateLimit/Feature/Health 子 Configuration） |
| governance-client | 1 | `GovernanceClientAutoConfiguration` |
| governance-server | 1 | `GovernanceServerAutoConfiguration` |
| **合计** | **80+** | |

### 附录 B：CommonErrorCode 完整清单

| 区间 | 类别 | 错误码 |
|------|------|--------|
| 10001-10099 | 通用 | `FAIL`(10001), `PARAM_ERROR`(10002), `PARAM_MISSING`(10003), `PARAM_FORMAT_ERROR`(10004) |
| 10100-10199 | 资源 | `NOT_FOUND`(10100), `RESOURCE_EXISTS`(10101), `RESOURCE_LOCKED`(10102) |
| 10200-10299 | 请求 | `METHOD_NOT_ALLOWED`(10200), `UNSUPPORTED_MEDIA_TYPE`(10201), `REQUEST_TIMEOUT`(10202), `PAYLOAD_TOO_LARGE`(10203) |
| 10300-10399 | 限流 | `TOO_MANY_REQUESTS`(10300), `RATE_LIMIT_EXCEEDED`(10301), `CIRCUIT_BREAKER_OPEN`(10302) |
| 10400-10499 | 认证 | `UNAUTHORIZED`(10400), `TOKEN_EXPIRED`(10401), `TOKEN_INVALID`(10402), `FORBIDDEN`(10403), `PERMISSION_DENIED`(10404), `ACCOUNT_DISABLED`(10405), `ACCOUNT_LOCKED`(10406), `PASSWORD_EXPIRED`(10407) |
| 11000-11999 | 数据 | `ENTITY_NOT_FOUND`(11000), `ENTITY_ALREADY_EXISTS`(11001), `FIELD_NOT_FOUND`(11002), `TABLE_NOT_FOUND`(11003), `DDL_ERROR`(11004), `QUERY_ERROR`(11005), `DATA_INTEGRITY_VIOLATION`(11006), `OPTIMISTIC_LOCK_ERROR`(11007) |
| 12000-12999 | 存储 | `FILE_NOT_FOUND`(12000), `FILE_UPLOAD_ERROR`(12001), `FILE_DOWNLOAD_ERROR`(12002), `FILE_TYPE_NOT_ALLOWED`(12003), `FILE_SIZE_EXCEEDED`(12004), `STORAGE_FULL`(12005) |
| 13000-13999 | 任务 | `JOB_NOT_FOUND`(13000), `JOB_EXECUTION_ERROR`(13001), `JOB_ALREADY_RUNNING`(13002), `JOB_PAUSED`(13003), `JOB_DISABLED`(13004) |
| 14000-14999 | HTTP | `CLIENT_REQUEST_FAILED`(14000), `CLIENT_TIMEOUT`(14001), `CLIENT_CONNECT_FAILED`(14002), `CLIENT_RETRY_EXHAUSTED`(14003), `CLIENT_CIRCUIT_OPEN`(14004) |
| 15000-15999 | 模块 | `MODULE_NOT_FOUND`(15000), `MODULE_DUPLICATE`(15001), `MODULE_CIRCULAR_DEPENDENCY`(15002), `MODULE_INIT_FAILED`(15003) |
| 16000-16999 | 配置 | `CONFIG_NOT_FOUND`(16000), `CONFIG_BINDING_ERROR`(16001) |
| 17000-17999 | 功能开关 | `FEATURE_DISABLED`(17000), `FEATURE_FALLBACK_FAILED`(17001) |
| 19000-19999 | 系统 | `SYSTEM_ERROR`(19000), `INTERNAL_ERROR`(19001), `SERVICE_UNAVAILABLE`(19002), `DEPENDENCY_ERROR`(19003), `CONFIG_ERROR`(19004) |

### 附录 C：竞品对比矩阵

| 功能 | Spring Boot+JPA | Spring Boot+MP | Solon | BladeX | JHipster | Causeway | Micronaut | Quarkus | **AFG** |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 数据访问 | Full | Full | Full | Full | Full | Full | Full | Full | **Full** |
| 安全 | Full | Full | Partial | Full | Full | Full | Full | Full | **Full** |
| 缓存 | Full | Full | Partial | Full | Full | Partial | Full | Full | **Full** |
| 消息 | Full | Full | Full | Full | Partial | Partial | Full | Full | **Full** |
| 调度 | Full | Full | Full | Full | None | None | Full | Full | **Full** |
| AI | Partial | None | Full | Full | None | None | Partial | Full | **Full** |
| 多租户 | None | Partial | Partial | Full | None | Partial | None | None | **Full** |
| 工作流 | None | None | Full | Full | None | None | None | Partial | **Full** |
| 数据权限 | None | Partial | None | Partial | None | Partial | None | None | **Full** |
| 审计 | Partial | Partial | None | Partial | Partial | Full | None | None | **Full** |
| i18n | Full | Full | Partial | Partial | Full | Full | Partial | Partial | **Full** |
| APT 元数据 | None | None | None | None | None | None | None | None | **Full** |
| 软删除 | None | Partial | None | Partial | None | None | None | None | **Full** |
| 模块化架构 | None | None | None | None | None | None | None | None | **Full** |
| AI 韧性/安全 | None | None | None | None | None | None | None | None | **Full** |
| AI 工作流 | None | None | Partial | None | None | None | None | None | **Full** |
| 导入/导出 | None | Partial | None | None | None | None | None | None | **Full** |
| 状态机 | None | None | None | None | None | None | None | None | **Full** |
| 云原生 | Full | Full | Full | Full | Full | None | Full | Full | **Partial** |
| Native Image | Partial | Partial | Full | None | None | None | Full | Full | **None** |

### 附录 D：AI 工作流节点完整清单

| 分类 | 节点类型 | 说明 |
|------|---------|------|
| **INPUT** | `InputNode` | 标准输入 |
| | `FileInputNode` | 文件输入（支持多格式） |
| | `HttpRequestNode` | HTTP 请求输入 |
| | `DatabaseQueryNode` | 数据库查询输入 |
| **AI** | `AiServiceNode` | AI 服务调用（prompt 模板 + 系统提示） |
| | `AiAgentNode` | AI Agent 执行 |
| | `AiChatNode` | AI 对话 |
| | `AiEmbeddingNode` | 文本向量化 |
| **LOGIC** | `ConditionNode` | 条件分支（if-else） |
| | `LoopNode` | 循环节点（for/while） |
| | `ParallelNode` | 并行执行 |
| | `SwitchNode` | 多路分支（switch-case） |
| | `MergeNode` | 并行结果合并 |
| | `DelayNode` | 延迟等待 |
| | `SubWorkflowNode` | 子流程调用 |
| **TOOL** | `ToolNode` | 工具调用 |
| | `HttpCallNode` | HTTP 调用 |
| | `DatabaseWriteNode` | 数据库写入 |
| | `CodeExecuteNode` | 代码执行（沙箱） |
| | `McpToolNode` | MCP 协议工具 |
| **OUTPUT** | `OutputNode` | 标准输出 |
| | `FileOutputNode` | 文件输出 |
| | `NotificationNode` | 通知发送 |
| | `WebhookNode` | Webhook 回调 |
| | `LogOutputNode` | 日志输出 |
| **HUMAN** | `HumanApprovalNode` | 人工审批 |
| | `HumanInputNode` | 人工输入 |
| | `HumanChoiceNode` | 人工选择 |
| **TRANSFORM** | `JsonTransformNode` | JSON 转换 |
| | `TextTransformNode` | 文本转换 |
| | `MappingNode` | 字段映射 |
| | `FilterNode` | 数据过滤 |
| | `AggregateNode` | 数据聚合 |
| **RAG** | `RetrievalNode` | 知识检索 |
| | `EmbeddingNode` | 文本嵌入 |
| | `ReRankNode` | 重排序 |
| **CHECKPOINT** | `CheckpointNode` | 断点保存 |
| | `RecoveryNode` | 断点恢复 |

### 附录 E：实体基类组合速查表

| 需求 | 基类 | 字段 | 特征接口 |
|------|------|------|---------|
| 基础 | `BaseEntity` | id, createdAt, updatedAt | — |
| 多租户 | `TenantEntity` | + tenantId | — |
| 软删除 | `SoftDeleteEntity` | + deleted(Boolean) | `SoftDeletable` |
| 软删除+时间 | `TimestampSoftDeleteEntity` | + deletedAt(Instant) | `TimestampSoftDeletable` |
| 乐观锁 | `VersionedEntity` | + version(Integer) | `Versioned` |
| 全功能 | `FullEntity` | + deleted + version + createBy + updateBy | `SoftDeletable` + `Versioned` + `Auditable` |
| 树形 | `TreeEntity<T>` | + parentId + level + path + sortOrder + children | `Treeable<T>` |

### 附录 F：条件查询操作符速查表

| 操作符 | SQL 映射 | Lambda 示例 | null 语义 |
|--------|---------|-------------|----------|
| `eq` | `= ?` | `.eq(User::getStatus, 1)` | null → `IS NULL` |
| `ne` | `!= ?` | `.ne(User::getDeleted, true)` | null → `IS NOT NULL` |
| `like` | `LIKE '%?%'` | `.like(User::getUsername, "张")` | — |
| `likeStartsWith` | `LIKE '?%'` | `.likeStartsWith(User::getEmail, "admin")` | — |
| `likeEndsWith` | `LIKE '%?'` | `.likeEndsWith(User::getEmail, ".com")` | — |
| `notLike` | `NOT LIKE '%?%'` | `.notLike(User::getUsername, "test")` | — |
| `in` | `IN (?, ?, ?)` | `.in(User::getDeptId, ids)` | 空集合 → `1=0` |
| `notIn` | `NOT IN (?, ?, ?)` | `.notIn(User::getStatus, List.of(0, -1))` | 空集合 → `1=1` |
| `isNull` | `IS NULL` | `.isNull(User::getDeletedAt)` | — |
| `isNotNull` | `IS NOT NULL` | `.isNotNull(User::getEmail)` | — |
| `gt` | `> ?` | `.gt(User::getAge, 18)` | — |
| `ge` | `>= ?` | `.ge(User::getCreatedAt, start)` | — |
| `lt` | `< ?` | `.lt(User::getAge, 60)` | — |
| `le` | `<= ?` | `.le(User::getCreatedAt, end)` | — |
| `between` | `BETWEEN ? AND ?` | `.between(User::getAge, 18, 60)` | — |
| `notBetween` | `NOT BETWEEN ? AND ?` | `.notBetween(User::getSalary, 10k, 20k)` | — |
| `jsonContains` | JSON_CONTAINS(?, ?) | `.jsonContains(User::getTags, "vip")` | — |
| `jsonPath` | JSON_EXTRACT(?, ?) | `.jsonPath(User::getMeta, "$.level", 3)` | — |
| `eqIfPresent` | `= ?` | `.eqIfPresent(User::getStatus, status)` | null → **跳过** |
| `likeIfPresent` | `LIKE '%?%'` | `.likeIfPresent(User::getName, name)` | null → **跳过** |
| `inIfPresent` | `IN (?, ?, ?)` | `.inIfPresent(User::getDeptId, ids)` | null/空 → **跳过** |
| `betweenIfPresent` | `BETWEEN ? AND ?` | `.betweenIfPresent(User::getAge, min, max)` | 任一 null → **跳过** |

### 附录 G：框架约定速查卡

```
╔══════════════════════════════════════════════════════════════╗
║                    AFG Framework 约定速查卡                    ║
╠══════════════════════════════════════════════════════════════╣
║ 注解                                                          ║
║   @AfEntity       触发 APT 元数据生成                           ║
║   @AfgEnum        枚举元数据 + i18n                             ║
║   @EncryptedField 字段级加密                                    ║
║   @AiChat         AI 对话                                      ║
║   @AiAgent        AI Agent 执行                                 ║
║   @AiResilient    AI 韧性（熔断/重试/降级）                      ║
║   @ContentSafety  AI 内容安全                                   ║
║   @Audited        操作审计                                     ║
║   @Lock           分布式锁                                     ║
║   @DistributedTask 分布式调度                                   ║
║   @FeatureToggle  功能开关                                     ║
║   @DuplicateSubmit 重复提交防护                                 ║
║   @RequirePermission 权限校验                                   ║
║   @RequireRole    角色校验                                     ║
║   @ExcelSheet     Excel 导入导出映射                            ║
║   @StateMachine   状态机                                       ║
║   @AfgModuleAnnotation 模块注册                                 ║
╠══════════════════════════════════════════════════════════════╣
║ 配置前缀                                                       ║
║   afg.core.*      核心模块                                     ║
║   afg.data.*      数据模块                                     ║
║   afg.security.*  安全模块                                     ║
║   afg.ai.*        AI 模块                                      ║
║   afg.redis.*     Redis 集成                                   ║
║   afg.governance.* 治理模块                                    ║
╠══════════════════════════════════════════════════════════════╣
║ 基类                                                          ║
║   BaseEntity → TenantEntity / SoftDeleteEntity / VersionedEntity ║
║             → FullEntity / TreeEntity / TimestampSoftDeleteEntity ║
╠══════════════════════════════════════════════════════════════╣
║ 核心 API                                                      ║
║   dataManager.entity(X.class).query().where(c).list()         ║
║   dataManager.entity(X.class).query().where(c).page(pr)       ║
║   dataManager.entity(X.class).project().select(::getId).list() ║
║   dataManager.save(X.class, entity)                            ║
║   dataManager.deleteById(X.class, id)                          ║
║   Conditions.builder(X.class).eq(X::getA, v).build()          ║
║   Result.success(data) / Result.fail(code, msg)                ║
║   PageData.of(records, total, page, size)                      ║
╚══════════════════════════════════════════════════════════════╝
```
