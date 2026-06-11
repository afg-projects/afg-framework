# 数据库迁移规范

> PRD 来源：§5.4 DataManager 数据访问模块（数据库迁移部分）
> CLAUDE.md 来源：数据库迁移章节

---

## 概述

AFG 框架使用 **Liquibase** 管理数据库迁移，迁移脚本使用 **XML 格式**。框架通过模块 jar 包提供内置迁移，业务应用通过 `classpath:` 引用。

核心原则：
1. **增量必须新增** — 禁止修改已执行的 changeSet
2. **changeSet id 全局唯一** — 格式 `v{版本}-{序号}-{表名}[-{操作}]`
3. **每个文件一个 changeSet** — 禁止在一个文件中定义多个 changeSet
4. **模块独立目录** — 不同模块的迁移文件放在各自目录下

---

## 框架内置迁移

框架模块 jar 包内置迁移脚本，业务应用通过 `classpath:` 引用：

| 模块 | 引用路径 | changeSet 数量 |
|------|---------|---------------|
| auth-server | `classpath:db/changelog/auth-server-changelog.xml` | 22 个（auth 11 + permission 7 + datascope 4） |
| ai-core | `classpath:db/changelog/ai-changelog.xml` | 18 个（ai/v1.0.0/001~018） |

---

## 目录结构

```
src/main/resources/db/
├── changelog.xml                              # 主 changelog（引用所有版本和框架模块）
└── changelog/
    ├── {module}/                              # 模块目录
    │   └── v1.0.0/                            # 版本目录
    │       ├── 001_sys_user.xml               # 序号 + 表名
    │       └── 002_sys_role.xml
    └── hotfix/                                # 紧急修复
        └── 20240115_fix_user_index.xml
```

### 目录命名规则

- 模块目录：与业务模块名一致（如 `platform/`、`auth/`、`ai/`）
- 版本目录：`v{major}.{minor}.{patch}` 格式（如 `v1.0.0/`）
- 文件名：`{序号}_{表名}.xml`（如 `001_sys_user.xml`）
- 紧急修复：`hotfix/` 目录，文件名 `YYYYMMDD_{描述}.xml`

---

## 主 Changelog 配置

### 引用框架模块迁移

```xml
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">
    <!-- 业务模块 -->
    <include file="platform/changelog.xml" relativeToChangelogFile="true"/>
    <!-- 框架模块（classpath 引用） -->
    <include file="classpath:db/changelog/auth-server-changelog.xml"/>
    <include file="classpath:db/changelog/ai-changelog.xml"/>
</databaseChangeLog>
```

### 引用规则

- 业务模块迁移：使用 `relativeToChangelogFile="true"` 相对路径引用
- 框架模块迁移：使用 `classpath:` 前缀引用框架 jar 包内置迁移
- 新增文件追加到主 changelog **末尾**，保持顺序

---

## ChangeSet 文件格式

### 完整模板

