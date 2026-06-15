# data模块功能与安全增强

## Goal

对 afg-framework 的 data 模块进行全面增强——安全性全面补齐（加密、脱敏、字段级访问控制、审计追踪、SQL注入防护）、功能性缺陷全面修复（25项），使其达到企业级生产可用标准。

## Requirements

### 一、安全性增强

#### 1.1 数据保护

**字段加密（盲索引完整方案）**
* AES-GCM 加密主列 + HMAC-SHA256 盲索引伴随列用于查询
* 提供生产级 `AesGcmFieldEncryptor` 替换 NoOp 默认
* `ConditionToSqlConverter` 重构以支持条件参数加密转换；查询走盲索引列（HMAC 值匹配）
* `FieldEncryptionKeyProvider` SPI：内置实现（配置文件密钥）+ 外部 Vault/KMS 适配
* 明文→密文迁移脚本 + 严格模式下无真实 FieldEncryptor 时启动拒绝

**数据脱敏（Jackson 序列化层）**
* `@SensitiveField` 注解（RUNTIME retention）+ `SensitiveType` 枚举（PHONE/ID_CARD/EMAIL/BANK_CARD/NAME/ADDRESS/CUSTOM）
* `MaskingStrategy` + `MaskingContext` SPI：支持按角色差异化脱敏
* Jackson `BeanSerializerModifier` 实现：实体持有真实值，脱敏在输出时动态应用
* Export 层集成 MaskingContext，Excel/PDF 导出时脱敏同样生效
* 自定义脱敏策略 SPI（可扩展）

**字段级访问控制（SQL 列排除 + Jackson 安全网）**
* 主强制：未授权列从 SQL SELECT 中排除（不查出来），与 DataScope SQL 重写模式一致
* 纵深防御：Jackson `BeanSerializerModifier` 二次检查
* 写保护：未授权列从 UPDATE SET 中排除
* Casbin 编码：`obj = sys_user.salary`，复用 `AfgEnforcer.enforce()` 接口，无需改 schema
* 字段权限与 RBAC 打通接口

#### 1.2 操作安全

**全表操作保护**
* updateAll/deleteByCondition 空条件时阻止执行
* 可配置行为：抛异常 / 强制加 LIMIT / 仅日志警告
* DataScope CUSTOM 参数化改造

**审计追踪持久化**
* 事件驱动：`@EventListener` 监听 `EntityChangedEvent<?>`
* 通过 `EntityMetadataCache` 计算字段级 diff
* `data_change_log` + `data_change_field` 双表设计
* 异步写入默认，合规关键实体可配同步模式
* 审计系统关联：实体级审计关联方法级 `@Audited` 的 `auditId`，形成"操作→数据变更"链路
* 审计日志归档：可插拔归档策略（按时间分区/按量归档/冷存储）
* 审计存储可插拔（JDBC / 文件 / 消息队列）

**SQL注入防护加强**
* RawSqlSecurityGuard 增强默认模式
* fromSubquery bug 修复：子查询不被 quoteIdentifier 包裹

### 二、功能性增强（25项）

#### 2.1 性能与批量操作
* **脏检查** — update() 仅更新变更列，宽表性能优化
* **MySQL 批量插入优化** — rewriteBatchedStatements 或 multi-value INSERT 方案
* **批量更新 JDBC batch** — EntityUpdateHandler.updateAll() 使用 JDBC batch 而非循环
* **批量大小可配置** — saveAll/insertAll batch size 可配置

#### 2.2 SQL 构建器增强
* **UNION / UNION ALL** — SqlQueryBuilder 支持
* **子查询 IN** — `WHERE col IN (SELECT ...)` 支持
* **CASE WHEN 表达式** — SQL 构建器支持
* **列别名** — `SELECT COUNT(*) AS total` 支持
* **算术/字符串表达式** — `salary * 12 AS annual_salary` 支持
* **Schema 限定表名** — `from("schema.table")` 产生 `` `schema`.`table` ``
* **CTE 递归体 UNION ALL** — 递归 CTE 可表达 anchor + recursive member
* **Upsert** — ON DUPLICATE KEY / ON CONFLICT / MERGE 支持
* **INSERT ... SELECT** — SQL 构建器支持

