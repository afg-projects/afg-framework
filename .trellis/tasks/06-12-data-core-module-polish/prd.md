# data-core 模块精细化打磨

## Goal

对 data-core 模块执行 PRD 阶段 3 的精细化打磨，确保条件构建器 IfPresent 系列、实体体系补全、AutoConfiguration 规范、SPI 接口默认实现、Javadoc 对齐等全部达到 6 条质量底线。

## What I already know

* data-core 模块有 2 个 AutoConfiguration：`TenantContextAutoConfiguration`、`TransactionAutoConfiguration`
* `TenantContextAutoConfiguration` 缺少 `@AutoConfigureAfter` 声明
* `ConditionBuilder` 和 `TypedConditionBuilder` 接口完全没有 IfPresent 系列方法
* `Conditions` 静态工厂类也没有 IfPresent 系列方法
* `EntityProxy` 接口没有 `withPessimisticLock()` 方法
* `TreeEntity<T>` 和 `Treeable<T>` 在 PRD 中定义但代码库中不存在
* `@EncryptedField` 注解已在 apt-api 中定义（阶段 2 新增），但 data-core 层无运行时支持
* `EntityTrait.AUDITABLE` 已声明但 data-jdbc 层的 Handler 中无审计字段自动填充实现
* `EntityTrait.DATA_SCOPE_AWARE` 已声明但无自动行为
* `ConditionBuilder` 接口 Javadoc 极简（仅"条件构建器（字符串字段名）"）
* `DefaultConditionBuilder` 和 `DefaultTypedConditionBuilder` 是 Conditions 的内部类，无独立测试

## Requirements

### T3.1: IfPresent 条件构建器系列

在 `ConditionBuilder`、`TypedConditionBuilder` 接口和 `Conditions` 静态工厂类中添加 IfPresent 系列方法。

**IfPresent 语义**：当值为 null 时跳过该条件（不添加到 criteria 列表），而非转换为 IS NULL / IS NOT NULL。这是动态查询场景的核心需求——前端传入的搜索条件可能为空，IfPresent 避免了手动 null 判断。

**需要添加的方法**（三个位置各一套）：

| 方法 | 语义 |
|------|------|
| `eqIfPresent(getter, value)` | value != null 时添加 eq 条件 |
| `neIfPresent(getter, value)` | value != null 时添加 ne 条件 |
| `likeIfPresent(getter, value)` | value != null 且非空字符串时添加 like 条件 |
| `likeStartsWithIfPresent(getter, value)` | value != null 且非空字符串时添加 likeStartsWith 条件 |
| `likeEndsWithIfPresent(getter, value)` | value != null 且非空字符串时添加 likeEndsWith 条件 |
| `notLikeIfPresent(getter, value)` | value != null 且非空字符串时添加 notLike 条件 |
| `inIfPresent(getter, values)` | values != null 且非空集合时添加 in 条件 |
| `notInIfPresent(getter, values)` | values != null 且非空集合时添加 notIn 条件 |
| `betweenIfPresent(getter, from, to)` | from != null 且 to != null 时添加 between 条件 |
| `notBetweenIfPresent(getter, from, to)` | from != null 且 to != null 时添加 notBetween 条件 |
| `gtIfPresent(getter, value)` | value != null 时添加 gt 条件 |
| `geIfPresent(getter, value)` | value != null 时添加 ge 条件 |
| `ltIfPresent(getter, value)` | value != null 时添加 lt 条件 |
| `leIfPresent(getter, value)` | value != null 时添加 le 条件 |

**设计决策**：
- `likeIfPresent` 等字符串方法：空字符串 `""` 也视为"不存在"而跳过（与 MyBatis-Plus IfPresent 行为一致）
- `inIfPresent`：空集合也视为"不存在"而跳过（与 `Conditions.in()` 的空集合→none() 语义不同）
- `betweenIfPresent`：from 和 to 都非 null 才添加（部分 null 时无法构成有效 BETWEEN）
- 不提供 `isNullIfPresent` / `isNotNullIfPresent`（这两个方法无"值"参数，IfPresent 无意义）
- 不提供 JSON IfPresent 系列（JSON 查询场景较少动态化，后续按需添加）

