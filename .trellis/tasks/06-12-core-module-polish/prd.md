# core 模块精细化打磨 — 6 质量基线全面落地

## Goal

对 afg-framework core 模块进行精细化打磨，确保 31 个 AutoConfiguration 全部满足 6 项质量基线：
1. NoOp fallback — 每个 SPI 接口有本地降级实现
2. on/off conditions — 每个 AutoConfiguration 有 @ConditionalOnProperty 开关
3. AutoConfiguration 依赖排序 — @AutoConfigureAfter/Before 声明完整
4. @AutoConfiguration 注解规范 — 不使用 @Configuration
5. @ConditionalOnMissingBean — 可替换 Bean 使用此注解
6. IllegalArgumentException→BusinessException — 业务错误不用 JDK 异常

## What I already know

* 31 个 AutoConfiguration，2 个使用 @Configuration（需改为 @AutoConfiguration）
* 29 个 AutoConfiguration 缺少 @AutoConfigureAfter/Before
* SPI 缺失 NoOp 的接口：DistributedLock, CacheStorageProvider, FileStorage, ServiceRegistry, ServiceDiscovery, DistributedTaskScheduler, EventPublisher
* 测试覆盖：24 个单元测试，0 个 AutoConfiguration 测试，大量功能模块无测试
* 约 15 处 IllegalArgumentException 需替换为 BusinessException
* 存在两个 AfgCoreProperties（config 包 vs properties 包），AutoConfiguration 引用 config 包版本

## Requirements

### T1: @AutoConfiguration 注解规范（2 个类）

- `CloudNativeAutoConfiguration` — @Configuration → @AutoConfiguration
- `KubernetesProbeAutoConfiguration` — @Configuration → @AutoConfiguration

### T2: @AutoConfigureAfter/Before 依赖排序（29 个类）

基于 AutoConfiguration 的 Bean 依赖关系，补充缺失的排序声明：

| AutoConfiguration | after |
|---|---|
| AfgAutoConfiguration | （根配置，无前置依赖） |
| AfgCoreAutoConfiguration | AfgAutoConfiguration |
| AfgSecurityAutoConfiguration | AfgAutoConfiguration, AfgCoreAutoConfiguration |
| AuditLogAutoConfiguration | AfgAutoConfiguration |
| BeanInvocationAutoConfiguration | AfgAutoConfiguration |
| CacheAutoConfiguration | AfgAutoConfiguration |
| CloudNativeAutoConfiguration | AfgAutoConfiguration |
| ContextAutoConfiguration | AfgAutoConfiguration |
| DataScopeAutoConfiguration | AfgAutoConfiguration, AfgSecurityAutoConfiguration |
| EncryptionAutoConfiguration | AfgAutoConfiguration |
| EventAutoConfiguration | AfgAutoConfiguration |
| FeatureFlagAutoConfiguration | AfgAutoConfiguration |
| FeatureFlagWebAutoConfiguration | AfgAutoConfiguration, FeatureFlagAutoConfiguration |
| HealthAutoConfiguration | AfgAutoConfiguration |
| HttpClientAutoConfiguration | AfgAutoConfiguration |
| KubernetesProbeAutoConfiguration | CloudNativeAutoConfiguration |
| LocaleAutoConfiguration | AfgAutoConfiguration |
| LockAutoConfiguration | AfgAutoConfiguration（@ConditionalOnBean(DistributedLock)，DistributedLock 由 afg-redis 提供） |
| LoggingAutoConfiguration | AfgAutoConfiguration |
| MetricsAutoConfiguration | AfgAutoConfiguration |
| ModuleAutoConfiguration | AfgAutoConfiguration |
| ModuleWebAutoConfiguration | AfgAutoConfiguration, ModuleAutoConfiguration |
| MultiDataSourceAutoConfiguration | （已有 @AutoConfigureBefore，补充 after） |
| AfgOpenApiAutoConfiguration | AfgAutoConfiguration |
| RateLimitAutoConfiguration | AfgAutoConfiguration |
| RemoteConfigAutoConfiguration | AfgAutoConfiguration |
| SchedulerAutoConfiguration | AfgAutoConfiguration |
| ShutdownAutoConfiguration | AfgAutoConfiguration |
| SignatureAutoConfiguration | AfgAutoConfiguration, AfgSecurityAutoConfiguration |
| VirtualThreadAutoConfiguration | AfgAutoConfiguration |
| WebAutoConfiguration | AfgAutoConfiguration |

