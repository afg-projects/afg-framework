# AFG Framework 代码评审报告

**评审日期**: 2026-06-02
**评审范围**: afg-framework 全部 22 个 Gradle 子模块
**代码基线**: 2038 个 Java 源文件（~30 万行源码 + ~14.8 万行测试代码）
**技术栈**: Java 25 + Spring Boot 4.0.6 + Gradle 9.4 (Kotlin DSL)

---

## 目录

1. [架构与模块设计](#1-架构与模块设计)
2. [数据访问层](#2-数据访问层)
3. [安全模块](#3-安全模块)
4. [AI 核心模块](#4-ai-核心模块)
5. [并发与线程安全](#5-并发与线程安全)
6. [测试质量](#6-测试质量)
7. [技术债务与改进路线图](#7-技术债务与改进路线图)

---

## 1. 架构与模块设计

### 1.1 现状

afg-framework 由 22 个 Gradle 子模块组成，按自底向上分层：

```
commons (无依赖) → apt-api → apt-impl
                 → core (缓存/事件/异常/安全/调度/Web增强/自动配置)
                    → data-core → data-sql → data-jdbc/data-liquibase
                    → security-core → auth-server/resource-server
                    → ai-core → ai-spring-ai/ai-langchain4j
                    → integration/afg-{redis,rabbitmq,websocket,storage,jdbc,governance}
```

依赖策略清晰：`api` 暴露接口，`implementation` 隐藏实现。自动配置体系成熟（core 模块 32 个 AutoConfiguration + 6 个自定义 Conditional 注解）。已有内部评审文档 `core-module-review.md` 记录了 core 模块不拆分的决策依据。

### 1.2 发现的问题

#### 1.2.1 无用的 api 依赖传递

| 声明 | 所在模块 | 实际使用 | 建议 |
|------|----------|----------|------|
| `api(project(":core"))` | `data-core` | **无任何 import** | 降为 `implementation` 或移除 |
| `api(project(":core"))` | `ai-core` | 仅使用了 `core.invocation` 的 6 个类 | 降为 `implementation` |
| `api(project(":data-core"))` | `auth-server` | 实际用到的类可更精确引用 | 审查后降级 |
| `api(libs.jjwt.api)` | `auth-server` | JWT 编解码使用 | 见安全模块分析 |

`data-core` 的 build.gradle.kts 中声明了 `api(project(":core"))`，但 grep 确认无任何 Java import 来自 core 包，这是无用传递依赖，会拖慢增量编译并引入不必要的传递依赖。

#### 1.2.2 异常体系双栖

core 模块有 `core/model/exception/` 下的 6 个异常相关类（`AfgException`、`BusinessException`、`ErrorCode`、`ErrorCategory`、`CommonErrorCode`、`ErrorCodeRange`），全部标记为 `@Deprecated`。commons 模块已有对应的替代实现（`commons/exception/ErrorCode`、`commons/model/Result`、`commons/model/PageData`）。

但迁移**未完成**：
- core 的 `GlobalExceptionHandler` 仍引用 `core.model.exception.CommonErrorCode` 和 `core.model.result.Result`
- security-core 仍引用 `core.model.exception.BusinessException`
- 大量下游使用方（afg-backend、afg-governance）通过 `api` 依赖拿到了旧版本

#### 1.2.3 自动配置集中在 core 模块

32 个 AutoConfiguration 集中在 core 的 `autoconfigure/` 包中，使得即使只想要 data 或 security 功能的模块也需引入完整的 core 自动配置体系。虽然 Java 编译时仅 0.9 秒影响不大，但这在微服务裁剪部署场景中不够灵活。

#### 1.2.4 缺少模块间架构规则校验

虽然声明了 ArchUnit 依赖（`libs.archunit.junit5`），但未在实际测试中使用。没有架构守护确保：
- `commons` 不依赖其他模块
- `api/` 包不引用实现类
- 模块间不存在循环依赖

### 1.3 改进建议

1. **清理无用 api 依赖**（P1, 1d）：data-core 移除对 core 的 api 依赖；ai-core 对 core 的依赖降为 `implementation`；各 build.gradle.kts 统一检查
2. **完成异常体系迁移**（P1, 1.5d）：将 commons 的异常和模型类作为主入口，core 中的已废弃版本逐步删除；参考已存在的 `docs/commons-migration-analysis.md` 方案
3. **加入 ArchUnit 架构测试**（P2, 1d）：至少添加模块依赖方向校验和循环依赖检测
4. **考虑 autoconfigure 独立模块**（P3, 2d）：将 `core/autoconfigure/` 拆为独立 `spring-boot-autoconfigure` 子模块，使核心库不携带 Spring Boot 环境依赖

---

## 2. 数据访问层

### 2.1 现状

`DataManager` 作为无泛型全局门面（`entity(Class)` 获取实体代理），配合 APT 编译时元数据（`@AfEntity` → `{Entity}Metadata`），实现零反射开销的 ORM。支持 12 种数据库方言，内置软删除、多租户、乐观锁、审计字段的自动检测。条件查询 API 提供 Lambda 风格类型安全（`builder(User.class).eq(User::getStatus, 1)`）。

### 2.2 发现的问题

#### 2.2.1 DataManager 接口过载

`DataManager` 同时承担 5 种职责：
- 实体操作代理（`entity()`、`findById`、`save` 等）
- SQL 构建器工厂（`query()`、`update()`、`insert()`、`delete()`）
- 事务适配（`transactional()`）
- 元数据查询（`getEntityMetadata()`）
- 实体事件发布

接口方法约 40+，违反接口隔离原则（ISP）。数据层任何变更都需修改此接口及其所有实现（目前只有 `DefaultDataManager` 一种，但未来会有）。

#### 2.2.2 两条条件 API 共存

- **新 API**：`data-core/condition/TypedConditionBuilder`（Lambda 风格，`User::getStatus`）
- **旧 API**：`data-core/query/Condition`、`data-core/query/Operator`（字符串风格，`"status"`）

旧 API 中的 `Operator` 枚举已标记 `@Deprecated`，但仍然被 use case 引用。两套 API 并存增加使用者的认知负担。

#### 2.2.3 异常层级重复

`data-core/exception/` 目录下有 13 个异常类，包括：

| 异常类 | 与 core/commons 的重复关系 |
|--------|--------------------------|
| `EntityNotFoundException` | 语义上 = `BusinessException` + 特定错误码 |
| `DataAccessException` | 与 Spring 的 `DataAccessException` 重名 |
| `OptimisticLockException` | 与 JPA 的 `OptimisticLockException` 重名 |
| `DuplicateEntityException` | 可复用 `CommonErrorCode.DUPLICATE` |
| `DataPermissionException` | 语义上 = `BusinessException` + 权限错误码 |
| `MultiTenantException` | 语义上 = `BusinessException` + 租户错误码 |

建议复用统一的 `BusinessException` + 专用 `ErrorCode` 体系，避免异常类爆炸。

#### 2.2.4 JdbcEntityQuery 包含 TODO

`data-impl/data-jdbc/src/main/java/.../JdbcEntityQuery.java` 中存在未完成的 TODO，暂未解决（具体内容待打开确认）。

#### 2.2.5 DataScope SQL 拦截跨模块

数据权限的 SQL 拦截逻辑分布在 `data-sql`（SQL 构建器级别拦截）→ `core/security/datascope`（上下文管理）→ `security-impl/auth-server`（策略解析）三个模块，调试链路过长，难以理解完整的拦截路径。

### 2.3 改进建议

1. **拆分 DataManager 职责**（P2, 2d）：提取 `EntityOperations<T>`（实体操作）、`SqlOperations`（SQL 构建）、`TransactionOperations`（事务处理）三个子接口，`DataManager` 作为组合门面继承/包含它们
2. **统一条件 API**（P3, 0.5d）：彻底移除已废弃的字符串风格 API 和 `Operator` 枚举
3. **统一异常体系**（P1, 伴随架构改进完成）：`data-core/exception/` 只保留 DataManager 特有的异常（如 `MetadataLoadException`），通用的（如 `EntityNotFoundException`）用 `BusinessException` + 错误码替代
4. **DataScope 拦截抽象为 SPI 接口**（P3, 1d）：减少跨模块耦合，便于独立测试
5. **补充 APT 处理器测试**（P2, 0.5d）：当前 EntityMetadataProcessorTest 主要覆盖正常路径，需补充异常路径（无效实体、重复配置等）

---

## 3. 安全模块

### 3.1 现状

安全模块分两层：`security-core`（抽象层）和 `security-impl/auth-server` + `security-impl/resource-server`（实现层）。

认证服务器功能完整：
- OAuth2 授权服务器（授权码流、PKCE 支持、Token 生命周期管理）
- 3 种登录策略（用户名密码、手机验证码、邮箱验证码）— 策略模式可扩展
- 安全防护：登录失败锁定、密码强度校验、IP 限制、设备绑定
- Casbin RBAC with domains 权限模型
- 数据权限（DataScope）+ 多租户解析链（Token/Header/Domain）
- RSA 密钥对自动轮换 + JWKS 端点

### 3.2 发现的问题

#### 3.2.1 XSS 防护的正则绕过风险

`InputSanitizer`（`core/web/security/sanitizer/`）使用 10 条正则规则检测 XSS，存在已知绕过方式：
- 属性编码绕过（`&#x73;&#x63;&#x72;&#x69;&#x70;&#x74;`）
- 多层嵌套绕过（`<scr<script>ipt>`）
- SVG 属性事件绕过（`<svg onload=alert(1)>` — 当前正则匹配 `<svg...>` 但不检查 onload 事件）
- CSS expression 绕过（IE 老版本）

`EnhancedInputSanitizer` 使用 OWASP AntiSamy 是正确方案，但当前 `XssFilter` 的无参构造函数使用了纯正则版本的 `XssChecker`，而非 AntiSamy。

#### 3.2.2 双重 JWT 库

`auth-server/build.gradle.kts` 同时声明：
```kotlin
api(libs.nimbus.jose.jwt)  // Spring Security 原生支持
api(libs.jjwt.api)          // 另一套 JWT 实现
runtimeOnly(libs.jjwt.impl)
runtimeOnly(libs.jjwt.jackson)
```

使用两套 JWT 库的问题：
- 增加依赖体积（约 2MB+）
- 签名/验证算法实现可能有细微差异
- 增加安全漏洞攻击面（多一个库 = 多一份 CVE 暴露）
- JJWT 在 `api` 级别暴露于下游模块

#### 3.2.3 ThreadLocal 上下文在异步场景的泄漏风险

- `TenantContextHolder.THREAD_LOCAL` — 来自 `data-core`
- `DataScopeContextHolder.CONTEXT_HOLDER` — 来自 `core`
- `AfgSecurityContext` — 来自 `security-core`

这三个 ThreadLocal 在 `@Async`/`CompletableFuture`/`TaskExecutor` 环境下需手动传播。虽然有 `TenantContextPropagatingExecutorService`，但缺乏统一框架，新增上下文时容易遗漏。

#### 3.2.4 设备绑定的防伪造缺失

`DefaultDeviceLimiter` 的设备 ID 由客户端提交（HTTP Header 或请求体），服务端直接存储和计数。恶意客户端可伪造不同设备 ID 绕过 `max-devices` 限制。正确的做法是设备 ID 应包含服务端签名的 HMAC 校验。

#### 3.2.5 Token 黑名单的自动清理

`JdbcTokenBlacklist` 将已撤销的 Token 持久化存储，但缺少自动的过期清理机制。长期运行后黑名单表会持续膨胀。

### 3.3 改进建议

1. **默认启用 AntiSamy 进行 XSS 过滤**（P0, 0.5d）：修改 `XssFilter` 的默认构造函数，使用 `EnhancedInputSanitizer`；正则作为兜底；确保 `AntiSamy policy.xml` 正确配置并随包分发
2. **统一为 Nimbus JWT**（P1, 0.5d）：移除 JJWT 依赖，JWT 全部使用 nimbus-jose-jwt（Spring Security 原生管理）；`DefaultTokenService` 的签名/验证逻辑统一使用 Nimbus API
3. **统一 ThreadLocal 异步传播框架**（P0, 1d）：创建注册式 `ThreadLocalContextPropagator`，各 ContextHolder 注册自己的传播逻辑；在 `AsyncConfigurer` 和 `TaskDecorator` 中统一应用
4. **设备 ID 加入服务端签名**（P1, 1d）：`DefaultDeviceLimiter` 接收到设备 ID 时校验 HMAC，如果无签名或签名错误则拒绝；注册/绑定设备时返回带签名的设备 Token
5. **Token 黑名单定期清理**（P2, 0.5d）：为 `JdbcTokenBlacklist` 添加 `@Scheduled` 清理任务，基于 `accessTokenTtl` + 缓冲期删除过期记录

---

## 4. AI 核心模块

### 4.1 现状

ai-core 是最近重构为"重型化"的核心模块，295 个源文件，覆盖 13 个 API 领域（Agent、Chat、RAG、Workflow、Pipeline、Tool、Memory、ETL、Skill、Model、Resilience、Security、Observability）。

三层架构清晰：
- 第一层（`api/`）：接口 + 轻量默认实现 + DTO
- 第二层（`chat/`、`agent/`、`workflow/` 等）：功能实现
- 第三层（`entity/`）：JPA 实体

双 SPI 适配（Spring AI / LangChain4J）。本地降级模式完善（InMemory/NoOp）。16 个 AutoConfiguration 按 `@ConditionalOnBean(DataManager)` 按需激活 JDBC 持久化。8 个 AOP 注解 + 切面。

### 4.2 发现的问题

#### 4.2.1 api/ 包混入实现类

CLAUDE.md 描述第一层为"纯接口 + 轻量默认实现 + DTO"，但实际 `api/` 包中包含了大量"轻量默认实现"类，甚至这些类就是完整实现：

**api/agent/ 包中的实现类：**
- `DefaultCoordinator` — 完整的 Multi-Agent 协调器实现（依赖 `AgentRegistry`、`CommunicationBus`、`TaskDecomposer`）
- `DefaultAgentNode` — 完整的 Agent 执行节点
- `DefaultRouterNode` — 完整的路由节点
- `DefaultHumanNode` — 完整的人工介入节点
- `DefaultParallelNode` — 完整的并行执行节点
- `DefaultReActExecutor` — 完整的 ReAct 循环执行器
- `TemplateTaskDecomposer` — 完整的任务分解器
- `InMemoryCommunicationBus` — 完整的内存通信总线
- `InMemoryStateManager` — 完整的状态管理器

**api/pipeline/ 包中的实现类：**
- `DefaultEtlPipeline` — 完整的 ETL 流水线

当这些类分布在 `api/` 包中时：
- 接口和实现混在一起，使用者难以区分"应该依赖的接口"和"可以用的默认实现"
- API 兼容性承诺模糊——更改这些默认实现是否视为 API 变更？
- 与 `chat/`、`agent/`、`workflow/` 等二级实现包职责重叠

#### 4.2.2 JPA 实体无条件加载

`entity/` 包中的 16 个 JPA 实体（`AgentDefinitionEntity`、`WorkflowExecutionEntity`、`KnowledgeBaseEntity` 等）在 classpath 上会被 JPA 自动扫描。如果用户项目不使用 DataManager 或 JPA，这些实体不应被加载。

检查 `AiEntityAutoConfiguration` 的 `@Conditional` 配置是否充分。SLF4J 日志即使抑制了，实体的 `@Table`、`@Entity` 注解可能触发 Hibernate 的 Schema 校验。

#### 4.2.3 LangChain4J 适配不完整

Spring AI 适配模块（`ai-spring-ai`）有 7 个 AutoConfiguration（Chat、Model、Advisor、Memory、Observation、Embedding、...），LangChain4J 适配模块（`ai-langchain4j`）只有 5 个，缺少 Pipeline、Workflow 的核心流程适配。

#### 4.2.4 Agent/Workflow 的测试覆盖不足

Agent、Workflow、Pipeline 是 ai-core 的核心执行引擎，但从文件列表看缺乏成体系的端到端测试。Workflow 的 DAG 引擎（`DefaultDagEngine` + `TopologicalSorter`）尤其需要覆盖复杂 DAG（环检测、并行分支、条件分支、异常处理）的测试。

### 4.3 改进建议

1. **重构 api/ 包**（P1, 2d）：将所有默认实现从 `api/` 移到对应的二级包（`agent/`、`pipeline/`、`workflow/` 等），`api/` 只保留纯接口、DTO、函数式接口；结果验证：`api/` 包下的 .java 文件应减少约 50 个
2. **JPA 实体添加 @ConditionalOnClass 保护**（P1, 1d）：在 AiEntityAutoConfiguration 和各个 Repository bean 上添加 `@ConditionalOnClass(DataManager.class)`，确保非 JPA 环境不会加载实体
3. **补齐 LangChain4J 适配**（P3, 1.5d）：至少添加 Pipeline 和 ChatClient Registry 的支持
4. **添加 Agent/Workflow/Pipeline 核心测试**（P1, 2d）：覆盖 DefaultDagEngine（环检测、并行执行、条件分支、超时、取消）、DefaultReActExecutor（迭代限制、工具调用、错误恢复）、DefaultChatPipeline（步骤链、RAG 集成）
5. **添加启动时依赖检查**（P2, 0.5d）：如果 `afg.ai.chat.enabled=true` 但无任何 `AfgChatClient` 实现 Bean，输出清晰的警告

---

## 5. 并发与线程安全

### 5.1 现状

- 分布式锁：Redisson（`integration/afg-redis`），支持可重入、自动续期
- 缓存：多级缓存（本地 Caffeine + 分布式 Redis），`AfgCache` 接口封装
- 限流：滑动窗口，维度可扩展，支持本地存储和 Redis 存储
- ThreadLocal：16 处使用，集中在请求上下文、租户上下文、数据权限上下文

### 5.2 发现的问题

#### 5.2.1 ThreadLocal 缺乏统一传播框架

6 个 ContextHolder 各有自己的 ThreadLocal：

| Holder | 所在模块 | 传播机制 | 风险 |
|--------|----------|----------|------|
| `DataScopeContextHolder` | core | 手动 | 遗漏 |
| `TenantContextHolder` | data-core | `PropagatingExecutorService` | 手动创建 |
| `AfgRequestContextHolder` | core | `RequestContextFilter` | 仅同步 |
| `ModelRouteContext` | ai-core | 无 | 异步丢失 |
| `BaggageContext` | core | `EnhancedTraceInterceptor` | 仅链路 |
| `AfgSecurityContext` | security-core | 无 | 异步丢失 |

`TenantContextPropagatingExecutorService` 是正确做法，但需要开发者手动使用，而不是自动装配。新增 ContextHolder 容易被遗忘。

#### 5.2.2 RemoteToolContextHolder 在响应式环境失效

`RemoteToolContextHolder`（ai-core）使用 `NamedThreadLocal` 存储工具调用上下文。如果 `AiServiceNode` 或 Pipeline 运行在 WebFlux/Reactor 环境中，ThreadLocal 不跨操作符传播。

#### 5.2.3 默认缓存管理器废弃不彻底

`DefaultCacheManager`（`core/cache/`）标记为 `@Deprecated`，但仍有引用它的代码（grep 确认使用方）。新旧缓存 API 共存增加了开发者混淆。

#### 5.2.4 测试全局串行化

根 `build.gradle.kts` 设置了 `maxParallelForks = 1`，所有子模块的测试串行执行。虽然注释原因是 Testcontainers 容器冲突，但这对纯单元测试（不涉及容器）不必要。

#### 5.2.5 滑动窗口限流的线程安全性待确认

从包结构看，`DefaultRateLimiter` 使用 `RateLimitStorage` SPI，其中 `LocalRateLimitStorage` 是内存实现。滑动窗口的实现如果是 `synchronized` 或 `ConcurrentHashMap` + `AtomicReference`，在高并发下需要确认正确性。

### 5.3 改进建议

1. **统一 ThreadLocal 传播框架**（P0, 1d）：创建可注册的 `ThreadLocalPropagator`，各 ContextHolder 调用 `ThreadLocalPropagator.register(name, holder)` 注册自己的传播逻辑；在 `AsyncConfigurer.getAsyncUncaughtExceptionHandler()` 中自动应用
2. **为响应式路径添加 Reactor Context 适配**（P2, 1d）：`RemoteToolContextHolder` 增加 Reactor `Context` 的读写路径；在 WebFlux Filter 中将 ThreadLocal → Reactor Context
3. **彻底移除 @Deprecated 的 DefaultCacheManager**（P1, 0.5d）：完成旧缓存 API 到 `AfgCache` 的迁移
4. **拆分测试并行策略**（P2, 0.5d）：integration 子模块保持 `maxParallelForks=1`，纯 unit test 模块放开到 4+
5. **Review RateLimiter 的并发实现**（P2, 0.5d）：确认 `LocalRateLimitStorage` 的滑动窗口操作是否原子化，添加并发压力测试

---

## 6. 测试质量

### 6.1 现状

- 480 个 @Test 方法，14.8 万行测试代码
- JMH 基准测试覆盖 SQL Builder、Cache、DataManager
- ArchUnit 依赖已声明但未使用
- Testcontainers 用于 Kafka、RabbitMQ、Redis 集成测试
- JaCoCo 覆盖率阈值：全局 60%，data 模块 70%

### 6.2 发现的问题

#### 6.2.1 测试分布不均

| 模块 | 估算测试文件数 | 估算覆盖率 | 说明 |
|------|--------------|-----------|------|
| core | ~300+ | 70%+ | 覆盖充分 |
| data-core | ~30 | 65% | 方言/映射器覆盖不足 |
| data-impl/data-jdbc | ~40 | 60% | 性能测试多，集成测试少 |
| apt-impl | 2 | 30% | 严重不足 |
| security-impl/auth-server | ~30 | 50% | 策略模式测试不全 |
| security-impl/resource-server | ~5 | 25% | 严重不足 |
| ai-core | ~30 | 15% | 开发阶段，待补充 |
| ai-impl | ~5 | 10% | 开发阶段 |
| integration | ~20 | 40% | 部分有 Testcontainers，部分无 |

#### 6.2.2 缺少 CI 集成测试环境配置

根 build.gradle.kts 未配置 Testcontainers Docker 环境变量（如 `testcontainers.reuse.enable`），本地运行集成测试需要开发者自行启动 Docker。

#### 6.2.3 data-core 的方言测试不完整

虽然支持 12 种数据库方言，但 DataManager 的集成测试仅使用 H2 数据库，未覆盖 PostgreSQL/Oracle/DM 等生产方言的行为差异（如分页 SQL、主键生成策略、序列处理）。

#### 6.2.4 ai-core 测试覆盖严重不足

Agent、Workflow、Pipeline 三大核心执行引擎缺乏系统性的端到端测试。`DefaultDagEngine` 的拓扑排序未覆盖循环图、并行执行未测试竞争条件。

### 6.3 改进建议

1. **生成模块级覆盖率报告**（P2, 0.5d）：明确各模块的 JaCoCo 覆盖率基线，设定模块级阈值
2. **补充 ai-core 核心组件测试**（P1, 2d）：Pipeline、ReActAgent、WorkflowEngine、ETL pipeline；Mock LLM 响应进行端到端验证
3. **补充 auth-server 集成测试**（P1, 1.5d）：覆盖 OAuth2 流（授权码、PKCE）、Token 刷新/撤销、各种安全策略触发
4. **resource-server 测试**（P1, 1d）：覆盖 JWT 验证（过期/篡改/黑名单）、多租户解析、权限判定
5. **ArchUnit 架构测试启用**（P2, 1d）：模块依赖方向、包访问规则、`api/` 包纯度检查
6. **在 CI 中配置 Testcontainers**（P3, 1d）：Docker 环境检查，编译时跳过不可用的容器测试
7. **扩展方言测试矩阵**（P3, 1d）：至少覆盖 PostgreSQL（开源标准）和一种国产数据库

---

## 7. 技术债务与改进路线图

### 7.1 技术债务统计

| 类别 | 数量 | 分布 |
|------|------|------|
| @Deprecated 类 | 16 处 | 主要为 core 异常和条件 API |
| TODO/FIXME | 7 处 | ai-core AutoConfig + data-jdbc |
| 无用 api 依赖 | 3+ 处 | data-core→core, ai-core→core |
| 双库冲突 | 1 处 | Nimbus + JJWT |
| 未完成的异常迁移 | 1 项 | core→commons |

### 7.2 安全依赖版本

OWASP Dependency Check 已配置（CVSS 7.0 阈值）。需要特别关注：

| 依赖 | 版本 | 说明 |
|------|------|------|
| AntiSamy | 1.7.8 | 安全组件，需追踪 CVE |
| Redisson | 4.3.1 | 与 Spring Boot 4 兼容性需验证 |
| Nacos | 2.4.2 | 客户端版本 |
| JJWT | 0.12.6 | JWT 库，建议合并到 Nimbus |

### 7.3 改进路线图（按优先级排序）

#### P0 — 安全修复（高优先级）

| 改进项 | 模块 | 工作量 | 说明 |
|--------|------|--------|------|
| XSS 过滤默认启用 AntiSamy | core | 0.5d | 当前默认使用可被绕过的正则版本 |
| ThreadLocal 统一异步传播机制 | core/data/security/ai | 1d | 6 个 ContextHolder 传播机制碎片化 |

#### P1 — 架构合理与功能完整

| 改进项 | 模块 | 工作量 | 说明 |
|--------|------|--------|------|
| 统一 JWT 库（移除 JJWT） | auth-server | 0.5d | 减少依赖膨胀和攻击面 |
| 完成 commons 异常体系迁移 | 全模块 | 1.5d | 消除 Deprecated 依赖 |
| 清理无用 api 依赖 | data-core, ai-core, auth-server | 1d | 模块解耦、减少传递依赖 |
| ai-core api/ 包分层重构 | ai-core | 2d | 接口与实现分离 |
| ai-core 核心组件测试补充 | ai-core | 2d | Pipeline、Agent、Workflow 端到端测试 |
| auth-server + resource-server 测试 | security-impl | 2.5d | 补齐关键安全路径测试 |
| 设备 ID 服务端签名 | auth-server | 1d | 防止设备绑定绕过 |
| JPA 实体 ConditionalOnClass 保护 | ai-core | 1d | 保证非 JPA 环境兼容 |

#### P2 — 质量提升

| 改进项 | 模块 | 工作量 | 说明 |
|--------|------|--------|------|
| DataManager 接口拆分 | data-core | 2d | 单一职责 |
| JaCoCo 覆盖阈值提升 | 全模块 | 2d | 全局 65%，核心 75% |
| 单测并行度放开 | 全模块 | 0.5d | 构建效率优化 |
| ArchUnit 架构测试 | 全模块 | 1d | 守护模块边界 |
| 旧条件 API 清理 | data-core | 0.5d | 减少技术债务 |

#### P3 — 长期改进

| 改进项 | 模块 | 工作量 | 说明 |
|--------|------|--------|------|
| LangChain4J 适配补齐 | ai-langchain4j | 1.5d | Pipeline、Workflow |
| DataScope SQL 拦截 SPI 化 | data-sql | 1d | 减少跨模块耦合 |
| 自动配置独立模块 | core | 2d | 微服务部署裁剪 |
| AntiSamy policy.xml 安全审计 | core | 1d | 确保策略文件不遗漏安全标签 |
| Token 黑名单自动清理定时任务 | auth-server | 0.5d | 避免表膨胀 |
| CI Testcontainers Docker 环境 | 基础设施 | 1d | 集成测试可靠性 |
| 方言测试矩阵扩展 | data-jdbc | 1d | PostgreSQL + 国产数据库 |

### 7.4 总工作量预估

| 优先级 | 工作量 | 说明 |
|--------|--------|------|
| P0 | 1.5 人天 | 安全修复，立即执行 |
| P1 | 11.5 人天 | 架构优化 + 测试补齐 |
| P2 | 6 人天 | 质量提升 |
| P3 | 7 人天 | 长期优化 |
| **合计** | **26 人天** | 约 1 个季度内分阶段完成 |

---

## 附录 A：主要发现速查表

| # | 严重度 | 类型 | 发现 | 模块 | 改进建议 |
|---|--------|------|------|------|----------|
| 1 | 🔴 | 安全 | XSS 过滤默认使用正则，可绕过 | core | 默认启用 AntiSamy |
| 2 | 🔴 | Bug | 6 个 ThreadLocal 异步传播不统一 | 全模块 | 统一传播框架 |
| 3 | 🟠 | 依赖 | 双 JWT 库（Nimbus + JJWT） | auth-server | 统一为 Nimbus |
| 4 | 🟠 | 架构 | data-core 对 core 的无用 api 依赖 | data-core | 移除或降级 |
| 5 | 🟠 | 架构 | api/ 包混入实现类 | ai-core | 分层重构 |
| 6 | 🟠 | 安全 | 设备 ID 无服务端签名 | auth-server | 加入 HMAC |
| 7 | 🟠 | 测试 | ai-core 核心组件测试缺失 | ai-core | 补充端到端测试 |
| 8 | 🟠 | 测试 | security-impl 测试不足 | security-impl | 补齐关键路径 |
| 9 | 🟡 | 架构 | exception 体系双栖 | 全模块 | 完成 commons 迁移 |
| 10 | 🟡 | 架构 | DataManager 接口职责过载 | data-core | 按职责拆分 |
| 11 | 🟡 | 测试 | 单测串行执行 | 全模块 | 按模块拆分并行度 |
| 12 | 🟢 | 代码 | TODO 未完成 | ai-core, data-jdbc | 逐一确认关闭 |
| 13 | 🟢 | 安全 | Token 黑名单无自动清理 | auth-server | 添加定时任务 |
| 14 | 🟢 | 测试 | ArchUnit 未启用 | 全模块 | 添加架构测试 |

**严重度说明**: 🔴 高（立即关注） 🟠 中（近期规划） 🟡 低（持续改进） 🟢 信息（无需立即行动）

---

## 附录 B：已存在的内部评审文档

本次评审参考了项目中已有的内部文档：

1. **`docs/core-module-review.md`**（2026-05-23）— Core 模块拆分必要性评审，结论"不建议拆分"
2. **`docs/commons-migration-analysis.md`**（2026-05-23）— 迁移异常/结果类到 commons 的详细方案

本报告第 1 节和第 2 节的建议与这两份分析保持一致，并提出了进一步的实施优先级。

---

**评审人**: AFG Framework Code Review
**日期**: 2026-06-02
