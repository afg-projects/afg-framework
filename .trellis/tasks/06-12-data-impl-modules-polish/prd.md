# data-sql + data-jdbc + data-liquibase 模块精细化打磨

## Goal

对 data-impl 三个子模块执行 PRD 阶段 4-6 的精细化打磨，聚焦 AutoConfiguration 规范整改、AUDITABLE/ENCRYPTED/TREEABLE trait 运行时实现、代码质量修复（IllegalArgumentException→BusinessException）、duplicate Properties 清理、Javadoc 补充。

## What I already know

### data-sql 审计结果
- 无 AutoConfiguration（纯工具模块）→ 无 on/off 规范问题
- ConditionToSqlConverter 无 NoOp → 但它是必需组件，NoOp 无意义（SQL 转换不能跳过）
- MySQLDialect 硬编码为默认 → 改为更合理的做法
- 14 个公共构造器缺少 Javadoc
- LIKE escape 字符 '!' 硬编码 → 应从 Dialect 获取
- Long.MIN_VALUE 魔术数字 → 应提取为命名常量
- DataScopeContextProvider.empty() 返回 () -> null → 应返回 () -> empty context

### data-jdbc 审计结果
- EntityCacheAutoConfiguration 缺少 @AutoConfigureAfter → 必须修复
- SqlMetricsAutoConfiguration 缺少 @AutoConfigureAfter → 必须修复
- AUDITABLE trait (createBy/updateBy) 自动填充未实现 → 必须实现
- @EncryptedField 加密/解密未实现 → 必须实现（EntityInsertHandler加密 + EntityMapper解密）
- TreeEntity path 自动计算未实现 → 必须实现
- Duplicate Properties classes → 清理旧的，保留 properties/ 子包版本
- 8 处 IllegalArgumentException → BusinessException
- ReflectiveFieldMetadata 3 处缺少 @Override

### data-liquibase 审计结果
- AutoConfiguration 规范合规 ✅
- LiquibaseAutoConfiguration 使用 @AutoConfiguration + afterName + @ConditionalOnProperty ✅
- 测试覆盖率不足（JdbcSchemaReader/ChangeLogSchemaReader/LiquibaseMigrationRunner 无测试）→ 本阶段不做完整补充，后续阶段处理
- EntityCodeGenerator TINYINT→Boolean 映射 bug → 修复

## Requirements

### T4.1: data-jdbc AutoConfiguration 规范整改

1. **EntityCacheAutoConfiguration**：添加 `@AutoConfiguration(after = DataManagerAutoConfiguration.class)` + 跨模块 afterName
2. **SqlMetricsAutoConfiguration**：添加 `@AutoConfiguration(after = DataManagerAutoConfiguration.class)` + 跨模块 afterName

### T4.2: AUDITABLE trait 自动填充实现

1. **EntityInsertHandler**：检测 `EntityTrait.AUDITABLE`，从 SecurityContext 获取当前用户 ID，填充 createBy + updateBy
2. **EntityUpdateHandler**：检测 `EntityTrait.AUDITABLE`，填充 updateBy
3. 注入方式：AutoConfiguration 中注入 `@Nullable AuditableContext`（SPI 接口），实现类通过 setter 覆盖

**AuditableContext SPI 接口**（data-core 层）：
```java
public interface AuditableContext {
    @Nullable String getCurrentUserId();
}
```

**DefaultAuditableContext**：从 Spring Security Authentication 获取当前用户
**NoOpAuditableContext**：返回 null（降级）

### T4.3: @EncryptedField 加密/解密实现

1. **EntityInsertHandler**：检测 `EntityTrait.ENCRYPTED`，对加密字段调用 `FieldEncryptor.encrypt()` 后再 INSERT
2. **EntityUpdateHandler**：同上，加密后 UPDATE
3. **EntityMapper**：afterLoad 后检测 `EntityTrait.ENCRYPTED`，对加密字段调用 `FieldEncryptor.decrypt()`
4. AutoConfiguration 注入 `@Nullable FieldEncryptor`（已有 NoOpFieldEncryptor 降级）

