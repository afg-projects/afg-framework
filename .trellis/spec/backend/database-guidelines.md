# 数据库与数据访问规范

> PRD 来源：§5.4 DataManager 数据访问模块、§6.5 实体设计规范、§9.1 数据库功能支持矩阵
> CLAUDE.md 来源：DataManager + APT 章节、数据库迁移章节

---

## 概述

AFG 框架使用 **DataManager** 作为统一数据操作门面，**不使用 JPA Repository**。所有数据 CRUD 操作通过 `DataManager` 完成，配合 APT 编译时元数据生成实现零反射，配合 Lambda 类型安全条件构建器实现类型安全查询，自动注入多租户/数据权限/软删除过滤条件。

### 核心原则

1. **DataManager 是唯一数据入口** — 禁止定义 JPA Repository，禁止引入 `spring-boot-starter-data-jpa`
2. **Lambda 类型安全优先** — 条件查询使用 `builder(EntityClass)` Lambda 方式，不推荐字符串方式
3. **自动注入优先** — 软删除过滤、多租户条件、数据权限条件由框架自动注入，无需手动处理
4. **APT 零反射** — 实体元数据在编译时生成，运行时通过 `EntityMetadataCache` 加载

---

## DataManager API

### 快捷方法

| 操作类别 | 方法 | 说明 |
|----------|------|------|
| **快捷查询** | `findById(Class, id)` | 按 ID 查找，返回 `Optional` |
| | `findAll(Class)` | 查询全部 |
| | `findOne(Class, Condition)` | 条件查单条，返回 `Optional` |
| | `findList(Class, Condition)` | 条件查列表 |
| | `existsById(Class, id)` | ID 是否存在 |
| | `count(Class)` | 总数 |
| **快捷写入** | `save(Class, entity)` | 新增或更新（id 为 null 则 insert，否则 update） |
| | `saveAll(Class, entities)` | 批量保存 |
| **快捷删除** | `deleteById(Class, id)` | 删除（软删除实体自动执行软删除） |
| | `deleteByCondition(Class, Condition)` | 条件删除 |
| **快捷恢复** | `restoreById(Class, id)` | 恢复软删除记录 |
| **租户** | `tenantScope(tenantId)` | 创建租户作用域（try-with-resources） |
| **事务** | `executeInTransaction(Supplier)` | 编程式事务 |
| **数据权限** | `findListWithDataScope(Class, Condition)` | 带数据权限的查询 |
| **字段查询** | `findOneByField()` / `findAllByField()` / `existsByField()` / `countByField()` | Lambda 字段查询 |

### EntityProxy 链式操作

通过 `dataManager.entity(EntityClass)` 获取 `EntityProxy<T>`，支持链式调用：

```java
// 查询链
dataManager.entity(User.class)        // → EntityProxy<User>
    .query()                          // → EntityQuery<User>
    .where(condition)                 // 过滤条件
    .orderByAsc(User::getCreatedAt)   // 排序
    .page(PageRequest.of(1, 20))      // 分页 → PageData<User>
    .list();                          // → List<User>

// 写入链
EntityProxy<User> proxy = dataManager.entity(User.class);
proxy.save(user);                     // 新增或更新
proxy.insert(user);                   // 强制 INSERT
proxy.update(user);                   // 强制 UPDATE
proxy.deleteById(id);                 // 删除（软删除实体执行软删除）
proxy.restoreById(id);                // 恢复软删除
proxy.updateAll(condition, updates);   // 条件更新 → 影响行数
proxy.deleteByCondition(condition);    // 条件删除 → 影响行数
```

### EntityProxy 完整方法

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
| 悲观锁 | `withPessimisticLock()` | `SELECT ... FOR UPDATE` |

---

## 条件查询构建器

### Lambda 方式（推荐）

```java
import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

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
    .jsonContained(User::getMetadata, someValue)
    .jsonPath(User::getMetadata, "$.level", 3)
    .build();
```

### 字符串方式（不推荐）

```java
// 易出错，无编译时检查，仅在不支持 Lambda 的场景使用
Condition condition = builder()
    .eq("status", 1)
    .build();
```

### 完整操作符列表