### T3.2: TreeEntity + Treeable 接口

在 `data-core/entity/` 包中新增树形结构实体支持：

**Treeable<T> 接口**：
```java
public interface Treeable<T> {
    Long getParentId();
    void setParentId(Long parentId);
    Integer getLevel();
    void setLevel(Integer level);
    String getPath();
    void setPath(String path);
    Integer getSortOrder();
    void setSortOrder(Integer sortOrder);
    List<T> getChildren();
    void setChildren(List<T> children);
}
```

**TreeEntity<T> 抽象类**：extends BaseEntity implements Treeable<T>
- `parentId` (Long, default null — 顶级节点)
- `level` (Integer, default 1)
- `path` (String, default "/" — 根路径)
- `sortOrder` (Integer, default 0)
- `children` (List<T>, transient — 不持久化)

**EntityTrait 新增**：`TREEABLE` — 检测 `parentId` + `path` 字段存在

**注意**：TreeQuery（递归查询、路径查询、子树查询）属于 data-jdbc 层实现，不在本阶段范围。

### T3.3: @EncryptedField 运行时支持（data-core 层声明）

在 data-core 层为 `@EncryptedField` 提供运行时支持的基础设施：

1. **FieldEncryption SPI 接口**（`data-core/entity/FieldEncryptor.java`）：
```java
public interface FieldEncryptor {
    String encrypt(String plaintext, String algorithm, String keyRef);
    String decrypt(String ciphertext, String algorithm, String keyRef);
}
```

2. **NoOpFieldEncryptor**：data-core 内置的 NoOp 降级实现（直接返回原文）

3. **EntityMetadata 扩展**：`isEncrypted(String fieldName)` / `getEncryptedFields()` 方法

4. **EntityTrait 新增**：`ENCRYPTED` — 检测实体是否有 @EncryptedField 标注的字段

**注意**：实际的加密/解密在 EntityInsertHandler/EntityMapper 中执行，属于 data-jdbc 层。本阶段只定义 SPI + NoOp + 元数据支持。

### T3.4: withPessimisticLock() 方法

在 `EntityQuery<T>` 接口和 `BaseQuery<Q,R>` 接口中添加 `withPessimisticLock()` 方法：

```java
// BaseQuery
Q withPessimisticLock();

// EntityQuery
EntityQuery<T> withPessimisticLock();
```

语义：查询时添加 `SELECT ... FOR UPDATE`（悲观锁），在 data-jdbc 层的 JdbcEntityQuery 中通过 Dialect 生成对应 SQL。

### T3.5: AutoConfiguration 规范整改

1. **TenantContextAutoConfiguration**：添加 `@AutoConfigureAfter` 声明（应在 TaskDecorator 相关配置之后）
2. **TransactionAutoConfiguration**：已有 `afterName`，符合规范 ✅
3. 两个 AutoConfiguration 都有 `@ConditionalOnClass` + `@ConditionalOnMissingBean` ✅
4. 两个 AutoConfiguration 都有 `enabled` 开关或合理的条件 ✅

### T3.6: Javadoc 对齐

为以下关键接口/类补充完整 Javadoc：
- `ConditionBuilder` — 当前仅一行描述
- `TypedConditionBuilder` — 已有较完整 Javadoc ✅
- `Conditions` — 当前仅"条件工厂类"
- `EntityReader` / `EntityWriter` — 检查 Javadoc 完整性
- `DataManager` — 检查 Javadoc 完整性
- `EntityQuery` — 检查 Javadoc 完整性
- `BaseQuery` — 检查 Javadoc 完整性

### T3.7: 测试补充

