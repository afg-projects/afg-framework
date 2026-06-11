# 实体设计规范

> PRD 来源：§3.4 实体基类选择决策树、§5.4 DataManager 数据访问模块（实体基类表）、§6.5 实体设计规范
> CLAUDE.md 来源：DataManager + APT 章节（实体基类、实体定义）

---

## 概述

AFG 框架实体体系围绕 **APT 编译时元数据生成** 设计，实体类通过 `@AfEntity` 注解触发 APT 处理器生成 `{Entity}Metadata` 元数据类，运行时由 `EntityMetadataCache` 加载（优先 APT 生成 > 反射降级 > 空元数据兜底）。

核心原则：
1. **@AfEntity + @Table + @Column 三者配合** — 缺少任何一项都会导致元数据生成失败
2. **特征接口优于继承** — 软删除、乐观锁、审计等能力通过接口标记，元数据系统通过 `EntityTrait` 检测
3. **组合优于继承** — `FullEntity` 不继承 `TenantEntity`，需要多租户 + 全功能时手动叠加

---

## 实体基类层次结构

```
BaseEntity (abstract)                  id(Long), createdAt(Instant), updatedAt(Instant)
  │
  ├── TenantEntity                     + tenantId(String)
  │                                       [无特征接口，元数据通过字段检测]
  │
  ├── SoftDeleteEntity                 + deleted(Boolean=false)
  │                                       implements SoftDeletable
  │
  ├── TimestampSoftDeleteEntity        + deletedAt(Instant)
  │                                       implements TimestampSoftDeletable
  │
  ├── VersionedEntity                  + version(Integer=0)
  │                                       implements Versioned
  │
  ├── FullEntity                       + deleted(Boolean=false)
  │                                    + version(Integer=0)
  │                                    + createBy(String)
  │                                    + updateBy(String)
  │                                       implements SoftDeletable, Versioned, Auditable
  │
  └── TreeEntity<T> (PRD 计划)        + parentId(Long)
                                      + level(Integer)
                                      + path(String)           — /1/5/12/ 格式
                                      + sortOrder(Integer)
                                      + children(List<T>)
                                         implements Treeable<T>
```

> **注意**：`FullEntity` 不继承 `TenantEntity`。需要多租户 + 全功能时，继承 `FullEntity` 并手动添加 `tenantId` 字段。

> **注意**：`TreeEntity` 和 `Treeable` 在 PRD §5.4 中定义为计划功能（树形结构 + TreeQuery 递归查询、路径查询、子树查询、闭包表支持），当前代码库中尚未实现。

---

## 基类选择决策树

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
├─ 需要全部功能（软删除 + 乐观锁 + 审计）？
│   └─ FullEntity
│
└─ 需要树形结构？
    └─ TreeEntity<T> (PRD 计划)
