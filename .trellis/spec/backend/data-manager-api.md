# DataManager API 规范

> 来源：PRD 5.4、CLAUDE.md DataManager 章节、实际代码接口

---

## 核心定位

DataManager 是 AFG 框架的**统一数据操作门面**，采用"一个 DataManager 操作所有实体"的设计，替代传统 JPA 的"每个实体一个 Repository"模式。配合 APT 编译时元数据生成，实现运行时零反射开销、类型安全条件查询，并自动注入多租户/数据权限/软删除过滤条件。

---

## 与传统方案对比

| 维度 | JPA Repository | DataManager | MyBatis-Plus |
|------|---------------|-------------|-------------|
| **设计理念** | 每个实体一个 Repository | 一个 DataManager 操作所有实体 | 每个实体一个 BaseMapper |
| **元数据加载** | 运行时反射 | APT 编译时生成，运行时零反射 | 运行时反射 |
| **查询方式** | 方法名派生 / @Query / Specification | Lambda 条件构建器 + 链式查询 | LambdaQueryWrapper |
| **多租户** | Hibernate Filter（需手动配置） | 自动注入租户条件（零配置） | TenantLineInnerInterceptor |
| **数据权限** | 无 | 自动注入数据权限条件（零配置） | 需手动实现 |
| **软删除** | @Where 注解（需手动） | 自动过滤（零配置） | @TableLogic |
| **聚合查询** | 无内置 | 内置 GROUP BY + HAVING + 聚合函数 | 无内置 |
| **关联加载** | N+1 问题 / FETCH JOIN | 预加载 / 批量加载 / 延迟加载 | 无内置 |
| **DTO 投影** | Interface Projection / Class Projection | ProjectedQuery + Lambda 字段选择 | 无内置 |
| **缓存** | 二级缓存（复杂） | EntityCache + Caffeine/Redis（简单） | 无内置 |
| **依赖** | Hibernate EntityManager | 纯 JDBC | MyBatis + PageHelper |

**迁移对照：**

| JPA Repository | AFG DataManager |
|---------------|----------------|
| `UserRepository extends JpaRepository` | `DataManager` |
| `repository.findById(id)` | `dataManager.findById(User.class, id)` |
| `repository.save(user)` | `dataManager.save(User.class, user)` |
| `@Query("WHERE status = ?1")` | `Conditions.builder(User.class).eq(User::getStatus, 1)` |

| MyBatis-Plus | AFG DataManager |
|-------------|----------------|
| `BaseMapper<User>` | `DataManager` |
| `mapper.selectById(id)` | `dataManager.findById(User.class, id)` |
| `LambdaQueryWrapper<User>` | `Conditions.builder(User.class)` |
| `Page<User>` | `PageData<User>` |
| `@TableLogic` | 继承 `SoftDeleteEntity` |
| `TenantLineInnerInterceptor` | 自动注入（零配置） |

---

## DataManager 完整 API

包路径：`io.github.afgprojects.framework.data.core.DataManager`

### 实体操作

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `entity(Class<T>)` | `EntityProxy<T>` | 获取实体操作代理 |
| `getEntityMetadata(Class<T>)` | `EntityMetadata<T>` | 获取实体元数据 |

### SQL 构建器

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `query()` | `SqlQueryBuilder` | SELECT 查询构建 |
| `update()` | `SqlUpdateBuilder` | UPDATE 构建 |
| `insert()` | `SqlInsertBuilder` | INSERT 构建 |
| `delete()` | `SqlDeleteBuilder` | DELETE 构建 |

### 事务管理

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `executeInTransaction(Runnable)` | `void` | 编程式事务（无返回值） |
| `executeInTransaction(Supplier<T>)` | `T` | 编程式事务（有返回值） |
| `executeInReadOnly(Supplier<T>)` | `T` | 只读事务 |

### 租户管理

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `tenantScope(String tenantId)` | `TenantScope` | 创建租户作用域（支持 try-with-resources） |
| `getTenantContextHolder()` | `TenantContextHolder` | 获取租户上下文持有器 |