| 操作符 | SQL 等价 | 说明 |
|--------|---------|------|
| `eq` | `= ?` | 等于 |
| `ne` | `!= ?` / `<> ?` | 不等于 |
| `like` | `LIKE ?` | 模糊匹配（全匹配） |
| `likeStartsWith` | `LIKE ?%` | 前缀匹配 |
| `likeEndsWith` | `LIKE %?` | 后缀匹配 |
| `notLike` | `NOT LIKE ?` | 排除模糊匹配 |
| `in` | `IN (?)` | 包含于 |
| `notIn` | `NOT IN (?)` | 不包含于 |
| `isNull` | `IS NULL` | 为空 |
| `isNotNull` | `IS NOT NULL` | 不为空 |
| `gt` | `> ?` | 大于 |
| `ge` | `>= ?` | 大于等于 |
| `lt` | `< ?` | 小于 |
| `le` | `<= ?` | 小于等于 |
| `between` | `BETWEEN ? AND ?` | 区间 |
| `notBetween` | `NOT BETWEEN ? AND ?` | 排除区间 |
| `jsonContains` | `JSON_CONTAINS(?, ?)` | JSON 包含（MySQL/PostgreSQL） |
| `jsonContained` | `JSON_CONTAINS(?, ?)` 反向 | JSON 被包含 |
| `jsonPath` | `JSON_EXTRACT(?, ?)` | JSON 路径查询 |

### 空值语义

框架对空值有明确的智能处理规则，避免手动判空：

| 写法 | 生成的 SQL | 说明 |
|------|-----------|------|
| `eq(field, null)` | `field IS NULL` | null 自动转为 IS NULL |
| `ne(field, null)` | `field IS NOT NULL` | null 自动转为 IS NOT NULL |
| `in(field, emptyList)` | `1=0` | 空集合不匹配任何记录 |
| `eqIfPresent(field, null)` | **跳过该条件** | null 值不参与查询 |

### 动态条件

用于搜索接口等场景，参数可能为 null 时自动跳过：

```java
Condition condition = builder(User.class)
    .eqIfPresent(User::getStatus, status)         // status != null 时添加
    .likeIfPresent(User::getUsername, name)        // name != null 时添加
    .geIfPresent(User::getCreatedAt, startTime)    // startTime != null 时添加
    .inIfPresent(User::getDeptId, deptIds)         // deptIds 非空时添加
    .betweenIfPresent(User::getAge, minAge, maxAge) // 两者都非 null 时添加
    .build();
```

动态条件操作符：`eqIfPresent`、`likeIfPresent`、`inIfPresent`、`betweenIfPresent` 等，null 值自动跳过。

### 组合条件

```java
// AND 组合
Condition condition = allOf(cond1, cond2);

// OR 组合
Condition condition = anyOf(cond1, cond2);

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

---

## 分页

### PageRequest 创建

```java
PageRequest.of(1, 20)                          // 第 1 页，每页 20 条
PageRequest.of(1, 20, Sort.by("createdAt"))    // 带排序
PageRequest.defaultPage()                       // 第 1 页，每页 10 条
```

### 分页查询

```java
PageData<User> page = dataManager.entity(User.class)
    .query()
    .where(condition)
    .page(PageRequest.of(1, 20));

// PageData 字段
page.records();    // 当前页数据列表
page.total();      // 总记录数
page.pages();      // 总页数
page.hasNext();    // 是否有下一页
page.hasPrevious(); // 是否有上一页
```

### PageData 构建

```java
PageData.of(records, total, page, size)  // → PageData(records, total, page, size, pages, hasNext, hasPrevious)
```

---

## 聚合查询

```java
List<AggregateResult> results = dataManager.entity(User.class)
    .query()
    .aggregate()
    .groupBy(User::getDeptId)
    .count("id", "userCount")
    .countDistinct("status", "statusTypes")
    .sum("salary", "totalSalary")
    .avg("age", "avgAge")
    .max("age", "maxAge")
    .min("age", "minAge")
    .having(builder().gt("userCount", 5).build())
    .list();

// 读取结果
results.get(0).getLong("userCount");
results.get(0).getDouble("avgAge");
```

**聚合函数**：`COUNT`、`COUNT_DISTINCT`、`SUM`、`AVG`、`MAX`、`MIN`

---

## DTO 投影查询

```java
List<OrderSummaryDTO> summaries = dataManager.entity(Order.class)
    .project()
    .select(Order::getId, Order::getOrderNo, Order::getTotalAmount)
    .where(builder(Order.class).eq(Order::getStatus, OrderStatus.ACTIVE).build())
    .list();
```

---

## 关联关系

### 关联注解

| 注解 | 默认 Fetch | 特有属性 |
|------|-----------|----------|
| `@ManyToOne` | EAGER | `foreignKey`、`optional` |
| `@OneToMany` | LAZY | `mappedBy`、`foreignKey`、`orphanRemoval` |
| `@OneToOne` | LAZY | `mappedBy`、`foreignKey` |
| `@ManyToMany` | LAZY | `mappedBy`、`joinTable`、`joinColumn`、`inverseJoinColumn` |

所有注解共有属性：`targetEntity`、`cascade`（PERSIST/MERGE/REMOVE/REFRESH/DETACH/ALL）、`fetch`

### 关联加载方式

```java
// 显式加载
dataManager.entity(User.class).fetch(user, "orders");