```

### 基类组合规则

| 需求 | 基类 | 特征接口 |
|------|------|---------|
| 基础 | `BaseEntity` | — |
| 多租户 | `TenantEntity` | — |
| 软删除 | `SoftDeleteEntity` | `SoftDeletable` |
| 软删除+时间戳 | `TimestampSoftDeleteEntity` | `TimestampSoftDeletable` |
| 乐观锁 | `VersionedEntity` | `Versioned` |
| 全功能 | `FullEntity` | `SoftDeletable` + `Versioned` + `Auditable` |
| 树形 | `TreeEntity<T>` | `Treeable<T>` |

### 特殊组合

- **多租户 + 全功能**：继承 `FullEntity` 并手动添加 `tenantId` 字段（`FullEntity` 不继承 `TenantEntity`）
- **多租户 + 软删除**：继承 `TenantEntity` → 手动添加 `deleted` 字段 + 实现 `SoftDeletable`
- **软删除 + 乐观锁**：继承 `BaseEntity` → 手动添加 `deleted` + `version` 字段 + 实现 `SoftDeletable` + `Versioned`

---

## 特征接口

### SoftDeletable — 布尔软删除

```java
public interface SoftDeletable {
    Boolean getDeleted();
    void setDeleted(Boolean deleted);
    default void markDeleted()    { setDeleted(true); }
    default void markNotDeleted() { setDeleted(false); }
    default boolean isDeleted()   { return Boolean.TRUE.equals(getDeleted()); }
}
```

- 对应 `@SoftDeleteField(strategy = BOOLEAN)`
- 查询时自动添加 `WHERE deleted = false`
- 软删除时设置 `deleted = true`
- 唯一约束必须包含 `deleted` 字段

### TimestampSoftDeletable — 时间戳软删除

```java
public interface TimestampSoftDeletable {
    Instant getDeletedAt();
    void setDeletedAt(Instant deletedAt);
    default void markDeleted()    { setDeletedAt(Instant.now()); }
    default void markNotDeleted() { setDeletedAt(null); }
    default boolean isDeleted()   { return getDeletedAt() != null; }
}
```

- 对应 `@SoftDeleteField(strategy = TIMESTAMP)`
- 查询时自动添加 `WHERE deleted_at IS NULL`
- 软删除时设置 `deleted_at = Instant.now()`
- `deleted_at = null` 表示未删除

### Versioned — 乐观锁

```java
public interface Versioned {
    Integer getVersion();
    void setVersion(Integer version);
    default void incrementVersion() {
        setVersion(getVersion() == null ? 1 : getVersion() + 1);
    }
}
```

- 更新时自动添加 `WHERE id = ? AND version = ?`
- 冲突时抛出 `OptimisticLockException`
- 更新成功后自动 `incrementVersion()`

### Auditable — 审计标记

```java
public interface Auditable { }  // 标记接口
```

- 表示实体有 `createBy`/`updateBy` 字段
- `createBy` 在创建时自动从 Security 上下文获取当前用户 ID
- `updateBy` 在更新时自动从 Security 上下文获取当前用户 ID

### AuditableCallback — 审计回调（可选）

```java
public interface AuditableCallback {
    void onCreate(AuditContext context);
    void onUpdate(AuditContext context);
    interface AuditContext {
        Object getCurrentUser();
        Instant getCurrentTime();
    }
}
```

- 需要自定义审计逻辑时实现此接口，超出简单的 `createBy`/`updateBy` 填充

---

## @SoftDeleteField 注解

用于自定义软删除字段的策略配置：

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SoftDeleteField {
    SoftDeleteStrategy strategy() default BOOLEAN;  // BOOLEAN | TIMESTAMP
    String deletedValue() default "true";            // BOOLEAN: "true", TIMESTAMP: 不需要
    String notDeletedValue() default "false";        // BOOLEAN: "false", TIMESTAMP: "null"
}
```

### SoftDeleteStrategy 枚举

| 策略 | 说明 | deleted 值 | not-deleted 值 |
|------|------|-----------|----------------|
| `BOOLEAN` | 布尔标记 | `true` | `false` |
| `TIMESTAMP` | 时间戳标记 | 当前时间 | `null`（SQL NULL） |

### 使用场景

- `SoftDeleteEntity` 默认使用 BOOLEAN 策略（`deleted` 字段）
- `TimestampSoftDeleteEntity` 默认使用 TIMESTAMP 筗略（`deletedAt` 字段）
- 自定义实体需要软删除但不想继承基类时，可在字段上添加 `@SoftDeleteField`

---

## @EncryptedField 注解

PII 数据加密存储标记：

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptedField {
    String algorithm() default "AES";    // 加密算法
    String keyRef() default "";          // 密钥引用名称
}
```

### 使用示例

```java
@EncryptedField(algorithm = AES, keyRef = "user-key")
private String idCard;     // 身份证号加密存储

@EncryptedField(algorithm = AES, keyRef = "phone-key")
private String phone;      // 手机号加密存储
```

### 配置

```yaml
afg:
  data:
    field-encryption:
      enabled: true
      default-algorithm: AES
```

---

## LifecycleCallbacks — 实体生命周期回调

类似 JPA 的 `@PrePersist`/`@PreUpdate`/`@PostLoad`/`@PreRemove`，框架提供 `LifecycleCallbacks` 接口：

```java
public interface LifecycleCallbacks {
    default void beforeCreate() {}   // 创建前（类似 @PrePersist）
    default void beforeUpdate() {}   // 更新前（类似 @PreUpdate）
    default void afterLoad() {}      // 加载后（类似 @PostLoad）
    default void beforeDelete() {}   // 删除前（类似 @PreRemove）

    static <T> void ifCallback(T entity, Consumer<LifecycleCallbacks> action) {
        if (entity instanceof LifecycleCallbacks lc) {
            action.accept(lc);
        }
    }
}
```

### 框架调用时机

| 回调 | 调用位置 | 说明 |
|------|---------|------|
| `beforeCreate()` | `EntityInsertHandler` | INSERT 之前调用 |
| `afterLoad()` | `EntityMapper` | ResultSet 映射为实体后调用 |
| `beforeUpdate()` | 当前未显式调用 | 更新前回调（计划在后续版本完善） |
| `beforeDelete()` | 当前未显式调用 | 删除前回调（计划在后续版本完善） |

### 使用示例

```java
@Getter @Setter
@AfEntity
@Table(name = "sys_user")
public class User extends SoftDeleteEntity implements LifecycleCallbacks {

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Override
    public void beforeCreate() {
        // 创建前的业务逻辑，如默认值设置
        if (getStatus() == null) {
            setStatus(1);
        }
    }

