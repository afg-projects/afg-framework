# Lambda 模式字段名转换机制设计

## 概述

为 data-jdbc 模块设计统一的字段名转换机制，支持 Lambda 模式下实体属性名到数据库列名的自动映射，特别处理 Boolean 类型字段的 `is_` 前缀问题。

## 背景

### 现有问题

1. **字段名转换逻辑分散**：存在于 `Conditions`、`ConditionToSqlConverter`、`EntityQueryHelper`、`SimpleFieldMetadata` 多处
2. **Boolean 字段映射问题**：遵循阿里规约，Java 属性用 `deleted`，数据库用 `is_deleted`，当前无法正确映射
3. **无统一命名策略**：缺乏可配置、可扩展的命名策略接口

### 阿里规约要求

- POJO 类布尔类型变量**不加 `is` 前缀**
- Getter 方法使用 `getXxx()`，不用 `isXxx()`
- 数据库字段**可以**使用 `is_xxx` 前缀
- 数据库实体禁用 `boolean` 基本类型，必须使用 `Boolean` 包装类型

## 设计目标

1. 统一字段名转换入口，集中管理转换逻辑
2. 支持 `@Column` 注解显式指定列名
3. 通过 APT 编译时生成元数据，优化性能
4. 元数据设计通用化，支持数据库、缓存、搜索等多种场景
5. 集成测试覆盖核心场景、边界场景和性能场景

## 架构设计

### 整体流程

```
Lambda::getDeleted
    ↓
TypedConditionBuilder.resolveFieldName(getter)
    ↓
FieldNameResolver.resolve(entityClass, getter)
    ↓
EntityMetadataCache.get(entityClass)  ← APT 编译时生成或运行时缓存
    ↓
DatabaseEntityMetadata.getColumnName("deleted")
    ↓
返回 "is_deleted"
    ↓
Condition 存储 "is_deleted"
```

### 模块职责

| 模块 | 职责 |
|------|------|
| data-core | 核心抽象：元数据接口、FieldNameResolver |
| data-jdbc | JDBC 实现：TypedConditionBuilder 集成 |
| data-sql | SQL 构建：移除字段名转换逻辑 |
| apt-api | APT 注解定义：@AfEntity、@AfgModuleAnnotation 等 |
| apt-impl | APT 处理器实现：编译时生成元数据类和索引文件 |

## 详细设计

### 一、APT 模块架构整合

#### 现有结构

```
afg-framework/
├── module-api/           # 模块注解定义
│   └── AfgModuleAnnotation.java
└── module-processor/     # 模块 APT 处理器
    └── AfgModuleAnnotationProcessor.java
```

#### 整合后结构

```
afg-framework/
├── apt-api/                          # APT 注解定义（统一）
│   └── src/main/java/io/github/afgprojects/framework/apt/
│       ├── module/
│       │   └── AfgModuleAnnotation.java   # 模块注解（迁移）
│       └── entity/
│           └── AfEntity.java             # 实体注解（新增）
│
└── apt-impl/                         # APT 处理器实现（统一）
    └── src/main/java/io/github/afgprojects/framework/apt/
        ├── module/
        │   └── AfgModuleAnnotationProcessor.java  # 模块处理器（迁移）
        └── entity/
            └── EntityMetadataProcessor.java        # 实体元数据处理器（新增）
```

#### 迁移计划

| 原模块 | 新位置 | 说明 |
|--------|--------|------|
| `module-api` | `apt-api/module/` | 注解定义迁移 |
| `module-processor` | `apt-impl/module/` | 处理器迁移 |
| - | `apt-api/entity/` | 新增 @AfEntity |
| - | `apt-impl/entity/` | 新增 EntityMetadataProcessor |

#### 模块依赖关系

```
apt-api (注解定义)
    ↑
apt-impl (APT 处理器)
    ↑
gradle-plugin (自动注册 APT)
    ↑
应用模块 (编译时使用)

data-core (运行时)
    ← 依赖 apt-api (仅注解)
```

#### Gradle 插件自动注册 APT

Gradle 插件会自动将 APT 处理器添加到项目的 `annotationProcessor` 配置中，用户无需手动配置。

**AfgPlugin.kt 配置逻辑：**