### 数据库信息

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getDatabaseType()` | `DatabaseType` | 获取数据库类型 |
| `getTransactionManager()` | `Object` (@Nullable) | 获取事务管理器 |
| `getTransactionAdapter()` | `TransactionAdapter` (@Nullable) | 获取事务适配器 |
| `setTransactionAdapter(TransactionAdapter)` | `void` | 设置事务适配器 |

### 原始 SQL 操作

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `executeUpdate(String sql, List<Object> params)` | `int` | 执行更新 SQL |
| `executeUpdate(String sql, Map<String, Object> params)` | `int` | 执行更新 SQL（命名参数） |
| `queryForList(String sql, List<Object> params, ResultMapper<T>)` | `List<T>` | 查询列表 |
| `queryForObject(String sql, List<Object> params, ResultMapper<T>)` | `T` (@Nullable) | 查询单条 |
| `queryForOptional(String sql, List<Object> params, ResultMapper<T>)` | `Optional<T>` | 查询单条（Optional） |
| `queryForCount(String sql, List<Object> params)` | `long` | 查询计数 |

### 快捷查询方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `findById(Class<T>, Object id)` | `Optional<T>` | 按 ID 查找 |
| `findOne(Class<T>, Condition)` | `Optional<T>` | 条件查单条 |
| `findList(Class<T>, Condition)` | `List<T>` | 条件查列表 |
| `findAll(Class<T>)` | `List<T>` | 查询全部 |
| `findAllById(Class<T>, Iterable<?>)` | `List<T>` | 按 ID 列表批量查询 |
| `existsById(Class<T>, Object id)` | `boolean` | ID 是否存在 |
| `existsByCondition(Class<T>, Condition)` | `boolean` | 条件是否存在 |
| `count(Class<T>)` | `long` | 总数 |
| `countByCondition(Class<T>, Condition)` | `long` | 条件计数 |
| `findOneByField(Class<T>, SFunction, Object)` | `Optional<T>` | Lambda 字段查单条 |
| `findAllByField(Class<T>, SFunction, Object)` | `List<T>` | Lambda 字段查列表 |
| `existsByField(Class<T>, SFunction, Object)` | `boolean` | Lambda 字段判断存在 |
| `countByField(Class<T>, SFunction, Object)` | `long` | Lambda 字段计数 |

### 快捷写入方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `save(Class<T>, T entity)` | `T` | 新增或更新（id 为 null 则 INSERT，否则 UPDATE） |
| `saveAll(Class<T>, Iterable<? extends T>)` | `List<T>` | 批量保存 |
| `insertAll(Class<T>, Iterable<T>)` | `List<T>` | 批量插入 |
| `update(Class<T>, T entity)` | `T` | 强制更新 |

### 快捷删除方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `deleteById(Class<T>, Object id)` | `void` | 删除（软删除实体执行软删除） |
| `deleteAllById(Class<T>, Iterable<?>)` | `void` | 批量删除 |
| `deleteByCondition(Class<T>, Condition)` | `long` | 条件删除，返回影响行数 |

### 快捷恢复方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `restoreById(Class<T>, Object id)` | `void` | 恢复软删除记录 |

### 数据权限方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `findListWithDataScope(Class<T>, Condition)` | `List<T>` | 带数据权限的查询 |
| `findListWithDataScope(Class<T>, String deptField, Condition)` | `List<T>` | 指定部门字段的带数据权限查询 |

---

## EntityProxy 链式 API

包路径：`io.github.afgprojects.framework.data.core.EntityProxy<T>`

EntityProxy 继承 `EntityReader<T>` 和 `EntityWriter<T>`，提供完整的实体操作能力。

### 继承自 EntityReader（查询操作）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `findById(Object id)` | `Optional<T>` | 按 ID 查找 |
| `findAllById(Iterable<?>)` | `List<T>` | 按 ID 列表查询 |
| `findAll()` | `List<T>` | 查询全部 |
| `count()` | `long` | 总数 |
| `existsById(Object id)` | `boolean` | ID 是否存在 |
| `query()` | `EntityQuery<T>` | 链式查询入口 |

### 继承自 EntityWriter（写入操作）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `save(T entity)` | `T` | 新增或更新 |
| `saveAll(Iterable<? extends T>)` | `List<T>` | 批量保存 |
| `insert(T entity)` | `T` | 强制 INSERT |
| `insertAll(Iterable<T>)` | `List<T>` | 批量插入 |
| `update(T entity)` | `T` | 强制 UPDATE |
| `updateAll(Iterable<T>)` | `List<T>` | 批量更新 |
| `deleteById(Object id)` | `void` | 删除（软删除实体自动软删除） |
| `delete(T entity)` | `void` | 删除指定实体 |
| `deleteAllById(Iterable<?>)` | `void` | 批量删除 |
| `deleteAll(Iterable<? extends T>)` | `void` | 批量删除指定实体 |
| `updateAll(Condition, Map<String, Object>)` | `long` | 条件更新，返回影响行数 |
| `deleteByCondition(Condition)` | `long` | 条件删除，返回影响行数 |
| `restoreById(Object id)` | `void` | 恢复软删除记录 |
| `restoreAllById(Iterable<?>)` | `void` | 批量恢复软删除 |

### EntityProxy 声明的方法（关联 + 作用域）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `fetch(T entity, String name)` | `R` | 显式加载关联关系 |
| `fetchAll(Iterable<T>, String name)` | `void` | 批量加载关联关系 |
| `withDataScope(DataScope)` | `EntityQuery<T>` | 启用数据权限 |
| `withDataScopes(DataScope...)` | `EntityQuery<T>` | 启用多个数据权限 |
| `withTenant(String tenantId)` | `EntityQuery<T>` | 指定租户作用域 |
| `withDataSource(String name)` | `EntityQuery<T>` | 切换数据源 |
| `withReadOnly()` | `EntityQuery<T>` | 只读事务 |
| `includeDeleted()` | `EntityQuery<T>` | 包含已软删除记录 |
| `withAssociation(String name)` | `EntityQuery<T>` | 预加载关联 |
| `withAssociations(String...)` | `EntityQuery<T>` | 预加载多个关联 |
| `clearAssociations()` | `EntityQuery<T>` | 清除关联预加载设置 |

### EntityProxy 便捷查询方法（default）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `findAll(Condition)` | `List<T>` | 条件查询列表 |
| `findAll(Condition, PageRequest)` | `PageData<T>` | 条件分页查询 |
| `count(Condition)` | `long` | 条件计数 |
| `exists(Condition)` | `boolean` | 条件是否存在 |
| `findOne(Condition)` | `Optional<T>` | 条件查单条 |
| `findFirst(Condition)` | `Optional<T>` | 条件查第一条 |

---

## EntityQuery 链式查询 API

包路径：`io.github.afgprojects.framework.data.core.EntityQuery<T>`

### 条件与排序

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `where(Condition)` | `EntityQuery<T>` | 设置查询条件 |
| `and(Condition)` | `EntityQuery<T>` | AND 追加条件 |
| `or(Condition)` | `EntityQuery<T>` | OR 追加条件 |
| `distinct()` | `EntityQuery<T>` | 去重查询 |
| `orderBy(Sort)` | `EntityQuery<T>` | 指定排序 |
| `orderByAsc(SFunction<T, R>)` | `EntityQuery<T>` | Lambda 升序排序 |
| `orderByDesc(SFunction<T, R>)` | `EntityQuery<T>` | Lambda 降序排序 |

### 数据权限与租户

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `withDataScope()` | `EntityQuery<T>` | 启用数据权限（默认范围） |
| `withDataScope(String deptField)` | `EntityQuery<T>` | 指定部门字段的数据权限 |
| `withDataScope(DataScopeType)` | `EntityQuery<T>` | 指定数据权限类型 |
| `withDataScope(DataScope)` | `EntityQuery<T>` | 自定义数据权限 |
| `withDataScopes(DataScope...)` | `EntityQuery<T>` | 多个数据权限 |
| `withTenant(String tenantId)` | `EntityQuery<T>` | 指定租户 |
| `includeDeleted()` | `EntityQuery<T>` | 包含已软删除 |

### 分页与限制

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `limit(int)` | `EntityQuery<T>` | 限制返回行数 |
| `offset(int)` | `EntityQuery<T>` | 偏移量 |
| `page(PageRequest)` | `PageData<T>` | 分页查询 |
| `withDataSource(String name)` | `EntityQuery<T>` | 切换数据源 |
| `withReadOnly()` | `EntityQuery<T>` | 只读事务 |

### 字段选择

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `select(String...)` | `EntityQuery<T>` | 选择指定字段 |
| `select(SFunction<T, ?>...))` | `EntityQuery<T>` | Lambda 字段选择 |
| `exclude(String...)` | `EntityQuery<T>` | 排除指定字段 |

### 关联预加载

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `withAssociation(String name)` | `EntityQuery<T>` | 预加载关联 |
| `withAssociations(String...)` | `EntityQuery<T>` | 预加载多个关联 |
| `clearAssociations()` | `EntityQuery<T>` | 清除关联预加载 |

### 终端操作

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `list()` | `List<T>` | 返回列表 |
| `one()` | `Optional<T>` | 返回单条（期望唯一） |
| `first()` | `Optional<T>` | 返回第一条 |
| `count()` | `long` | 计数 |
| `exists()` | `boolean` | 是否存在 |

### 转换操作

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `aggregate()` | `AggregateQuery<T>` | 进入聚合查询模式 |
| `project(Class<R>)` | `ProjectedQuery<T, R>` | DTO 投影（按类型） |
| `project(Projection<T, R>)` | `ProjectedQuery<T, R>` | DTO 投影（按投影定义） |

---

## ProjectedQuery DTO 投影 API

包路径：`io.github.afgprojects.framework.data.core.query.ProjectedQuery<T, R>`

### 字段选择

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `select(SFunction<T, ?>...)` | `ProjectedQuery<T, R>` | Lambda 字段选择 |
| `select(String...)` | `ProjectedQuery<T, R>` | 字符串字段选择 |

### 条件与排序

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `where(Condition)` | `ProjectedQuery<T, R>` | 设置查询条件 |
| `and(Condition)` | `ProjectedQuery<T, R>` | AND 追加条件 |
| `or(Condition)` | `ProjectedQuery<T, R>` | OR 追加条件 |
| `orderBy(Sort)` | `ProjectedQuery<T, R>` | 指定排序 |

### 作用域

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `withDataScope()` | `ProjectedQuery<T, R>` | 启用数据权限 |
| `withDataScope(String deptField)` | `ProjectedQuery<T, R>` | 指定部门字段 |
| `withDataScope(DataScopeType)` | `ProjectedQuery<T, R>` | 指定权限类型 |
| `withTenant(String tenantId)` | `ProjectedQuery<T, R>` | 指定租户 |
| `includeDeleted()` | `ProjectedQuery<T, R>` | 包含已软删除 |

### 分页与限制

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `limit(int)` | `ProjectedQuery<T, R>` | 限制返回行数 |
| `offset(int)` | `ProjectedQuery<T, R>` | 偏移量 |
| `page(PageRequest)` | `PageData<R>` | 分页查询 |

### 终端操作

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `list()` | `List<R>` | 返回投影列表 |
| `one()` | `Optional<R>` | 返回单条 |
| `first()` | `Optional<R>` | 返回第一条 |
| `count()` | `long` | 计数 |

---

## 条件查询构建器

包路径：`io.github.afgprojects.framework.data.core.condition.Conditions`

### Lambda 类型安全构建

```java
import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