    @Override
    public void afterLoad() {
        // 加载后的业务逻辑，如初始化瞬态字段
    }
}
```

所有方法都有默认空实现，实体可以选择性覆盖需要的方法。

---

## 实体定义规范

### 必须三注解配合

```java
@Getter @Setter
@AfEntity                    // 触发 APT 元数据生成
@Table(name = "sys_user")    // 表名映射
public class User extends SoftDeleteEntity {

    @Column(name = "username", nullable = false, length = 50)  // 列映射
    private String username;

    private Integer status = 1;
}
```

缺少 `@AfEntity` 将导致 APT 不生成 `{Entity}Metadata` 类，DataManager 运行时找不到实体元数据。

### 注解包路径

| 注解 | 包路径 |
|------|--------|
| `@AfEntity` | `io.github.afgprojects.framework.apt.entity.AfEntity` |
| `@Table` | `io.github.afgprojects.framework.data.core.annotation.Table` |
| `@Column` | `io.github.afgprojects.framework.data.core.annotation.Column` |
| `@SoftDeleteField` | `io.github.afgprojects.framework.data.core.entity.SoftDeleteField` |
| `@EncryptedField` | `io.github.afgprojects.framework.apt.entity.EncryptedField` |

---

## 字段命名约定

### Java 与数据库列名映射

| Java 字段 | 数据库列 | 说明 |
|----------|---------|------|
| `createdAt` | `created_at` | Java camelCase → DB snake_case（框架自动转换） |
| `updatedAt` | `updated_at` | 同上 |
| `deletedAt` | `deleted_at` | 同上 |
| `deleted` | `deleted` | 单词字段无转换 |
| `version` | `version` | 同上 |
| `tenantId` | `tenant_id` | 同上 |
| `createBy` | `create_by` | 同上 |
| `updateBy` | `update_by` | 同上 |

> 框架通过 `NamingUtils.toSnakeCase()` 自动将 Java camelCase 字段名转为数据库 snake_case 列名。使用 `@Column(name = ...)` 可显式指定列名覆盖默认映射。

### 字段类型约定

| 场景 | 推荐类型 | 说明 |
|------|---------|------|
| 时间戳 | `Instant` | UTC 时间，无时区，**禁止使用 LocalDateTime** |
| 金额 | `BigDecimal` | 精确计算，**禁止使用 Double/Float** |
| 枚举（数据库） | `String` | 避免用 ordinal，配合 `EnumTypeHandler` |
| 枚举（Java） | Java enum | 类型安全 |
| JSON | `String` + `@JsonTypeHandler` | 自动序列化/反序列化 |
| UUID | `UUID` | 标准格式，配合 `UUIDTypeHandler` |
| 大文本 | `String` + `@Column(columnDefinition = "TEXT")` | — |
| PII 数据 | `String` + `@EncryptedField` | 加密存储 |
| 外键关联 | `Long` | 关联 ID 字段 |

---

## 软删除约束规则

### 唯一约束必须包含 deleted 字段

软删除实体的唯一约束必须包含 `deleted` 字段，否则已删除记录与新建记录冲突：

```xml
<!-- 错误：不含 deleted 字段 -->
<constraints nullable="false" unique="true" uniqueConstraintName="uk_user_username"/>