#### 2.3 锁与并发
* **悲观读锁** — FOR SHARE 支持
* **锁超时配置** — NOWAIT / WAIT / SKIP_LOCKED 选项

#### 2.4 实体与生命周期
* **生命周期回调补齐** — afterCreate / afterUpdate / afterDelete
* **软删除列名可配置** — 通过元数据而非硬编码 `deleted`/`deleted_at`
* **ID 列名通过元数据** — WHERE 子句中使用 EntityMetadata 获取 ID 列名

#### 2.5 架构增强
* **读写分离路由** — @DataSource 注解扩展，内置读写分离路由策略
* **复合主键** — BaseEntity ID 类型参数化或复合 ID 支持
* **嵌入式/值对象** — @Embedded 等价物
* **多表实体映射** — @SecondaryTable 等价物
* **命名查询** — @NamedQuery 等价物
* **N+1 查询防护** — 自动批量获取或 join fetch 策略
* **treeQuery 编译时防护** — 非树实体编译时而非运行时失败

## Acceptance Criteria

### 安全性
* [ ] @EncryptedField 标注的字段，无真实 FieldEncryptor 时应用启动拒绝（严格模式）
* [ ] AesGcmFieldEncryptor 正确加密/解密，盲索引列支持等值查询
* [ ] @SensitiveField 标注的字段，API 响应和导出中正确脱敏
* [ ] 不同角色看到不同脱敏级别
* [ ] 字段级访问控制：无权限列不出现在 SQL SELECT 中
* [ ] 字段级写保护：无权限列不出现在 UPDATE SET 中
* [ ] 全表 updateAll/deleteByCondition 空条件时被阻止
* [ ] 审计日志表自动创建，字段级 diff 正确记录
* [ ] 实体级审计与方法级审计通过 auditId 关联
* [ ] fromSubquery 不再被 quoteIdentifier 包裹

### 功能性
* [ ] update() 仅更新变更列（脏检查）
* [ ] MySQL 批量插入使用 multi-value INSERT
* [ ] 批量更新使用 JDBC batch
* [ ] SQL 构建器支持 UNION/UNION ALL
* [ ] SQL 构建器支持子查询 IN
* [ ] SQL 构建器支持 CASE WHEN
* [ ] SQL 构建器支持列别名和表达式
* [ ] Schema 限定表名正确处理
* [ ] CTE 递归体支持 UNION ALL
* [ ] 支持 Upsert 操作
* [ ] 支持 INSERT ... SELECT
* [ ] 支持 FOR SHARE 悲观读锁
* [ ] 支持锁超时配置（NOWAIT/WAIT/SKIP_LOCKED）
* [ ] 生命周期回调补齐 afterCreate/afterUpdate/afterDelete
* [ ] 软删除列名从元数据获取
* [ ] ID 列名从元数据获取
* [ ] 读写分离路由工作
* [ ] 复合主键支持
* [ ] @Embedded 值对象支持
* [ ] @SecondaryTable 多表映射支持
* [ ] 命名查询支持
* [ ] N+1 查询自动防护
* [ ] treeQuery 编译时防护
* [ ] 批量大小可配置

## Definition of Done

* 所有 Acceptance Criteria 通过（集成测试为主，单元测试为辅）
* 禁止 Mockito mock — 使用真实 Bean + Testcontainers
* Lint / typecheck / CI green
* CLAUDE.md 更新（新注解、新 SPI、新配置）
* Liquibase 迁移脚本（审计日志表、盲索引列）
* NoOp 安全类必须有生产级替代或启动时强制警告
* 向后兼容：现有代码无需修改即可升级