```xml
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">
    <changeSet id="v1.0.0-001-sys-user" author="afg">
        <createTable tableName="sys_user" remarks="用户表">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" primaryKeyName="pk_sys_user"/>
            </column>
            <column name="username" type="VARCHAR(50)" remarks="用户名">
                <constraints nullable="false" unique="true" uniqueConstraintName="uk_user_username"/>
            </column>
            <column name="status" type="INT" remarks="状态">
                <constraints nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(64)" remarks="租户ID"/>
            <column name="deleted" type="BOOLEAN" defaultValueBoolean="false" remarks="是否删除">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="INT" defaultValueNumeric="0" remarks="乐观锁版本">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" remarks="创建时间">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP" remarks="更新时间">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

### 常见列模式

| 列类型 | XML 定义 | 说明 |
|--------|---------|------|
| 主键 | `<column name="id" type="BIGINT" autoIncrement="true"><constraints primaryKey="true" primaryKeyName="pk_{table}"/></column>` | 自增主键 |
| 外键列 | `<column name="{ref}_id" type="BIGINT"/>` | 外键字段 |
| 租户 | `<column name="tenant_id" type="VARCHAR(64)"/>` | 多租户字段 |
| 软删除 | `<column name="deleted" type="BOOLEAN" defaultValueBoolean="false"><constraints nullable="false"/></column>` | BOOLEAN 策略 |
| 时间戳软删除 | `<column name="deleted_at" type="TIMESTAMP"/>` | TIMESTAMP 策略 |
| 乐观锁 | `<column name="version" type="INT" defaultValueNumeric="0"><constraints nullable="false"/></column>` | 乐观锁版本 |
| 创建时间 | `<column name="created_at" type="TIMESTAMP"><constraints nullable="false"/></column>` | — |
| 更新时间 | `<column name="updated_at" type="TIMESTAMP"><constraints nullable="false"/></column>` | — |
| 审计字段 | `<column name="create_by" type="BIGINT"/>` + `<column name="update_by" type="BIGINT"/>` | 创建/更新者 |
| 枚举 | `<column name="status" type="VARCHAR(32)"/>` | 字符串存储枚举 |
| JSON | `<column name="metadata" type="JSON"/>` 或 `<column name="metadata" type="TEXT"/>` | JSON 数据 |
| 大文本 | `<column name="content" type="TEXT"/>` | — |
| 金额 | `<column name="amount" type="DECIMAL(19,2)"/>` | 精确小数 |

---

## 命名规范

### ChangeSet ID

格式：`v{版本}-{序号}-{表名}[-{操作}]`

| 场景 | 格式 | 示例 |
|------|------|------|
| 建表 | `v{版本}-{序号}-{表名}` | `v1.0.0-001-sys-user` |
| 加列 | `v{版本}-{序号}-{表名}-add-{列名}` | `v1.0.0-002-sys-user-add-email` |
| 改列 | `v{版本}-{序号}-{表名}-alter-{列名}` | `v1.0.0-003-sys-user-alter-username` |
| 加索引 | `v{版本}-{序号}-{表名}-add-index` | `v1.0.0-004-sys-user-add-index` |
| 删列 | `v{版本}-{序号}-{表名}-drop-{列名}` | `v1.0.0-005-sys-user-drop-nickname` |

### Author

统一使用 `afg`，禁止使用个人名称。

### 约束命名

| 约束类型 | 格式 | 示例 |
|---------|------|------|
| 主键 | `pk_{表名}` | `pk_sys_user` |
| 唯一约束 | `uk_{表名}_{字段名}` | `uk_user_username` |
| 普通索引 | `idx_{表名}_{字段名}` | `idx_user_tenant` |
| 外键约束 | `fk_{表名}_{关联表}` | `fk_user_dept` |

### 表名和列名

- 表名：snake_case，模块前缀（如 `sys_user`、`ai_model_config`）
- 列名：snake_case（如 `created_at`、`tenant_id`）
- 框架通过 `NamingUtils.toSnakeCase()` 自动转换 Java camelCase 到 DB snake_case

---

## 铁律

### 1. 禁止修改已执行的 changeSet

已执行的 changeSet 不可修改，增量必须通过新增 changeSet 实现。修改已执行的 changeSet 会导致 Liquibase 校验失败。

### 2. changeSet id 全局唯一

格式 `v{版本}-{序号}-{表名}[-{操作}]`，id 重复会导致 Liquibase 执行异常。

### 3. author 统一 afg

所有 changeSet 的 author 必须为 `afg`，禁止使用个人名称。

### 4. 每个文件一个 changeSet

一个 XML 文件只包含一个 `<changeSet>` 元素，禁止在一个文件中定义多个 changeSet。

### 5. 新增文件追加到主 changelog 末尾

新增的迁移文件引用必须追加到主 changelog.xml 的末尾，保持执行顺序。

### 6. 模块独立目录

不同模块的迁移文件放在各自目录下（`platform/`、`auth/`、`ai/`），禁止跨模块混放。

### 7. 分支开发使用独立序号区间

多人并行开发时，不同分支使用独立序号区间避免合并冲突：
- feature-a: 100+（如 `v1.0.0-101-sys-user-add-phone`）
- feature-b: 200+（如 `v1.0.0-201-sys-user-add-avatar`）

### 8. XML 命名空间必须完整

Liquibase 5.x 要求 `xsi:schemaLocation` 完整声明，缺少会导致 XML 解析失败：

```xml
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">
```

### 9. 合并前检查序号和 id 冲突

分支合并前必须检查 changeSet 序号和 id 是否冲突，避免合并后 Liquibase 执行异常。

---

## 常见操作示例

### 创建表

```xml
<changeSet id="v1.0.0-001-sys-role" author="afg">
    <createTable tableName="sys_role" remarks="角色表">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" primaryKeyName="pk_sys_role"/>
        </column>
        <column name="role_name" type="VARCHAR(50)" remarks="角色名称">
            <constraints nullable="false"/>
        </column>
        <column name="role_code" type="VARCHAR(50)" remarks="角色编码">
            <constraints nullable="false" unique="true" uniqueConstraintName="uk_role_code"/>
        </column>
        <column name="deleted" type="BOOLEAN" defaultValueBoolean="false">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

### 添加列

```xml
<changeSet id="v1.0.0-002-sys-user-add-email" author="afg">
    <addColumn tableName="sys_user">
        <column name="email" type="VARCHAR(100)" remarks="邮箱地址"/>
    </addColumn>
</changeSet>
```