### T4.4: TreeEntity path 自动计算

1. **EntityInsertHandler**：检测 `EntityTrait.TREEABLE`，根据 parentId 计算 level 和 path
   - parentId=null → level=1, path="/"
   - parentId!=null → 查询父节点，level=parent.level+1, path=parent.path+parent.id+"/"
2. **EntityUpdateHandler**：parentId 变化时重新计算 level 和 path

**TreePathCalculator** 辅助类：计算 level + path，供 handler 调用

### T4.5: Duplicate Properties 清理

删除旧位置的 Properties 类：
- `cache/EntityCacheProperties.java` → 已有 `properties/cache/EntityCacheProperties.java`
- `metrics/SqlMetricsProperties.java` → 已有 `properties/metrics/SqlMetricsProperties.java`

更新 AutoConfiguration import 到新位置。

### T4.6: IllegalArgumentException → BusinessException

替换 8 处 IllegalArgumentException：
- EntityInsertHandler: batchSize 校验
- EntityConditionalHandler: 无效字段名
- JdbcEntityQuery: 无效字段名(2处)、关联名不存在
- JdbcEntityProxy: batchSize 校验、实体类不存在、实体无 ID

根据语义选择 CommonErrorCode：
- 参数校验 → PARAM_ERROR (10002)
- 实体/字段不存在 → ENTITY_NOT_FOUND (11000) / FIELD_NOT_FOUND (11002)

### T4.7: data-sql 代码质量修复

1. **MySQLDialect 硬编码**：4 个 builder 无参构造器 → 添加 Javadoc 注释说明默认值，或移除无参构造器
2. **LIKE escape 字符**：从 Dialect 获取 escape 字符（添加 `getLikeEscapeCharacter()` 方法）
3. **Long.MIN_VALUE 魔术数字**：提取为 `DataScopeUserContext.IMPOSSIBLE_MATCH_ID` 命名常量
4. **DataScopeContextProvider.empty()**：改为返回 `() -> DataScopeUserContext.empty()`
5. **Javadoc 补充**：公共构造器补充 Javadoc

### T4.8: data-liquibase 小修复

1. **EntityCodeGenerator TINYINT bug**：修复 unreachable TINYINT→Boolean 分支
2. **LiquibaseAutoConfiguration afterName**：添加 DataManagerAutoConfiguration afterName（确保 DataManager 在 Liquibase 之前配置）

### T4.9: ReflectiveFieldMetadata @Override 补充

添加 3 处缺少的 @Override 注解。

## Acceptance Criteria

- [ ] EntityCacheAutoConfiguration + SqlMetricsAutoConfiguration 声明 @AutoConfigureAfter
- [ ] AUDITABLE trait 自动填充（createBy/updateBy）在 insert/update handler 中实现
- [ ] AuditableContext SPI + DefaultAuditableContext + NoOpAuditableContext 实现
- [ ] @EncryptedField 加密/解密在 insert/update/mapper handler 中实现
- [ ] TreeEntity path 自动计算在 insert/update handler 中实现
- [ ] Duplicate Properties 清理完成
- [ ] 8 处 IllegalArgumentException 替换为 BusinessException
- [ ] data-sql LIKE escape 从 Dialect 获取
- [ ] Long.MIN_VALUE 提取为命名常量
- [ ] DataScopeContextProvider.empty() 返回 empty context
- [ ] EntityCodeGenerator TINYINT bug 修复
- [ ] ReflectiveFieldMetadata @Override 补充
- [ ] 全量构建 BUILD SUCCESSFUL

## Out of Scope

- data-liquibase SPI 结构重构（MigrationService DI化）
- data-liquibase JdbcSchemaReader/ChangeLogSchemaReader/LiquibaseMigrationRunner 测试补充
- data-liquibase ChangeLogGenerator.generateIncremental() unused module 参数
- data-jdbc handler 类直接单元测试（通过 JdbcDataManager 集成测试间接覆盖）
- ConditionToSqlConverter NoOp（SQL 转换不能 NoOp，必须执行）