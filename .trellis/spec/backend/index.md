# 后端开发规范

> **权威来源**：[`docs/framework-prd.md`](../../docs/framework-prd.md)
> **框架版本**：1.0.0-SNAPSHOT | **技术栈**：Java 25 + Spring Boot 4 + Gradle

---

## 规范文件索引

| 文件 | 描述 | 状态 |
|------|------|------|
| [directory-structure.md](./directory-structure.md) | 20 个 Gradle 子模块的目录结构、依赖关系图、Maven 坐标、包路径、命名规范、模块选择指南 | Active |
| [data-manager-api.md](./data-manager-api.md) | DataManager 完整 API 参考：45 方法签名、EntityProxy 链式 API、条件构建器、对比表 | Active |
| [database-guidelines.md](./database-guidelines.md) | 查询模式、条件构建器、分页、聚合、10 种方言、类型处理器、SQL 监控、分布式 ID | Active |
| [entity-design.md](./entity-design.md) | 实体基类体系、决策树、特征接口、字段命名/类型约定、@AfEntity 注解、@EncryptedField | Active |
| [migration-guidelines.md](./migration-guidelines.md) | Liquibase XML 规范、8 条铁律、命名约定、内置迁移清单、Gradle 任务 | Active |
| [error-handling.md](./error-handling.md) | BusinessException 异常体系、94 个 CommonErrorCode 错误码、Result/PageData 统一响应、GlobalExceptionHandler 行为 | Active |
| [quality-guidelines.md](./quality-guidelines.md) | 6 条质量铁律、禁止/强制模式、AutoConfiguration 编写规则、SPI 设计、模块间通信 | Active |
| [autoconfiguration-guidelines.md](./autoconfiguration-guidelines.md) | AutoConfiguration 专项：5 条编写铁律、依赖链、80+ 配置清单、@ImportAutoConfiguration 测试 | Active |
| [logging-guidelines.md](./logging-guidelines.md) | @Slf4j 使用规范、MDC 8 字段自动注入、结构化日志、脱敏 6 类、审计日志 | Active |
| [testing-guidelines.md](./testing-guidelines.md) | 禁止 Mockito 铁律、分层策略（单元/集成）、命名规范、数据准备、基类模式、@Transactional 规则 | Active |
| [ai-module.md](./ai-module.md) | AI 模块：双引擎架构（Spring AI / LangChain4J）、9 注解、37 节点、16 AutoConfiguration、本地降级 | Active |
| [security-module.md](./security-module.md) | 安全模块：OAuth2 Server、Casbin RBAC、7 登录策略、多租户 3 模式、数据权限 5 类型、AfgUserDetailsService | Active |
| [gradle-plugin.md](./gradle-plugin.md) | Gradle 插件：扩展配置 9 属性、7 任务、自动行为、securityMode、代码生成 SPI | Active |

---

## 模块架构概览

> 来源：PRD §4.1

框架由 **20 个 Gradle 子模块**组成，按职责分为 7 大类：

```
afg-framework/
├── 通用层       commons / apt-api / apt-impl
├── 核心层       core（31+ AutoConfiguration）
├── 数据层       data-core / data-impl/{data-sql, data-jdbc, data-liquibase}
├── 安全层       security-core / security-impl/{auth-server, resource-server}
├── AI 层        ai-core（16 AutoConfiguration）/ ai-impl/{ai-spring-ai, ai-langchain4j}
├── 集成层       afg-redis
├── 治理层       governance/{proto, client, server}
└── 工具层       gradle-plugin
```

合计 **80+ AutoConfiguration**：core(31+) + data-core(2) + data-jdbc(4) + data-liquibase(1) + ai-core(16) + ai-langchain4j(7) + ai-spring-ai(7) + auth-server(9) + resource-server(2) + afg-redis(1+) + governance-client(1) + governance-server(1)。

详细模块结构、依赖关系、Maven 坐标、包路径请参阅 [directory-structure.md](./directory-structure.md)。

---

## 设计哲学

> 来源：PRD §1.2

框架的每一条设计决策遵循以下 5 条哲学：

### 1. 约定优于配置

合理的默认值优于显式配置。框架的每个功能都有开箱即用的默认行为，配置只用于覆盖。

```
引入 afg-framework-auth-server → OAuth2 授权服务器自动可用
引入 afg-framework-afg-redis → 分布式缓存/锁/调度自动可用
无需任何配置，即可运行
```

### 2. 增强而非替代

所有功能基于 Spring Boot 增强，不替换原生能力。开发者可以同时使用框架增强和 Spring Boot 原生 API。

```
框架提供 DataManager → 增强 Spring JDBC，不替代 JdbcTemplate
框架提供 @AiChat → 增强 Spring AI，不替代 ChatClient
框架提供 AfgCache → 增强 Spring Cache，不替代 @Cacheable
```

### 3. 编译时安全

APT 编译时生成元数据，运行时零反射开销。类型安全优先，错误在编译期暴露而非运行时。

```
@AfEntity → 编译时生成 {Entity}Metadata.java → 运行时零反射
Conditions.builder(User.class).eq(User::getStatus, 1) → Lambda 类型安全
APT 编译期校验 → 缺少 @AfEntity、字段类型不支持等错误即时报告
```

### 4. 开箱即用

每个功能都有本地降级实现，不引入外部依赖即可运行。引入外部依赖后自动升级为生产级实现。

```
无 Redis → Caffeine 本地缓存 + 内存锁 + 本地调度
有 Redis → Redisson 分布式缓存/锁/调度，自动升级
无 AI 引擎 → NoOp 默认实现，框架正常运行
有 Spring AI / LangChain4J → 自动发现并装配
```

### 5. 模块化按需加载

引入即生效，不引入零侵入。每个模块通过 `@ConditionalOnBean` / `@ConditionalOnClass` 自动发现和装配。

```
引入 afg-framework-ai-core → AI 能力自动可用
不引入 → 零 AI 相关 Bean 注册，零性能开销
```

---

## 质量底线

> 来源：PRD §1.5

框架的每个功能必须满足以下底线：

1. **每个功能必须有 NoOp 降级实现** — 不引入外部依赖即可运行
2. **每个功能必须有开/关注解或条件** — `@ConditionalOnXxx` + `enabled` 开关
3. **每个功能必须有测试覆盖** — 集成测试 + Testcontainers，禁止 Mockito
4. **每个 AutoConfiguration 必须声明依赖排序** — `@AutoConfigureAfter` / `@AutoConfigureBefore`
5. **每个配置项必须有合理默认值** — 零配置即可运行
6. **每个 SPI 必须有本地默认实现** — 不依赖外部实现即可工作

---

## 开发者体验优先级

> 来源：PRD §1.4

1. **声明式 > 编程式** — `@AiChat` 优于手动调用 ChatClient
2. **链式 > 嵌套** — `dataManager.entity(User.class).query().where(condition).list()` 优于 `query.setParameter(...)`
3. **Lambda > 字符串** — `User::getStatus` 优于 `"status"`
4. **注解 > 配置** — `@DistributedTask` 优于 `afg.scheduler.task.xxx.enabled=true`
5. **自动发现 > 手动注册** — `@ConditionalOnBean` 优于 `@Bean` 手动声明
6. **默认安全 > 默认开放** — 新增 API 默认需要认证，显式 `permitAll()` 才开放