// 推荐：Lambda 方式（类型安全）
Condition condition = builder(User.class)
    .eq(User::getStatus, 1)
    .like(User::getUsername, "张")
    .ge(User::getCreatedAt, startTime)
    .between(User::getAge, 18, 60)
    .in(User::getDeptId, deptIds)
    .isNull(User::getDeletedAt)
    .build();

// 不推荐：字符串方式（易出错）
Condition condition = builder()
    .eq("status", 1)
    .build();
```

### 完整操作符

| 操作符 | SQL 映射 | Lambda 示例 | null 语义 |
|--------|---------|-------------|----------|
| `eq` | `= ?` | `.eq(User::getStatus, 1)` | null -> `IS NULL` |
| `ne` | `!= ?` | `.ne(User::getDeleted, true)` | null -> `IS NOT NULL` |
| `like` | `LIKE '%?%'` | `.like(User::getUsername, "张")` | -- |
| `likeStartsWith` | `LIKE '?%'` | `.likeStartsWith(User::getEmail, "admin")` | -- |
| `likeEndsWith` | `LIKE '%?'` | `.likeEndsWith(User::getEmail, ".com")` | -- |
| `notLike` | `NOT LIKE '%?%'` | `.notLike(User::getUsername, "test")` | -- |
| `in` | `IN (?, ?, ?)` | `.in(User::getDeptId, deptIds)` | 空集合 -> `1=0` |
| `notIn` | `NOT IN (?, ?, ?)` | `.notIn(User::getStatus, List.of(0, -1))` | 空集合 -> `1=1` |
| `isNull` | `IS NULL` | `.isNull(User::getDeletedAt)` | -- |
| `isNotNull` | `IS NOT NULL` | `.isNotNull(User::getEmail)` | -- |
| `gt` | `> ?` | `.gt(User::getAge, 18)` | -- |
| `ge` | `>= ?` | `.ge(User::getCreatedAt, start)` | -- |
| `lt` | `< ?` | `.lt(User::getAge, 60)` | -- |
| `le` | `<= ?` | `.le(User::getCreatedAt, end)` | -- |
| `between` | `BETWEEN ? AND ?` | `.between(User::getAge, 18, 60)` | -- |
| `notBetween` | `NOT BETWEEN ? AND ?` | `.notBetween(User::getSalary, 10k, 20k)` | -- |
| `jsonContains` | JSON_CONTAINS(?, ?) | `.jsonContains(User::getTags, "vip")` | -- |
| `jsonContained` | JSON_CONTAINED(?, ?) | `.jsonContained(User::getTags, "vip")` | -- |
| `jsonPath` | JSON_EXTRACT(?, ?) | `.jsonPath(User::getMeta, "$.level", 3)` | -- |

### 动态条件（null 值自动跳过）

| 操作符 | 说明 | null 处理 |
|--------|------|----------|
| `eqIfPresent` | 等于（值存在时） | null -> 跳过该条件 |
| `likeIfPresent` | LIKE（值存在时） | null -> 跳过该条件 |
| `geIfPresent` | 大于等于（值存在时） | null -> 跳过该条件 |
| `leIfPresent` | 小于等于（值存在时） | null -> 跳过该条件 |
| `inIfPresent` | IN（集合非空时） | null/空集合 -> 跳过 |
| `betweenIfPresent` | BETWEEN（两者都非 null 时） | 任一 null -> 跳过 |

```java
Condition condition = builder(User.class)
    .eqIfPresent(User::getStatus, status)         // status != null 时添加
    .likeIfPresent(User::getUsername, name)        // name != null 时添加
    .geIfPresent(User::getCreatedAt, startTime)    // startTime != null 时添加
    .inIfPresent(User::getDeptId, deptIds)          // deptIds 非空时添加
    .betweenIfPresent(User::getAge, minAge, maxAge) // 两者都非 null 时添加
    .build();