// 预加载（查询时指定）
dataManager.entity(User.class)
    .query().withAssociation("orders").list();

// 批量加载
dataManager.entity(User.class).fetchAll(users, "orders");
```

### 关联设计规则

- `@ManyToOne` 默认 EAGER，注意 N+1 问题，可改为 LAZY + `withAssociation()` 预加载
- `@OneToMany` 默认 LAZY，使用 `withAssociation()` 显式加载
- **避免 `@ManyToMany`** — 优先用中间实体 + 两个 `@OneToMany`
- `orphanRemoval = true` 谨慎使用（级联删除不可恢复）

---

## 自动注入机制

### 软删除自动过滤

继承 `SoftDeleteEntity` 或 `TimestampSoftDeleteEntity` 的实体，查询时自动添加 `WHERE deleted = false` 或 `WHERE deleted_at IS NULL` 条件，无需手动处理。

```java
// 自动过滤软删除记录
dataManager.findAll(User.class);  // → WHERE deleted = false

// 包含已软删除记录
dataManager.entity(User.class).includeDeleted().findAll(condition);

// 软删除（自动检测实体类型）
dataManager.deleteById(User.class, id);  // → UPDATE SET deleted = true

// 恢复软删除
dataManager.restoreById(User.class, id);  // → UPDATE SET deleted = false
```

### 多租户自动注入

继承 `TenantEntity` 的实体，查询时自动注入 `WHERE tenant_id = ?` 条件，写入时自动填充 `tenantId`。

```java
// 自动注入租户条件
dataManager.findAll(Order.class);  // → WHERE tenant_id = 'current-tenant'

// 显式指定租户作用域
try (var scope = dataManager.tenantScope("tenant-1")) {
    dataManager.findAll(Order.class);  // → WHERE tenant_id = 'tenant-1'
}
```

### 数据权限自动注入

```java
dataManager.entity(Order.class).query().withDataScope().list();
// ALL → 无额外条件
// SELF → WHERE create_by = 'current-user'
// DEPT → WHERE dept_id = 'current-dept'
// DEPT_AND_CHILD → WHERE dept_id IN (current-dept + child-depts)
// CUSTOM → WHERE custom_condition
```

---

## 事务管理

### 声明式事务

```java
@PostMapping
@Transactional
public User create(@RequestBody User user) {
    return dataManager.save(User.class, user);
}
```

### 编程式事务

```java
User result = dataManager.executeInTransaction(() -> {
    User user = dataManager.save(User.class, createUser());
    dataManager.save(Log.class, createLog(user.getId()));
    return user;
});
```

### 事务注意事项

- `@Transactional` 与 `dataManager.executeInTransaction()` 可以共存，框架正确处理事务传播
- API 端到端测试中 `@Transactional` 无效（HTTP 不同线程），应使用 `RestClient` + 真实数据
- 并发/锁测试不应使用 `@Transactional`，需要多事务场景

---

## 实体缓存

### 配置

```yaml
afg:
  data:
    entity-cache:
      enabled: true
      max-size: 1000
      ttl-seconds: 300
```

### 缓存策略

- **默认**：Caffeine 本地缓存（引入 `afg-framework-data-jdbc` 即可用）
- **升级**：引入 `afg-framework-afg-redis` 后自动升级为 Redis 分布式缓存，替换本地实现
- 缓存淘汰策略：LRU（max-size 限制）
- 缓存一致性：实体保存/更新/删除后自动清除缓存

---

## SQL 监控

### 配置

```yaml
afg:
  data:
    sql-metrics:
      enabled: true
      slow-query-threshold-ms: 500
