# AFG Framework 跨层思维指南

> **目的**：实现跨层功能前，理解 AFG 框架特有的层边界和数据流，避免层边界缺陷。
> **溯源**：PRD 第 4 节"模块架构"、第 5.3-5.6 节"逐模块功能需求"

---

## 问题本质

**大多数缺陷发生在层边界，而非层内部**。在 AFG 框架中，层边界尤其复杂：

- Controller → DataManager → JDBC → Database（数据访问链路）
- @AiChat → AOP → ChatClientRegistry → AfgChatClient → Spring AI/LangChain4J → LLM（AI 调用链路）
- @RequirePermission → AOP → AfgEnforcer → Casbin → JDBC 策略存储（安全校验链路）
- @Lock → AOP → DistributedLock SPI → Redis/内存实现（分布式能力链路）
- @DistributedTask → AOP → TaskScheduler SPI → Redis/本地实现（调度链路）

---

## AFG 特有层边界

### 数据访问层链路

```
Controller
  │  注入 DataManager
  │  调用 dataManager.entity(X.class).query().where(c).list()
  ▼
DataManager (统一门面)
  │  解析实体元数据 (APT 生成 → EntityMetadataCache)
  │  构建条件 (Conditions → Condition 对象)
  │  注入软删除过滤 (SoftDeleteEntity → 自动追加 deleted = false)
  │  注入租户条件 (TenantEntity → 自动追加 tenant_id = ?)
  │  注入数据权限 (withDataScope() → DataScopeType → 自动追加条件)
  ▼
JdbcDataManager
  │  SQL 构建 (Condition → SqlQueryBuilder → 参数化 SQL)
  │  SQL 重写 (SqlRewriteContext → 租户隔离 / 数据权限注入)
  │  类型处理 (TypeHandler → Java 类型 ↔ 数据库类型)
  ▼
JdbcTemplate → DataSource → Database
```

**关键层边界关注点**：

| 边界 | 关注点 | 常见错误 |
|------|--------|----------|
| Controller → DataManager | 条件构建使用 Lambda 而非字符串 | 使用字符串 Conditions，丢失类型安全 |
| DataManager → EntityMetadataCache | APT 元数据 vs 反射降级 | 实体缺少 @AfEntity，APT 不生成元数据 |
| Conditions → SQL | 空值语义（eq(null) → IS NULL，eqIfPresent(null) → 跳过） | 混淆 eq 和 eqIfPresent 的 null 语义 |
| SQL 重写层 | 软删除/租户/数据权限条件注入顺序 | 在实体层重复过滤已在 SQL 重写层处理的条件 |
| JdbcDataManager → JdbcTemplate | 类型处理器选择 | Instant 字段未配置 InstantTypeHandler |

**溯源**：PRD 第 5.4 节"DataManager 数据访问模块"

### AI 调用链路

```
@Service 方法
  │  @AiChat / @AiAgent / @Workflow 注解
  ▼
AOP 切面
  │  解析注解参数 (client, systemPrompt, temperature, ...)
  │  可选：@AiResilient → 熔断/重试/降级
  │  可选：@ContentSafety → 输入/输出安全检查
  │  可选：@AiAudited → 审计记录
  ▼
ChatClientRegistry / AgentExecutor / DagEngine
  │  查找命名客户端 (client = "default")
  │  获取 AfgChatClient 实现
  ▼
AfgChatClient SPI
  │  SpringAiChatClient (ai-spring-ai 模块)
  │  Lc4jChatClient (ai-langchain4j 模块)
  │  NoOp 默认实现 (无 AI 引擎依赖时)
  ▼
Spring AI / LangChain4J
  │  调用 LLM API (OpenAI / Anthropic / 通义 / ...)
  ▼
LLM 服务
```

**关键层边界关注点**：

| 边界 | 关注点 | 常见错误 |
|------|--------|----------|
| 注解 → AOP | 方法签名匹配（fallbackMethod 必须与原方法同签名） | fallbackMethod 参数类型不匹配 |
| AOP → Registry | 客户端名称解析（client = "default"） | 注册的客户端名称与注解中不一致 |
| Registry → SPI | NoOp 降级（无 AI 引擎依赖时返回空结果） | 忘记处理 NoOp 场景，导致 NPE |
| @AiResilient → 熔断器 | 熔断器窗口/阈值配置 | 熔断器配置过严，正常调用被拒绝 |
| @ContentSafety → 检查器 | 输入/输出检查顺序 | checkInput=true 但输入检查抛异常后仍执行 LLM 调用 |

**溯源**：PRD 第 5.6 节"AI 核心模块"

### 安全校验链路

```
HTTP 请求
  │  Spring Security Filter Chain
  ▼
TenantFilter (多租户解析)
  │  TenantResolverChain → TOKEN / HEADER / DOMAIN 策略
  │  解析 tenantId → TenantContextHolder
  ▼
JWT 验证 (Resource Server 模式)
  │  JWK Set URI / Issuer URI
  │  验证 Token 签名和过期
  ▼
@RequirePermission / @RequireRole AOP
  │  AfgEnforcer → Casbin 策略引擎
  │  检查 (subject, permission) 或 (subject, role)
  ▼
Casbin → JDBC Policy Adapter
  │  从数据库加载 RBAC 策略
  ▼
Controller 方法执行
```