```kotlin
private fun configureFrameworkDependencies(project: Project, extension: AfgExtension) {
    val frameworkVersion = extension.frameworkVersion.getOrElse(DEFAULT_FRAMEWORK_VERSION)

    // 自动添加 APT 注解处理器（统一入口）
    // apt-impl 包含所有处理器：模块索引、实体元数据等
    add("annotationProcessor", "io.github.afg-projects:afg-framework-apt-impl:$frameworkVersion")

    // 自动添加 APT 注解依赖（编译时可用）
    add("compileOnly", "io.github.afg-projects:afg-framework-apt-api:$frameworkVersion")

    // 其他依赖配置...
}
```

**用户使用时无需任何配置：**

```kotlin
// build.gradle.kts
plugins {
    id("io.github.afg-projects.framework-plugin")
}

// 无需手动配置 annotationProcessor
// 插件会自动添加：
// - annotationProcessor("io.github.afg-projects:afg-framework-apt-impl")
// - compileOnly("io.github.afg-projects:afg-framework-apt-api")
```

**APT 处理器自动发现机制：**

`apt-impl` 模块的 `META-INF/services/javax.annotation.processing.Processor` 文件：

```
io.github.afgprojects.framework.apt.module.AfgModuleAnnotationProcessor
io.github.afgprojects.framework.apt.entity.EntityMetadataProcessor
```

Gradle 编译时会自动发现并执行所有注册的处理器。

### 二、注解体系

#### @AfEntity 注解

```java
package io.github.afgprojects.framework.apt.entity;

/**
 * 框架实体标记注解
 * 触发 APT 生成元数据类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AfEntity {
}
```

#### 注解组合规则

| 注解组合 | 生成的元数据接口 |
|---------|-----------------|
| `@AfEntity` | `EntityMetadata` |
| `@AfEntity + @Table` | `EntityMetadata + TableAware + IdAware + ColumnNameAware` |
| `@AfEntity + @CacheEntity` | `EntityMetadata + CacheAware` |
| `@AfEntity + @Table + @CacheEntity` | 所有特征接口组合 |

### 二、元数据接口体系

#### 核心元数据接口

```java
package io.github.afgprojects.framework.data.core.metadata;

/**
 * 实体元数据基础接口
 */
public interface EntityMetadata<T> {
    /**
     * 获取实体名称
     */
    String getEntityName();

    /**
     * 获取所有字段元数据
     */
    List<FieldMetadata> getFields();

    /**
     * 根据属性名获取字段元数据
     */
    FieldMetadata getField(String name);
}

/**
 * 字段元数据基础接口
 */
public interface FieldMetadata {
    /**
     * 获取属性名
     */
    String getPropertyName();

    /**
     * 获取字段类型
     */
    Class<?> getFieldType();
}
```

#### 特征接口（可组合）

```java
/**
 * 表名感知特征
 */
public interface TableAware {
    String getTableName();
}

/**
 * 主键感知特征
 */
public interface IdAware {
    FieldMetadata getIdField();
}

/**
 * 列名感知特征（数据库场景）
 */
public interface ColumnNameAware {
    /**
     * 根据属性名获取数据库列名
     */
    String getColumnName(String propertyName);
}

/**
 * 缓存感知特征
 */
public interface CacheAware {
    String getCacheKey(Object entity);
    int getTtl();
}
```

#### 数据库扩展接口

```java
/**
 * 数据库字段元数据
 */
public interface DatabaseFieldMetadata extends FieldMetadata {
    /**
     * 获取数据库列名
     */
    String getColumnName();

    /**
     * 是否为主键字段
     */
    boolean isId();

    /**
     * 是否为自动生成字段
     */
    boolean isGenerated();
}

/**
 * 数据库实体元数据
 */
public interface DatabaseEntityMetadata<T>
    extends EntityMetadata<T>, TableAware, IdAware, ColumnNameAware {

    @Override
    List<DatabaseFieldMetadata> getFields();

    @Override
    DatabaseFieldMetadata getField(String name);
}
```

### 三、FieldNameResolver 组件

