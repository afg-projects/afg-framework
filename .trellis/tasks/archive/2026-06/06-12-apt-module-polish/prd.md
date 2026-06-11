# APT 模块精细化打磨 — 注解整改 + 新增 + 测试补充

## Goal

按 PRD 阶段 2 对 APT 模块（apt-api + apt-impl）进行精细化整改，确保每个注解有完整 Javadoc、每个 Processor 有编译期校验、新增 @AfgEnum 和 @EncryptedField 注解、补充缺失的测试覆盖。

## What I already know

### apt-api 模块现有注解（8 个）
- `@AfEntity`（TYPE）— 触发 APT 元数据生成，属性：tableName、generateRelations
- `@CommonFieldDefinition`（FIELD/TYPE）— 可复用字段元数据，6 个属性
- `@CommonFieldDefinitions`（TYPE）— 容器注解
- `@AfService`（TYPE）— 动态可调用服务标记，5 个属性
- `@AfOperation`（METHOD）— 操作标记，11 个属性
- `@AfParam`（PARAMETER）— 参数元数据，5 个属性
- `@AfResult`（METHOD）— 返回值元数据，3 个属性
- `@AfgModuleAnnotation`（TYPE）— 模块注册（@Configuration + @ComponentScan），9 个属性

### apt-impl 模块现有 Processor（3 个）
- `EntityMetadataProcessor` — 处理 @AfEntity，生成 XxxMetadata
- `ServiceMetadataProcessor` — 处理 @AfService，生成 XxxServiceMetadata + service-metadata.index
- `AfgModuleAnnotationProcessor` — 处理 @AfgModuleAnnotation，生成 afg-modules.index

### 现有编译期校验（5 条）
1. 重复操作名校验（@AfService 内 @AfOperation name 重复 → ERROR）
2. SQL 标识符校验（表名/列名格式 → ERROR）
3. 通用字段优先级校验（框架字段不可覆盖 → WARNING）
4. 配置文件字段重复校验（→ WARNING）
5. 注解字段重复校验（→ WARNING）

### 缺失的校验
- @AfEntity 类缺少 @Table → 无校验
- @AfEntity 类缺少主键字段 → 无校验
- @AfEntity 类非 public → 无校验
- @AfOperation 方法签名校验 → 无
- 表名冲突检测 → 无

### 测试覆盖现状
- EntityMetadataProcessorTest: 24 个测试（良好覆盖）
- ServiceMetadataProcessorTest: 10 个测试（良好覆盖）
- AfgModuleAnnotationProcessorTest: **缺失**
- apt-api 模块: **零测试**

### @AfgEnum 和 @EncryptedField
- 两个注解在代码库中**完全不存在**
- PRD 定义了 @AfgEnum（valueField/labelField/i18nPrefix）和 @EncryptedField（algorithm/keyRef）

## Decisions (confirmed)

1. **整改而非重写**：基于现有代码按 PRD 标准整改
2. **先整改后新增**：先完善现有注解/Processor 的质量，再新增功能
3. **编译期校验必须实现**：5 条缺失校验规则全部实现
4. **@AfgEnum 放在 apt-api 的 entity 子包**：与 @AfEntity 同包，因为都是 APT 编译期注解
5. **@EncryptedField 放在 apt-api 的 entity 子包**：与 @AfEntity 同包，APT 处理器需要扫描它生成元数据；运行时加解密行为在 data-jdbc 实现

## Requirements

### T2.1 APT 注解整改 + 新增

**现有注解整改：**
- 所有 8 个注解添加完整 Javadoc（每个属性说明 + 使用示例）
- @AfEntity 添加 `boolean autoFillTimestamps() default true` 属性（控制 createdAt/updatedAt 自动填充）
- @AfService 添加 `String icon() default ""` 和 `String[] examples() default {}` 属性（UI 展示支持）

**新增注解：**
- `@AfgEnum`（TYPE）— 枚举管理标记，属性：valueField、labelField、i18nPrefix
- `@EncryptedField`（FIELD）— 字段加密标记，属性：algorithm（默认 AES）、keyRef（默认 ""）

**新增 Processor：**
- `EnumMetadataProcessor` — 处理 @AfgEnum，生成 XxxEnumMetadata + enum-metadata.index

**编译期校验规则实现（5 条）：**
1. @AfEntity 类缺少 @Table → ERROR（"实体类 {0} 缺少 @Table 注解"）
2. @AfEntity 类缺少主键字段 → WARNING（"实体类 {0} 未定义主键字段"）
3. @AfEntity 类非 public → ERROR（"实体类 {0} 必须是 public"）
4. @AfgEnum 标注的类必须是 enum → ERROR（"@AfgEnum 只能标注枚举类"）
5. @EncryptedField 标注的字段必须是 String → ERROR（"@EncryptedField 只能标注 String 字段"）

### T2.2 APT 模块测试补充

- AfgModuleAnnotationProcessor 测试（index 文件生成、basePackage 提取、contextPath 默认值）
- 编译期校验规则测试（5 条规则各至少 1 个测试）
- @AfgEnum + EnumMetadataProcessor 测试
- @EncryptedField 校验测试
- apt-api 注解的单元测试（验证属性默认值、@Target/@Retention 正确性）

## Acceptance Criteria

- [ ] 所有注解有完整 Javadoc
- [ ] @AfgEnum 和 @EncryptedField 注解定义完成
- [ ] EnumMetadataProcessor 实现并生成元数据类 + index
- [ ] 5 条编译期校验规则全部实现
- [ ] AfgModuleAnnotationProcessor 测试覆盖
- [ ] 编译期校验规则测试覆盖
- [ ] 新增注解/Processor 测试覆盖
- [ ] `./gradlew :apt-api:build :apt-impl:build` 通过
- [ ] PMD 无违规

## Definition of Done

- 所有测试全绿
- PMD 无告警
- Javadoc 完整
- 6 条质量底线达标（每个功能有 NoOp 降级、开/关条件、测试覆盖、AutoConfiguration 排序、合理默认值、SPI 默认实现）

## Out of Scope

- @EncryptedField 的运行时加解密实现（属于 data-core/data-jdbc 阶段）
- EnumManagementAutoConfiguration（属于 core 模块阶段）
- @AfgEnum 的运行时管理服务（属于 core 模块阶段）
- ImportExportAutoConfiguration + @ExcelSheet（属于 core 模块阶段）

## Technical Notes

- 编译测试使用 Google compile-testing 库（已在 apt-impl 依赖中）
- JavaPoet 用于源码生成（已在 apt-impl 依赖中）
- AutoService 用于自动注册 Processor（已在 apt-impl 依赖中）
- @AfgModuleAnnotation 是唯一 RUNTIME 级别的注解（因为 @Configuration + @ComponentScan 需要 Spring 运行时识别）
- EntityFeatureDetector 已在上轮修复 TIMESTAMPED vs AUDITABLE trait 区分