```

- `slow-query-threshold-ms`：慢查询阈值（毫秒），超过此值的 SQL 将被记录
- 默认阈值 500ms

---

## 数据库方言

### 支持的 10 种数据库方言

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

### 功能支持矩阵

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

### 方言扩展 SPI

添加新数据库方言的步骤：
1. 实现 `DatabaseDialect` 接口
2. 注册到 `DialectRegistry`
3. 添加 Liquibase 迁移脚本兼容性测试

---

## 类型处理器

框架内置 16+ 类型处理器，自动处理 Java 类型与数据库类型的转换：

| 类型处理器 | 说明 |
|-----------|------|
| `BigDecimalTypeHandler` | BigDecimal 数值 |
| `BlobTypeHandler` | BLOB 二进制 |
| `BooleanNumberTypeHandler` | Boolean → 数据库数字（0/1） |
| `DateTimeTypeHandler` | 日期时间 |
| `EnumTypeHandler` | 枚举 → 字符串（避免 ordinal） |
| `InstantTypeHandler` | Instant 时间戳 |
| `JsonTypeHandler` | JSON 序列化/反序列化 |
| `LocalDateTypeHandler` | LocalDate |
| `LocalTimeTypeHandler` | LocalTime |
| `NumberTypeHandler` | 数值类型 |
| `OffsetDateTimeTypeHandler` | OffsetDateTime |
| `StringTypeHandler` | 字符串 |
| `UUIDTypeHandler` | UUID |
| `YearMonthTypeHandler` | YearMonth |
| `YearTypeHandler` | Year |
| `ZonedDateTimeTypeHandler` | ZonedDateTime |
| `EncryptedTypeHandler` | 字段级加密 |

---

## SQL 构建器（data-sql 模块）

| 构建器 | 说明 |
|--------|------|
| `SqlQueryBuilder` | SELECT 查询构建（CTE / JOIN / 子查询 / 窗口函数） |
| `SqlInsertBuilder` | INSERT 构建（批量 / ON DUPLICATE KEY UPDATE） |
| `SqlUpdateBuilder` | UPDATE 构建 |
| `SqlDeleteBuilder` | DELETE 构建 |
| `WindowFunctionBuilder` | 窗口函数构建（ROW_NUMBER / RANK / SUM OVER 等） |
| `SqlRewriteContext` | SQL 重写上下文（租户隔离 / 数据权限注入） |

---

## 分布式 ID 生成

### 配置

```yaml
afg:
  data:
    id-generator:
      type: SNOWFLAKE                    # SNOWFLAKE / SEGMENT / UUID
      snowflake:
        worker-id: 1
        datacenter-id: 1
```

### 使用

```java
@GeneratedValue(generator = IdGenerator.SNOWFLAKE)
private Long id;
```

三种策略：Snowflake（雪花算法）、Segment（号段模式）、UUID

---

## 字段级加密

### 配置

```yaml
afg:
  data:
    field-encryption:
      enabled: true
      default-algorithm: AES
```

### 使用

```java
@EncryptedField(algorithm = AES, keyRef = "user-key")
private String idCard;  // PII 数据加密存储
```

---

## 禁止事项

| 禁止 | 替代方案 |
|------|---------|
| `spring-boot-starter-data-jpa` | `afg-framework-data-jdbc` |
| JPA `@Entity` | `@AfEntity` + `@Table` + `@Column` |
| JPA Repository | `DataManager` |
| Hibernate EntityManagerFactory | `JdbcDataManager` |
| JPA Specification | `Conditions.builder()` |
| JPA `@Cacheable` | AFG EntityCache + Caffeine/Redis |
| H2 内存数据库（测试） | Testcontainers 真实数据库 |
| Mockito mock DataManager | 注入真实 DataManager + Testcontainers |

引入 `spring-boot-starter-data-jpa` 会导致 Hibernate EntityManagerFactory 被自动配置，与 DataManager 产生冲突。框架项目中禁止引入任何 JPA 依赖。

---

## 常见错误

### 引入 JPA 导致框架冲突

**症状**：应用启动报 Hibernate EntityManagerFactory 相关错误，或 DataManager 查询结果与预期不符

**原因**：AFG 框架使用 DataManager（基于 JDBC），引入 `spring-boot-starter-data-jpa` 会导致 Hibernate 被自动配置

**解决**：删除 `spring-boot-starter-data-jpa` 依赖，使用 `afg-framework-data-jdbc` 替代

**预防**：框架项目中禁止引入任何 JPA 依赖

### 软删除实体唯一约束冲突

**症状**：软删除后重新创建同名记录时唯一约束报错

**原因**：唯一约束未包含 `deleted` 字段，已删除记录与新记录冲突

**解决**：唯一约束必须包含 `deleted` 字段，例如 `uk_user_username_deleted` 包含 `(username, deleted)`

**预防**：软删除实体的唯一约束必须包含 `deleted` 字段

### 实体缺少 @AfEntity 导致 APT 不生成元数据

**症状**：DataManager 操作实体时报错，提示找不到实体元数据

**原因**：实体类只有 `@Table`/`@Column` 但缺少 `@AfEntity` 注解

**解决**：在所有实体类上添加 `@AfEntity` 注解

**预防**：实体类必须同时有 `@AfEntity` + `@Table` + `@Column`，三者配合使用

### @ManyToOne N+1 查询问题

**症状**：列表查询关联实体时产生大量 SQL

**原因**：`@ManyToOne` 默认 EAGER 加载，每个关联实体单独查询

**解决**：将 `@ManyToOne` 改为 `fetch = LAZY`，使用 `withAssociation()` 预加载

**预防**：列表查询场景优先使用 `withAssociation()` 预加载关联