## Out of Scope

* 不实现完整的 JPA Criteria API 元模型
* 不实现 JPA EntityManager / Session 语义
* 不实现 first-level cache（identity map）
* 不实现 second-level cache（已有 EntityCacheManager）
* 不实现 query result cache
* 不实现数据库端加密函数（pgcrypto 等）
* 不实现 LIKE/范围查询的加密字段搜索（客户端加密下根本不可行）
* 不重写 Casbin 模型（仅利用现有 4 元组编码字段级 ACL）

## Technical Approach

### 决策记录（ADR-lite）

**1. 数据脱敏**
* Context：需要在输出层脱敏，业务逻辑保留真实值，按角色差异化
* Decision：Jackson 序列化层脱敏（`@SensitiveField` + `BeanSerializerModifier`）
* Consequences：仅 Jackson 序列化路径脱敏，直接 entity.getField() 不脱敏；导出需单独集成

**2. 字段加密**
* Context：需要可搜索的加密方案，NoOp 默认不可接受
* Decision：盲索引完整方案（AES-GCM + HMAC-SHA256 + KeyProvider SPI + 迁移 + 严格模式）
* Consequences：需要 Bouncy Castle 依赖；LIKE/范围查询不可搜索；每加密字段需额外一列盲索引

**3. 审计追踪**
* Context：已有 EntityChangedEvent 但无持久化；两套审计系统需关联
* Decision：事件驱动双表设计 + 方法级审计 auditId 关联
* Consequences：异步写入有应用崩溃丢数据风险（可用消息队列缓解）

**4. 字段级访问控制**
* Context：需要列级读写控制，与 Casbin RBAC 集成
* Decision：SQL SELECT 列排除 + Jackson 安全网
* Consequences：需按权限 profile 缓存 SQL；与 DataScope 模式一致

**5. 审计系统关系**
* Context：方法级 `@Audited` 与实体级 `EntityChangedEvent` 独立无关联
* Decision：关联绑定（实体级审计关联方法级 auditId）
* Consequences：形成完整审计链路，但需在 AuditLogAspect 中传播 auditId 到事件

## Technical Notes

### 关键文件
* data-core: `data-core/src/main/java/io/github/afgprojects/framework/data/core/`
* data-sql: `data-impl/data-sql/src/main/java/io/github/afgprojects/framework/data/sql/`
* data-jdbc: `data-impl/data-jdbc/src/main/java/io/github/afgprojects/framework/data/jdbc/`
* JdbcDataManager: `data-impl/data-jdbc/src/main/java/.../JdbcDataManager.java`
* SqlBuilder: `data-impl/data-jdbc/src/main/java/.../SqlBuilder.java`
* ConditionToSqlConverter: `data-impl/data-sql/src/main/java/.../converter/ConditionToSqlConverter.java`
* RawSqlSecurityGuard: `data-impl/data-jdbc/src/main/java/.../metrics/RawSqlSecurityGuard.java`

### 已有安全机制
* SqlIdentifierValidator — 白名单正则验证列名/表名/别名
* RawSqlSecurityGuard — DDL 防护（MODERATE/STRICT/PERMISSIVE 三模式）
* ConditionToSqlConverter — 全参数化转换
* FieldEncryptor SPI — 字段加密接口（默认 NoOp）
* DataScope — 行级数据权限
* TenantContext — 多租户隔离

### Research References
* [`research/data-masking-patterns.md`](research/data-masking-patterns.md) — Jackson 序列化层脱敏为推荐方案
* [`research/field-encryption-patterns.md`](research/field-encryption-patterns.md) — 盲索引列为推荐方案
* [`research/audit-trail-patterns.md`](research/audit-trail-patterns.md) — 事件驱动双表设计为推荐方案
* [`research/field-level-access-control.md`](research/field-level-access-control.md) — SQL 列排除 + Jackson 安全网为推荐方案