```

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

### 空值语义铁律

| 写法 | 转换结果 | 说明 |
|------|---------|------|
| `eq(field, null)` | `field IS NULL` | null 被语义化为 IS NULL |
| `ne(field, null)` | `field IS NOT NULL` | null 被语义化为 IS NOT NULL |
| `in(field, emptyList)` | `1=0` | 空集合不匹配任何记录 |
| `eqIfPresent(field, null)` | **跳过该条件** | null 时不参与查询 |

---

## 聚合查询 API

通过 `EntityQuery.aggregate()` 进入聚合模式：

```java
List<AggregateResult> results = dataManager.entity(User.class)
    .query()
    .aggregate()
    .groupBy(User::getDeptId)
    .count("id", "userCount")
    .sum("salary", "totalSalary")
    .avg("age", "avgAge")
    .max("createdAt", "latestCreated")
    .min("createdAt", "earliestCreated")
    .having(builder().gt("userCount", 5).build())
    .list();

results.get(0).getLong("userCount");
results.get(0).getDouble("avgAge");
```

**聚合函数**：`COUNT`, `COUNT_DISTINCT`, `SUM`, `AVG`, `MAX`, `MIN`

---

## 实体基类体系

包路径：`io.github.afgprojects.framework.data.core.entity`

| 基类 | 字段 | 特征接口 |
|------|------|---------|
| `BaseEntity` (abstract) | `id`(Long), `createdAt`(Instant), `updatedAt`(Instant) | -- |
| `TenantEntity` | + `tenantId`(String) | -- |
| `SoftDeleteEntity` | + `deleted`(Boolean=false) | `SoftDeletable` |
| `TimestampSoftDeleteEntity` | + `deletedAt`(Instant) | `TimestampSoftDeletable` |
| `VersionedEntity` | + `version`(Integer=0) | `Versioned` |
| `FullEntity` | + `deleted` + `version` + `createBy` + `updateBy` | `SoftDeletable` + `Versioned` + `Auditable` |
| `TreeEntity<T>` | + `parentId` + `level` + `path` + `sortOrder` + `children` | `Treeable<T>` |

**特征接口行为：**

| 特征接口 | 方法 | 自动行为 |
|---------|------|---------|
| `SoftDeletable` | `getDeleted()`, `setDeleted()`, `markDeleted()`, `isDeleted()` | 删除操作自动调用 `markDeleted()`，查询自动过滤 `deleted=false` |
| `TimestampSoftDeletable` | `getDeletedAt()`, `setDeletedAt()`, `markDeleted()`, `isDeleted()` | 删除操作自动设置 `deletedAt=now()` |
| `Versioned` | `getVersion()`, `setVersion()`, `incrementVersion()` | 更新操作自动 `version+1`，WHERE 条件包含 `version=?` |
| `Auditable` | 标记接口 | 框架自动从 Security 上下文获取当前用户 ID，填充 `createBy`/`updateBy` |

**重要**：`FullEntity` 不继承 `TenantEntity`。需要多租户 + 全功能时，继承 `FullEntity` 并手动添加 `tenantId` 字段。

---

## 数据权限

DataScopeType 枚举定义 5 种数据权限范围：

| DataScopeType | 自动注入条件 | 说明 |
|--------------|-------------|------|
| `ALL` | 无额外条件 | 看到所有数据 |
| `SELF` | `create_by = currentUserId` | 仅看自己创建的 |
| `DEPT` | `dept_id = currentDeptId` | 仅看本部门 |
| `DEPT_AND_CHILD` | `dept_id IN (currentDept + children)` | 本部门及子部门 |
| `CUSTOM` | 自定义策略 | 业务自定义条件 |

```java
// 查询时指定数据权限
dataManager.entity(User.class)
    .query()
    .withDataScope()            // 自动注入当前用户的数据权限范围
    .list();