```java
package io.github.afgprojects.framework.data.core.naming;

/**
 * 字段名解析器
 * Lambda → 列名转换的统一入口
 */
public class FieldNameResolver {

    private final EntityMetadataCache cache;

    public FieldNameResolver(EntityMetadataCache cache) {
        this.cache = cache;
    }

    /**
     * 解析 Lambda 方法引用对应的数据库列名
     *
     * @param entityClass 实体类
     * @param getter Lambda 方法引用
     * @return 数据库列名
     */
    public <T, R> String resolveColumnName(Class<T> entityClass, SFunction<T, R> getter) {
        // 1. 从 Lambda 提取属性名
        String propertyName = Conditions.getFieldName(getter);

        // 2. 获取实体元数据
        EntityMetadata<?> metadata = cache.get(entityClass);

        // 3. 如果支持列名感知，使用元数据转换
        if (metadata instanceof ColumnNameAware columnNameAware) {
            return columnNameAware.getColumnName(propertyName);
        }

        // 4. 降级：camelCase → snake_case
        return toSnakeCase(propertyName);
    }

    /**
     * camelCase 转 snake_case
     */
    private String toSnakeCase(String propertyName) {
        StringBuilder columnName = new StringBuilder();
        for (int i = 0; i < propertyName.length(); i++) {
            char c = propertyName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                columnName.append('_');
            }
            columnName.append(Character.toLowerCase(c));
        }
        return columnName.toString();
    }
}
```

### 四、EntityMetadataCache 组件

```java
package io.github.afgprojects.framework.data.core.metadata;

/**
 * 实体元数据缓存
 * 优先使用 APT 生成的元数据，降级到运行时反射
 */
public class EntityMetadataCache {

    private final ConcurrentHashMap<Class<?>, EntityMetadata<?>> cache = new ConcurrentHashMap<>();

    /**
     * 获取实体元数据
     */
    @SuppressWarnings("unchecked")
    public <T> EntityMetadata<T> get(Class<T> entityClass) {
        return (EntityMetadata<T>) cache.computeIfAbsent(entityClass, this::resolveMetadata);
    }

    /**
     * 解析实体元数据
     * 1. 尝试加载 APT 生成的元数据类
     * 2. 降级到运行时反射解析
     */
    private <T> EntityMetadata<T> resolveMetadata(Class<T> entityClass) {
        // 1. 尝试加载 APT 生成的元数据类
        EntityMetadata<T> generated = loadGeneratedMetadata(entityClass);
        if (generated != null) {
            return generated;
        }

        // 2. 降级：运行时反射解析
        return ReflectiveEntityMetadata.create(entityClass);
    }

    /**
     * 加载 APT 生成的元数据类
     */
    @SuppressWarnings("unchecked")
    private <T> EntityMetadata<T> loadGeneratedMetadata(Class<T> entityClass) {
        try {
            String metadataClassName = entityClass.getPackageName() + ".metadata." + entityClass.getSimpleName() + "Metadata";
            Class<?> metadataClass = Class.forName(metadataClassName);
            return (EntityMetadata<T>) metadataClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 五、APT 生成示例

#### 输入：实体类

```java
package io.github.afgprojects.modules.system.entity;

@AfEntity
@Table(name = "sys_user")
public class User {
    @Id
    private Long id;

    @Column(name = "is_deleted")
    private Boolean deleted;

    private String userName;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // getter/setter
}
```

#### 输出：生成的元数据类

```java
package io.github.afgprojects.modules.system.entity.metadata;

/**
 * 自动生成，请勿修改
 * @generated by AFG Framework APT
 */
public class UserMetadata implements DatabaseEntityMetadata<User> {

    private static final String TABLE_NAME = "sys_user";

    private static final Map<String, String> COLUMN_MAP = Map.of(
        "id", "id",
        "deleted", "is_deleted",
        "userName", "user_name",
        "createTime", "create_time"
    );

    private static final List<DatabaseFieldMetadata> FIELDS = List.of(
        new IdFieldMetadata(),
        new DeletedFieldMetadata(),
        new UserNameFieldMetadata(),
        new CreateTimeFieldMetadata()
    );

    private static final DatabaseFieldMetadata ID_FIELD = FIELDS.get(0);

    @Override
    public String getEntityName() {
        return "User";
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getColumnName(String propertyName) {
        return COLUMN_MAP.getOrDefault(propertyName, propertyName);
    }

    @Override
    public List<DatabaseFieldMetadata> getFields() {
        return FIELDS;
    }

    @Override
    public DatabaseFieldMetadata getIdField() {
        return ID_FIELD;
    }

    @Override
    public DatabaseFieldMetadata getField(String name) {
        return FIELDS.stream()
            .filter(f -> f.getPropertyName().equals(name))
            .findFirst()
            .orElse(null);
    }

    // 内部字段元数据实现
    private static class IdFieldMetadata implements DatabaseFieldMetadata {
        @Override public String getPropertyName() { return "id"; }
        @Override public Class<?> getFieldType() { return Long.class; }
        @Override public String getColumnName() { return "id"; }
        @Override public boolean isId() { return true; }
        @Override public boolean isGenerated() { return true; }
    }

    private static class DeletedFieldMetadata implements DatabaseFieldMetadata {
        @Override public String getPropertyName() { return "deleted"; }
        @Override public Class<?> getFieldType() { return Boolean.class; }
        @Override public String getColumnName() { return "is_deleted"; }
        @Override public boolean isId() { return false; }
        @Override public boolean isGenerated() { return false; }
    }

    // ... 其他字段
}
```

### 六、TypedConditionBuilder 集成

```java
// data-jdbc 模块

public class DefaultTypedConditionBuilder<T> implements TypedConditionBuilder<T> {