注意：跨模块依赖使用 afterName 字符串引用。

### T3: SPI NoOp fallback 补充

| SPI 接口 | 当前状态 | 需要的 NoOp |
|---|---|---|
| DistributedLock | 无默认实现 | NoOpDistributedLock（直接返回 true/解锁成功） |
| CacheStorageProvider | 无默认实现 | NoOpCacheStorageProvider（空操作） |
| EventPublisher | 有 LocalEventPublisher | 不需要 NoOp，本地发布已是降级 |
| DistributedTaskScheduler | 无默认实现 | 不需要 NoOp（仅在 afg-redis 环境下激活） |
| FileStorage | 无默认实现 | NoOpFileStorage（空操作/抛异常提示配置存储） |
| ServiceRegistry | 无默认实现 | NoOpServiceRegistry（空操作） |
| ServiceDiscovery | 无默认实现 | NoOpServiceDiscovery（返回空列表） |
| RedisHealthChecker | 已有 NoOpRedisHealthChecker | ✅ 已完成 |
| RemoteConfigClient | 已有 NoOpRemoteConfigClient | ✅ 已完成 |

每个 NoOp 实现需在对应 AutoConfiguration 中注册为 @ConditionalOnMissingBean。

### T4: IllegalArgumentException→BusinessException 替换

**必须替换（业务逻辑错误）：**

| 文件 | 替换理由 | 目标 ErrorCode |
|---|---|---|
| datasource/lb/RoundRobinStrategy | 空候选列表 | PARAM_ERROR |
| datasource/lb/WeightedStrategy | 空候选列表 | PARAM_ERROR |
| datasource/lb/LeastConnectionsStrategy | 空候选列表 | PARAM_ERROR |
| codegen/CodeGeneratorManager | 不支持的模板类型 | PARAM_ERROR |
| scheduler/CronUtils | 无效 cron 表达式 | PARAM_ERROR |
| model/version/ApiVersion | 无效版本格式 | PARAM_ERROR |
| config/AesConfigEncryptor | 解密失败/密文格式无效 | 需新增 ENCRYPTION_ERROR |
| web/security/signature/SignatureGenerator | 无效密钥 | 需新增 INVALID_SECRET_KEY |
| config/AfgConfigRegistry (行93,251) | Config 不存在 | 需新增 CONFIG_NOT_FOUND |

**保留 IllegalArgumentException（纯参数校验）：**

| 文件 | 理由 |
|---|---|
| config/ConfigRefresher | 纯 null 检查，编程错误 |
| config/AfgConfigRegistry (行45,48,51) | 纯 null/blank 检查 |
| config/ConfigEntry | 纯 null/blank 检查 |

### T5: @ConditionalOnProperty 缺失检查

31 个 AutoConfiguration 已全部有 on/off 条件（或合理无条件加载如 AfgAutoConfiguration），无需额外添加。

### T6: CloudNativeAutoConfiguration 内部逻辑重构

当前 CloudNativeAutoConfiguration 使用编程式判断（properties→return null/实例），应改为标准的 @ConditionalOn* 注解方式。

## Acceptance Criteria

- [ ] 所有 31 个 AutoConfiguration 使用 @AutoConfiguration 注解
- [ ] 所有 AutoConfiguration 声明 @AutoConfigureAfter
- [ ] DistributedLock/CacheStorageProvider/FileStorage/ServiceRegistry/ServiceDiscovery 有 NoOp 降级实现
- [ ] NoOp 实现在 AutoConfiguration 中通过 @ConditionalOnMissingBean 注册
- [ ] 业务逻辑 IllegalArgumentException 替换为 BusinessException
- [ ] CommonErrorCode 新增 ENCRYPTION_ERROR / INVALID_SECRET_KEY / CONFIG_NOT_FOUND 错误码
- [ ] 全量构建通过（./gradlew build）

## Definition of Done

- 代码修改完成
- ./gradlew build 通过
- commit 提交

## Out of Scope

- AutoConfiguration 测试覆盖（后续阶段）
- Properties 重复清理（两个 AfgCoreProperties 问题）
- 功能模块本身的逻辑重构

## Technical Notes

- core 模块路径: core/src/main/java/io/github/afgprojects/framework/core/
- AutoConfiguration 注册文件: core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
- 跨模块依赖用 afterName 字符串引用
- BusinessException 在 commons 模块，ErrorCode 在 commons 模块
