# AFG Framework — 规范索引

> **权威来源**：所有规范派生自 [`docs/framework-prd.md`](../../docs/framework-prd.md)（产品需求文档）
> **状态**：PRD 尚非最终版本，规范将随 PRD 更新而同步修订

---

## 规范更新规则

当 `docs/framework-prd.md` 发生变更时，**对应的规范章节必须同步更新**。具体映射关系如下：

| PRD 章节 | 需更新的规范文件 |
|----------|-----------------|
| §1 设计哲学 / §1.5 质量底线 | `backend/index.md`（设计哲学部分） |
| §4.1 模块清单 | `backend/directory-structure.md`（模块结构） |
| §4.2 依赖关系 | `backend/directory-structure.md`（依赖图） |
| §4.3 Maven 坐标 | `backend/directory-structure.md`（Maven 坐标表） |
| §4.4 模块选择指南 | `backend/directory-structure.md`（模块选择指南） |
| §5.1 Commons | `backend/error-handling.md`（异常体系、错误码） |
| §5.2 APT 注解处理 | `backend/autoconfiguration-guidelines.md`（APT 注解约定）、`backend/entity-design.md`（@AfEntity） |
| §5.3 Core 核心模块 | `backend/quality-guidelines.md`（AutoConfiguration 规则）、`backend/logging-guidelines.md` |
| §5.4 DataManager | `backend/database-guidelines.md`、`backend/data-manager-api.md`、`backend/entity-design.md`、`backend/migration-guidelines.md` |
| §5.5 Security | `backend/security-module.md` |
| §5.6 AI 核心模块 | `backend/ai-module.md` |
| §5.7 Redis 集成 | `backend/quality-guidelines.md`（SPI 升级模式） |
| §5.8 Governance | `backend/directory-structure.md`（治理模块结构） |
| §5.9 Gradle 插件 | `backend/gradle-plugin.md` |
| §6.1 编码规范 / 命名约定 | `backend/directory-structure.md`（命名规范） |
| §6.2 API 设计规范 | `backend/error-handling.md`（异常与响应格式） |
| §6.3 异常处理规范 | `backend/error-handling.md` |
| §6.5 实体设计规范 | `backend/database-guidelines.md` |
| §6.6 模块开发规范 | `backend/quality-guidelines.md` |

---

## 规范目录一览

### `backend/` — 后端开发规范

| 文件 | 内容概述 | 状态 |
|------|---------|------|
| [`index.md`](./backend/index.md) | 后端规范总览：模块架构、设计哲学、规范索引 | Active |
| [`directory-structure.md`](./backend/directory-structure.md) | 模块目录结构、依赖关系、Maven 坐标、包路径、命名规范、模块选择指南 | Active |
| [`database-guidelines.md`](./backend/database-guidelines.md) | 数据库规范：查询模式、条件构建器、分页、聚合、10 种方言、SQL 监控 | Active |
| [`data-manager-api.md`](./backend/data-manager-api.md) | DataManager API 完整参考：45 方法签名、EntityProxy 链式 API、条件操作符、对比表 | Active |
| [`entity-design.md`](./backend/entity-design.md) | 实体设计：基类体系、决策树、特征接口、字段约定、@AfEntity 注解 | Active |
| [`migration-guidelines.md`](./backend/migration-guidelines.md) | 数据库迁移：Liquibase XML 规范、8 条铁律、命名约定、内置迁移清单 | Active |
| [`error-handling.md`](./backend/error-handling.md) | 异常处理：异常体系、CommonErrorCode 94 码、Result/PageData、全局处理 | Active |
| [`quality-guidelines.md`](./backend/quality-guidelines.md) | 质量规范：6 条铁律、禁止/强制模式、AutoConfiguration 编写规则、SPI 设计 | Active |
| [`autoconfiguration-guidelines.md`](./backend/autoconfiguration-guidelines.md) | AutoConfiguration 专项：5 条编写铁律、依赖链、80+ 配置清单、SPI 模式 | Active |
| [`logging-guidelines.md`](./backend/logging-guidelines.md) | 日志规范：@Slf4j、MDC 8 字段、结构化日志、脱敏 6 类 | Active |
| [`testing-guidelines.md`](./backend/testing-guidelines.md) | 测试规范：禁止 Mockito 铁律、分层策略、命名规范、数据准备、基类模式 | Active |
| [`ai-module.md`](./backend/ai-module.md) | AI 模块：双引擎架构、9 注解、37 节点、16 AutoConfiguration、本地降级 | Active |
| [`security-module.md`](./backend/security-module.md) | 安全模块：OAuth2 Server、Casbin RBAC、7 登录策略、多租户 3 模式、数据权限 5 类型 | Active |
| [`gradle-plugin.md`](./backend/gradle-plugin.md) | Gradle 插件：扩展配置、7 任务、自动行为、securityMode、代码生成 | Active |

### `guides/` — 思维指南

| 文件 | 内容概述 |
|------|---------|
| [`index.md`](./guides/index.md) | 思维指南总览：AFG 特定触发器和快速参考 |
| [`code-reuse-thinking-guide.md`](./guides/code-reuse-thinking-guide.md) | 代码复用：5 种 AFG 重复模式、ErrorCode/SPI 复用、搜索优先 |
| [`cross-layer-thinking-guide.md`](./guides/cross-layer-thinking-guide.md) | 跨层数据流：5 条 AFG 层边界链、自动注入机制、常见错误 |
| [`autoconfiguration-thinking-guide.md`](./guides/autoconfiguration-thinking-guide.md) | AutoConfiguration 思维：4 项写前检查、6 种常见错误、依赖链图 |

---

## 快速定位

- **想了解模块划分和依赖关系** → `backend/directory-structure.md`
- **想了解框架设计哲学和质量底线** → `backend/index.md`
- **想了解 DataManager 完整 API** → `backend/data-manager-api.md`
- **想了解实体基类选择和字段约定** → `backend/entity-design.md`
- **想了解数据库查询和方言支持** → `backend/database-guidelines.md`
- **想了解数据库迁移规范** → `backend/migration-guidelines.md`
- **想了解错误码体系和异常处理** → `backend/error-handling.md`
- **想了解 AutoConfiguration 编写规则** → `backend/autoconfiguration-guidelines.md`
- **想了解质量铁律和禁止模式** → `backend/quality-guidelines.md`
- **想了解日志级别和脱敏规则** → `backend/logging-guidelines.md`
- **想了解测试铁律和分层策略** → `backend/testing-guidelines.md`
- **想了解 AI 模块架构和注解** → `backend/ai-module.md`
- **想了解安全模块和多租户** → `backend/security-module.md`
- **想了解 Gradle 插件配置** → `backend/gradle-plugin.md`
- **写 AutoConfiguration 前的检查清单** → `guides/autoconfiguration-thinking-guide.md`