<!-- 正确：包含 deleted 字段 -->
<constraints nullable="false" uniqueConstraintName="uk_user_username_deleted"/>
<!-- 对应的列组合：(username, deleted) -->
```

### BOOLEAN 策略 vs TIMESTAMP 策略

| 策略 | 基类 | deleted 值 | 查询过滤 | 唯一约束包含字段 |
|------|------|-----------|---------|----------------|
| `BOOLEAN` | `SoftDeleteEntity` | `true` / `false` | `WHERE deleted = false` | `deleted` |
| `TIMESTAMP` | `TimestampSoftDeleteEntity` | `Instant.now()` / `null` | `WHERE deleted_at IS NULL` | `deleted_at` |

---

## 实体特征检测机制

### EntityTrait 枚举

框架通过 `EntityTrait` 枚举检测实体特征，而非依赖接口继承：

| Trait | 检测方式 | 对应基类/接口 | 自动行为 |
|-------|---------|-------------|---------|
| `SOFT_DELETABLE` | `deleted` 字段存在 | `SoftDeletable` | 查询自动过滤 `deleted=false`，删除时设置 `deleted=true` |
| `TIMESTAMP_SOFT_DELETABLE` | `deletedAt` 字段存在 | `TimestampSoftDeletable` | 查询自动过滤 `deleted_at IS NULL`，删除时设置 `deleted_at=now()` |
| `TENANT_AWARE` | `tenantId` 字段存在 | `TenantEntity`（无接口） | 查询自动添加租户过滤 |
| `VERSIONED` | `version` 字段存在 | `Versioned` | 更新时自动添加 `WHERE version=?`，冲突抛 `OptimisticLockException` |
| `AUDITABLE` | `createBy`/`updateBy` 字段存在 | `Auditable` | 创建时填充 `createBy`，更新时填充 `updateBy` |
| `DATA_SCOPE_AWARE` | `deptId` 字段存在 | — | 数据权限自动过滤 |
| `TIMESTAMPED` | `createdAt`/`updatedAt` 字段存在 | `BaseEntity` | 创建时填充 `createdAt`/`updatedAt`，更新时刷新 `updatedAt` |

> **关键区分**：`TIMESTAMPED` 和 `AUDITABLE` 是两个独立的 trait。`TIMESTAMPED` 检测 `createdAt`/`updatedAt`（时间戳自动填充），`AUDITABLE` 检测 `createBy`/`updateBy`（操作人自动填充）。`BaseEntity` 只有 `TIMESTAMPED`，`FullEntity` 同时有 `TIMESTAMPED` + `AUDITABLE`。APT 处理器和 `ReflectiveEntityMetadata` 必须正确区分这两个 trait，不能将 `createdAt`/`updatedAt` 误映射为 `AUDITABLE`。

### 元数据加载链

```
编译时：实体类 (@AfEntity) → APT 处理器 → {Entity}Metadata.java
运行时：EntityMetadataCache → AptMetadataLoader（优先）→ ReflectiveMetadataLoader（降级）→ 空元数据兜底
```

- APT 生成的元数据类是零反射的首选路径
- `ReflectiveEntityMetadata` 通过运行时反射检测字段和特征作为降级方案
- 缓存失败结果避免重复尝试

---

## TreeEntity（PRD 计划）

PRD §5.4 定义的树形结构实体，当前代码库尚未实现：

| 字段 | 类型 | 说明 |
|------|------|------|
| `parentId` | `Long` | 父节点 ID |
| `level` | `Integer` | 层级深度 |
| `path` | `String` | 祖先 ID 路径，格式 `/1/5/12/` |
| `sortOrder` | `Integer` | 同级排序 |
| `children` | `List<T>` | 子节点列表 |

配套功能：
- `TreeQuery` — 递归查询、路径查询、子树查询、闭包表支持
- `Treeable<T>` — 特征接口

> `TreeEntity` 的 `path` 字段使用 `/` 分隔的祖先 ID 路径（如 `/1/5/12/`），用于快速查询子树。

---

## 常见错误

### 实体类缺少 @AfEntity

**症状**：DataManager 操作实体时报错，提示找不到实体元数据

**原因**：实体类只有 `@Table`/`@Column` 但缺少 `@AfEntity` 注解，APT 不生成元数据类

**解决**：在所有实体类上添加 `@AfEntity` 注解

**预防**：实体类必须同时有 `@AfEntity` + `@Table` + `@Column`，三者配合使用

### FullEntity 多租户缺失

**症状**：多租户过滤不生效，查询返回其他租户的数据

**原因**：继承 `FullEntity` 但 `FullEntity` 不继承 `TenantEntity`，缺少 `tenantId` 字段

**解决**：在 `FullEntity` 子类中手动添加 `tenantId` 字段

**预防**：需要多租户 + 全功能时，继承 `FullEntity` 并手动添加 `tenantId` 字段

### 软删除唯一约束冲突

**症状**：软删除后重新创建同名记录时唯一约束报错

**原因**：唯一约束未包含 `deleted` 字段

**解决**：唯一约束必须包含 `deleted` 字段

**预防**：软删除实体的唯一约束必须包含软删除字段（BOOLEAN: `deleted`，TIMESTAMP: `deleted_at`）

### 时间戳字段使用 LocalDateTime

**症状**：跨时区部署时时间显示不一致

**原因**：使用 `LocalDateTime` 而非 `Instant`，缺少时区信息

**解决**：所有时间戳字段使用 `Instant` 类型

**预防**：时间戳字段约定使用 `Instant`（UTC），禁止使用 `LocalDateTime`

### 金额字段使用 Double

**症状**：金额计算出现精度丢失

**原因**：使用 `Double`/`Float` 而非 `BigDecimal`

**解决**：所有金额字段使用 `BigDecimal` 类型

**预防**：金额字段约定使用 `BigDecimal`，禁止使用 `Double`/`Float`