**关键层边界关注点**：

| 边界 | 关注点 | 常见错误 |
|------|--------|----------|
| HTTP → TenantFilter | 租户解析策略顺序（TOKEN → HEADER → DOMAIN → DEFAULT） | 租户解析失败但未配置 fail-if-unresolved |
| TenantFilter → JWT | 租户解析在 JWT 验证之前还是之后 | OAuth2 authorize 端点被 TenantFilter 拦截 |
| JWT → @RequirePermission | Token 中的权限/角色信息 | Token 未包含权限信息，Casbin 策略检查失败 |
| @RequirePermission → Casbin | Casbin model 类型（rbac-domain）与策略匹配 | model-type 配置错误，策略不生效 |
| Security → 模块 context-path | Security 规则需匹配带前缀路径 | Security 配置 `/auth/login` 但实际路径是 `/auth-api/auth/login` |

**溯源**：PRD 第 5.5 节"Security 安全模块"

### 分布式能力链路

```
@Lock / @DistributedTask 注解
  │  AOP 切面拦截
  ▼
DistributedLock / TaskScheduler SPI
  │  有 Redis Bean → Redisson 实现
  │  无 Redis Bean → 内存/本地实现（降级）
  ▼
Redis / 内存
```

**关键层边界关注点**：

| 边界 | 关注点 | 常见错误 |
|------|--------|----------|
| 注解 → SPI | SPI 实现自动发现（@ConditionalOnBean） | 引入了 afg-redis 但 Redis 连接配置错误，降级为内存实现而不知 |
| SPI → Redis | Redisson Watchdog 自动续期 | 锁持有时间超过 waitTime 但未配置 Watchdog |
| @DistributedTask → Scheduler | 集群唯一执行 | 多实例部署时本地调度在每个实例都执行 |

**溯源**：PRD 第 5.3 节"Core 核心模块"、第 5.7 节"afg-redis"

---

## AFG 特有跨层关注点

### 软删除过滤：JDBC 层 vs 实体层

**规则**：软删除过滤在 SQL 重写层自动处理，不要在实体层或 Controller 层重复过滤

```java
// 错误 — 在条件中重复添加软删除过滤
Condition condition = builder(User.class)
    .eq(User::getDeleted, false)  // 多余！框架自动处理
    .eq(User::getStatus, 1)
    .build();

// 正确 — 框架自动过滤，无需手动添加
Condition condition = builder(User.class)
    .eq(User::getStatus, 1)
    .build();

// 需要包含已删除记录时，显式声明
dataManager.entity(User.class)
    .includeDeleted()  // 显式包含
    .findAll(condition);
```

**溯源**：PRD 第 5.4 节"DataManager — 软删除自动过滤"

### 租户条件注入：SQL 重写层

**规则**：租户条件在 SQL 重写层自动注入，开发者无需手动处理

```java
// 错误 — 手动添加租户条件
Condition condition = builder(User.class)
    .eq(User::getTenantId, currentTenantId)  // 多余！框架自动注入
    .build();

// 正确 — 框架自动注入租户条件
dataManager.findAll(User.class);  // → WHERE tenant_id = 'current-tenant'

// 显式指定租户（跨租户操作）
try (var scope = dataManager.tenantScope("other-tenant")) {
    dataManager.findAll(User.class);
}
```

**溯源**：PRD 第 5.5 节"多租户"

### 数据权限条件注入：查询层

**规则**：数据权限通过 withDataScope() 在查询层自动注入

```java
// 自动注入当前用户的数据权限范围
dataManager.entity(Order.class)
    .query()
    .withDataScope()  // ALL/SELF/DEPT/DEPT_AND_CHILD/CUSTOM
    .list();
```

| DataScopeType | 自动注入条件 |
|--------------|-------------|
| ALL | 无额外条件 |
| SELF | create_by = currentUserId |
| DEPT | dept_id = currentDeptId |
| DEPT_AND_CHILD | dept_id IN (currentDept + children) |
| CUSTOM | 自定义策略 |

**溯源**：PRD 第 5.5 节"数据权限"

### APT 元数据：编译时 → 运行时零反射

**规则**：APT 在编译时生成元数据，运行时优先使用 APT 生成的类，降级使用反射

```
编译时：实体类 (@AfEntity) → APT 处理器 → {Entity}Metadata.java
运行时：DataManager → EntityMetadataCache
  → AptMetadataLoader（优先）
  → ReflectiveMetadataLoader（降级）
```

**常见错误**：实体类缺少 @AfEntity，APT 不生成元数据，运行时降级为反射

**溯源**：PRD 第 3.2 节"APT 编译时元数据"

---

## 常见跨层错误

### 错误 1：API 端到端测试中使用 @Transactional

**问题**：HTTP 请求在不同线程处理，@Transactional 的 ThreadLocal 事务上下文无法传播