    private final Class<T> entityClass;
    private final FieldNameResolver fieldNameResolver;
    private final DefaultConditionBuilder delegate = new DefaultConditionBuilder();

    @Override
    public <R> TypedConditionBuilder<T> eq(SFunction<T, R> getter, @Nullable Object value) {
        // 使用 FieldNameResolver 解析列名
        String columnName = fieldNameResolver.resolveColumnName(entityClass, getter);
        delegate.eq(columnName, value);
        return this;
    }

    // 其他方法类似...
}
```

### 七、组件位置

| 组件 | 模块 | 包路径 |
|------|------|--------|
| `@AfEntity` | apt-api | `io.github.afgprojects.framework.apt.entity` |
| `@AfgModuleAnnotation` | apt-api | `io.github.afgprojects.framework.apt.module` |
| `EntityMetadata` | data-core | `io.github.afgprojects.framework.data.core.metadata` |
| `FieldMetadata` | data-core | `io.github.afgprojects.framework.data.core.metadata` |
| `TableAware` 等特征接口 | data-core | `io.github.afgprojects.framework.data.core.metadata` |
| `DatabaseFieldMetadata` | data-core | `io.github.afgprojects.framework.data.core.metadata` |
| `FieldNameResolver` | data-core | `io.github.afgprojects.framework.data.core.naming` |
| `EntityMetadataCache` | data-core | `io.github.afgprojects.framework.data.core.metadata` |
| `EntityMetadataProcessor` | apt-impl | `io.github.afgprojects.framework.apt.entity` |
| `AfgModuleAnnotationProcessor` | apt-impl | `io.github.afgprojects.framework.apt.module` |
| APT 自动注册 | gradle-plugin | `AfgPlugin.configureFrameworkDependencies()` |
| 生成的元数据类 | 编译输出 | `{package}.metadata.{Entity}Metadata` |

## 集成测试场景

### 核心场景

| 场景 | 输入 | 预期输出 |
|------|------|----------|
| 普通字段转换 | `userName` | `user_name` |
| Boolean + @Column | `deleted` | `is_deleted` |
| 无注解 Boolean | `deleted` | `deleted`（降级 snake_case） |
| Lambda 条件构建 | `Conditions.builder(User.class).eq(User::getDeleted, true)` | Condition 存储 `is_deleted` |
| SQL 转换完整链路 | Condition | `WHERE is_deleted = ?` |

### 边界场景

| 场景 | 测试内容 |
|------|----------|
| 嵌套条件 | AND/OR 组合中的字段名转换 |
| 实体继承 | BaseEntity 字段正确映射到子类 |
| 字段名与列名相同 | `id` → `id`，无需转换 |
| 特殊字符字段名 | 下划线开头、数字结尾等边界情况 |
| 多表关联 | JOIN 查询中的字段名前缀处理 |

### 性能场景

| 场景 | 指标 |
|------|------|
| 单次转换耗时 | < 1ms |
| 100 次转换耗时 | < 10ms |
| 元数据缓存命中率 | > 99% |
| APT 生成 vs 反射解析 | 性能提升 > 10x |

## 实施计划

### 阶段零：APT 模块整合

1. 创建 `apt-api` 模块，迁移 `module-api` 中的注解
2. 创建 `apt-impl` 模块，迁移 `module-processor` 中的处理器
3. 更新 `settings.gradle.kts` 和依赖关系
4. 更新 Gradle 插件 `AfgPlugin.kt`：
   - 替换 `module-processor` 为 `apt-impl`
   - 添加 `apt-api` 作为 `compileOnly` 依赖
5. 配置 `apt-impl` 的 `META-INF/services/javax.annotation.processing.Processor`
6. 验证现有模块索引功能正常
7. 标记 `module-api` 和 `module-processor` 为废弃

### 阶段一：核心接口和运行时实现

1. 在 data-core 模块新增元数据接口（EntityMetadata、FieldMetadata、特征接口）
2. 实现 FieldNameResolver 和 EntityMetadataCache
3. 在 data-jdbc 模块实现 ReflectiveEntityMetadata（运行时反射）
4. 修改 TypedConditionBuilder 集成 FieldNameResolver
5. 编写集成测试

### 阶段二：APT 编译时生成

1. 在 apt-impl 模块新增 EntityMetadataProcessor
2. 实现 `@AfEntity` 注解处理逻辑
3. 生成元数据类代码
4. EntityMetadataCache 优先加载 APT 生成的类
5. 性能测试对比

### 阶段三：清理和优化

1. 移除 ConditionToSqlConverter 中的字段名转换逻辑
2. 移除 EntityQueryHelper 中重复的转换方法
3. 删除废弃的 `module-api` 和 `module-processor` 模块
4. 更新文档和示例代码

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| APT 兼容性问题 | 编译失败 | 提供运行时反射降级方案 |
| 现有代码兼容性 | 破坏性变更 | 保持 TypedConditionBuilder 接口不变 |
| 性能回退 | 运行时反射开销 | APT 生成优先，缓存机制 |

## 参考资料

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Jakarta Persistence API](https://jakarta.ee/specifications/persistence/)
- [Java Annotation Processing](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Processor.html)

---

## 使用方式

### 1. 项目配置

#### 1.1 使用 Gradle 插件（推荐）

在 `build.gradle.kts` 中应用 AFG Framework 插件，APT 会自动配置：

```kotlin
plugins {
    id("io.github.afg-projects.framework-plugin")
}

afg {
    moduleType.set("data")  // 数据模块会自动添加 JPA 依赖
}
```

插件会自动添加：
- `annotationProcessor("io.github.afg-projects:afg-framework-apt-impl")`
- `compileOnly("io.github.afg-projects:afg-framework-apt-api")`

#### 1.2 手动配置（不使用插件）

```kotlin
dependencies {
    // APT 注解
    compileOnly("io.github.afg-projects:afg-framework-apt-api:1.0.0-SNAPSHOT")

    // APT 处理器
    annotationProcessor("io.github.afg-projects:afg-framework-apt-impl:1.0.0-SNAPSHOT")

    // 运行时依赖
    implementation("io.github.afg-projects:afg-framework-data-jdbc:1.0.0-SNAPSHOT")

    // JPA 注解（可选，用于 @Table、@Column 等）
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
}
```

### 2. 定义实体类

#### 2.1 基本实体

```java
import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;

@AfEntity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    private String email;