dataManager.findListWithDataScope(User.class, condition);
```

---

## 分页

```java
PageRequest.of(1, 20)                          // 第 1 页，每页 20 条
PageRequest.of(1, 20, Sort.by("createdAt"))    // 带排序
PageRequest.defaultPage()                       // 第 1 页，每页 10 条

// 查询返回
PageData<User> page = dataManager.entity(User.class)
    .query().where(condition)
    .page(PageRequest.of(1, 20));
// page.records(), page.total(), page.pages(), page.hasNext(), page.hasPrevious()
```

---

## 关联关系

框架使用自有关联注解（非 JPA），包路径：`io.github.afgprojects.framework.data.core.relation`

| 注解 | 默认 Fetch | 特有属性 |
|------|-----------|---------|
| `@ManyToOne` | EAGER | `foreignKey`, `optional` |
| `@OneToMany` | LAZY | `mappedBy`, `foreignKey`, `orphanRemoval` |
| `@OneToOne` | LAZY | `mappedBy`, `foreignKey` |
| `@ManyToMany` | LAZY | `mappedBy`, `joinTable`, `joinColumn`, `inverseJoinColumn` |

所有注解共有：`targetEntity`, `cascade`（PERSIST/MERGE/REMOVE/REFRESH/DETACH/ALL）, `fetch`

```java
// 显式加载
dataManager.entity(User.class).fetch(user, "orders");

// 预加载
dataManager.entity(User.class)
    .query().withAssociation("orders").list();

// 批量加载
dataManager.entity(User.class).fetchAll(users, "orders");
```

---

## 使用示例

### 完整 Controller 示例

```java
@RestController
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
            .orElse(Result.fail(CommonErrorCode.ENTITY_NOT_FOUND));
    }

    @GetMapping
    public Result<PageData<User>> list(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Condition condition = builder(User.class)
            .likeIfPresent(User::getUsername, username)
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
        dataManager.deleteById(User.class, id);
        return Result.success();
    }
}
```