1. **IfPresent 系列单元测试**：`ConditionIfPresentTest.java` — 测试所有 IfPresent 方法的 null/空值跳过行为
2. **TreeEntity 单元测试**：`TreeEntityTest.java` — 测试默认值、markDeleted、path 操作
3. **Treeable 接口测试**：验证接口契约
4. **FieldEncryptor SPI 测试**：`NoOpFieldEncryptorTest.java` — 测试 NoOp 实现
5. **EntityMetadata 加密字段测试**：验证 isEncrypted / getEncryptedFields

## Acceptance Criteria

- [ ] IfPresent 系列 14 个方法在 ConditionBuilder + TypedConditionBuilder + Conditions 三处全部实现
- [ ] IfPresent 单元测试覆盖 null 跳过、空字符串跳过、空集合跳过、正常值添加
- [ ] TreeEntity<T> + Treeable<T> 接口实现，EntityTrait.TREEABLE 枚举值添加
- [ ] FieldEncryptor SPI + NoOpFieldEncryptor 实现
- [ ] EntityMetadata 扩展 isEncrypted / getEncryptedFields 方法
- [ ] EntityTrait.ENCRYPTED 枚举值添加
- [ ] withPessimisticLock() 在 BaseQuery + EntityQuery 接口中声明
- [ ] TenantContextAutoConfiguration 添加 @AutoConfigureAfter
- [ ] 关键接口 Javadoc 补充完整
- [ ] 全量构建 BUILD SUCCESSFUL

## Definition of Done

- Tests added/updated（IfPresent + TreeEntity + FieldEncryptor）
- PMD / checkstyle green
- 全量 `./gradlew build` BUILD SUCCESSFUL
- Spec 更新（如有新模式需要记录）

## Out of Scope

- TreeQuery 递归查询实现（data-jdbc 层，阶段 4/5）
- @EncryptedField 实际加密/解密执行（data-jdbc 层 EntityInsertHandler/EntityMapper）
- AUDITABLE 审计字段自动填充实现（data-jdbc 层，阶段 5）
- withPessimisticLock() 的 JDBC 实现（data-jdbc 层，阶段 5）
- data-sql / data-jdbc / data-liquibase 模块（后续阶段）

## Technical Notes

### 文件变更预估

| 文件 | 变更类型 |
|------|---------|
| `condition/ConditionBuilder.java` | 修改：添加 14 个 IfPresent 方法 |
| `condition/TypedConditionBuilder.java` | 修改：添加 14 个 IfPresent 方法 |
| `condition/Conditions.java` | 修改：添加 IfPresent 静态方法 + DefaultBuilder 内部类实现 |
| `entity/Treeable.java` | 新增 |
| `entity/TreeEntity.java` | 新增 |
| `entity/FieldEncryptor.java` | 新增 |
| `entity/NoOpFieldEncryptor.java` | 新增 |
| `query/EntityTrait.java` | 修改：添加 TREEABLE + ENCRYPTED |
| `metadata/EntityMetadata.java` | 修改：添加 isEncrypted / getEncryptedFields |
| `query/BaseQuery.java` | 修改：添加 withPessimisticLock() |
| `query/EntityQuery.java` | 修改：添加 withPessimisticLock() override |
| `autoconfigure/TenantContextAutoConfiguration.java` | 修改：添加 @AutoConfigureAfter |
| 测试文件若干 | 新增 |

### IfPresent 实现策略

DefaultConditionBuilder 和 DefaultTypedConditionBuilder 是 Conditions 的私有内部类。IfPresent 方法在内部类中实现为：检查 value == null → return this（跳过），否则调用对应的非 IfPresent 方法。TypedConditionBuilder 的 IfPresent 方法委托给 DefaultTypedConditionBuilder，后者再委托给 DefaultConditionBuilder。

### TreeEntity path 格式

PRD 定义 path 格式为 `/1/5/12/`（前后都有 `/`），根节点 path 为 `/`。level 从 1 开始。path 用于 LIKE 查询子树：`path LIKE '/1/5/%'`。