    @Column(name = "is_deleted")
    private Boolean deleted;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // getter/setter
    public Long getId() { return id; }
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public Boolean getDeleted() { return deleted; }
    public LocalDateTime getCreateTime() { return createTime; }

    public void setId(Long id) { this.id = id; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setEmail(String email) { this.email = email; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

#### 2.2 关键规则

| 规则 | 说明 |
|------|------|
| `@AfEntity` | 必须添加，触发 APT 生成元数据 |
| `@Table(name=)` | 可选，指定表名，不指定则使用类名转 snake_case |
| `@Column(name=)` | 可选，指定列名，不指定则使用属性名转 snake_case |
| `@Id` | 可选，标记主键，不指定则默认 `id` 字段 |
| Boolean 字段 | 遵循阿里规约：属性名不加 `is` 前缀，通过 `@Column(name="is_xxx")` 指定列名 |

### 3. 使用 Lambda 条件构建

#### 3.1 基本用法

```java
import io.github.afgprojects.framework.data.core.condition.Conditions;
import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

// 创建条件构建器
Condition condition = Conditions.builder(User.class)
    .eq(User::getDeleted, false)        // deleted = false → is_deleted = false
    .like(User::getUserName, "admin")    // userName LIKE '%admin%' → user_name LIKE '%admin%'
    .isNotNull(User::getEmail)           // email IS NOT NULL
    .build();

// 使用静态方法
Condition condition = eq(User.class, User::getDeleted, false);
```

#### 3.2 复杂条件

```java
// 多条件组合
Condition condition = Conditions.builder(User.class)
    .eq(User::getDeleted, false)
    .gt(User::getCreateTime, LocalDateTime.now().minusDays(7))
    .in(User::getUserName, List.of("admin", "root", "system"))
    .build();

// 嵌套条件
Condition nested = Conditions.builder(User.class)
    .eq(User::getDeleted, false)
    .build();

Condition condition = Conditions.builder(User.class)
    .like(User::getUserName, "admin")
    .and(nested)
    .build();

// OR 条件
Condition condition = Conditions.builder(User.class)
    .eq(User::getDeleted, false)
    .or(Conditions.builder(User.class)
        .isNull(User::getDeleted)
        .build())
    .build();
```

#### 3.3 字段名转换规则

| Java 属性名 | 数据库列名 | 说明 |
|-------------|------------|------|
| `id` | `id` | 无需转换 |
| `userName` | `user_name` | camelCase → snake_case |
| `createTime` | `create_time` | camelCase → snake_case |
| `deleted` | `is_deleted` | 通过 `@Column(name="is_deleted")` 指定 |

### 4. 生成的元数据类

#### 4.1 自动生成的代码

编译后，APT 会在 `{package}.metadata` 包下生成元数据类：

```java
package io.github.afgprojects.modules.system.entity.metadata;

public class UserMetadata implements DatabaseEntityMetadata<User> {

    private static final String TABLE_NAME = "sys_user";

    private static final Map<String, String> COLUMN_MAP = Map.of(
        "id", "id",
        "userName", "user_name",
        "email", "email",
        "deleted", "is_deleted",
        "createTime", "create_time"
    );

    @Override
    public String getTableName() { return TABLE_NAME; }

    @Override
    public String getColumnName(String propertyName) {
        return COLUMN_MAP.getOrDefault(propertyName, propertyName);
    }

    // ... 其他方法
}
```

#### 4.2 直接使用元数据

```java
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.metadata.DatabaseEntityMetadata;

// 获取元数据
DatabaseEntityMetadata<User> metadata = EntityMetadataCache.getInstance()
    .get(User.class, DatabaseEntityMetadata.class);

// 获取表名
String tableName = metadata.getTableName();  // "sys_user"

// 获取列名
String columnName = metadata.getColumnName("deleted");  // "is_deleted"

// 获取主键字段
DatabaseFieldMetadata idField = metadata.getIdField();
```

### 5. 与 DataManager 集成

```java
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;

@Service
public class UserService {

    private final DataManager dataManager;

    public UserService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    // 使用 Lambda 条件查询
    public List<User> findActiveUsers() {
        Condition condition = Conditions.builder(User.class)
            .eq(User::getDeleted, false)
            .build();

        return dataManager.entity(User.class)
            .query()
            .where(condition)
            .list();
    }

    // 使用 Lambda 条件更新
    @Transactional
    public void softDelete(Long userId) {
        User user = dataManager.findById(User.class, userId).orElseThrow();
        user.setDeleted(true);
        dataManager.save(User.class, user);
    }
}
```

### 6. 性能优化

#### 6.1 APT vs 反射

| 场景 | APT 生成 | 反射解析 |
|------|----------|----------|
| 首次访问 | ~0.1ms | ~10ms |
| 后续访问 | ~0.001ms（缓存） | ~0.001ms（缓存） |
| 内存占用 | 低（静态代码） | 中（反射元数据） |

#### 6.2 降级策略

```
APT 生成的元数据类（最优）
    ↓ 不存在
ReflectiveEntityMetadata（降级）
    ↓ 类不可用
EmptyEntityMetadata（最终降级，仅 snake_case 转换）
```

### 7. 常见问题

#### Q1: 为什么 Boolean 字段不自动添加 `is_` 前缀？

遵循阿里规约，Boolean 属性名不加 `is` 前缀，需要通过 `@Column(name="is_xxx")` 显式指定列名。这样设计的原因：
1. 避免序列化框架（Jackson、Fastjson）的兼容性问题
2. 保持属性名语义清晰
3. 数据库列名可灵活配置

#### Q2: 如何处理继承的实体？

APT 会自动扫描父类字段：

```java
@AfEntity
@Table(name = "sys_user")
public class User extends BaseEntity<Long> {
    // BaseEntity 中的 id、createTime、updateTime 字段会被自动包含
    private String userName;
}

@MappedSuperclass
public abstract class BaseEntity<ID> {
    @Id
    private ID id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### Q3: 如何禁用 APT 生成？

如果不需要 APT 生成，可以：
1. 不添加 `@AfEntity` 注解
2. 系统会自动降级到反射解析

#### Q4: 如何自定义命名策略？

当前版本暂不支持自定义命名策略，计划后续版本支持：

```java
// 计划支持（未实现）
@AfEntity(naming = CustomNamingStrategy.class)
public class User { }
```