```java
// 错误 — API 测试中 @Transactional 无效
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional  // 无效！HTTP 不同线程
class UserApiTest {
    @Test
    void shouldCreateUser() {
        restClient.post().body(user).retrieve().body(User.class);
        // 数据已提交，不会回滚
    }
}

// 正确 — API 测试不使用 @Transactional，手动清理
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTest {
    @Test
    void shouldCreateUser() {
        User created = restClient.post().body(user).retrieve().body(User.class);
        // 验证后手动清理
        restClient.delete().uri("/users/" + created.getId()).retrieve();
    }
}
```

**规则**：
- DataManager 数据操作测试 → 使用 @Transactional（自动回滚）
- API 端到端测试 → 不使用 @Transactional
- 并发/锁测试 → 不使用 @Transactional（需要多事务）

**溯源**：PRD 第 5.4 节"注意事项/限制"、CLAUDE.md"测试规范"

### 错误 2：模块 context-path 未在 Security 配置中匹配

**问题**：框架通过 ModuleWebAutoConfiguration 为 Controller 添加模块路径前缀，Security 规则需匹配带前缀路径

```java
// 错误 — Security 配置未包含模块前缀
http.requestMatchers("/auth/login").permitAll();
// 实际路径：/auth-api/auth/login（模块前缀 /auth-api）

// 正确 — Security 配置包含模块前缀
http.requestMatchers("/auth-api/auth/login").permitAll();
```

**注意**：PathMatchConfigurer.addPathPrefix() 会创建两个可访问路径：原始路径 + 带前缀路径

**溯源**：PRD 第 3.5 节"模块化架构理念"、CLAUDE.md"模块 Context-Path"

### 错误 3：AutoConfiguration 排序问题

**问题**：缺少 @AutoConfigureAfter 导致 Bean 创建顺序不确定

```java
// 错误 — 未声明依赖排序
@AutoConfiguration
public class DataManagerAutoConfiguration { }
// DataManager 可能在 DataSource 之前创建，导致启动失败

// 正确 — 声明依赖排序
@AutoConfiguration(after = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
public class DataManagerAutoConfiguration { }
```

**跨模块引用使用 afterName**：

```java
// 正确 — 字符串引用，不需要编译期依赖
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class LiquibaseAutoConfiguration { }
```

**溯源**：PRD 第 6.6 节"AutoConfiguration 编写规则"

### 错误 4：隐式格式假设

**问题**：假设 Instant 字段在数据库中是 TIMESTAMP，但未配置 InstantTypeHandler

**正确**：框架内置 16+ TypeHandler（PRD 第 5.4 节），确保实体字段类型有对应 TypeHandler

### 错误 5：分散的校验逻辑

**问题**：同一校验在 Controller、Service、DataManager 多层重复

**正确**：校验一次在入口点（Controller 层 Bean Validation），框架自动转为 Result.fail

**溯源**：PRD 第 5.3 节"ValidationAutoConfiguration"

---

## 实现跨层功能前

### 步骤 1：映射数据流

画出数据如何在层间流动：

```
HTTP 请求 → Controller → DataManager → JdbcDataManager → JdbcTemplate → Database
     │           │            │               │                │
   JSON      Result<T>    Condition      参数化 SQL        ResultSet
```

对每个箭头，问：
- 数据是什么格式？
- 可能出什么问题？
- 谁负责校验？

### 步骤 2：识别 AFG 特有边界

| 边界 | AFG 特有关注 |
|------|-------------|
| Controller → DataManager | Lambda vs 字符串 Conditions、快捷 vs 链式调用 |
| DataManager → SQL 重写层 | 软删除/租户/数据权限自动注入 |
| SQL 重写层 → JDBC | 参数化 SQL、TypeHandler |
| 注解 → AOP → SPI | 声明式注解的参数解析和 SPI 查找 |
| SPI → 实现 | Default/NoOp 降级、@ConditionalOnBean 自动发现 |

### 步骤 3：定义契约

对每个边界：
- 精确的输入格式是什么？
- 精确的输出格式是什么？
- 可能发生什么错误？
- AFG 框架自动处理了什么？（软删除、租户、数据权限）

---

## 实现后清单

- [ ] 测试了边界情况（null、空集合、无效值）
- [ ] 验证了每个边界的错误处理
- [ ] 确认数据能完整往返
- [ ] 确认未在实体层重复处理 SQL 重写层已自动处理的逻辑（软删除、租户）
- [ ] API 端到端测试未使用 @Transactional
- [ ] Security 配置匹配了带模块前缀的路径
- [ ] AutoConfiguration 声明了 @AutoConfigureAfter

---

## 何时创建流程文档

创建详细流程文档的条件：
- 功能跨越 3+ 层
- 涉及多个模块
- 数据格式复杂
- 功能曾导致缺陷
- 涉及 AI 调用链路（注解 → AOP → SPI → 引擎 → LLM）

---

**溯源声明**：本指南内容均溯源至 [docs/framework-prd.md](../../docs/framework-prd.md) 中第 4 节"模块架构"、第 5.3-5.6 节"逐模块功能需求"、第 6.6 节"模块开发规范"。