### 添加索引

```xml
<changeSet id="v1.0.0-003-sys-user-add-index" author="afg">
    <createIndex tableName="sys_user" indexName="idx_user_tenant">
        <column name="tenant_id"/>
    </createIndex>
</changeSet>
```

### 添加外键

```xml
<changeSet id="v1.0.0-004-sys-user-add-fk-dept" author="afg">
    <addForeignKeyConstraint
        baseTableName="sys_user"
        baseColumnNames="dept_id"
        constraintName="fk_user_dept"
        referencedTableName="sys_dept"
        referencedColumnNames="id"/>
</changeSet>
```

### 修改列类型

```xml
<changeSet id="v1.0.0-005-sys-user-alter-username" author="afg">
    <modifyDataType tableName="sys_user" columnName="username" newDataType="VARCHAR(100)"/>
</changeSet>
```

### 添加唯一约束（软删除实体）

```xml
<!-- 软删除实体的唯一约束必须包含 deleted 字段 -->
<changeSet id="v1.0.0-006-sys-user-add-uk-username" author="afg">
    <addUniqueConstraint
        tableName="sys_user"
        columnNames="username,deleted"
        constraintName="uk_user_username_deleted"/>
</changeSet>
```

### 插入初始数据

```xml
<changeSet id="v1.0.0-007-sys-role-init-data" author="afg">
    <insert tableName="sys_role">
        <column name="role_name" value="管理员"/>
        <column name="role_code" value="ADMIN"/>
        <column name="deleted" valueBoolean="false"/>
        <column name="created_at" valueDate="now()"/>
        <column name="updated_at" valueDate="now()"/>
    </insert>
</changeSet>
```

---

## Gradle 任务

框架提供以下 Gradle 任务辅助数据库迁移：

| 任务 | 说明 |
|------|------|
| `generateMigration` | 从实体类生成迁移脚本（实体 → DDL） |
| `dbMigrate` | 执行数据库迁移 |
| `generateEntity` | 生成实体类 |
| `generateEntityFromDb` | 从数据库反向生成实体（DB → Java） |

### 使用示例

```bash
# 从实体生成迁移脚本
./gradlew generateMigration

# 执行数据库迁移
./gradlew dbMigrate

# 从数据库反向生成实体
./gradlew generateEntityFromDb
```

---

## Schema 对比

框架提供 `SchemaComparator` 进行三方差异对比：

- **实体定义**（Java 类 + 注解）
- **数据库现状**（实际 Schema）
- **基线**（Liquibase 已执行的 changeSet）

> `SchemaComparator` 需要数据库连接才能执行三方对比。

---

## AutoConfiguration

框架通过 `LiquibaseAutoConfiguration` 自动集成 Liquibase，引入 `afg-framework-data-liquibase` 后自动执行迁移。

### 依赖链

```
DataSourceAutoConfiguration (Spring Boot)
  └→ DataSourceTransactionManagerAutoConfiguration (Spring Boot)
       └→ TransactionAutoConfiguration (data-core) — TransactionAdapter
       └→ TenantContextAutoConfiguration (data-core) — TenantContextHolder
            └→ DataManagerAutoConfiguration (data-jdbc) — JdbcDataManager
                 └→ LiquibaseAutoConfiguration (data-liquibase) — SpringLiquibase
```

---

## 常见错误

### 修改已执行的 changeSet

**症状**：Liquibase 启动报 `Validation Failed` 错误

**原因**：修改了已执行的 changeSet 的 id、author 或内容

**解决**：回退修改，新增 changeSet 实现增量变更

**预防**：已执行的 changeSet 不可修改，增量必须新增

### changeSet id 冲突

**症状**：Liquibase 报 `ChangeSet id already exists` 错误

**原因**：不同分支使用了相同的 changeSet id

**解决**：修改冲突的 id，使用独立序号区间

**预防**：分支开发使用独立序号区间（feature-a: 100+, feature-b: 200+），合并前检查 id 冲突

### 缺少 XML 命名空间

**症状**：Liquibase XML 解析失败

**原因**：`xsi:schemaLocation` 声明不完整

**解决**：补全 XML 命名空间声明

**预防**：从模板复制完整的 XML 头部，Liquibase 5.x 要求 `xsi:schemaLocation` 完整

### 软删除实体唯一约束遗漏 deleted 字段

**症状**：软删除后重新创建同名记录时唯一约束报错

**原因**：唯一约束未包含 `deleted` 字段

**解决**：删除旧唯一约束，新增包含 `deleted` 字段的唯一约束

**预防**：软删除实体的唯一约束必须包含 `deleted` 字段（BOOLEAN 策略）或 `deleted_at` 字段（TIMESTAMP 策略）